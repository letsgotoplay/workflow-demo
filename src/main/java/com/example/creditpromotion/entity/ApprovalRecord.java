package com.example.creditpromotion.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.example.creditpromotion.enums.ActionStatus;
import com.example.creditpromotion.enums.ActionType;

/**
 * 审批记录表实体类
 */
@Data
@Entity
@Table(name = "approval_record")
public class ApprovalRecord {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_instance_id", nullable = false)
    private Long processInstanceId;
    
    @Column(name = "node_instance_id", nullable = false)
    private Long nodeInstanceId;
    
    @Column(name = "approver_instance_id")
    private Long approverInstanceId;
    
    @Column(name = "approver_id")
    private Long approverId;
    
    @Column(name = "approver_name", length = 100)
    private String approverName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", nullable = false, length = 50)
    private ActionStatus actionStatus;
    
    @Column(name = "action_comment", length = 1000)
    private String actionComment;
    
    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;
    
    @Column(name = "target_approver_id")
    private Long targetApproverId;
    
    @Column(name = "target_approver_name", length = 100)
    private String targetApproverName;
    
    @Column(name = "target_node_id")
    private Long targetNodeId;
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
}