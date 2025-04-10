package com.example.creditpromotion.util;

import org.springframework.util.StringUtils;

import com.example.creditpromotion.exception.BusinessException;
import com.example.creditpromotion.exception.ErrorCode;

/**
 * 验证工具类
 */
public class ValidationUtil {
    
    /**
     * 非空验证
     */
    public static void notNull(Object obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 非空字符串验证
     */
    public static void notEmpty(String str, ErrorCode errorCode) {
        if (!StringUtils.hasText(str)) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 条件验证
     */
    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 长度验证
     */
    public static void maxLength(String str, int maxLength, ErrorCode errorCode) {
        if (str != null && str.length() > maxLength) {
            throw new BusinessException(errorCode);
        }
    }
}