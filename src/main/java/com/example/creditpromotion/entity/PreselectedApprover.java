package com.example.creditpromotion.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 申请人预选审批人表实体类
 */
@Data
@Entity
@Table(name = "preselected_approver")
public class PreselectedApprover {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_instance_id", nullable = false)
    private Long processInstanceId;
    
    @Column(name = "node_definition_id", nullable = false)
    private Long nodeDefinitionId;
    
    @Column(name = "approver_id", nullable = false)
    private Long approverId;
    
    @Column(name = "approver_name", nullable = false, length = 100)
    private String approverName;
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
}