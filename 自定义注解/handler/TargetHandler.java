package com.blackfish.cashloan.business.handler;

import org.aspectj.lang.ProceedingJoinPoint;

public class TargetHandler extends AbstractHandler<ProceedingJoinPoint> {
    public TargetHandler(int order) {
        super(order);
    }

    @Override
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        return this.handleNext(result, joinPoint);
    }

}
