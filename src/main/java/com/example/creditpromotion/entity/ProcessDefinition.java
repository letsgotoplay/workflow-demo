package com.example.creditpromotion.entity;

import lombok.Data;
import org.hibernate.annotations.Where;

import com.example.creditpromotion.enums.ProcessPriority;
import com.example.creditpromotion.enums.ProcessStatus;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 流程定义表实体类
 */
@Data
@Entity
@Table(name = "process_definition")
@Where(clause = "is_deleted = 0")
public class ProcessDefinition {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_code", nullable = false, length = 50, unique = true)
    private String processCode;
    
    @Column(name = "process_name", nullable = false, length = 100)
    private String processName;
    
    @Column(name = "process_type_code", nullable = false, length = 50)
    private String processTypeCode;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Lob
    @Column(name = "form_config")
    private String formConfig;
    
    @Column(name = "timeout_days")
    private Integer timeoutDays;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private ProcessPriority priority = ProcessPriority.NORMAL;
    
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