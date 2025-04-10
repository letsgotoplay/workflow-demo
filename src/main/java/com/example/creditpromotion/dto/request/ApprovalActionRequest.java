package com.example.creditpromotion.dto.request;

import lombok.Data;

import com.example.creditpromotion.enums.ActionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 审批操作请求DTO
 */
@Data
public class ApprovalActionRequest {

    @NotNull(message = "流程实例ID不能为空")
    private Long processInstanceId;

    @NotNull(message = "节点实例ID不能为空")
    private Long nodeInstanceId;

    @NotNull(message = "审批人实例ID不能为空")
    private Long approverInstanceId;

    @NotNull(message = "操作类型不能为空")
    private ActionType actionType;

    @Size(max = 1000, message = "意见长度不能超过1000")
    private String comment;

    // 转交相关
    private Long targetApproverId;
    
    private String targetApproverName;

    // 重做相关
    private Long targetNodeId;
}