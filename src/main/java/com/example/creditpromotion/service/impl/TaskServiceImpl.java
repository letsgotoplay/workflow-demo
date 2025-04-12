package com.example.creditpromotion.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.creditpromotion.dto.response.TaskResponse;
import com.example.creditpromotion.entity.NodeApproverInstance;
import com.example.creditpromotion.entity.ProcessInstance;
import com.example.creditpromotion.entity.ProcessNodeInstance;
import com.example.creditpromotion.enums.ApprovalStatus;
import com.example.creditpromotion.enums.ProcessInstanceStatus;
import com.example.creditpromotion.exception.BusinessException;
import com.example.creditpromotion.exception.ErrorCode;
import com.example.creditpromotion.repository.NodeApproverInstanceRepository;
import com.example.creditpromotion.repository.ProcessDefinitionRepository;
import com.example.creditpromotion.repository.ProcessInstanceRepository;
import com.example.creditpromotion.repository.ProcessNodeInstanceRepository;
import com.example.creditpromotion.service.TaskService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务服务实现类
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessNodeInstanceRepository nodeInstanceRepository;
    private final NodeApproverInstanceRepository approverInstanceRepository;
    private final ProcessDefinitionRepository processDefinitionRepository;

    @Override
    public Page<TaskResponse> getUserTasks(Long userId, Pageable pageable) {
        // 分别查询待审批和待重做任务
        List<TaskResponse> approvalTasks = getUserApprovalTasksList(userId);
        List<TaskResponse> reworkTasks = getUserReworkTasksList(userId);
        
        // 合并任务列表
        List<TaskResponse> allTasks = new ArrayList<>();
        allTasks.addAll(approvalTasks);
        allTasks.addAll(reworkTasks);
        
        // 排序（按优先级和申请时间）
        allTasks.sort((t1, t2) -> {
            // 首先按优先级降序
            int priorityCompare = Integer.compare(t2.getPriority(), t1.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 其次按申请时间降序
            return t2.getApplyTime().compareTo(t1.getApplyTime());
        });
        
        // 分页处理
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allTasks.size());
        
        if (start > allTasks.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, allTasks.size());
        }
        
        return new PageImpl<>(allTasks.subList(start, end), pageable, allTasks.size());
    }

    @Override
    public Page<TaskResponse> getUserApprovalTasks(Long userId, Pageable pageable) {
        // 查询待审批任务列表
        List<TaskResponse> tasks = getUserApprovalTasksList(userId);
        
        // 排序（按优先级和申请时间）
        tasks.sort((t1, t2) -> {
            // 首先按优先级降序
            int priorityCompare = Integer.compare(t2.getPriority(), t1.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 其次按申请时间降序
            return t2.getApplyTime().compareTo(t1.getApplyTime());
        });
        
        // 分页处理
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), tasks.size());
        
        if (start > tasks.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, tasks.size());
        }
        
        return new PageImpl<>(tasks.subList(start, end), pageable, tasks.size());
    }

    @Override
    public Page<TaskResponse> getUserReworkTasks(Long userId, Pageable pageable) {
        // 查询待重做任务列表
        List<TaskResponse> tasks = getUserReworkTasksList(userId);
        
        // 排序（按优先级和申请时间）
        tasks.sort((t1, t2) -> {
            // 首先按优先级降序
            int priorityCompare = Integer.compare(t2.getPriority(), t1.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 其次按申请时间降序
            return t2.getApplyTime().compareTo(t1.getApplyTime());
        });
        
        // 分页处理
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), tasks.size());
        
        if (start > tasks.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, tasks.size());
        }
        
        return new PageImpl<>(tasks.subList(start, end), pageable, tasks.size());
    }

    @Override
    public TaskResponse getTaskDetail(Long taskId, String taskType) {
        if ("APPROVAL".equals(taskType)) {
            // 待审批任务详情
            NodeApproverInstance approverInstance = approverInstanceRepository.findById(taskId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
            
            return buildApprovalTaskResponse(approverInstance);
        } else if ("REWORK".equals(taskType)) {
            // 待重做任务详情
            ProcessInstance instance = processInstanceRepository.findById(taskId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
            
            return buildReworkTaskResponse(instance);
        } else {
            throw new BusinessException(ErrorCode.INVALID_TASK_TYPE);
        }
    }

    /**
     * 查询用户待审批任务列表
     */
    private List<TaskResponse> getUserApprovalTasksList(Long userId) {
        // 查询用户待处理的审批任务
        List<NodeApproverInstance> approverInstances = approverInstanceRepository
                .findPendingTasksByApproverId(userId);
        
        // 转换为响应DTO
        return approverInstances.stream()
                .map(this::buildApprovalTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询发起用户待重做任务列表
     */
    private List<TaskResponse> getUserReworkTasksList(Long userId) {
        // 查询用户发起的且为重做状态的流程实例
        List<ProcessInstance> instances = processInstanceRepository
                .findByApplyUserIdAndStatus(userId, ProcessInstanceStatus.REWORK);
        
        // 转换为响应DTO
        return instances.stream()
                .map(this::buildReworkTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * 构建审批任务响应
     */
    private TaskResponse buildApprovalTaskResponse(NodeApproverInstance approverInstance) {
        TaskResponse response = new TaskResponse();
        response.setId(approverInstance.getId());
        response.setTaskType("APPROVAL");
        response.setApproverInstanceId(approverInstance.getId());
        
        // 查询节点实例
        ProcessNodeInstance nodeInstance = nodeInstanceRepository.findById(approverInstance.getNodeInstanceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NODE_INSTANCE_NOT_FOUND));
        
        response.setNodeInstanceId(nodeInstance.getId());
        response.setNodeName(nodeInstance.getNodeName());
        
        // 查询流程实例
        ProcessInstance instance = processInstanceRepository.findById(nodeInstance.getProcessInstanceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));
        
        response.setProcessInstanceId(instance.getId());
        response.setProcessNo(instance.getProcessNo());
        
        // 查询流程定义名称
        processDefinitionRepository.findById(instance.getProcessDefinitionId())
                .ifPresent(pd -> response.setProcessName(pd.getProcessName()));
        
        response.setEmployeeId(instance.getEmployeeId());
        response.setEmployeeName(instance.getEmployeeName());
        response.setApplyUserName(instance.getApplyUserName());
        response.setApplyTime(instance.getApplyTime());
        response.setAssignTime(approverInstance.getAssignTime());
        response.setDueTime(approverInstance.getDueTime());
        
        // 检查是否超时
        if (approverInstance.getDueTime() != null) {
            response.setIsOverdue(LocalDateTime.now().isAfter(approverInstance.getDueTime()));
        } else {
            response.setIsOverdue(false);
        }
        
        // 设置优先级（数字越大越优先）
        int priority = 0;
        if (instance.getPriority() != null) {
            switch (instance.getPriority()) {
                case HIGH:
                    priority = 3;
                    break;
                case NORMAL:
                    priority = 2;
                    break;
                case LOW:
                    priority = 1;
                    break;
            }
        }
        
        // 如果超时，提高优先级
        if (Boolean.TRUE.equals(response.getIsOverdue())) {
            priority += 10;
        }
        
        response.setPriority(priority);
        
        return response;
    }

    /**
     * 构建重做任务响应
     */
    private TaskResponse buildReworkTaskResponse(ProcessInstance instance) {
        TaskResponse response = new TaskResponse();
        response.setId(instance.getId());
        response.setTaskType("REWORK");
        response.setProcessInstanceId(instance.getId());
        response.setProcessNo(instance.getProcessNo());
        
        // 查询流程定义名称
        processDefinitionRepository.findById(instance.getProcessDefinitionId())
                .ifPresent(pd -> response.setProcessName(pd.getProcessName()));
        
        response.setEmployeeId(instance.getEmployeeId());
        response.setEmployeeName(instance.getEmployeeName());
        response.setApplyUserName(instance.getApplyUserName());
        response.setApplyTime(instance.getApplyTime());
        response.setAssignTime(instance.getUpdatedTime()); // 使用更新时间作为分配时间
        response.setDueTime(instance.getDueTime());
        
        // 检查是否超时
        if (instance.getDueTime() != null) {
            response.setIsOverdue(LocalDateTime.now().isAfter(instance.getDueTime()));
        } else {
            response.setIsOverdue(false);
        }
        
        // 设置优先级（数字越大越优先）
        int priority = 0;
        if (instance.getPriority() != null) {
            switch (instance.getPriority()) {
                case HIGH:
                    priority = 3;
                    break;
                case NORMAL:
                    priority = 2;
                    break;
                case LOW:
                    priority = 1;
                    break;
            }
        }
        
        // 如果超时，提高优先级
        if (Boolean.TRUE.equals(response.getIsOverdue())) {
            priority += 10;
        }
        
        response.setPriority(priority);
        
        return response;
    }
}