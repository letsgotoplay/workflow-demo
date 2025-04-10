package com.example.creditpromotion.entity;

import lombok.Data;
import org.hibernate.annotations.Where;

import com.example.creditpromotion.enums.TransitionType;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 节点转换关系表实体类
 */
@Data
@Entity
@Table(name = "node_transition")
@Where(clause = "is_deleted = 0")
public class NodeTransition {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_definition_id", nullable = false)
    private Long processDefinitionId;
    
    @Column(name = "source_node_id", nullable = false)
    private Long sourceNodeId;
    
    @Column(name = "target_node_id", nullable = false)
    private Long targetNodeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transition_type", nullable = false, length = 50)
    private TransitionType transitionType;
    
    @Column(name = "condition_expression", length = 500)
    private String conditionExpression;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 1;
    
    @Column(name = "description", length = 255)
    private String description;
    
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
}