package com.example.creditpromotion.dto.request;

import lombok.Data;

import com.example.creditpromotion.enums.ReworkType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 重做配置DTO
 */
@Data
public class ReworkConfigDTO {

    private Long id;

    @NotBlank(message = "节点标识不能为空")
    @Size(max = 100, message = "节点标识长度不能超过100")
    private String nodeKey;

    @NotNull(message = "重做类型不能为空")
    private ReworkType reworkType;

    @Size(max = 100, message = "目标节点标识长度不能超过100")
    private String targetNodeKey;

    private Boolean allowCommentRequired = true;

    @Size(max = 255, message = "描述长度不能超过255")
    private String description;
}