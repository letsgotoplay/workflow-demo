package com.example.creditpromotion.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.example.creditpromotion.enums.ReminderStatus;
import com.example.creditpromotion.enums.ReminderType;

/**
 * 提醒记录表实体类
 */
@Data
@Entity
@Table(name = "reminder_record")
public class ReminderRecord {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "reminder_config_id", nullable = false)
    private Long reminderConfigId;
    
    @Column(name = "process_instance_id", nullable = false)
    private Long processInstanceId;
    
    @Column(name = "node_instance_id")
    private Long nodeInstanceId;
    
    @Column(name = "approver_instance_id")
    private Long approverInstanceId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 50)
    private ReminderType reminderType;
    
    @Column(name = "reminder_time", nullable = false)
    private LocalDateTime reminderTime;
    
    @Lob
    @Column(name = "reminder_content")
    private String reminderContent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReminderStatus status;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
}