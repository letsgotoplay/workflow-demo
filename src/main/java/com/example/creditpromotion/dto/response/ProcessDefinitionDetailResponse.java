package com.example.creditpromotion.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.example.creditpromotion.dto.request.NodeDefinitionDTO;
import com.example.creditpromotion.dto.request.ReminderConfigDTO;
import com.example.creditpromotion.dto.request.ReworkConfigDTO;
import com.example.creditpromotion.dto.request.TransitionDTO;
import com.example.creditpromotion.enums.ProcessPriority;
import com.example.creditpromotion.enums.ProcessStatus;

/**
 * 流程定义详情响应DTO
 */
@Data
public class ProcessDefinitionDetailResponse {

    private Long id;
    private String processCode;
    private String processName;
    private String processTypeCode;
    private String description;
    private String formConfig;
    private Integer timeoutDays;
    private ProcessStatus status;
    private ProcessPriority priority;
    private Integer version;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    private List<NodeDefinitionDTO> nodes;
    private List<TransitionDTO> transitions;
    private List<ReworkConfigDTO> reworkConfigs;
    private List<ReminderConfigDTO> reminderConfigs;
}