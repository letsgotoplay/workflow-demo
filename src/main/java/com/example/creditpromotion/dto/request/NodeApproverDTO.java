package com.example.creditpromotion.dto.request;

import lombok.Data;

import com.example.creditpromotion.enums.ApproverType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 节点审批人DTO
 */
@Data
public class NodeApproverDTO {

    private Long id;

    @NotNull(message = "审批人类型不能为空")
    private ApproverType approverType;

    @Size(max = 100, message = "审批人ID长度不能超过100")
    private String approverId;

    @Size(max = 500, message = "表达式长度不能超过500")
    private String expression;

    @Size(max = 255, message = "描述长度不能超过255")
    private String description;

    private Boolean isRequired = true;

    private Integer priority = 1;
}