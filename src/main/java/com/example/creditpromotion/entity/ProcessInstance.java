package com.example.creditpromotion.entity;

import lombok.Data;
import org.hibernate.annotations.Where;

import com.example.creditpromotion.enums.ProcessInstanceStatus;
import com.example.creditpromotion.enums.ProcessPriority;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 流程实例表实体类
 */
@Data
@Entity
@Table(name = "process_instance")
@Where(clause = "is_deleted = 0")
public class ProcessInstance {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_no", nullable = false, length = 50, unique = true)
    private String processNo;
    
    @Column(name = "process_definition_id", nullable = false)
    private Long processDefinitionId;
    
    @Column(name = "officer_id")
    private Long officerId;
    
    @Column(name = "employee_id")
    private Long employeeId;
    
    @Column(name = "employee_name", length = 100)
    private String employeeName;
    
    @Lob
    @Column(name = "form_data")
    private String formData;
    
    @Column(name = "current_node_id")
    private Long currentNodeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessInstanceStatus status;
    
    @Column(name = "rework_count", nullable = false)
    private Integer reworkCount = 0;
    
    @Column(name = "apply_user_id", nullable = false)
    private Long applyUserId;
    
    @Column(name = "apply_user_name", nullable = false, length = 100)
    private String applyUserName;
    
    @Column(name = "apply_time", nullable = false)
    private LocalDateTime applyTime;
    
    @Column(name = "complete_time")
    private LocalDateTime completeTime;
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    @Column(name = "due_time")
    private LocalDateTime dueTime;
    
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