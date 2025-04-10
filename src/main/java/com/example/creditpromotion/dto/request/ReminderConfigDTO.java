package com.example.creditpromotion.dto.request;

import lombok.Data;

import com.example.creditpromotion.enums.ReminderType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 提醒配置DTO
 */
@Data
public class ReminderConfigDTO {

    private Long id;

    @Size(max = 100, message = "节点标识长度不能超过100")
    private String nodeKey;

    @NotNull(message = "提醒类型不能为空")
    private ReminderType reminderType;

    @NotBlank(message = "时间表达式不能为空")
    @Size(max = 100, message = "时间表达式长度不能超过100")
    private String timeExpression;

    @NotBlank(message = "提醒模板代码不能为空")
    @Size(max = 100, message = "提醒模板代码长度不能超过100")
    private String reminderTemplateCode;

    private Boolean enabled = true;

    @Size(max = 255, message = "描述长度不能超过255")
    private String description;
}