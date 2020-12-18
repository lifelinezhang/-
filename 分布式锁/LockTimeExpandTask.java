package com.blackfish.cashloan.business.task;

import com.blackfish.cashloan.business.util.RedisUtil;
import com.blackfish.cashloan.business.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 延长锁过期时间任务
 */
@Slf4j
public class LockTimeExpandTask implements Runnable {
    private RedisUtil redisUtil = SpringUtil.getBean(RedisUtil.class);

    private String lockKey;
    private int lockTimeoutSecond;

    public LockTimeExpandTask(String lockKey, int lockTimeoutSecond) {
        this.lockKey = lockKey;
        this.lockTimeoutSecond = lockTimeoutSecond;
    }

    @Override
    public void run() {
        int waitTime = lockTimeoutSecond * 1000 * 2 / 3;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(waitTime);
                if (redisUtil.expire(lockKey, lockTimeoutSecond)) {
                    log.info("LockTimeExpandTask expand success, lockKey={}", lockKey);
                } else {
                    log.info("LockTimeExpandTask expand failed, lockKey={}", lockKey);
                }
            } catch (InterruptedException e) {
                log.info("LockTimeExpandTask interrupted, lockKey={}", lockKey);
                return;
            } catch (Exception e) {
                log.error("LockTimeExpandTask run error", e);
            }
        }
    }
}
