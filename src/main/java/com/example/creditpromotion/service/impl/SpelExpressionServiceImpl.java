package com.example.creditpromotion.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import com.example.creditpromotion.service.SpelExpressionService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SpEL表达式服务实现类
 */
@Service
@RequiredArgsConstructor
public class SpelExpressionServiceImpl implements SpelExpressionService {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public boolean evaluateBoolean(String expression, Map<String, Object> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return true; // 空表达式视为真
        }
        
        EvaluationContext context = createEvaluationContext(variables);
        Expression exp = expressionParser.parseExpression(expression);
        return Boolean.TRUE.equals(exp.getValue(context, Boolean.class));
    }

    @Override
    public String evaluateString(String expression, Map<String, Object> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return ""; // 空表达式返回空字符串
        }
        
        EvaluationContext context = createEvaluationContext(variables);
        Expression exp = expressionParser.parseExpression(expression);
        return exp.getValue(context, String.class);
    }
    
    @Override
    public boolean evaluateGroupExpression(String expression, List<String> userGroups, Map<String, Object> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return true; // 空表达式视为真
        }
        
        Map<String, Object> contextVariables = variables != null ? variables : Map.of();
        // Add user groups to the context
        contextVariables.put("userGroups", userGroups);
        
        EvaluationContext context = createEvaluationContext(contextVariables);
        // Register helper functions for group evaluation
        registerGroupFunctions(context, userGroups);
        
        Expression exp = expressionParser.parseExpression(expression);
        return Boolean.TRUE.equals(exp.getValue(context, Boolean.class));
    }
    
    @Override
    public boolean evaluateRoleExpression(String expression, Set<String> userRoles, Map<String, Object> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return true; // 空表达式视为真
        }
        
        Map<String, Object> contextVariables = variables != null ? variables : Map.of();
        // Add user roles to the context
        contextVariables.put("userRoles", userRoles);
        
        EvaluationContext context = createEvaluationContext(contextVariables);
        // Register helper functions for role evaluation
        registerRoleFunctions(context, userRoles);
        
        Expression exp = expressionParser.parseExpression(expression);
        return Boolean.TRUE.equals(exp.getValue(context, Boolean.class));
    }

    @Override
    public <T> T evaluateObject(String expression, Map<String, Object> variables, Class<T> resultType) {
        if (expression == null || expression.trim().isEmpty()) {
            return null; // 空表达式返回null
        }
        
        EvaluationContext context = createEvaluationContext(variables);
        Expression exp = expressionParser.parseExpression(expression);
        return exp.getValue(context, resultType);
    }

    /**
     * 创建评估上下文并设置变量
     */
    private EvaluationContext createEvaluationContext(Map<String, Object> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }
        
        return context;
    }
    
    /**
     * 注册用于组表达式评估的辅助函数
     */
    private void registerGroupFunctions(EvaluationContext context, List<String> userGroups) {
        // Register hasGroup function
        context.setVariable("hasGroup", (HasGroupFunction) group -> 
            userGroups != null && userGroups.contains(group));
        
        // Register hasAnyGroup function
        context.setVariable("hasAnyGroup", (HasAnyGroupFunction) groups -> {
            if (userGroups == null || groups == null) {
                return false;
            }
            for (String group : groups) {
                if (userGroups.contains(group)) {
                    return true;
                }
            }
            return false;
        });
        
        // Register hasAllGroups function
        context.setVariable("hasAllGroups", (HasAllGroupsFunction) groups -> {
            if (userGroups == null || groups == null) {
                return false;
            }
            for (String group : groups) {
                if (!userGroups.contains(group)) {
                    return false;
                }
            }
            return true;
        });
    }
    
    /**
     * 注册用于角色表达式评估的辅助函数
     */
    private void registerRoleFunctions(EvaluationContext context, Set<String> userRoles) {
        // Register hasRole function
        context.setVariable("hasRole", (HasRoleFunction) role -> 
            userRoles != null && userRoles.contains(role));
        
        // Register hasAnyRole function
        context.setVariable("hasAnyRole", (HasAnyRoleFunction) roles -> {
            if (userRoles == null || roles == null) {
                return false;
            }
            for (String role : roles) {
                if (userRoles.contains(role)) {
                    return true;
                }
            }
            return false;
        });
        
        // Register hasAllRoles function
        context.setVariable("hasAllRoles", (HasAllRolesFunction) roles -> {
            if (userRoles == null || roles == null) {
                return false;
            }
            for (String role : roles) {
                if (!userRoles.contains(role)) {
                    return false;
                }
            }
            return true;
        });
    }
    
    // Functional interfaces for group evaluation
    @FunctionalInterface
    public interface HasGroupFunction {
        boolean hasGroup(String group);
    }
    
    @FunctionalInterface
    public interface HasAnyGroupFunction {
        boolean hasAnyGroup(String... groups);
    }
    
    @FunctionalInterface
    public interface HasAllGroupsFunction {
        boolean hasAllGroups(String... groups);
    }
    
    // Functional interfaces for role evaluation
    @FunctionalInterface
    public interface HasRoleFunction {
        boolean hasRole(String role);
    }
    
    @FunctionalInterface
    public interface HasAnyRoleFunction {
        boolean hasAnyRole(String... roles);
    }
    
    @FunctionalInterface
    public interface HasAllRolesFunction {
        boolean hasAllRoles(String... roles);
    }
}