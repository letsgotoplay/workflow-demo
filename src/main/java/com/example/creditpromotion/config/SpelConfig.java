package com.example.creditpromotion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * SpEL表达式配置类
 */
@Configuration
public class SpelConfig {
    
    @Bean
    public ExpressionParser expressionParser() {
        return new SpelExpressionParser();
    }
}