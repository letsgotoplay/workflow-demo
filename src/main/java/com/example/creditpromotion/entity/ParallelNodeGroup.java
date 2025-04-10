package com.example.creditpromotion.entity;

import lombok.Data;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 并行节点组关系表实体类
 */
@Data
@Entity
@Table(name = "parallel_node_group")
@Where(clause = "is_deleted = 0")
public class ParallelNodeGroup {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_definition_id", nullable = false)
    private Long processDefinitionId;
    
    @Column(name = "group_key", nullable = false, length = 100)
    private String groupKey;
    
    @Column(name = "parent_node_id", nullable = false)
    private Long parentNodeId;
    
    @Column(name = "child_node_id", nullable = false)
    private Long childNodeId;
    
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