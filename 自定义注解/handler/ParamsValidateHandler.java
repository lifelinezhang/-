package com.blackfish.cashloan.business.handler;

import com.blackfish.cashloan.business.config.BizConfig;
import com.blackfish.cashloan.business.dto.base.BaseReqDto;
import com.blackfish.cashloan.business.util.SpringUtil;
import com.blackfish.cashloan.business.util.ValidateUtil;
import org.aspectj.lang.ProceedingJoinPoint;

public class ParamsValidateHandler extends AbstractHandler<ProceedingJoinPoint> {
    private BizConfig bizConfig = SpringUtil.getBean(BizConfig.class);

    public ParamsValidateHandler(int order) {
        super(order);
    }

    @Override
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        BaseReqDto reqDto = null;
        for (Object arg : args) {
            ValidateUtil.checkParams(arg);
            if (arg instanceof BaseReqDto) {
                reqDto = (BaseReqDto) arg;
            }
        }

        return this.handleNext(null, joinPoint);
    }

}
