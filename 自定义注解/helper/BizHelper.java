package com.blackfish.cashloan.business.helper;

import com.blackfish.cashloan.base.util.ArrayUtil;
import com.blackfish.cashloan.base.util.DateUtil;
import com.blackfish.cashloan.base.util.ParseUtil;
import com.blackfish.cashloan.business.enumeration.ApiCodeEnum;
import com.blackfish.cashloan.business.enumeration.PayoutStatusEnum;
import com.blackfish.cashloan.business.exception.BizException;
import com.blackfish.cashloan.business.handler.AbstractHandler;
import com.google.common.collect.Maps;
import org.apache.http.entity.ContentType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.BeanUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.blackfish.core.utils.Reflections.getAccessibleField;

public class BizHelper {

    public static class Time {
        private Date createTime;
        private Date updateTime;

        public Time() {
        }

        public Time(Date createTime, Date updateTime) {
            this.createTime = createTime;
            this.updateTime = updateTime;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public Date getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(Date updateTime) {
            this.updateTime = updateTime;
        }
    }

    public static AbstractHandler createHandlerChain(AbstractHandler... handlers) {
        Arrays.sort(handlers);
        for (int i = 0; i < handlers.length - 1; i++) {
            AbstractHandler current = handlers[i];
            AbstractHandler next = handlers[i + 1];
            current.setNext(next);
        }
        return handlers[0];
    }

    public static <T> T getParamByClass(ProceedingJoinPoint joinPoint, Class<T> clazz) {
        T t = null;
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (clazz.isInstance(arg)) {
                t = clazz.cast(arg);
                break;
            }
        }
        return t;
    }

    public static String getRedisKey(String redisCacheName, String key) {
        return getRedisKey(redisCacheName, "", key);
    }

    public static String getRedisKey(String redisCacheName, String appCode, String key) {
        return redisCacheName + ":" + appCode + ":" + key;
    }

    public static String getVerifyCode() {
        return String.valueOf(new Random().nextInt(899999) + 100000);
    }

    public static String getTargetMethodName(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
    }

    public static void setCreateTimeAndUpdateTimeAsNow(Object object) {
        Date now = new Date();
        Time time = new Time(now, now);
        BeanUtils.copyProperties(time, object);
    }

    public static void setUpdateTimeAsNow(Object object) {
        Date now = new Date();
        Time time = new Time(now, now);
        BeanUtils.copyProperties(time, object, "createTime");
    }

    public static <T1, T2> List<T2> getFieldValueList(List<T1> objectList, Class<T2> fieldClazz, String fieldName) {
        List<T2> fieldValueList = new ArrayList<T2>();
        if (ArrayUtil.isEmpty(objectList)) {
            return fieldValueList;
        }
        for (T1 object : objectList) {
            try {
                Field field = object.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                T2 t = fieldClazz.cast(field.get(object));
                fieldValueList.add(t);
            } catch (Exception e) {
                fieldValueList = null;
                break;
            }
        }
        return fieldValueList;
    }

    public static <T1, T2> Map<T2, T1> getMapFromList(List<T1> objectList, Class<T2> fieldClazz, String fieldName) {
        Map<T2, T1> map = Maps.newLinkedHashMap();
        for (T1 object : objectList) {
            try {
                Field field = object.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                T2 t = fieldClazz.cast(field.get(object));
                map.put(t, object);
            } catch (Exception e) {
                throw new BizException(ApiCodeEnum.FAIL);
            }
        }
        return map;
    }

    public static String getHumpFromLine(String str) {
        Pattern linePattern = Pattern.compile("_(\\w)");
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String getUrl(String... path) {
        StringBuilder sb = new StringBuilder();
        for (String s : path) {
            int start = 0;
            int end = s.length();
            if (s.startsWith("/")) {
                start = 1;
            }
            if (s.endsWith("/")) {
                end = s.length() - 1;
            }
            s = s.substring(start, end);
            sb.append(s).append("/");
        }
        return sb.substring(0, sb.length() - 1);
    }

    public static File multipartFileToFile(MultipartFile multipartFile, String fileDir) {
        File file = null;
        try {
            String originalFilename = multipartFile.getOriginalFilename();
            file = new File(fileDir, originalFilename);
            file = new File(file.getAbsolutePath());
            file.createNewFile();
            multipartFile.transferTo(file);
        } catch (Exception e) {
            throw new BizException(ApiCodeEnum.FAIL);
        }
        return file;
    }

    public static MultipartFile fileToMultipartFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            return new MockMultipartFile(file.getName(), file.getName(), ContentType.APPLICATION_OCTET_STREAM.toString(), fileInputStream);
        } catch (Exception e) {
            throw new BizException(ApiCodeEnum.FAIL);
        }
    }

    public static void convertNullString(Object object) {
        try {
            Field[] fieldArr = object.getClass().getDeclaredFields();
            for (Field field : fieldArr) {
                field.setAccessible(true);
                if (String.class == field.getType() && field.get(object) == null) {
                    field.set(object, "");
                }
            }
        } catch (Exception e) {
            throw new BizException(ApiCodeEnum.FAIL);
        }
    }

    public static void convertNullStringList(List<?> objectList) {
        for (Object object : objectList) {
            convertNullString(object);
        }
    }

    public static void setFieldValueByString(Object obj, String fieldName, String value, String tips) {
        Field field = getAccessibleField(obj, fieldName);
        if (field == null) {
            throw new BizException(ApiCodeEnum.FAIL);
        }
        Object realValue = null;
        if (value != null) {
            switch (field.getType().getName()) {
                case "java.lang.Integer":
                    realValue = ParseUtil.stringToInteger(value);
                    break;
                case "java.lang.Double":
                    realValue = ParseUtil.stringToDouble(value);
                    break;
                case "java.lang.Float":
                    realValue = ParseUtil.stringToFloat(value);
                    break;
                case "java.util.Date":
                    realValue = DateUtil.parseDate(value, "dd/MM/yyyy");
                    break;
                default:
                    realValue = value;
                    break;
            }
            if (realValue == null) {
                throw new BizException(ApiCodeEnum.PARAM_FORMAT_ERROR, tips, fieldName);
            }
        }
        try {
            field.set(obj, realValue);
        } catch (IllegalAccessException e) {
            throw new BizException(ApiCodeEnum.FAIL);
        }
    }

    public static Map<String, String> object2Map(Object obj) {
        Map<String, String> map = Maps.newHashMap();
        Field[] fs = obj.getClass().getDeclaredFields();
        for (Field f : fs) {
            f.setAccessible(true);
            try {
                Object val = f.get(obj);
                if (val != null) {
                    map.put(f.getName(), val.toString());
                }
            } catch (Exception e) {
                throw new BizException(ApiCodeEnum.FAIL);
            }
        }
        return map;
    }

    public static PayoutStatusEnum getPayoutStatusEnum(Integer payStatus) {
        switch (payStatus) {
            case 1:
                return PayoutStatusEnum.SUCCESS;
            case 2:
                return PayoutStatusEnum.WAITING_PAYOUT;
            case 3:
                return PayoutStatusEnum.FAILED;
            default:
                return PayoutStatusEnum.WAITING_PAYOUT;
        }
    }

    public static Date getEndOfDay(Date date) {
        return DateUtil.parseDate(DateUtil.formatDate(date, DateUtil.DEFAULT_DATE_FORMAT) + " 23:59:59", DateUtil.DATE_TIME_FORMAT);
    }

    /**
     * 获取真实ip地址，避免获取代理ip
     *
     * @param request
     * @return
     */
    public static String getRequestIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
