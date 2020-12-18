package com.blackfish.cashloan.business.aspect;

import com.blackfish.cashloan.base.util.GenerateUtil;
import com.blackfish.cashloan.business.dto.base.BaseRespDto;
import com.blackfish.cashloan.business.enumeration.ApiCodeEnum;
import com.blackfish.cashloan.business.exception.BizException;
import com.blackfish.cashloan.business.factory.WebHandlerFactory;
import com.blackfish.cashloan.business.handler.AbstractHandler;
import com.blackfish.cashloan.business.helper.BizHelper;
import com.blackfish.cashloan.business.pub.LogPub;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by zx on 2019/9/3.
 */

@Component
@Aspect
@Slf4j
public class WebAspect {

    @Autowired
    private LogPub logPub;

    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)" +
            "||@annotation(org.springframework.web.bind.annotation.PostMapping)" +
            "||@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void pointService() {

    }

    @Around("pointService()")
    public Object handle(ProceedingJoinPoint joinPoint) {
        Object result = null;
        long beginMills = System.currentTimeMillis();

        String requestId = GenerateUtil.genUUID();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        logPub.publishRequestLog(joinPoint, requestId, request.getRequestURL().toString());

        try {
            AbstractHandler handler = WebHandlerFactory.getInstance().getObject();
            result = handler.handle(joinPoint);
        } catch (BizException e) {
            log.debug("method={},biz exception={}", BizHelper.getTargetMethodName(joinPoint), e.toString());
            result = new BaseRespDto(e.getCode(), e.getMessage(), e.getSubcode());
        } catch (Throwable t) {
            logPub.publishErrorLog(BizHelper.getTargetMethodName(joinPoint), "Request unexpected error", t, requestId);
            result = new BaseRespDto(ApiCodeEnum.BUSY);
        }
        long endMills = System.currentTimeMillis();
        logPub.publishResponseLog(joinPoint, result, (endMills - beginMills), requestId);
        return result;
    }
}
