package com.example.creditpromotion.entity;

import lombok.Data;
import org.hibernate.annotations.Where;

import com.example.creditpromotion.enums.ApproverType;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 节点审批人定义表实体类
 */
@Data
@Entity
@Table(name = "node_approver_definition")
@Where(clause = "is_deleted = 0")
public class NodeApproverDefinition {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "node_id", nullable = false)
    private Long nodeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type", nullable = false, length = 50)
    private ApproverType approverType;
    
    @Column(name = "approver_id", length = 100)
    private String approverId;
    
    @Column(name = "expression", length = 500)
    private String expression;
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "is_required", nullable = false)
    private Integer isRequired = 1;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 1;
    
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
    public boolean isRequired() {
        return Integer.valueOf(1).equals(this.isRequired);
    }
}