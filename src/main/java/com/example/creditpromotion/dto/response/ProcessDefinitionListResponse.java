package com.example.creditpromotion.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

import com.example.creditpromotion.enums.ProcessPriority;
import com.example.creditpromotion.enums.ProcessStatus;

/**
 * 流程定义列表响应DTO
 */
@Data
public class ProcessDefinitionListResponse {

    private Long id;
    private String processCode;
    private String processName;
    private String processTypeCode;
    private String description;
    private Integer timeoutDays;
    private ProcessStatus status;
    private ProcessPriority priority;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;
    private Integer nodeCount; // 节点数量
}