package com.example.creditpromotion.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.example.creditpromotion.enums.ApprovalStatus;
import com.example.creditpromotion.enums.ApproverType;

/**
 * 节点审批人实例表实体类
 */
@Data
@Entity
@Table(name = "node_approver_instance")
public class NodeApproverInstance {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "node_instance_id", nullable = false)
    private Long nodeInstanceId;
    
    @Column(name = "approver_id", nullable = false)
    private Long approverId;
    
    @Column(name = "approver_name", nullable = false, length = 100)
    private String approverName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type", nullable = false, length = 50)
    private ApproverType approverType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 50)
    private ApprovalStatus approvalStatus;
    
    @Column(name = "assign_time", nullable = false)
    private LocalDateTime assignTime;
    
    @Column(name = "action_time")
    private LocalDateTime actionTime;
    
    @Column(name = "due_time")
    private LocalDateTime dueTime;
    
    @Column(name = "comments", length = 1000)
    private String comments;
    
    @Column(name = "transferred_to_id")
    private Long transferredToId;
    
    @Column(name = "transferred_to_name", length = 100)
    private String transferredToName;
    
    @Column(name = "is_preselected", nullable = false)
    private Integer isPreselected = 0;
    
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
    
    @Column(name = "role_id")
    private String roleId;

    @Column(name = "expression")
    private String expression;
    
    // 便捷方法
    public boolean isPreselected() {
        return Integer.valueOf(1).equals(this.isPreselected);
    }
    
    

}