package com.example.creditpromotion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA配置类
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.example.creditpromotion.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
    // 可以配置自定义的JPA设置
}