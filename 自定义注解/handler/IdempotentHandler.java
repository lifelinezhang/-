package com.blackfish.cashloan.business.handler;

import com.blackfish.cashloan.business.annotation.Idem;
import com.blackfish.cashloan.business.constant.BizConstant;
import com.blackfish.cashloan.business.dto.base.BaseRespDto;
import com.blackfish.cashloan.business.dto.base.Idempotent;
import com.blackfish.cashloan.business.enumeration.ApiCodeEnum;
import com.blackfish.cashloan.business.helper.BizHelper;
import com.blackfish.cashloan.business.util.ClassUtil;
import com.blackfish.cashloan.business.util.RedisUtil;
import com.blackfish.cashloan.business.util.SpringUtil;
import org.aspectj.lang.ProceedingJoinPoint;

public class IdempotentHandler extends AbstractHandler<ProceedingJoinPoint> {
    private RedisUtil redisUtil = SpringUtil.getBean(RedisUtil.class);

    public IdempotentHandler(int order) {
        super(order);
    }

    @Override
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean needIdempotent = ClassUtil.containClassAnnotation(joinPoint, Idem.class) || ClassUtil.containMethodAnnotation(joinPoint, Idem.class);
        // 需要校验幂等
        if (needIdempotent) {
            Idempotent reqDto = BizHelper.getParamByClass(joinPoint, Idempotent.class);
            if (reqDto == null) {
                return new BaseRespDto(ApiCodeEnum.PARAM_ERROR);
            }
            Boolean delResult = redisUtil.delete(BizHelper.getRedisKey(BizConstant.RedisCacheName.IDEMPOTENT_CODE, reqDto.getAppCode(), reqDto.getIdempotentCode()));
            if (!delResult) {
                return new BaseRespDto(ApiCodeEnum.REPEAT_OPERATION);
            }
        }
        return this.handleNext(null, joinPoint);
    }
}
