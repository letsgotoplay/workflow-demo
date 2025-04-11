package com.example.creditpromotion.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SpelExpressionService {
    
    /**
     * 评估布尔表达式
     * @param expression SpEL表达式
     * @param variables 变量映射
     * @return 布尔结果
     */
    boolean evaluateBoolean(String expression, Map<String, Object> variables);
    
    /**
     * 评估字符串表达式
     * @param expression SpEL表达式
     * @param variables 变量映射
     * @return 字符串结果
     */
    String evaluateString(String expression, Map<String, Object> variables);
    
    /**
     * 评估对象表达式
     * @param expression SpEL表达式
     * @param variables 变量映射
     * @param resultType 结果类型
     * @return 指定类型的结果
     */
    <T> T evaluateObject(String expression, Map<String, Object> variables, Class<T> resultType);
    
    /**
     * 评估组表达式
     * @param expression SpEL表达式
     * @param userGroups 用户所属组列表
     * @param variables 其他变量映射
     * @return 布尔结果
     */
    boolean evaluateGroupExpression(String expression, List<String> userGroups, Map<String, Object> variables);
    
    /**
     * 评估角色表达式
     * @param expression SpEL表达式
     * @param userRoles 用户角色集合
     * @param variables 其他变量映射
     * @return 布尔结果
     */
    boolean evaluateRoleExpression(String expression, Set<String> userRoles, Map<String, Object> variables);
}