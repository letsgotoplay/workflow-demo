package com.example.creditpromotion.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.example.creditpromotion.enums.OperationType;

/**
 * 流程操作日志表实体类
 */
@Data
@Entity
@Table(name = "process_operation_log")
public class ProcessOperationLog {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "process_instance_id", nullable = false)
    private Long processInstanceId;
    
    @Column(name = "node_instance_id")
    private Long nodeInstanceId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 50)
    private OperationType operationType;
    
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;
    
    @Column(name = "operator_name", nullable = false, length = 100)
    private String operatorName;
    
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;
    
    @Lob
    @Column(name = "operation_details")
    private String operationDetails;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "device_info", length = 255)
    private String deviceInfo;
    
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
}