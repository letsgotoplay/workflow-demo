package com.example.creditpromotion.enums;

/**
 * 流程实例状态枚举
 */
public enum ProcessInstanceStatus {
    DRAFT,       // 草稿
    IN_PROGRESS, // 进行中
    APPROVED,    // 已批准
    REJECTED,    // 已拒绝
    CANCELED,    // 已取消
    REWORK       // 重做
}