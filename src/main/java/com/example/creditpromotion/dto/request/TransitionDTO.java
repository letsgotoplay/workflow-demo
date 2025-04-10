package com.example.creditpromotion.dto.request;

import lombok.Data;

import com.example.creditpromotion.enums.TransitionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 节点转换关系DTO
 */
@Data
public class TransitionDTO {

    private Long id;

    @NotBlank(message = "源节点标识不能为空")
    @Size(max = 100, message = "源节点标识长度不能超过100")
    private String sourceNodeKey;

    @NotBlank(message = "目标节点标识不能为空")
    @Size(max = 100, message = "目标节点标识长度不能超过100")
    private String targetNodeKey;

    @NotNull(message = "转换类型不能为空")
    private TransitionType transitionType;

    @Size(max = 500, message = "条件表达式长度不能超过500")
    private String conditionExpression;

    private Integer priority = 1;

    @Size(max = 255, message = "描述长度不能超过255")
    private String description;
}