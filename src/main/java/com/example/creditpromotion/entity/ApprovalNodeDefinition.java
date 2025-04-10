package com.example.creditpromotion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import org.hibernate.annotations.Where;

import com.example.creditpromotion.enums.ApprovalStrategy;
import com.example.creditpromotion.enums.ApproverType;
import com.example.creditpromotion.enums.NodeType;

import java.time.LocalDateTime;

/**
 * 审批节点定义表实体类
 */
@Data
@Entity
@Table(name = "approval_node_definition")
@Where(clause = "is_deleted = 0")
public class ApprovalNodeDefinition {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_definition_id", nullable = false)
    private Long processDefinitionId;
    
    @Column(name = "node_key", nullable = false, length = 100)
    private String nodeKey;
    
    @Column(name = "node_name", nullable = false, length = 100)
    private String nodeName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 50)
    private NodeType nodeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type", nullable = false, length = 50)
    private ApproverType approverType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_strategy", nullable = false, length = 50)
    private ApprovalStrategy approvalStrategy;
    
    @Column(name = "timeout_hours")
    private Integer timeoutHours;
    
    @Column(name = "is_start_node", nullable = false)
    private Integer isStartNode = 0;
    
    @Column(name = "is_end_node", nullable = false)
    private Integer isEndNode = 0;
    
    @Column(name = "allow_approver_selection", nullable = false)
    private Integer allowApproverSelection = 0;
    
    @Lob
    @Column(name = "form_permissions")
    private String formPermissions;
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
    
    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted = 0;
    
    // 便捷方法
    public boolean isStartNode() {
        return Integer.valueOf(1).equals(this.isStartNode);
    }
    
    public boolean isEndNode() {
        return Integer.valueOf(1).equals(this.isEndNode);
    }
    
    public boolean allowApproverSelection() {
        return Integer.valueOf(1).equals(this.allowApproverSelection);
    }
}