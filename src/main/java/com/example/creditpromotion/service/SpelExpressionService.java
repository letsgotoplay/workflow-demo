package com.example.creditpromotion.service;

import java.util.Map;

/**
 * SpEL表达式服务接口
 */
public interface SpelExpressionService {

    /**
     * 求值布尔表达式
     * 
     * @param expression SpEL表达式
     * @param variables 变量映射
     * @return 布尔结果
     */
    boolean evaluateBoolean(String expression, Map<String, Object> variables);

    /**
     * 求值字符串表达式
     * 
     * @param expression SpEL表达式
     * @param variables 变量映射
     * @return 字符串结果
     */
    String evaluateString(String expression, Map<String, Object> variables);

    /**
     * 求值对象表达式
     * 
     * @param expression SpEL表达式
     * @param variables 变量映射
     * @param resultType 结果类型
     * @return 对象结果
     */
    <T> T evaluateObject(String expression, Map<String, Object> variables, Class<T> resultType);
}