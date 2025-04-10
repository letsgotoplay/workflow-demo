package com.example.creditpromotion.entity;

import lombok.Data;
import org.hibernate.annotations.Where;

import com.example.creditpromotion.enums.ReworkType;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 重做配置表实体类
 */
@Data
@Entity
@Table(name = "rework_configuration")
@Where(clause = "is_deleted = 0")
public class ReworkConfiguration {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_definition_id", nullable = false)
    private Long processDefinitionId;
    
    @Column(name = "node_id", nullable = false)
    private Long nodeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rework_type", nullable = false, length = 50)
    private ReworkType reworkType;
    
    @Column(name = "target_node_id")
    private Long targetNodeId;
    
    @Column(name = "allow_comment_required", nullable = false)
    private Integer allowCommentRequired = 1;
    
    @Column(name = "description", length = 255)
    private String description;
    
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
    public boolean isCommentRequired() {
        return Integer.valueOf(1).equals(this.allowCommentRequired);
    }
}