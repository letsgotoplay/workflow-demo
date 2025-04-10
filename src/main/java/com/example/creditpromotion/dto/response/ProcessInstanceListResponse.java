package com.example.creditpromotion.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.creditpromotion.enums.ProcessInstanceStatus;
import com.example.creditpromotion.enums.ProcessPriority;

/**
 * 流程实例列表响应DTO
 */
@Data
public class ProcessInstanceListResponse {

    private Long id;
    private String processNo;
    private Long processDefinitionId;
    private String processName;
    private Long officerId;
    private Long employeeId;
    private String employeeName;
    private String currentNodeName;
    private ProcessInstanceStatus status;
    private Integer reworkCount;
    private Long applyUserId;
    private String applyUserName;
    private LocalDateTime applyTime;
    private LocalDateTime completeTime;
    private LocalDate effectiveDate;
    private LocalDateTime dueTime;
    private ProcessPriority priority;
    private String createdBy;
    private LocalDateTime createdTime;
}