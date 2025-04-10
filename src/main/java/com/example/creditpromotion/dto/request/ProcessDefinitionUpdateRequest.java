package com.example.creditpromotion.dto.request;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import com.example.creditpromotion.enums.ProcessPriority;
import com.example.creditpromotion.enums.ProcessStatus;

/**
 * 流程定义更新请求DTO
 */
@Data
public class ProcessDefinitionUpdateRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 100, message = "流程名称长度不能超过100")
    private String processName;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    private String formConfig;

    private Integer timeoutDays;

    @NotNull(message = "状态不能为空")
    private ProcessStatus status;

    private ProcessPriority priority;

    @Valid
    private List<NodeDefinitionDTO> nodes;

    @Valid
    private List<TransitionDTO> transitions;

    @Valid
    private List<ReworkConfigDTO> reworkConfigs;

    @Valid
    private List<ReminderConfigDTO> reminderConfigs;
}