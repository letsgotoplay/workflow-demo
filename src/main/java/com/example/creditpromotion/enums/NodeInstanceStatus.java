package com.example.creditpromotion.enums;

/**
 * 节点实例状态枚举
 */
public enum NodeInstanceStatus {
    PENDING,     // 待处理
    IN_PROGRESS, // 处理中
    APPROVED,    // 已批准
    REJECTED,    // 已拒绝
    CANCELED,    // 已取消
    REWORK       // 重做
}