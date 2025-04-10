package com.example.creditpromotion.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务响应DTO（统一待办任务）
 */
@Data
public class TaskResponse {

    private Long id;
    private String taskType; // APPROVAL-待审批, REWORK-待重做
    private Long processInstanceId;
    private String processNo;
    private String processName;
    private Long nodeInstanceId;
    private String nodeName;
    private Long approverInstanceId; // 审批任务时有效
    private Long employeeId;
    private String employeeName;
    private String applyUserName;
    private LocalDateTime applyTime;
    private LocalDateTime assignTime;
    private LocalDateTime dueTime;
    private Boolean isOverdue;
    private Integer priority;
}