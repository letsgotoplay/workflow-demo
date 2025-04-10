package com.example.creditpromotion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * 信贷员晋升流程管理系统主应用
 */
@SpringBootApplication
@EntityScan(basePackages = "com.example.creditpromotion.entity")
public class CreditPromotionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditPromotionApplication.class, args);
    }
}