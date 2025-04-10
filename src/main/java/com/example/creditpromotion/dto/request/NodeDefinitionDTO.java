package com.example.creditpromotion.dto.request;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import com.example.creditpromotion.enums.ApprovalStrategy;
import com.example.creditpromotion.enums.ApproverType;
import com.example.creditpromotion.enums.NodeType;

/**
 * 节点定义DTO
 */
@Data
public class NodeDefinitionDTO {

    private Long id;

    @NotBlank(message = "节点标识不能为空")
    @Size(max = 100, message = "节点标识长度不能超过100")
    private String nodeKey;

    @NotBlank(message = "节点名称不能为空")
    @Size(max = 100, message = "节点名称长度不能超过100")
    private String nodeName;

    @NotNull(message = "节点类型不能为空")
    private NodeType nodeType;

    @NotNull(message = "审批人类型不能为空")
    private ApproverType approverType;

    @NotNull(message = "审批策略不能为空")
    private ApprovalStrategy approvalStrategy;

    private Integer timeoutHours;

    private Boolean isStartNode = false;

    private Boolean isEndNode = false;

    private Boolean allowApproverSelection = false;

    private String formPermissions;

    @Valid
    private List<NodeApproverDTO> approvers;
}