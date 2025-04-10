package com.example.creditpromotion.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import com.example.creditpromotion.service.SpelExpressionService;

import java.util.Map;

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
}