package com.example.creditpromotion.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.creditpromotion.enums.ProcessInstanceStatus;
import com.example.creditpromotion.enums.ProcessPriority;

/**
 * 流程实例详情响应DTO
 */
@Data
public class ProcessInstanceDetailResponse {

    private Long id;
    private String processNo;
    private Long processDefinitionId;
    private String processName;
    private Long officerId;
    private Long employeeId;
    private String employeeName;
    private Map<String, Object> formData;
    private Long currentNodeId;
    private String currentNodeName;
    private ProcessInstanceStatus status;
    private Integer reworkCount;
    private Long applyUserId;
    private String applyUserName;
    private LocalDateTime applyTime;
    private LocalDateTime completeTime;
    private LocalDate effectiveDate;
    private LocalDateTime dueTime;
    private ProcessPriority priority;
    private String formConfig;

    // 流程历史节点
    private List<NodeHistoryDTO> nodeHistory;

    // 审批记录
    private List<ApprovalRecordDTO> approvalRecords;

    // 操作日志
    private List<OperationLogDTO> operationLogs;

    @Data
    public static class NodeHistoryDTO {
        private Long id;
        private Long nodeDefinitionId;
        private String nodeName;
        private String nodeStatus;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<ApproverDTO> approvers;
    }

    @Data
    public static class ApproverDTO {
        private Long id;
        private Long approverId;
        private String approverName;
        private String approverType;
        private String approvalStatus;
        private LocalDateTime assignTime;
        private LocalDateTime actionTime;
        private String comments;
    }

    @Data
    public static class ApprovalRecordDTO {
        private Long id;
        private Long approverId;
        private String approverName;
        private String actionType;
        private String actionStatus;
        private String actionComment;
        private LocalDateTime actionTime;
        private String targetApproverName;
    }

    @Data
    public static class OperationLogDTO {
        private Long id;
        private String operationType;
        private Long operatorId;
        private String operatorName;
        private LocalDateTime operationTime;
        private String operationDetails;
    }
}