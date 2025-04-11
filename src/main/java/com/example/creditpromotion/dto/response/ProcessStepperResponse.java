package com.example.creditpromotion.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程步骤展示响应DTO，用于前端Stepper组件展示
 */
@Data
public class ProcessStepperResponse {
    
    private Long processInstanceId;    // 流程实例ID
    private String processNo;          // 流程编号
    private String processName;        // 流程名称
    private String status;             // 流程状态
    private LocalDateTime submitTime;  // 提交时间
    private String submitter;          // 提交人
    
    // 步骤列表（已经处理过并行节点和条件节点）
    private List<StepInfo> steps;
    
    /**
     * 步骤信息
     */
    @Data
    public static class StepInfo {
        private Long stepId;            // 步骤ID（节点实例ID）
        private String stepName;        // 步骤名称
        private String status;          // 步骤状态：COMPLETED, IN_PROGRESS, PENDING， NOT_STARTED
        private Boolean isParallel;     // 是否为并行节点
        private String approvalStrategy; // 审批策略（ALL/ANY）一个node 多个审批者 使用
        private LocalDateTime startTime; // 开始时间
        private LocalDateTime endTime;   // 结束时间
        private int stepOrder;          // 步骤顺序（从1开始）
        // 步骤详细信息（根据状态不同显示不同信息）
        private StepDetail detail;
        private Boolean isConditional = false;
        
    }
    
    /**
     * 步骤详细信息
     */
    @Data
    public static class StepDetail {
        private List<ApproverInfo> approvers; // 审批人信息列表
        private String description;           // 步骤描述
        private LocalDateTime dueTime;        // 截止时间
        private String comments;              // 审批意见（已完成的步骤）
    }
    
    /**
     * 审批人信息
     */
    @Data
    public static class ApproverInfo {
        private Long approverId;       // 审批人ID
        private String approverName;   // 审批人姓名
        private String approverRole;   // 审批人角色/部门
        private String approvalStatus; // 审批状态
        private LocalDateTime actionTime; // 操作时间
        private String comments;       // 审批意见
    }
}