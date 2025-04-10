package com.example.creditpromotion.dto.request;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

import com.example.creditpromotion.enums.ProcessPriority;

/**
 * 流程定义创建请求DTO
 */
@Data
public class ProcessDefinitionCreateRequest {

    @NotBlank(message = "流程编码不能为空")
    @Size(max = 50, message = "流程编码长度不能超过50")
    private String processCode;

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 100, message = "流程名称长度不能超过100")
    private String processName;

    @NotBlank(message = "流程类型代码不能为空")
    @Size(max = 50, message = "流程类型代码长度不能超过50")
    private String processTypeCode;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    private String formConfig;

    private Integer timeoutDays;

    private ProcessPriority priority = ProcessPriority.NORMAL;

    @NotEmpty(message = "节点定义不能为空")
    @Valid
    private List<NodeDefinitionDTO> nodes;

    @Valid
    private List<TransitionDTO> transitions;

    @Valid
    private List<ReworkConfigDTO> reworkConfigs;

    @Valid
    private List<ReminderConfigDTO> reminderConfigs;
}