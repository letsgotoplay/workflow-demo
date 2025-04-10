package com.example.creditpromotion.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.example.creditpromotion.enums.NodeInstanceStatus;

/**
 * 流程节点实例表实体类
 */
@Data
@Entity
@Table(name = "process_node_instance")
public class ProcessNodeInstance {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_instance_id", nullable = false)
    private Long processInstanceId;
    
    @Column(name = "node_definition_id", nullable = false)
    private Long nodeDefinitionId;
    
    @Column(name = "node_name", nullable = false, length = 100)
    private String nodeName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "node_status", nullable = false, length = 50)
    private NodeInstanceStatus nodeStatus;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "due_time")
    private LocalDateTime dueTime;
    
    @Column(name = "prev_node_instance_id")
    private Long prevNodeInstanceId;
    
    @Column(name = "parent_node_instance_id")
    private Long parentNodeInstanceId;
    
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
}