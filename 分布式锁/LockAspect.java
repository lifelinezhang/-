package com.blackfish.cashloan.business.aspect;

import com.blackfish.cashloan.business.annotation.Lock;
import com.blackfish.cashloan.business.config.SysConfig;
import com.blackfish.cashloan.business.dto.base.BaseRespDto;
import com.blackfish.cashloan.business.enumeration.ApiCodeEnum;
import com.blackfish.cashloan.business.task.LockTimeExpandTask;
import com.blackfish.cashloan.business.util.ClassUtil;
import com.blackfish.cashloan.business.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 可重入分布式锁
 * Created by zx on 2020/11/6.
 */

@Component
@Aspect
@Slf4j
public class LockAspect {
    private static final String LOCK_PREFIX = "lock";

    private ExpressionParser parser = new SpelExpressionParser();
    private LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SysConfig sysConfig;

    @Pointcut("@annotation(com.blackfish.cashloan.business.annotation.Lock)")
    public void pointService() {

    }

    @Around("pointService()")
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {
        long beginTryTimeMillis = System.currentTimeMillis();
        boolean needLock = ClassUtil.containMethodAnnotation(joinPoint, Lock.class);
        String lockKey = this.getLockKey(joinPoint);
        String lockValue = this.getLockValue();
        String oldLockValue = (String) redisUtil.get(lockKey);
        // 判断锁是否被当前线程所持有，实现可重入
        boolean isSelf = lockValue.equals(oldLockValue);
        if (needLock && !isSelf) {
            // 递归重试抢锁
            return this.lock(joinPoint, lockKey, lockValue, beginTryTimeMillis);
        } else {
            return joinPoint.proceed();
        }
    }

    private Object lock(ProceedingJoinPoint joinPoint, String lockKey, String lockValue, long beginTryTimeMillis) throws Throwable {
        // 判断抢锁是否超时
        if (System.currentTimeMillis() > beginTryTimeMillis + sysConfig.getTryTimeoutSecond() * 1000) {
            return new BaseRespDto(ApiCodeEnum.LOCK_FAIL);
        }
        // 抢锁
        boolean lockSuccess = redisUtil.setNx(lockKey, lockValue, sysConfig.getLockTimeoutSecond());
        if (lockSuccess) {
            long currentTimeMillis = System.currentTimeMillis();
            long tryTime = currentTimeMillis - beginTryTimeMillis;
            log.info("lock success, lockKey={}, lockValue={}, tryTime={}ms", lockKey, lockValue, tryTime);
            Object result;
            // 开启守护线程
            LockTimeExpandTask lockTimeExpandTask = new LockTimeExpandTask(lockKey, sysConfig.getLockTimeoutSecond());
            Thread lockTimeExpandThread = new Thread(lockTimeExpandTask);
            lockTimeExpandThread.start();
            try {
                result = joinPoint.proceed();
            } catch (Throwable t) {
                throw t;
            } finally {
                // 中断守护线程
                lockTimeExpandThread.interrupt();
                // 释放锁
                this.releaseLock(lockKey, lockValue, currentTimeMillis);
            }
            return result;
        } else {
            log.debug("lock failed, lockKey={}, lockValue={}", lockKey, lockValue);
            // 抢锁失败则sleep后重试
            Thread.sleep(100);
            return this.lock(joinPoint, lockKey, lockValue, beginTryTimeMillis);
        }
    }

    private void releaseLock(String lockKey, String lockValue, long beginLockTimeMillis) {
        // 删除lockKey
        boolean releaseLockSuccess = redisUtil.delete(lockKey);
        long lockTime = System.currentTimeMillis() - beginLockTimeMillis;
        if (releaseLockSuccess) {
            log.info("release lock success, lockKey={}, lockValue={}, lockTime={}ms", lockKey, lockValue, lockTime);
        } else {
            log.error("release lock failed, lockKey={}, lockValue={}", lockKey, lockValue);
        }
    }

    private String getLockKey(ProceedingJoinPoint joinPoint) throws UnknownHostException {
        Lock lock = ClassUtil.getMethodAnnotation(joinPoint, Lock.class);
        String service = lock.service();
        String key = this.getSpel(joinPoint, lock.key());
        return LOCK_PREFIX + ":" + service + ":" + key;
    }

    private String getLockValue() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress() + "_" + Thread.currentThread().getName();
    }

    private String getSpel(ProceedingJoinPoint joinPoint, String str) {
        Object[] args = joinPoint.getArgs();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String[] params = discoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();
        for (int len = 0; len < params.length; len++) {
            context.setVariable(params[len], args[len]);
        }

        Expression expression = parser.parseExpression(str);
        return expression.getValue(context, String.class);
    }
}
