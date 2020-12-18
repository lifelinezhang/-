package com.blackfish.cashloan.business.handler;

import com.blackfish.cashloan.base.util.StringUtil;
import com.blackfish.cashloan.business.annotation.Auth;
import com.blackfish.cashloan.business.annotation.NotAuth;
import com.blackfish.cashloan.business.config.BizConfig;
import com.blackfish.cashloan.business.constant.BizConstant;
import com.blackfish.cashloan.business.dto.base.Authority;
import com.blackfish.cashloan.business.dto.base.BaseRespDto;
import com.blackfish.cashloan.business.enumeration.ApiCodeEnum;
import com.blackfish.cashloan.business.helper.BizHelper;
import com.blackfish.cashloan.business.util.ClassUtil;
import com.blackfish.cashloan.business.util.RedisUtil;
import com.blackfish.cashloan.business.util.SpringUtil;
import com.blackfish.manager.entity.user.TUser;
import com.blackfish.manager.manager.user.TUserManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.BeanUtils;

public class AuthorityHandler extends AbstractHandler<ProceedingJoinPoint> {
    private RedisUtil redisUtil = SpringUtil.getBean(RedisUtil.class);
    private BizConfig bizConfig = SpringUtil.getBean(BizConfig.class);
    private TUserManager tUserManager = SpringUtil.getBean(TUserManager.class);

    public AuthorityHandler(int order) {
        super(order);
    }

    @Override
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean needAuth = (ClassUtil.containClassAnnotation(joinPoint, Auth.class) || ClassUtil.containMethodAnnotation(joinPoint, Auth.class))
                && !ClassUtil.containMethodAnnotation(joinPoint, NotAuth.class);
        // 需要校验权限
        if (needAuth) {
            Authority reqDto = BizHelper.getParamByClass(joinPoint, Authority.class);
            if (reqDto == null) {
                return new BaseRespDto(ApiCodeEnum.NO_PERMISSION);
            }
            String token = reqDto.getToken();
            if (StringUtil.isEmpty(token)) {
                return new BaseRespDto(ApiCodeEnum.NEED_LOGIN);
            }
            // 验证请求中的token和redis中的是否一致
            String key = BizHelper.getRedisKey(BizConstant.RedisCacheName.TOKEN, reqDto.getAppCode(), token);
            Integer userId = (Integer) redisUtil.get(key);
            if (userId == null) {
                return new BaseRespDto(ApiCodeEnum.NEED_LOGIN);
            }

            // 验证token成功后将token有效期重置
            redisUtil.expire(key, bizConfig.getTokenExpire());

            // 给入参User赋值
            TUser userParam = BizHelper.getParamByClass(joinPoint, TUser.class);
            if (userParam != null) {
                TUser user = tUserManager.findById(userId);
                BeanUtils.copyProperties(user, userParam);
            }
        }
        return this.handleNext(null, joinPoint);
    }

}
