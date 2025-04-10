package com.example.creditpromotion.entity;

import lombok.Data;
import org.hibernate.annotations.Where;

import com.example.creditpromotion.enums.ReminderType;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 提醒配置表实体类
 */
@Data
@Entity
@Table(name = "reminder_configuration")
@Where(clause = "is_deleted = 0")
public class ReminderConfiguration {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_definition_id", nullable = false)
    private Long processDefinitionId;
    
    @Column(name = "node_id")
    private Long nodeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 50)
    private ReminderType reminderType;
    
    @Column(name = "time_expression", nullable = false, length = 100)
    private String timeExpression;
    
    @Column(name = "reminder_template_code", nullable = false, length = 100)
    private String reminderTemplateCode;
    
    @Column(name = "enabled", nullable = false)
    private Integer enabled = 1;
    
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
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(this.enabled);
    }
}