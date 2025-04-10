package com.example.creditpromotion.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全工具类
 */
public class SecurityUtil {
    
    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        // Mock implementation - replace with your actual logic
        return "testuser";
    }
    
    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        // Mock implementation - replace with your actual logic
        return 1L;
    }
    
    /**
     * 判断当前用户是否管理员
     */
    public static boolean isAdmin() {
        // Mock implementation - replace with your actual logic
        return false;
    }
    
    /**
     * 获取当前请求IP地址
     */
    public static String getCurrentIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        return ip;
    }
    
    /**
     * 获取当前设备信息
     */
    public static String getCurrentDeviceInfo() {
        // Mock implementation - replace with your actual logic
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
    }
}