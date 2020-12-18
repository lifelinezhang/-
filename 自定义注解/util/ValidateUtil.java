package com.blackfish.cashloan.business.util;

import com.blackfish.cashloan.base.util.StringUtil;
import com.blackfish.cashloan.business.enumeration.ApiCodeEnum;
import com.blackfish.cashloan.business.exception.BizException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

public class ValidateUtil {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public static void checkParams(Object object) {
        StringBuilder errorBuilder = new StringBuilder();
        String param = null;
        Set violations = validator.validate(object);
        for (Object violation : violations) {
            ConstraintViolation constraintViolation = (ConstraintViolation) violation;
            if (!StringUtil.isEmpty(errorBuilder.toString())) {
                errorBuilder.append(" ");
            }
            errorBuilder.append(constraintViolation.getMessage());
            param = constraintViolation.getPropertyPath().toString();
            break;
        }
        if (!StringUtil.isEmpty(errorBuilder.toString())) {
            throw new BizException(ApiCodeEnum.PARAM_ERROR, null, param);
        }

    }


}
