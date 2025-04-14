package com.example.creditpromotion.service.impl;

import com.example.creditpromotion.dto.request.ApprovalActionRequest;
import com.example.creditpromotion.dto.request.ProcessInstanceCreateRequest;
import com.example.creditpromotion.dto.response.ProcessInstanceDetailResponse;
import com.example.creditpromotion.dto.response.ProcessInstanceListResponse;
import com.example.creditpromotion.entity.*;
import com.example.creditpromotion.enums.*;
import com.example.creditpromotion.exception.BusinessException;
import com.example.creditpromotion.exception.ErrorCode;
import com.example.creditpromotion.repository.*;
import com.example.creditpromotion.service.ProcessInstanceService;
import com.example.creditpromotion.service.SpelExpressionService;
import com.example.creditpromotion.util.SecurityUtil;
import com.example.creditpromotion.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 流程实例服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    private final ProcessDefinitionRepository processDefinitionRepository;
    private final ApprovalNodeDefinitionRepository nodeDefinitionRepository;
    private final NodeApproverDefinitionRepository nodeApproverRepository;
    private final NodeTransitionRepository nodeTransitionRepository;
    private final ReworkConfigurationRepository reworkConfigRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessNodeInstanceRepository nodeInstanceRepository;
    private final NodeApproverInstanceRepository approverInstanceRepository;
    private final PreselectedApproverRepository preselectedApproverRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final ProcessOperationLogRepository operationLogRepository;
    private final SpelExpressionService spelExpressionService;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final ParallelNodeGroupRepository parallelNodeGroupRepository;

    @Override
    @Transactional
    public Long createProcessInstance(ProcessInstanceCreateRequest request) {
        // 查找流程定义和开始节点定义
        ProcessDefinition processDefinition = findAndValidateProcessDefinition(request.getProcessDefinitionId());
        ApprovalNodeDefinition startNodeDef = findStartNodeDefinition(processDefinition.getId());

        // 创建流程实例
        ProcessInstance instance = createProcessInstanceEntity(request, processDefinition, startNodeDef);

        // 创建开始节点实例
         createNodeInstance(instance.getId(), startNodeDef.getId(), null);
       
        // 保存预选审批人（如果有）
        savePreselectedApprovers(instance.getId(), request.getPreselectedApprovers());

        // 记录操作日志
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String currentUsername = SecurityUtil.getCurrentUsername();
        createOperationLog(instance.getId(), null, OperationType.CREATE, currentUserId, currentUsername);

        return instance.getId();
    }

    /**
     * 查找并验证流程定义
     */
    private ProcessDefinition findAndValidateProcessDefinition(Long processDefinitionId) {
        ProcessDefinition processDefinition = processDefinitionRepository.findById(processDefinitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_DEFINITION_NOT_FOUND));

        // 检查流程状态，只有ACTIVE状态的流程才能发起
        if (processDefinition.getStatus() != ProcessStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PROCESS_NOT_ACTIVE);
        }

        return processDefinition;
    }

    /**
     * 查找开始节点定义
     */
    private ApprovalNodeDefinition findStartNodeDefinition(Long processDefinitionId) {
        return nodeDefinitionRepository.findByProcessDefinitionIdAndIsStartNode(processDefinitionId, 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_DEFINITION_NOT_FOUND, "流程定义中未找到开始节点"));
    }

    /**
     * 创建流程实例实体
     */
    private ProcessInstance createProcessInstanceEntity(ProcessInstanceCreateRequest request,
            ProcessDefinition processDefinition,
            ApprovalNodeDefinition startNodeDef) {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(idGenerator.nextId());
        instance.setProcessNo(generateProcessNo(processDefinition.getProcessCode()));
        instance.setProcessDefinitionId(processDefinition.getId());
        instance.setOfficerId(request.getOfficerId());
        instance.setEmployeeId(request.getEmployeeId());
        instance.setEmployeeName(request.getEmployeeName());
        instance.setCurrentNodeId(startNodeDef.getId());

        // 设置表单数据
        // TODO there is no form needed at create stage
        setFormDataToInstance(instance, request.getFormData());

        instance.setStatus(ProcessInstanceStatus.DRAFT);

        // 设置申请人信息
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String currentUsername = SecurityUtil.getCurrentUsername();
        instance.setApplyUserId(currentUserId);
        instance.setApplyUserName(currentUsername);
        instance.setApplyTime(LocalDateTime.now());
        instance.setEffectiveDate(request.getEffectiveDate());
        instance.setPriority(request.getPriority());

        // 设置超时时间（如果有配置）
        if (processDefinition.getTimeoutDays() != null) {
            instance.setDueTime(LocalDateTime.now().plusDays(processDefinition.getTimeoutDays()));
        }

        // 设置元数据
        instance.setCreatedBy(currentUsername);
        instance.setCreatedTime(LocalDateTime.now());

        // 保存流程实例
        return processInstanceRepository.save(instance);
    }

    /**
     * 设置表单数据到流程实例
     */
    private void setFormDataToInstance(ProcessInstance instance, Map<String, Object> formData) {
        try {
            // 转换表单数据为JSON字符串
            instance.setFormData(objectMapper.writeValueAsString(formData));
        } catch (Exception e) {
            log.error("Error serializing form data", e);
            throw new BusinessException(ErrorCode.INVALID_FORM_DATA);
        }
    }

    /**
     * 创建开始节点实例
     */
    private ProcessNodeInstance createStartNodeInstance(ProcessInstance instance,
            ApprovalNodeDefinition startNodeDef,
            String createdBy, Long previousNodeId) {
        ProcessNodeInstance startNodeInstance = new ProcessNodeInstance();
        startNodeInstance.setId(idGenerator.nextId());
        startNodeInstance.setProcessInstanceId(instance.getId());
        startNodeInstance.setNodeDefinitionId(startNodeDef.getId());
        startNodeInstance.setNodeName(startNodeDef.getNodeName());
        startNodeInstance.setNodeStatus(NodeInstanceStatus.IN_PROGRESS); // 设置为进行中
        startNodeInstance.setPrevNodeInstanceId(previousNodeId);
        startNodeInstance.setStartTime(LocalDateTime.now());
        startNodeInstance.setCreatedBy(createdBy);
        startNodeInstance.setCreatedTime(LocalDateTime.now());

        // 保存开始节点实例
        return nodeInstanceRepository.save(startNodeInstance);
    }

    /**
     * 创建开始节点的审批人实例
     */
    private NodeApproverInstance createStartNodeApproverInstance(ProcessNodeInstance nodeInstance,
            Long approverId,
            String approverName) {
        // TODO: CREATEDBY SHOULD BE USERID
        NodeApproverInstance approverInstance = new NodeApproverInstance();
        approverInstance.setId(idGenerator.nextId());
        approverInstance.setNodeInstanceId(nodeInstance.getId());
        approverInstance.setApproverId(approverId);
        approverInstance.setApproverName(approverName);
        approverInstance.setApproverType(ApproverType.USER); // 设置为申请人类型
        approverInstance.setApprovalStatus(ApprovalStatus.PENDING); // 设置为进行中
        approverInstance.setCreatedBy(approverName);
        approverInstance.setCreatedTime(LocalDateTime.now());
        approverInstance.setAssignTime(LocalDateTime.now());

        // 保存审批人实例
        return approverInstanceRepository.save(approverInstance);
    }

    @Override
    @Transactional
    public boolean submitProcessInstance(Long id) {
        // 查找流程实例并验证状态
        ProcessInstance instance = findAndValidateProcessInstanceForSubmit(id);

        // 查找开始节点定义和实例
        ApprovalNodeDefinition nodeDef = findStartNodeDefinition(instance.getProcessDefinitionId());
        ProcessNodeInstance startNodeInstance = findStartNodeInstance(id, nodeDef.getId());

        // 更新开始节点审批人状态
        NodeApproverInstance approverInstance = updateStartNodeApproverStatus(startNodeInstance.getId());

        // 创建审批记录
        createSubmitApprovalRecord(id, startNodeInstance.getId());

        // 检查节点是否完成并处理
        boolean isNodeComplete = checkNodeComplete(startNodeInstance.getId(), nodeDef.getApprovalStrategy());
        if (isNodeComplete) {
            // 更新节点状态为已批准
            updateNodeStatusToApproved(startNodeInstance);

            // 处理节点类型并流转
            processNodeByType(instance, startNodeInstance, nodeDef);
        }

        // 记录操作日志
        createOperationLog(instance.getId(), startNodeInstance.getId(), OperationType.SUBMIT,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUsername());

        return true;
    }

    /**
     * 查找并验证流程实例状态是否可提交
     */
    private ProcessInstance findAndValidateProcessInstanceForSubmit(Long id) {
        ProcessInstance instance = processInstanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));

        // 检查状态，只有DRAFT或REWORK状态可以提交
        if (instance.getStatus() != ProcessInstanceStatus.DRAFT
                && instance.getStatus() != ProcessInstanceStatus.REWORK) {
            throw new BusinessException(ErrorCode.PROCESS_STATUS_NOT_SUBMITTABLE);
        }

        return instance;
    }

    /**
     * 查找开始节点实例
     */
    private ProcessNodeInstance findStartNodeInstance(Long processInstanceId, Long nodeDefinitionId) {
        return nodeInstanceRepository.findByProcessInstanceIdAndNodeDefinitionId(
                processInstanceId, nodeDefinitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND, "未找到开始节点实例"));
    }

    /**
     * 更新开始节点审批人状态
     */
    private NodeApproverInstance updateStartNodeApproverStatus(Long nodeInstanceId) {
        NodeApproverInstance approverInstance = approverInstanceRepository.findByNodeInstanceIdAndApproverId(
                nodeInstanceId, SecurityUtil.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVER_INSTANCE_NOT_FOUND, "未找到开始节点审批人实例"));

        approverInstance.setApprovalStatus(ApprovalStatus.APPROVED);
        approverInstance.setActionTime(LocalDateTime.now());
        approverInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        approverInstance.setUpdatedTime(LocalDateTime.now());

        return approverInstanceRepository.save(approverInstance);
    }

    /**
     * 创建提交申请的审批记录
     */
    private ApprovalRecord createSubmitApprovalRecord(Long processInstanceId, Long nodeInstanceId) {
        ApprovalRecord approvalRecord = new ApprovalRecord();
        approvalRecord.setId(idGenerator.nextId());
        approvalRecord.setProcessInstanceId(processInstanceId);
        approvalRecord.setNodeInstanceId(nodeInstanceId);
        approvalRecord.setApproverId(SecurityUtil.getCurrentUserId());
        approvalRecord.setApproverName(SecurityUtil.getCurrentUsername());
        approvalRecord.setActionType(ActionType.APPROVE);
        approvalRecord.setActionTime(LocalDateTime.now());
        approvalRecord.setActionComment("提交申请");
        approvalRecord.setCreatedBy(SecurityUtil.getCurrentUsername());
        approvalRecord.setCreatedTime(LocalDateTime.now());

        return approvalRecordRepository.save(approvalRecord);
    }

    /**
     * 更新节点状态为已批准
     */
    private void updateNodeStatusToApproved(ProcessNodeInstance nodeInstance) {
        nodeInstance.setNodeStatus(NodeInstanceStatus.APPROVED);
        nodeInstance.setEndTime(LocalDateTime.now());
        nodeInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        nodeInstance.setUpdatedTime(LocalDateTime.now());
        nodeInstanceRepository.save(nodeInstance);
    }

    /**
     * 根据节点类型处理并流转
     */
    private void processNodeByType(ProcessInstance instance, ProcessNodeInstance nodeInstance,
            ApprovalNodeDefinition nodeDef) {
        // 根据节点类型处理
        if (nodeDef.getNodeType() == NodeType.CONDITIONAL) {
            processConditionalNode(instance, nodeInstance, nodeDef);
        } else if (nodeDef.getNodeType() == NodeType.PARALLEL) {
            processParallelNode(instance, nodeInstance, nodeDef);
        } else {
            processNextNode(instance, nodeInstance, nodeDef);
        }
    }

    @Override
    @Transactional
    public boolean cancelProcessInstance(Long id, String reason) {
        // 查找流程实例
        ProcessInstance instance = processInstanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));

        // 检查状态，只有DRAFT、IN_PROGRESS或REWORK状态可以取消
        if (instance.getStatus() != ProcessInstanceStatus.DRAFT &&
                instance.getStatus() != ProcessInstanceStatus.IN_PROGRESS &&
                instance.getStatus() != ProcessInstanceStatus.REWORK) {
            throw new BusinessException(ErrorCode.PROCESS_STATUS_NOT_CANCELABLE);
        }

        // 检查操作权限（只允许发起人或管理员取消）
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!currentUserId.equals(instance.getApplyUserId()) && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 更新流程实例状态
        instance.setStatus(ProcessInstanceStatus.CANCELED);
        instance.setCompleteTime(LocalDateTime.now());
        instance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        instance.setUpdatedTime(LocalDateTime.now());

        processInstanceRepository.save(instance);

        // 如果有进行中的节点，更新节点状态
        if (instance.getCurrentNodeId() != null) {
            List<ProcessNodeInstance> activeNodes = nodeInstanceRepository
                    .findByProcessInstanceIdAndNodeStatus(instance.getId(), NodeInstanceStatus.IN_PROGRESS);

            for (ProcessNodeInstance nodeInstance : activeNodes) {
                nodeInstance.setNodeStatus(NodeInstanceStatus.CANCELED);
                nodeInstance.setEndTime(LocalDateTime.now());
                nodeInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
                nodeInstance.setUpdatedTime(LocalDateTime.now());

                nodeInstanceRepository.save(nodeInstance);

                // 更新审批人状态
                List<NodeApproverInstance> approvers = approverInstanceRepository
                        .findByNodeInstanceId(nodeInstance.getId());
                for (NodeApproverInstance approver : approvers) {
                    if (approver.getApprovalStatus() == ApprovalStatus.PENDING) {
                        approver.setApprovalStatus(ApprovalStatus.REJECTED); // 使用REJECTED表示取消
                        approver.setActionTime(LocalDateTime.now());
                        approver.setComments("流程被取消");
                        approver.setUpdatedBy(SecurityUtil.getCurrentUsername());
                        approver.setUpdatedTime(LocalDateTime.now());

                        approverInstanceRepository.save(approver);
                    }
                }

                // 记录审批记录
                createApprovalRecord(instance.getId(), nodeInstance.getId(), null,
                        SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUsername(),
                        ActionType.CANCEL, ActionStatus.SUCCESS, reason);
            }
        }

        // 记录操作日志
        createOperationLog(instance.getId(), null, OperationType.CANCEL,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUsername());

        return true;
    }

    @Override
    @Transactional
    public boolean processApprovalAction(ApprovalActionRequest request) {
        // 查找流程实例
        ProcessInstance instance = processInstanceRepository.findById(request.getProcessInstanceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));

        // 检查流程状态
        if (instance.getStatus() != ProcessInstanceStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.PROCESS_NOT_IN_PROGRESS);
        }

        // 查找节点实例
        ProcessNodeInstance nodeInstance = nodeInstanceRepository.findById(request.getNodeInstanceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));

        // 检查节点状态
        if (nodeInstance.getNodeStatus() != NodeInstanceStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.NODE_NOT_IN_PROGRESS);
        }

        // 查找审批人实例
        NodeApproverInstance approverInstance = approverInstanceRepository.findById(request.getApproverInstanceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVER_INSTANCE_NOT_FOUND));

        // 检查审批人权限
        validateApproverPermission(approverInstance);

        // 检查审批人状态
        if (approverInstance.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_PROCESSED);
        }

        // 根据操作类型处理
        switch (request.getActionType()) {
            case APPROVE:
                return handleApproveAction(instance, nodeInstance, approverInstance, request.getComment());
            case REJECT:
                return handleRejectAction(instance, nodeInstance, approverInstance, request.getComment());
            case REWORK:
                return handleReworkAction(instance, nodeInstance, approverInstance, request.getComment(),
                        request.getTargetNodeId());
            case TRANSFER:
                return handleTransferAction(instance, nodeInstance, approverInstance, request.getComment(),
                        request.getTargetApproverId(), request.getTargetApproverName());
            default:
                throw new BusinessException(ErrorCode.INVALID_ACTION_TYPE);
        }
    }

    /**
     * Validate approver permission
     */
    private void validateApproverPermission(NodeApproverInstance approverInstance) {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        // If it's a direct user assignment, check user ID
        if (approverInstance.getApproverType() == ApproverType.USER &&
                !approverInstance.getApproverId().equals(currentUserId) &&
                !SecurityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // If it's a role-based assignment, check if the current user has the role
        if (approverInstance.getApproverType() == ApproverType.ROLE) {
            String roleId = approverInstance.getRoleId();
            if (roleId != null && !SecurityUtil.hasRole(roleId) && !SecurityUtil.isAdmin()) {
                throw new BusinessException(ErrorCode.NO_PERMISSION, "您没有所需的角色权限");
            }

            // If this is the first time a user with the role is approving,
            // assign this task to the current user
            if (approverInstance.getApproverId() == null) {
                approverInstance.setApproverId(currentUserId);
                approverInstance.setApproverName(SecurityUtil.getCurrentUsername());
                approverInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
                approverInstance.setUpdatedTime(LocalDateTime.now());
                approverInstanceRepository.save(approverInstance);

                log.info("Role-based task claimed by user: {}, role: {}",
                        currentUserId, roleId);
            }
            // If the task has already been claimed by another user with the role
            else if (!approverInstance.getApproverId().equals(currentUserId) && !SecurityUtil.isAdmin()) {
                throw new BusinessException(ErrorCode.NO_PERMISSION, "该任务已被其他用户认领");
            }

        }
        // If it's an expression-based assignment, evaluate the expression as a whole
        if (approverInstance.getApproverType() == ApproverType.EXPRESSION) {
            String expression = approverInstance.getExpression();
            if (expression != null && !expression.trim().isEmpty()) {
                // Prepare variables for expression evaluation
                Map<String, Object> variables = buildExpressionContext();

                // Evaluate the expression using SpEL
                boolean hasPermission = false;
                try {
                    hasPermission = spelExpressionService.evaluateBoolean(expression, variables);
                } catch (Exception e) {
                    log.error("Error evaluating expression permission: " + expression, e);
                    throw new BusinessException(ErrorCode.EXPRESSION_EVALUATION_ERROR, "表达式权限评估错误");
                }

                if (!hasPermission && !SecurityUtil.isAdmin()) {
                    throw new BusinessException(ErrorCode.NO_PERMISSION, "您不满足表达式权限条件");
                }

                // If this is the first time a user with the expression permission is approving,
                // assign this task to the current user
                if (approverInstance.getApproverId() == null) {
                    approverInstance.setApproverId(currentUserId);
                    approverInstance.setApproverName(SecurityUtil.getCurrentUsername());
                    approverInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
                    approverInstance.setUpdatedTime(LocalDateTime.now());
                    approverInstanceRepository.save(approverInstance);

                    log.info("Expression-based task claimed by user: {}, expression: {}",
                            currentUserId, expression);
                }
                // If the task has already been claimed by another user
                else if (!approverInstance.getApproverId().equals(currentUserId) && !SecurityUtil.isAdmin()) {
                    throw new BusinessException(ErrorCode.NO_PERMISSION, "该任务已被其他用户认领");
                }
            }
        }
    }

    /**
     * Build context for expression evaluation
     * 
     * @return Map of variables for expression evaluation
     */

    private Map<String, Object> buildExpressionContext() {
        Map<String, Object> variables = new HashMap<>();
        // TODO: REAL COMMON CONTEXT BUILDING
        // Add user information
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String currentUsername = SecurityUtil.getCurrentUsername();
        variables.put("userId", currentUserId);
        variables.put("username", currentUsername);

        // Add user roles and groups
        Set<String> userRoles = SecurityUtil.getCurrentUserRoles();
        List<String> userGroups = SecurityUtil.getCurrentUserGroups();
        variables.put("userRoles", userRoles);
        variables.put("userGroups", userGroups);

        // Add helper functions for expression evaluation
        variables.put("hasRole", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String role) {
                return userRoles.contains(role);
            }
        });

        variables.put("hasGroup", new Function<String, Boolean>() {
            @Override
            public Boolean apply(String group) {
                return userGroups.contains(group);
            }
        });

        return variables;
    }

    @Override
    public ProcessInstanceDetailResponse getProcessInstanceDetail(Long id) {
        //TODO:  too complex and no need on this implementation. return process instance, node info, also possible allowed actions as well as first matched approver instance info.
        // TODO:  if ui send exact approver instance id, then we know exact approver instance, the following action will be for that approver instance only in case of multiple has permission approval instance.
        // 查找流程实例
        ProcessInstance instance = processInstanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));

        // 查找流程定义
        ProcessDefinition processDefinition = processDefinitionRepository.findById(instance.getProcessDefinitionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_DEFINITION_NOT_FOUND));

        // 查找当前节点
        ApprovalNodeDefinition currentNode = null;
        if (instance.getCurrentNodeId() != null) {
            currentNode = nodeDefinitionRepository.findById(instance.getCurrentNodeId()).orElse(null);
        }

        // 构建响应对象
        ProcessInstanceDetailResponse response = new ProcessInstanceDetailResponse();
        response.setId(instance.getId());
        response.setProcessNo(instance.getProcessNo());
        response.setProcessDefinitionId(instance.getProcessDefinitionId());
        response.setProcessName(processDefinition.getProcessName());
        response.setOfficerId(instance.getOfficerId());
        response.setEmployeeId(instance.getEmployeeId());
        response.setEmployeeName(instance.getEmployeeName());

        try {
            // 反序列化表单数据
            @SuppressWarnings("unchecked")
            Map<String, Object> formData = objectMapper.readValue(instance.getFormData(), Map.class);
            response.setFormData(formData);
        } catch (Exception e) {
            log.error("Error deserializing form data", e);
            response.setFormData(new HashMap<>());
        }

        response.setCurrentNodeId(instance.getCurrentNodeId());
        response.setCurrentNodeName(currentNode != null ? currentNode.getNodeName() : null);
        response.setStatus(instance.getStatus());
        response.setReworkCount(instance.getReworkCount());
        response.setApplyUserId(instance.getApplyUserId());
        response.setApplyUserName(instance.getApplyUserName());
        response.setApplyTime(instance.getApplyTime());
        response.setCompleteTime(instance.getCompleteTime());
        response.setEffectiveDate(instance.getEffectiveDate());
        response.setDueTime(instance.getDueTime());
        response.setPriority(instance.getPriority());
        response.setFormConfig(processDefinition.getFormConfig());

        // 查询流程历史节点
        List<ProcessNodeInstance> nodeInstances = nodeInstanceRepository.findByProcessInstanceId(instance.getId());
        List<ProcessInstanceDetailResponse.NodeHistoryDTO> nodeHistoryList = new ArrayList<>();

        for (ProcessNodeInstance nodeInst : nodeInstances) {
            ProcessInstanceDetailResponse.NodeHistoryDTO nodeHistoryDTO = new ProcessInstanceDetailResponse.NodeHistoryDTO();
            nodeHistoryDTO.setId(nodeInst.getId());
            nodeHistoryDTO.setNodeDefinitionId(nodeInst.getNodeDefinitionId());
            nodeHistoryDTO.setNodeName(nodeInst.getNodeName());
            nodeHistoryDTO.setNodeStatus(nodeInst.getNodeStatus().name());
            nodeHistoryDTO.setStartTime(nodeInst.getStartTime());
            nodeHistoryDTO.setEndTime(nodeInst.getEndTime());

            // 查询审批人
            List<NodeApproverInstance> approvers = approverInstanceRepository.findByNodeInstanceId(nodeInst.getId());
            List<ProcessInstanceDetailResponse.ApproverDTO> approverList = new ArrayList<>();

            for (NodeApproverInstance approver : approvers) {
                ProcessInstanceDetailResponse.ApproverDTO approverDTO = new ProcessInstanceDetailResponse.ApproverDTO();
                approverDTO.setId(approver.getId());
                approverDTO.setApproverId(approver.getApproverId());
                approverDTO.setApproverName(approver.getApproverName());
                approverDTO.setApproverType(approver.getApproverType().name());
                approverDTO.setApprovalStatus(approver.getApprovalStatus().name());
                approverDTO.setAssignTime(approver.getAssignTime());
                approverDTO.setActionTime(approver.getActionTime());
                approverDTO.setComments(approver.getComments());

                approverList.add(approverDTO);
            }

            nodeHistoryDTO.setApprovers(approverList);
            nodeHistoryList.add(nodeHistoryDTO);
        }

        response.setNodeHistory(nodeHistoryList);

        // 查询审批记录
        List<ApprovalRecord> approvalRecords = approvalRecordRepository
                .findByProcessInstanceIdOrderByActionTimeDesc(instance.getId());
        List<ProcessInstanceDetailResponse.ApprovalRecordDTO> recordList = new ArrayList<>();

        for (ApprovalRecord record : approvalRecords) {
            ProcessInstanceDetailResponse.ApprovalRecordDTO recordDTO = new ProcessInstanceDetailResponse.ApprovalRecordDTO();
            recordDTO.setId(record.getId());
            recordDTO.setApproverId(record.getApproverId());
            recordDTO.setApproverName(record.getApproverName());
            recordDTO.setActionType(record.getActionType().name());
            recordDTO.setActionStatus(record.getActionStatus().name());
            recordDTO.setActionComment(record.getActionComment());
            recordDTO.setActionTime(record.getActionTime());
            recordDTO.setTargetApproverName(record.getTargetApproverName());

            recordList.add(recordDTO);
        }

        response.setApprovalRecords(recordList);

        // 查询操作日志
        List<ProcessOperationLog> operationLogs = operationLogRepository
                .findByProcessInstanceIdOrderByOperationTimeDesc(instance.getId());
        List<ProcessInstanceDetailResponse.OperationLogDTO> logList = new ArrayList<>();

        for (ProcessOperationLog log : operationLogs) {
            ProcessInstanceDetailResponse.OperationLogDTO logDTO = new ProcessInstanceDetailResponse.OperationLogDTO();
            logDTO.setId(log.getId());
            logDTO.setOperationType(log.getOperationType().name());
            logDTO.setOperatorId(log.getOperatorId());
            logDTO.setOperatorName(log.getOperatorName());
            logDTO.setOperationTime(log.getOperationTime());
            logDTO.setOperationDetails(log.getOperationDetails());

            logList.add(logDTO);
        }

        response.setOperationLogs(logList);

        return response;
    }

    @Override
    public Page<ProcessInstanceListResponse> getUserProcessInstances(
            Long userId, ProcessInstanceStatus status, String keyword, Pageable pageable) {

        Page<ProcessInstance> page;

        // 根据查询条件调用不同的Repository方法
        if (StringUtils.hasText(keyword)) {
            page = processInstanceRepository.searchByKeyword(keyword, pageable);
        } else if (status != null) {
            page = processInstanceRepository.findByApplyUserIdAndStatus(userId, status, pageable);
        } else {
            page = processInstanceRepository.findByApplyUserId(userId, pageable);
        }

        // 转换为响应DTO
        return page.map(this::convertToListResponse);
    }

    @Override
    public Page<ProcessInstanceListResponse> getProcessInstanceList(
            Long processDefinitionId, ProcessInstanceStatus status, String keyword, Pageable pageable) {

        Page<ProcessInstance> page;

        // 根据查询条件调用不同的Repository方法
        if (StringUtils.hasText(keyword)) {
            page = processInstanceRepository.searchByKeyword(keyword, pageable);
        } else if (processDefinitionId != null) {
            page = processInstanceRepository.findByProcessDefinitionId(processDefinitionId, pageable);
        } else {
            page = processInstanceRepository.findAll(pageable);
        }

        // 转换为响应DTO
        return page.map(this::convertToListResponse);
    }
   
/**
 * Get allowed actions for the current user on a process instance
 * Follows these rules:
 * 1. If current node is start node: SUBMIT, CANCEL are allowed
 * 2. If not start node: APPROVE, REJECT, REWORK, TRANSFER are allowed when user is an eligible approver
 * 3. User can only approve once - no actions allowed after approval
 * 
 * @param processInstanceId Process instance ID
 * @return List of allowed actions
 */
@Override
public List<ActionType> getAllowedActionsForCurrentUser(Long processInstanceId) {
     // Get current user ID and roles
     Long currentUserId = SecurityUtil.getCurrentUserId();
     Set<String> userRoles = SecurityUtil.getCurrentUserRoles();

     // Get the process instance
     ProcessInstance instance = processInstanceRepository.findById(processInstanceId)
             .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));
     
     // If process is not DRAFT, REWORK or IN_PROGRESS, no actions allowed
     if (instance.getStatus() != ProcessInstanceStatus.DRAFT && 
         instance.getStatus() != ProcessInstanceStatus.REWORK && 
         instance.getStatus() != ProcessInstanceStatus.IN_PROGRESS) {
         return Collections.emptyList();
     }
     
     // Find the current active node
     ProcessNodeInstance currentNode = findCurrentActiveNode(instance);
     if (currentNode == null) {
         // No active node found
         return Collections.emptyList();
     }
     
     // Get node definition to check if it's a start node
     ApprovalNodeDefinition nodeDef = nodeDefinitionRepository.findById(currentNode.getNodeDefinitionId())
             .orElseThrow(() -> new BusinessException(ErrorCode.NODE_DEFINITION_NOT_FOUND));
     
     // Case 1: Start node
     if (nodeDef.isStartNode()) {
         // For start node, only the applicant can submit/cancel
         if (instance.getApplyUserId().equals(currentUserId) || SecurityUtil.isAdmin()) {
             // Only allow SUBMIT/CANCEL if node is in progress and in DRAFT/REWORK status
             if (currentNode.getNodeStatus() == NodeInstanceStatus.IN_PROGRESS &&
                 (instance.getStatus() == ProcessInstanceStatus.DRAFT || 
                  instance.getStatus() == ProcessInstanceStatus.REWORK)) {
                 return Arrays.asList(ActionType.SUBMIT, ActionType.CANCEL);
             }
         }
         return Collections.emptyList();
     }
     
     // Case 2: Regular approval node
     // Check if user has already approved this node
     boolean hasAlreadyApproved = approverInstanceRepository
             .findByNodeInstanceIdAndApproverId(currentNode.getId(), currentUserId)
             .filter(approver -> approver.getApprovalStatus() != ApprovalStatus.PENDING)
             .isPresent();
     
     if (hasAlreadyApproved) {
         // User has already taken action on this node
         return Collections.emptyList();
     }
     
     // Get all pending approver instances for this node
     List<NodeApproverInstance> pendingApprovers = approverInstanceRepository
             .findByNodeInstanceIdAndApprovalStatus(currentNode.getId(), ApprovalStatus.PENDING);
     
     // Check if the user is eligible for any of the pending approver instances
     boolean isEligibleApprover = pendingApprovers.stream().anyMatch(approver -> {
         // Direct user assignment
         if (approver.getApproverType() == ApproverType.USER && 
             currentUserId.equals(approver.getApproverId())) {
             return true;
         }
         
         // Role-based assignment
         if (approver.getApproverType() == ApproverType.ROLE && 
             approver.getRoleId() != null && 
             userRoles.contains(approver.getRoleId())) {
             return true;
         }
         // Expression-based assignment (simplified check)
         if (approver.getApproverType() == ApproverType.EXPRESSION && 
             StringUtils.hasText(approver.getExpression())) {
             // Prepare variables for expression evaluation
             Map<String, Object> variables = buildExpressionContext();

             // Evaluate the expression using SpEL
             boolean hasPermission = false;
             try {
                 hasPermission = spelExpressionService.evaluateBoolean(approver.getExpression(), variables);
             } catch (Exception e) {
                 log.error("Error evaluating expression permission: " + approver.getExpression(), e);
                 throw new BusinessException(ErrorCode.EXPRESSION_EVALUATION_ERROR, "表达式权限评估错误");
             }

             return hasPermission;
         }
         
         return false;
     });
     
     // If user is eligible to approve, return appropriate actions
     if (isEligibleApprover || SecurityUtil.isAdmin()) {
         List<ActionType> actions = new ArrayList<>();
         actions.add(ActionType.APPROVE);
         actions.add(ActionType.REJECT);
         actions.add(ActionType.REWORK);
         // Add TRANSFER - assuming it's always allowed
         actions.add(ActionType.TRANSFER);
         
         return actions;
     }
     
     // No actions allowed
     return Collections.emptyList();
}

    /**
     * 处理同意操作
     */
    private boolean handleApproveAction(
            ProcessInstance instance,
            ProcessNodeInstance nodeInstance,
            NodeApproverInstance approverInstance,
            String comment) {

        // 更新审批人状态
        approverInstance.setApprovalStatus(ApprovalStatus.APPROVED);
        approverInstance.setActionTime(LocalDateTime.now());
        approverInstance.setComments(comment);
        approverInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        approverInstance.setUpdatedTime(LocalDateTime.now());

        approverInstanceRepository.save(approverInstance);

        // 查找节点定义
        ApprovalNodeDefinition nodeDef = nodeDefinitionRepository.findById(nodeInstance.getNodeDefinitionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_DEFINITION_NOT_FOUND));

        // 检查节点是否完成
        boolean isNodeComplete = checkNodeComplete(nodeInstance.getId(), nodeDef.getApprovalStrategy());

        // 记录审批记录
        createApprovalRecord(instance.getId(), nodeInstance.getId(), approverInstance.getId(),
                approverInstance.getApproverId(), approverInstance.getApproverName(),
                ActionType.APPROVE, ActionStatus.SUCCESS, comment);

        if (isNodeComplete) {
            // 更新节点状态
            nodeInstance.setNodeStatus(NodeInstanceStatus.APPROVED);
            nodeInstance.setEndTime(LocalDateTime.now());
            nodeInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
            nodeInstance.setUpdatedTime(LocalDateTime.now());

            nodeInstanceRepository.save(nodeInstance);

            // 如果是条件节点，执行条件评估
            if (nodeDef.getNodeType() == NodeType.CONDITIONAL) {
                processConditionalNode(instance, nodeInstance, nodeDef);
            }
            // 如果是并行节点，检查并行组是否完成
            else if (nodeDef.getNodeType() == NodeType.PARALLEL) {
                processParallelNode(instance, nodeInstance, nodeDef);
            }
            // 如果是普通节点或者其他节点类型已处理完成，流转到下一节点
            else {
                processNextNode(instance, nodeInstance, nodeDef);
            }

            // 记录操作日志
            createOperationLog(instance.getId(), nodeInstance.getId(), OperationType.APPROVE,
                    approverInstance.getApproverId(), approverInstance.getApproverName());
        }

        return true;
    }

    /**
     * 处理拒绝操作
     */
    private boolean handleRejectAction(
            ProcessInstance instance,
            ProcessNodeInstance nodeInstance,
            NodeApproverInstance approverInstance,
            String comment) {

        // 更新审批人状态
        approverInstance.setApprovalStatus(ApprovalStatus.REJECTED);
        approverInstance.setActionTime(LocalDateTime.now());
        approverInstance.setComments(comment);
        approverInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        approverInstance.setUpdatedTime(LocalDateTime.now());

        approverInstanceRepository.save(approverInstance);

        // 更新节点状态
        nodeInstance.setNodeStatus(NodeInstanceStatus.REJECTED);
        nodeInstance.setEndTime(LocalDateTime.now());
        nodeInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        nodeInstance.setUpdatedTime(LocalDateTime.now());

        nodeInstanceRepository.save(nodeInstance);

        // 更新流程实例状态
        instance.setStatus(ProcessInstanceStatus.REJECTED);
        instance.setCompleteTime(LocalDateTime.now());
        instance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        instance.setUpdatedTime(LocalDateTime.now());

        processInstanceRepository.save(instance);

        // 记录审批记录
        createApprovalRecord(instance.getId(), nodeInstance.getId(), approverInstance.getId(),
                approverInstance.getApproverId(), approverInstance.getApproverName(),
                ActionType.REJECT, ActionStatus.SUCCESS, comment);

        // 记录操作日志
        createOperationLog(instance.getId(), nodeInstance.getId(), OperationType.REJECT,
                approverInstance.getApproverId(), approverInstance.getApproverName());

        return true;
    }

    /**
     * 处理重做操作
     */
    private boolean handleReworkAction(
            ProcessInstance instance,
            ProcessNodeInstance nodeInstance,
            NodeApproverInstance approverInstance,
            String comment,
            Long targetNodeId) {

        // 更新审批人状态
        approverInstance.setApprovalStatus(ApprovalStatus.APPROVED); // 使用APPROVED表示已处理
        approverInstance.setActionTime(LocalDateTime.now());
        approverInstance.setComments(comment);
        approverInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        approverInstance.setUpdatedTime(LocalDateTime.now());

        approverInstanceRepository.save(approverInstance);

        // 更新节点状态
        nodeInstance.setNodeStatus(NodeInstanceStatus.REWORK);
        nodeInstance.setEndTime(LocalDateTime.now());
        nodeInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        nodeInstance.setUpdatedTime(LocalDateTime.now());

        nodeInstanceRepository.save(nodeInstance);

        // 查找节点定义
        ApprovalNodeDefinition nodeDef = nodeDefinitionRepository.findById(nodeInstance.getNodeDefinitionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_DEFINITION_NOT_FOUND));

        // Determine the rework target node
        Long reworkTargetNodeId = determineReworkTargetNode(nodeDef, targetNodeId, nodeInstance);

        // Get target node definition to check if it's a start node
        ApprovalNodeDefinition targetNodeDef = nodeDefinitionRepository.findById(reworkTargetNodeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_DEFINITION_NOT_FOUND));

        // Update process instance status and current node
        boolean isReworkToStartNode = targetNodeDef.isStartNode();
        if (isReworkToStartNode) {
            // For rework to start node, set status to REWORK
            instance.setStatus(ProcessInstanceStatus.REWORK);
        } else {
            // For rework to other nodes, keep status as IN_PROGRESS
            instance.setStatus(ProcessInstanceStatus.IN_PROGRESS);
        }

        instance.setCurrentNodeId(reworkTargetNodeId);
        instance.setReworkCount(instance.getReworkCount() + 1);
        instance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        instance.setUpdatedTime(LocalDateTime.now());
        processInstanceRepository.save(instance);

        // Create target node instance
        createNodeInstance(instance.getId(), reworkTargetNodeId, nodeInstance.getId());

        // Record approval action
        createApprovalRecord(instance.getId(), nodeInstance.getId(), approverInstance.getId(),
                approverInstance.getApproverId(), approverInstance.getApproverName(),
                ActionType.REWORK, ActionStatus.SUCCESS, comment);

        // Record operation log with details
        Map<String, Object> details = new HashMap<>();
        details.put("targetNodeId", reworkTargetNodeId);
        details.put("isStartNode", isReworkToStartNode);
        details.put("reworkType", getReworkType(nodeDef, targetNodeId).name());

        createOperationLog(instance.getId(), nodeInstance.getId(), OperationType.REWORK,
                approverInstance.getApproverId(), approverInstance.getApproverName(), details);

        return true;
    }

    /**
     * Determines the appropriate rework target node based on configuration and
     * input
     */
    private Long determineReworkTargetNode(ApprovalNodeDefinition nodeDef, Long targetNodeId,
            ProcessNodeInstance nodeInstance) {
        // If target node specified in request, validate it
        if (targetNodeId != null) {
            return targetNodeId;
        }
        // Query rework configuration for this node
        List<ReworkConfiguration> reworkConfigs = reworkConfigRepository.findByNodeId(nodeDef.getId());

        // If no rework configuration exists, default to sending back to initiator
        // (start node)
        if (reworkConfigs.isEmpty()) {
            ApprovalNodeDefinition startNodeDef = findStartNodeDefinition(nodeDef.getProcessDefinitionId());
            return startNodeDef.getId();
        }

        ReworkConfiguration reworkConfig = reworkConfigs.get(0);

        // Otherwise determine target node based on configuration
        switch (reworkConfig.getReworkType()) {
            case TO_INITIATOR:
                // Rework to initiator means going back to the start node
                ApprovalNodeDefinition startNodeDef = findStartNodeDefinition(nodeDef.getProcessDefinitionId());
                return startNodeDef.getId();

            case TO_PREV_NODE:
                // Rework to previous node
                if (nodeInstance.getPrevNodeInstanceId() == null) {
                    throw new BusinessException(ErrorCode.PREV_NODE_NOT_FOUND);
                }

                ProcessNodeInstance prevNodeInstance = nodeInstanceRepository
                        .findById(nodeInstance.getPrevNodeInstanceId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));

                return prevNodeInstance.getNodeDefinitionId();

            case TO_SPECIFIC_NODE:
                // Rework to specific configured node
                if (reworkConfig.getTargetNodeId() == null) {
                    throw new BusinessException(ErrorCode.REWORK_TARGET_NOT_SPECIFIED);
                }
                return reworkConfig.getTargetNodeId();

            default:
                throw new BusinessException(ErrorCode.INVALID_REWORK_TYPE);
        }
    }

    /**
     * Gets the effective rework type being applied
     */
    private ReworkType getReworkType(ApprovalNodeDefinition nodeDef, Long targetNodeId) {
        List<ReworkConfiguration> reworkConfigs = reworkConfigRepository.findByNodeId(nodeDef.getId());

        // If no config, it's implicitly TO_INITIATOR
        if (reworkConfigs.isEmpty()) {
            return ReworkType.TO_INITIATOR;
        }

        // If target specified, it's TO_SPECIFIC_NODE
        if (targetNodeId != null) {
            return ReworkType.TO_SPECIFIC_NODE;
        }

        // Otherwise return the configured type
        return reworkConfigs.get(0).getReworkType();
    }

    /**
     * 处理转交操作
     */
    private boolean handleTransferAction(
            ProcessInstance instance,
            ProcessNodeInstance nodeInstance,
            NodeApproverInstance approverInstance,
            String comment,
            Long targetApproverId,
            String targetApproverName) {

        // 参数校验
        if (targetApproverId == null || !StringUtils.hasText(targetApproverName)) {
            throw new BusinessException(ErrorCode.INVALID_TARGET_APPROVER);
        }

        // 更新审批人状态
        approverInstance.setApprovalStatus(ApprovalStatus.TRANSFERRED);
        approverInstance.setActionTime(LocalDateTime.now());
        approverInstance.setComments(comment);
        approverInstance.setTransferredToId(targetApproverId);
        approverInstance.setTransferredToName(targetApproverName);
        approverInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        approverInstance.setUpdatedTime(LocalDateTime.now());

        approverInstanceRepository.save(approverInstance);

        // 创建新的审批人实例
        NodeApproverInstance newApproverInstance = new NodeApproverInstance();
        newApproverInstance.setId(idGenerator.nextId());
        newApproverInstance.setNodeInstanceId(nodeInstance.getId());
        newApproverInstance.setApproverId(targetApproverId);
        newApproverInstance.setApproverName(targetApproverName);
        newApproverInstance.setApproverType(approverInstance.getApproverType()); // 继承原审批人类型
        newApproverInstance.setApprovalStatus(ApprovalStatus.PENDING);
        newApproverInstance.setAssignTime(LocalDateTime.now());

        // 设置超时时间（如果有）
        if (nodeInstance.getDueTime() != null) {
            newApproverInstance.setDueTime(nodeInstance.getDueTime());
        }

        // 设置元数据
        String currentUsername = SecurityUtil.getCurrentUsername();
        newApproverInstance.setCreatedBy(currentUsername);
        newApproverInstance.setCreatedTime(LocalDateTime.now());

        approverInstanceRepository.save(newApproverInstance);

        // 记录审批记录
        createApprovalRecord(instance.getId(), nodeInstance.getId(), approverInstance.getId(),
                approverInstance.getApproverId(), approverInstance.getApproverName(),
                ActionType.TRANSFER, ActionStatus.SUCCESS, comment,
                targetApproverId, targetApproverName, null);

        // 记录操作日志
        Map<String, Object> details = new HashMap<>();
        details.put("targetApproverId", targetApproverId);
        details.put("targetApproverName", targetApproverName);

        createOperationLog(instance.getId(), nodeInstance.getId(), OperationType.TRANSFER,
                approverInstance.getApproverId(), approverInstance.getApproverName(), details);

        return true;
    }

    /**
     * 检查节点是否完成（根据审批策略）
     */
    private boolean checkNodeComplete(Long nodeInstanceId, ApprovalStrategy strategy) {
        List<NodeApproverInstance> approvers = approverInstanceRepository.findByNodeInstanceId(nodeInstanceId);

        // 过滤出未处理的审批人（转交的不算）
        List<NodeApproverInstance> pendingApprovers = approvers.stream()
                .filter(a -> a.getApprovalStatus() == ApprovalStatus.PENDING)
                .collect(Collectors.toList());

        // 如果没有待处理的审批人，节点完成
        if (pendingApprovers.isEmpty()) {
            return true;
        }

        // 根据审批策略判断
        if (strategy == ApprovalStrategy.ANY) {
            // 任一人同意策略，至少有一人同意即可
            return approvers.stream()
                    .anyMatch(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED);
        }

        // ALL策略，需要所有人都处理完成
        return false;
    }

    /**
     * 处理条件节点
     */
    private void processConditionalNode(
            ProcessInstance instance,
            ProcessNodeInstance nodeInstance,
            ApprovalNodeDefinition nodeDef) {

        // 查询所有从该节点出发的条件转换
        List<NodeTransition> transitions = nodeTransitionRepository
                .findBySourceNodeIdOrderByPriorityAsc(nodeDef.getId());

        if (transitions.isEmpty()) {
            throw new BusinessException(ErrorCode.TRANSITION_NOT_FOUND);
        }

        // 反序列化表单数据，用于条件表达式求值
        Map<String, Object> variables = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> formData = objectMapper.readValue(instance.getFormData(), Map.class);
            variables.put("form", formData);
        } catch (Exception e) {
            log.error("Error deserializing form data", e);
            variables.put("form", new HashMap<>());
        }

        // 按优先级依次评估条件，找到第一个匹配的转换
        NodeTransition matchedTransition = null;
        for (NodeTransition transition : transitions) {
            if (transition.getTransitionType() == TransitionType.CONDITIONAL) {
                // 如果是条件转换，评估条件
                if (StringUtils.hasText(transition.getConditionExpression())) {
                    boolean result = spelExpressionService.evaluateBoolean(
                            transition.getConditionExpression(), variables);

                    if (result) {
                        matchedTransition = transition;
                        break;
                    }
                }
            } else if (transition.getTransitionType() == TransitionType.NORMAL) {
                // 如果是普通转换（默认路径），直接使用
                matchedTransition = transition;
                break;
            }
        }

        if (matchedTransition == null) {
            throw new BusinessException(ErrorCode.NO_MATCHING_TRANSITION);
        }

        // 创建目标节点实例
        ProcessNodeInstance nextNodeInstance = createNodeInstance(
                instance.getId(), matchedTransition.getTargetNodeId(), nodeInstance.getId());

        // 更新流程实例当前节点
        instance.setCurrentNodeId(matchedTransition.getTargetNodeId());
        instance.setUpdatedBy(SecurityUtil.getCurrentUsername());
        instance.setUpdatedTime(LocalDateTime.now());

        processInstanceRepository.save(instance);

        // 记录日志
        Map<String, Object> details = new HashMap<>();
        details.put("conditionExpression", matchedTransition.getConditionExpression());
        details.put("targetNodeId", matchedTransition.getTargetNodeId());

        createOperationLog(instance.getId(), nodeInstance.getId(), OperationType.APPROVE,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUsername(), details);
    }

    /**
     * 处理并行节点
     */
    private void processParallelNode(
            ProcessInstance instance,
            ProcessNodeInstance nodeInstance,
            ApprovalNodeDefinition nodeDef) {

        // 如果是并行子节点，检查其他子节点是否都完成
        if (nodeInstance.getParentNodeInstanceId() != null) {
            ProcessNodeInstance parentNodeInstance = nodeInstanceRepository
                    .findById(nodeInstance.getParentNodeInstanceId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));

            // 查找同一父节点的所有子节点
            List<ProcessNodeInstance> siblingNodes = nodeInstanceRepository
                    .findByParentNodeInstanceId(parentNodeInstance.getId());

            // 检查是否所有子节点都已完成
            boolean allComplete = siblingNodes.stream()
                    .allMatch(n -> n.getNodeStatus() == NodeInstanceStatus.APPROVED);

            if (allComplete) {
                // 更新父节点状态
                parentNodeInstance.setNodeStatus(NodeInstanceStatus.APPROVED);
                parentNodeInstance.setEndTime(LocalDateTime.now());
                parentNodeInstance.setUpdatedBy(SecurityUtil.getCurrentUsername());
                parentNodeInstance.setUpdatedTime(LocalDateTime.now());

                nodeInstanceRepository.save(parentNodeInstance);

                // 继续处理父节点的下一步
                ApprovalNodeDefinition parentNodeDef = nodeDefinitionRepository
                        .findById(parentNodeInstance.getNodeDefinitionId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NODE_DEFINITION_NOT_FOUND));

                processNextNode(instance, parentNodeInstance, parentNodeDef);
            }
        }
        // 如果是并行父节点，创建所有子节点
        else {
            // 查找该节点的所有子节点定义
            List<ParallelNodeGroup> parallelGroups = parallelNodeGroupRepository
                    .findByParentNodeId(nodeDef.getId());

            if (!parallelGroups.isEmpty()) {
                for (ParallelNodeGroup group : parallelGroups) {
                    // 创建子节点实例
                    createNodeInstance(instance.getId(), group.getChildNodeId(), null, nodeInstance.getId());
                }

                // 更新流程实例当前节点（保持在并行父节点）
                instance.setCurrentNodeId(nodeDef.getId());
                instance.setUpdatedBy(SecurityUtil.getCurrentUsername());
                instance.setUpdatedTime(LocalDateTime.now());

                processInstanceRepository.save(instance);
            } else {
                // 如果没有配置子节点，视为普通节点处理
                processNextNode(instance, nodeInstance, nodeDef);
            }
        }
    }

    /**
     * 处理流转到下一个节点
     */
    private void processNextNode(
            ProcessInstance instance,
            ProcessNodeInstance nodeInstance,
            ApprovalNodeDefinition nodeDef) {

        // 检查是否是结束节点
        if (nodeDef.isEndNode()) {
            // 更新流程实例状态为已批准
            instance.setStatus(ProcessInstanceStatus.APPROVED);
            instance.setCompleteTime(LocalDateTime.now());
            instance.setUpdatedBy(SecurityUtil.getCurrentUsername());
            instance.setUpdatedTime(LocalDateTime.now());

            processInstanceRepository.save(instance);
            return;
        }

        // 查询下一个节点
        List<NodeTransition> transitions = nodeTransitionRepository
                .findBySourceNodeId(nodeDef.getId());

        if (transitions.isEmpty()) {
            throw new BusinessException(ErrorCode.TRANSITION_NOT_FOUND);
        }

        // 如果只有一个转换，直接使用
        if (transitions.size() == 1) {
            NodeTransition transition = transitions.get(0);

            // 创建下一个节点实例
            ProcessNodeInstance nextNodeInstance = createNodeInstance(
                    instance.getId(), transition.getTargetNodeId(), nodeInstance.getId());

            // 更新流程实例当前节点

            // 更新流程实例状态
            if (nodeDef.isStartNode()) {
                instance.setStatus(ProcessInstanceStatus.IN_PROGRESS);
            }

            instance.setCurrentNodeId(transition.getTargetNodeId());
            instance.setUpdatedBy(SecurityUtil.getCurrentUsername());
            instance.setUpdatedTime(LocalDateTime.now());

            processInstanceRepository.save(instance);
        }
        // 如果有多个转换，应该是配置错误或者条件节点漏处理
        else {
            throw new BusinessException(ErrorCode.MULTIPLE_TRANSITIONS);
        }
    }

    /**
     * 创建节点实例
     */
    private ProcessNodeInstance createNodeInstance(
            Long processInstanceId, Long nodeDefinitionId, Long prevNodeInstanceId) {
        return createNodeInstance(processInstanceId, nodeDefinitionId, prevNodeInstanceId, null);
    }

    private ProcessNodeInstance createNodeInstance(
            Long processInstanceId, Long nodeDefinitionId, Long prevNodeInstanceId, Long parentNodeInstanceId) {
// TODO: CREATEDBY SHOULD BE USERID not name
        // 查找节点定义
        ApprovalNodeDefinition nodeDef = nodeDefinitionRepository.findById(nodeDefinitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_DEFINITION_NOT_FOUND));

        // 创建节点实例
        ProcessNodeInstance nodeInstance = new ProcessNodeInstance();
        nodeInstance.setId(idGenerator.nextId());
        nodeInstance.setProcessInstanceId(processInstanceId);
        nodeInstance.setNodeDefinitionId(nodeDefinitionId);
        nodeInstance.setNodeName(nodeDef.getNodeName());
        nodeInstance.setNodeStatus(NodeInstanceStatus.IN_PROGRESS);
        nodeInstance.setStartTime(LocalDateTime.now());
        nodeInstance.setPrevNodeInstanceId(prevNodeInstanceId);
        nodeInstance.setParentNodeInstanceId(parentNodeInstanceId);

        // 设置超时时间（如果有配置）
        if (nodeDef.getTimeoutHours() != null) {
            nodeInstance.setDueTime(LocalDateTime.now().plusHours(nodeDef.getTimeoutHours()));
        }

        // 设置元数据
        String currentUsername = SecurityUtil.getCurrentUsername();
        nodeInstance.setCreatedBy(currentUsername);
        nodeInstance.setCreatedTime(LocalDateTime.now());

        nodeInstance = nodeInstanceRepository.save(nodeInstance);

        // 创建审批人实例
        createApproverInstances(nodeInstance.getId(), nodeDef.getId(), processInstanceId);

        return nodeInstance;
    }

    /**
     * 创建审批人实例
     */
    private void createApproverInstances(Long nodeInstanceId, Long nodeDefinitionId, Long processInstanceId) {
        // Query node definition
        ApprovalNodeDefinition nodeDef = nodeDefinitionRepository.findById(nodeDefinitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_DEFINITION_NOT_FOUND));

        // Special handling for start nodes - they should always use the original
        // applicant
        if (nodeDef.isStartNode()) {
            createStartNodeApprovers(nodeInstanceId, processInstanceId);
            return;
        }

        // 查询节点审批人定义
        List<NodeApproverDefinition> approverDefs = nodeApproverRepository.findByNodeId(nodeDefinitionId);

        // 查询流程实例数据
        ProcessInstance instance = processInstanceRepository.findById(processInstanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));

        // 查询预选审批人
        List<PreselectedApprover> preselectedApprovers = preselectedApproverRepository
                .findByProcessInstanceIdAndNodeDefinitionId(processInstanceId, nodeDefinitionId);

        // 如果有预选审批人，使用预选审批人
        if (!preselectedApprovers.isEmpty()) {
            createPreselectedApprovers(nodeInstanceId, preselectedApprovers);
        }
        // 没有预选审批人，根据定义创建
        else if (!approverDefs.isEmpty()) {
            for (NodeApproverDefinition approverDef : approverDefs) {
                // 根据审批人类型处理
                switch (approverDef.getApproverType()) {
                    case USER:
                        // 具体用户
                        if (StringUtils.hasText(approverDef.getApproverId())) {
                            createUserApprover(nodeInstanceId, approverDef);
                        }
                        break;

                    case ROLE:
                        // 角色（需要查询该角色下的所有用户）
                        // 实际项目中需要调用用户服务获取
                        // TODO: ROLE level
                        createRoleApprovers(nodeInstanceId, approverDef);
                        break;

                    case DEPARTMENT:
                        // 部门（需要查询该部门下的所有用户）
                        // 实际项目中需要调用组织结构服务获取
                        createDepartmentApprovers(nodeInstanceId, approverDef);
                        break;

                    case EXPRESSION:
                        // 表达式（需要动态计算）
                        // TODO: Expression implementation
                        if (StringUtils.hasText(approverDef.getExpression())) {
                            createExpressionApprovers(nodeInstanceId, approverDef);
                        }
                        break;

                    default:
                        // 其他类型暂不处理
                        break;
                }
            }
        }
    }

    /**
     * Creates approver instances for a start node during rework, using the original
     * applicant
     */
    private void createStartNodeApprovers(Long nodeInstanceId, Long processInstanceId) {
        // Find process instance to get original applicant
        ProcessInstance instance = processInstanceRepository.findById(processInstanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));

        // Create approver instance for the original applicant
        NodeApproverInstance approverInstance = new NodeApproverInstance();
        approverInstance.setId(idGenerator.nextId());
        approverInstance.setNodeInstanceId(nodeInstanceId);
        approverInstance.setApproverId(instance.getApplyUserId());
        approverInstance.setApproverName(instance.getApplyUserName());
        approverInstance.setApproverType(ApproverType.USER);
        approverInstance.setApprovalStatus(ApprovalStatus.PENDING);
        approverInstance.setAssignTime(LocalDateTime.now());

        // Set timeout (inherit from node instance)
        ProcessNodeInstance nodeInstance = nodeInstanceRepository.findById(nodeInstanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));
        if (nodeInstance.getDueTime() != null) {
            approverInstance.setDueTime(nodeInstance.getDueTime());
        }

        // Set metadata
        String currentUsername = SecurityUtil.getCurrentUsername();
        approverInstance.setCreatedBy(currentUsername);
        approverInstance.setCreatedTime(LocalDateTime.now());

        approverInstanceRepository.save(approverInstance);

        log.info("Created start node approver instance for original applicant during rework: {}",
                instance.getApplyUserName());
    }

    private void createPreselectedApprovers(Long nodeInstanceId, List<PreselectedApprover> preselectedApprovers) {
        for (PreselectedApprover preselected : preselectedApprovers) {
            NodeApproverInstance approverInstance = new NodeApproverInstance();
            approverInstance.setId(idGenerator.nextId());
            approverInstance.setNodeInstanceId(nodeInstanceId);
            approverInstance.setApproverId(preselected.getApproverId());
            approverInstance.setApproverName(preselected.getApproverName());
            approverInstance.setApproverType(ApproverType.USER); // 预选的一般是具体用户
            approverInstance.setApprovalStatus(ApprovalStatus.PENDING);
            approverInstance.setAssignTime(LocalDateTime.now());
            approverInstance.setIsPreselected(1); // 标记为预选

            // 设置超时时间（继承节点实例的超时时间）
            ProcessNodeInstance nodeInstance = nodeInstanceRepository.findById(nodeInstanceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));
            if (nodeInstance.getDueTime() != null) {
                approverInstance.setDueTime(nodeInstance.getDueTime());
            }

            // 设置元数据
            String currentUsername = SecurityUtil.getCurrentUsername();
            approverInstance.setCreatedBy(currentUsername);
            approverInstance.setCreatedTime(LocalDateTime.now());

            approverInstanceRepository.save(approverInstance);
        }
    }

    /**
     * 创建具体用户的审批人实例
     */
    private void createUserApprover(Long nodeInstanceId, NodeApproverDefinition approverDef) {
        // 实际项目中需要查询用户信息
        // TODO: CREATED BY SHOULD BE USER ID
        Long approverId = Long.parseLong(approverDef.getApproverId());
        String approverName = "User " + approverId; // TODO: 实际项目中应查询用户名称

        NodeApproverInstance approverInstance = new NodeApproverInstance();
        approverInstance.setId(idGenerator.nextId());
        approverInstance.setNodeInstanceId(nodeInstanceId);
        approverInstance.setApproverId(approverId);
        approverInstance.setApproverName(approverName);
        approverInstance.setApproverType(ApproverType.USER);
        approverInstance.setApprovalStatus(ApprovalStatus.PENDING);
        approverInstance.setAssignTime(LocalDateTime.now());

        // 设置超时时间（继承节点实例的超时时间）
        ProcessNodeInstance nodeInstance = nodeInstanceRepository.findById(nodeInstanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));
        if (nodeInstance.getDueTime() != null) {
            approverInstance.setDueTime(nodeInstance.getDueTime());
        }

        // 设置元数据
        String currentUsername = SecurityUtil.getCurrentUsername();
        approverInstance.setCreatedBy(currentUsername);
        approverInstance.setCreatedTime(LocalDateTime.now());

        approverInstanceRepository.save(approverInstance);
    }

    /**
     * 创建角色类型的审批人实例
     */
    private void createRoleApprovers(Long nodeInstanceId, NodeApproverDefinition approverDef) {
        // Instead of creating an approver instance for a specific user,
        // we'll create a role-based approver instance that can be claimed by any user
        // with the role

        // Extract the role ID/code from the approver definition
        String roleId = approverDef.getApproverId();
        String roleName = approverDef.getDescription();

        if (!StringUtils.hasText(roleId)) {
            log.error("Role ID is empty for node approver definition: {}", approverDef.getId());
            throw new BusinessException(ErrorCode.INVALID_APPROVER_DEFINITION, "角色ID不能为空");
        }

        // Create a special approver instance for the role
        NodeApproverInstance approverInstance = new NodeApproverInstance();
        approverInstance.setId(idGenerator.nextId());
        approverInstance.setNodeInstanceId(nodeInstanceId);
        approverInstance.setApproverId(null); // No specific approver ID yet
        approverInstance.setApproverName(roleName != null ? roleName : "Role: " + roleId);
        approverInstance.setApproverType(ApproverType.ROLE);
        approverInstance.setApprovalStatus(ApprovalStatus.PENDING);
        approverInstance.setAssignTime(LocalDateTime.now());
        approverInstance.setRoleId(roleId); // Store the role ID for later authorization checks

        // Set timeout (inherit from node instance)
        ProcessNodeInstance nodeInstance = nodeInstanceRepository.findById(nodeInstanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));
        if (nodeInstance.getDueTime() != null) {
            approverInstance.setDueTime(nodeInstance.getDueTime());
        }

        // Set metadata
        String currentUsername = SecurityUtil.getCurrentUsername();
        approverInstance.setCreatedBy(currentUsername);
        approverInstance.setCreatedTime(LocalDateTime.now());

        approverInstanceRepository.save(approverInstance);

        // Log the creation of a role-based approver
        log.info("Created role-based approver instance for role: {}, node instance: {}",
                roleId, nodeInstanceId);
    }

    /**
     * 创建部门类型的审批人实例
     */
    private void createDepartmentApprovers(Long nodeInstanceId, NodeApproverDefinition approverDef) {
        // 实际项目中需要调用组织结构服务获取该部门下的所有用户
        // 这里简化处理，假设只有一个用户
        // TODO: NOT NEEDED or need real implementation
        Long approverId = 200L; // 模拟部门下的用户ID
        String approverName = "Department User"; // 模拟用户名称

        NodeApproverInstance approverInstance = new NodeApproverInstance();
        approverInstance.setId(idGenerator.nextId());
        approverInstance.setNodeInstanceId(nodeInstanceId);
        approverInstance.setApproverId(approverId);
        approverInstance.setApproverName(approverName);
        approverInstance.setApproverType(ApproverType.DEPARTMENT);
        approverInstance.setApprovalStatus(ApprovalStatus.PENDING);
        approverInstance.setAssignTime(LocalDateTime.now());

        // 设置超时时间（继承节点实例的超时时间）
        ProcessNodeInstance nodeInstance = nodeInstanceRepository.findById(nodeInstanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));
        if (nodeInstance.getDueTime() != null) {
            approverInstance.setDueTime(nodeInstance.getDueTime());
        }

        // 设置元数据
        String currentUsername = SecurityUtil.getCurrentUsername();
        approverInstance.setCreatedBy(currentUsername);
        approverInstance.setCreatedTime(LocalDateTime.now());

        approverInstanceRepository.save(approverInstance);
    }

    /**
     * 创建表达式类型的审批人实例
     */
    private void createExpressionApprovers(Long nodeInstanceId, NodeApproverDefinition approverDef) {
        // 检查表达式类型
        String expression = approverDef.getExpression();

        // 如果是权限表达式（以#hasRole或#hasGroup开头）
        if (expression != null && !expression.trim().isEmpty()) {
            // 创建一个表达式类型的审批人实例，但不指定具体审批人
            NodeApproverInstance approverInstance = new NodeApproverInstance();
            approverInstance.setId(idGenerator.nextId());
            approverInstance.setNodeInstanceId(nodeInstanceId);
            approverInstance.setApproverId(null); // 暂不指定审批人，等待有权限的人认领
            approverInstance.setApproverName(approverDef.getDescription()); // 临时名称
            approverInstance.setApproverType(ApproverType.EXPRESSION);
            approverInstance.setApprovalStatus(ApprovalStatus.PENDING);
            approverInstance.setAssignTime(LocalDateTime.now());
            approverInstance.setExpression(expression); // 存储表达式

            // 设置超时时间（继承节点实例的超时时间）
            ProcessNodeInstance nodeInstance = nodeInstanceRepository.findById(nodeInstanceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));
            if (nodeInstance.getDueTime() != null) {
                approverInstance.setDueTime(nodeInstance.getDueTime());
            }

            // 设置元数据
            String currentUsername = SecurityUtil.getCurrentUsername();
            approverInstance.setCreatedBy(currentUsername);
            approverInstance.setCreatedTime(LocalDateTime.now());

            approverInstanceRepository.save(approverInstance);

            log.info("Created expression-based approver instance with expression: {}", expression);
        }
    }
/**
 * Find the current active node for a process instance
 * No support for parallel nodes
 * @param instance Process instance
 * @return Current active node instance
 */
private ProcessNodeInstance findCurrentActiveNode(ProcessInstance instance) {
    // If current node ID is set, try to find the active node instance
    if (instance.getCurrentNodeId() != null) {
        Optional<ProcessNodeInstance> activeNode = nodeInstanceRepository
                .findByProcessInstanceIdAndNodeDefinitionIdAndNodeStatus(
                    instance.getId(), 
                    instance.getCurrentNodeId(),
                    NodeInstanceStatus.IN_PROGRESS);
        
        if (activeNode.isPresent()) {
            return activeNode.get();
        }
    }
    
    // For DRAFT/REWORK status, find the start node
    if (instance.getStatus() == ProcessInstanceStatus.DRAFT || 
        instance.getStatus() == ProcessInstanceStatus.REWORK) {
        
        // Find start node definition
        ApprovalNodeDefinition startNodeDef = nodeDefinitionRepository
                .findByProcessDefinitionIdAndIsStartNode(instance.getProcessDefinitionId(), 1)
                .orElse(null);
        
        if (startNodeDef != null) {
            // Find or create start node instance
            Optional<ProcessNodeInstance> startNodeOpt = nodeInstanceRepository
                    .findByProcessInstanceIdAndNodeDefinitionId(
                        instance.getId(), startNodeDef.getId());
            
            return startNodeOpt.orElse(null);
        }
    }
    
    return null;
}

    /**
     * 记录审批记录
     */
    private void createApprovalRecord(
            Long processInstanceId, Long nodeInstanceId, Long approverInstanceId,
            Long approverId, String approverName, ActionType actionType,
            ActionStatus actionStatus, String comment) {

        createApprovalRecord(processInstanceId, nodeInstanceId, approverInstanceId,
                approverId, approverName, actionType, actionStatus, comment, null, null, null);
    }

    private void createApprovalRecord(
            Long processInstanceId, Long nodeInstanceId, Long approverInstanceId,
            Long approverId, String approverName, ActionType actionType,
            ActionStatus actionStatus, String comment,
            Long targetApproverId, String targetApproverName, Long targetNodeId) {

        ApprovalRecord record = new ApprovalRecord();
        record.setId(idGenerator.nextId());
        record.setProcessInstanceId(processInstanceId);
        record.setNodeInstanceId(nodeInstanceId);
        record.setApproverInstanceId(approverInstanceId);
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setActionType(actionType);
        record.setActionStatus(actionStatus);
        record.setActionComment(comment);
        record.setActionTime(LocalDateTime.now());
        record.setTargetApproverId(targetApproverId);
        record.setTargetApproverName(targetApproverName);
        record.setTargetNodeId(targetNodeId);

        // 设置元数据
        record.setCreatedBy(SecurityUtil.getCurrentUsername());
        record.setCreatedTime(LocalDateTime.now());

        approvalRecordRepository.save(record);
    }

    /**
     * 记录操作日志
     */
    private void createOperationLog(
            Long processInstanceId, Long nodeInstanceId, OperationType operationType,
            Long operatorId, String operatorName) {

        createOperationLog(processInstanceId, nodeInstanceId, operationType, operatorId, operatorName, null);
    }

    private void createOperationLog(
            Long processInstanceId, Long nodeInstanceId, OperationType operationType,
            Long operatorId, String operatorName, Map<String, Object> details) {

        ProcessOperationLog log = new ProcessOperationLog();
        log.setId(idGenerator.nextId());
        log.setProcessInstanceId(processInstanceId);
        log.setNodeInstanceId(nodeInstanceId);
        log.setOperationType(operationType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperationTime(LocalDateTime.now());

        // 序列化操作详情
        if (details != null) {
            try {
                log.setOperationDetails(objectMapper.writeValueAsString(details));
            } catch (Exception e) {
                log.setOperationDetails("{}");
            }
        }

        // 记录IP和设备信息
        log.setIpAddress(SecurityUtil.getCurrentIpAddress());
        log.setDeviceInfo(SecurityUtil.getCurrentDeviceInfo());

        // 设置元数据
        log.setCreatedBy(operatorName);
        log.setCreatedTime(LocalDateTime.now());

        operationLogRepository.save(log);
    }

    /**
     * 生成流程实例编号
     */
    private String generateProcessNo(String processCode) {
        // 格式：流程编码-年月-5位序号
        String yearMonth = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String randomNum = String.format("%05d", new Random().nextInt(100000));
        return processCode + "-" + yearMonth + "-" + randomNum;
    }

    /**
     * 保存预选审批人
     */
    private void savePreselectedApprovers(Long processInstanceId,
            Map<String, List<Map<String, Object>>> preselectedApprovers) {
        if (preselectedApprovers == null || preselectedApprovers.isEmpty()) {
            return;
        }

        // 处理每个节点的预选审批人
        for (Map.Entry<String, List<Map<String, Object>>> entry : preselectedApprovers.entrySet()) {
            String nodeKey = entry.getKey();
            List<Map<String, Object>> approvers = entry.getValue();

            if (approvers == null || approvers.isEmpty()) {
                continue;
            }

            // 查找节点定义
            Optional<ApprovalNodeDefinition> nodeDefOpt = nodeDefinitionRepository
                    .findByProcessDefinitionIdAndNodeKey(processInstanceRepository.findById(processInstanceId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND))
                            .getProcessDefinitionId(), nodeKey);

            if (!nodeDefOpt.isPresent()) {
                continue;
            }

            ApprovalNodeDefinition nodeDef = nodeDefOpt.get();

            // 检查节点是否允许预选审批人
            if (!nodeDef.allowApproverSelection()) {
                continue;
            }

            // 保存每个预选审批人
            for (Map<String, Object> approver : approvers) {
                if (!approver.containsKey("id") || !approver.containsKey("name")) {
                    continue;
                }

                Long approverId = Long.valueOf(approver.get("id").toString());
                String approverName = approver.get("name").toString();

                PreselectedApprover preselected = new PreselectedApprover();
                preselected.setId(idGenerator.nextId());
                preselected.setProcessInstanceId(processInstanceId);
                preselected.setNodeDefinitionId(nodeDef.getId());
                preselected.setApproverId(approverId);
                preselected.setApproverName(approverName);

                // 设置元数据
                String currentUsername = SecurityUtil.getCurrentUsername();
                preselected.setCreatedBy(currentUsername);
                preselected.setCreatedTime(LocalDateTime.now());

                preselectedApproverRepository.save(preselected);
            }
        }
    }

    /**
     * 转换为列表响应DTO
     */
    private ProcessInstanceListResponse convertToListResponse(ProcessInstance instance) {
        ProcessInstanceListResponse response = new ProcessInstanceListResponse();
        response.setId(instance.getId());
        response.setProcessNo(instance.getProcessNo());
        response.setProcessDefinitionId(instance.getProcessDefinitionId());

        // 查询流程定义名称
        ProcessDefinition processDefinition = processDefinitionRepository
                .findById(instance.getProcessDefinitionId()).orElse(null);
        if (processDefinition != null) {
            response.setProcessName(processDefinition.getProcessName());
        }

        response.setOfficerId(instance.getOfficerId());
        response.setEmployeeId(instance.getEmployeeId());
        response.setEmployeeName(instance.getEmployeeName());

        // 查询当前节点名称
        if (instance.getCurrentNodeId() != null) {
            ApprovalNodeDefinition currentNode = nodeDefinitionRepository
                    .findById(instance.getCurrentNodeId()).orElse(null);
            if (currentNode != null) {
                response.setCurrentNodeName(currentNode.getNodeName());
            }
        }

        response.setStatus(instance.getStatus());
        response.setReworkCount(instance.getReworkCount());
        response.setApplyUserId(instance.getApplyUserId());
        response.setApplyUserName(instance.getApplyUserName());
        response.setApplyTime(instance.getApplyTime());
        response.setCompleteTime(instance.getCompleteTime());
        response.setEffectiveDate(instance.getEffectiveDate());
        response.setDueTime(instance.getDueTime());
        response.setPriority(instance.getPriority());
        response.setCreatedBy(instance.getCreatedBy());
        response.setCreatedTime(instance.getCreatedTime());

        return response;
    }
}