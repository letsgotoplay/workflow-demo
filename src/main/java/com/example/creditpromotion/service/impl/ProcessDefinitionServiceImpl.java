package com.example.creditpromotion.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.creditpromotion.dto.request.*;
import com.example.creditpromotion.dto.response.ProcessDefinitionDetailResponse;
import com.example.creditpromotion.dto.response.ProcessDefinitionListResponse;
import com.example.creditpromotion.entity.*;
import com.example.creditpromotion.enums.ProcessStatus;
import com.example.creditpromotion.exception.BusinessException;
import com.example.creditpromotion.exception.ErrorCode;
import com.example.creditpromotion.repository.*;
import com.example.creditpromotion.service.ProcessDefinitionService;
import com.example.creditpromotion.util.SecurityUtil;
import com.example.creditpromotion.util.SnowflakeIdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现类
 */
@Service
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private final ProcessDefinitionRepository processDefinitionRepository;
    private final ApprovalNodeDefinitionRepository nodeDefinitionRepository;
    private final NodeApproverDefinitionRepository nodeApproverRepository;
    private final NodeTransitionRepository nodeTransitionRepository;
    private final ParallelNodeGroupRepository parallelNodeGroupRepository;
    private final ReworkConfigurationRepository reworkConfigRepository;
    private final ReminderConfigurationRepository reminderConfigRepository;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public Long createProcessDefinition(ProcessDefinitionCreateRequest request) {
        // 检查流程编码是否已存在
        if (processDefinitionRepository.findByProcessCode(request.getProcessCode()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_PROCESS_CODE);
        }

        // 创建流程定义
        ProcessDefinition processDefinition = new ProcessDefinition();
        BeanUtils.copyProperties(request, processDefinition);
        
        // 设置默认值和元数据
        processDefinition.setId(idGenerator.nextId());
        processDefinition.setStatus(ProcessStatus.ACTIVE);
        String currentUser = SecurityUtil.getCurrentUsername();
        processDefinition.setCreatedBy(currentUser);
        processDefinition.setCreatedTime(LocalDateTime.now());
        
        // 保存流程定义
        processDefinition = processDefinitionRepository.save(processDefinition);
        Long processDefinitionId = processDefinition.getId();
        
        // 创建节点和关系
        if (request.getNodes() != null && !request.getNodes().isEmpty()) {
            createNodesAndRelationships(processDefinitionId, request.getNodes(), 
                    request.getTransitions(), request.getReworkConfigs(), request.getReminderConfigs());
        }
        
        return processDefinitionId;
    }

    @Override
    @Transactional
    public Long updateProcessDefinition(ProcessDefinitionUpdateRequest request) {
        // 查找流程定义
        ProcessDefinition processDefinition = processDefinitionRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_DEFINITION_NOT_FOUND));
        
        // 检查流程状态，只有ACTIVE状态的流程才能编辑
        if (processDefinition.getStatus() != ProcessStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PROCESS_STATUS_NOT_EDITABLE);
        }
        
        // 更新基本属性
        processDefinition.setProcessName(request.getProcessName());
        processDefinition.setDescription(request.getDescription());
        processDefinition.setFormConfig(request.getFormConfig());
        processDefinition.setTimeoutDays(request.getTimeoutDays());
        processDefinition.setStatus(request.getStatus());
        processDefinition.setPriority(request.getPriority());
        
        // 设置更新元数据
        String currentUser = SecurityUtil.getCurrentUsername();
        processDefinition.setUpdatedBy(currentUser);
        processDefinition.setUpdatedTime(LocalDateTime.now());
        
        processDefinitionRepository.save(processDefinition);
        
        // 如果请求中包含节点和关系，则重新创建
        if (request.getNodes() != null && !request.getNodes().isEmpty()) {
            // 删除旧的节点和关系
            List<ApprovalNodeDefinition> oldNodes = nodeDefinitionRepository
                    .findByProcessDefinitionId(processDefinition.getId());
            
            // 删除节点审批人
            for (ApprovalNodeDefinition node : oldNodes) {
                List<NodeApproverDefinition> approvers = nodeApproverRepository.findByNodeId(node.getId());
                for (NodeApproverDefinition approver : approvers) {
                    approver.setIsDeleted(1);
                    nodeApproverRepository.save(approver);
                }
                
                // 标记节点删除
                node.setIsDeleted(1);
                nodeDefinitionRepository.save(node);
            }
            
            // 删除转换关系
            List<NodeTransition> transitions = nodeTransitionRepository
                    .findByProcessDefinitionId(processDefinition.getId());
            for (NodeTransition transition : transitions) {
                transition.setIsDeleted(1);
                nodeTransitionRepository.save(transition);
            }
            
            // 删除并行组关系
            List<ParallelNodeGroup> groups = parallelNodeGroupRepository
                    .findByProcessDefinitionId(processDefinition.getId());
            for (ParallelNodeGroup group : groups) {
                group.setIsDeleted(1);
                parallelNodeGroupRepository.save(group);
            }
            
            // 删除重做配置
            List<ReworkConfiguration> reworks = reworkConfigRepository
                    .findByProcessDefinitionId(processDefinition.getId());
            for (ReworkConfiguration rework : reworks) {
                rework.setIsDeleted(1);
                reworkConfigRepository.save(rework);
            }
            
            // 删除提醒配置
            List<ReminderConfiguration> reminders = reminderConfigRepository
                    .findByProcessDefinitionId(processDefinition.getId());
            for (ReminderConfiguration reminder : reminders) {
                reminder.setIsDeleted(1);
                reminderConfigRepository.save(reminder);
            }
            
            // 创建新的节点和关系
            createNodesAndRelationships(processDefinition.getId(), request.getNodes(), 
                    request.getTransitions(), request.getReworkConfigs(), request.getReminderConfigs());
        }
        
        return processDefinition.getId();
    }

    @Override
    public ProcessDefinitionDetailResponse getProcessDefinitionDetail(Long id) {
        // 查找流程定义
        ProcessDefinition processDefinition = processDefinitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_DEFINITION_NOT_FOUND));
        
        ProcessDefinitionDetailResponse response = new ProcessDefinitionDetailResponse();
        BeanUtils.copyProperties(processDefinition, response);
        
        // 查询节点定义
        List<ApprovalNodeDefinition> nodes = nodeDefinitionRepository.findByProcessDefinitionId(id);
        List<Long> nodeIds = nodes.stream().map(ApprovalNodeDefinition::getId).collect(Collectors.toList());
        
        // 查询节点审批人
        List<NodeApproverDefinition> allApprovers = nodeApproverRepository.findByNodeIdIn(nodeIds);
        
        // 按节点ID分组审批人
        Map<Long, List<NodeApproverDefinition>> approverMap = allApprovers.stream()
                .collect(Collectors.groupingBy(NodeApproverDefinition::getNodeId));
        
        // 转换为DTO
        List<NodeDefinitionDTO> nodeList = nodes.stream().map(node -> {
            NodeDefinitionDTO dto = new NodeDefinitionDTO();
            BeanUtils.copyProperties(node, dto);
            dto.setIsStartNode(node.isStartNode());
            dto.setIsEndNode(node.isEndNode());
            dto.setAllowApproverSelection(node.allowApproverSelection());
            
            // 设置审批人
            List<NodeApproverDefinition> approvers = approverMap.getOrDefault(node.getId(), new ArrayList<>());
            if (!approvers.isEmpty()) {
                List<NodeApproverDTO> approverDtos = approvers.stream().map(approver -> {
                    NodeApproverDTO approverDto = new NodeApproverDTO();
                    BeanUtils.copyProperties(approver, approverDto);
                    approverDto.setIsRequired(approver.isRequired());
                    return approverDto;
                }).collect(Collectors.toList());
                dto.setApprovers(approverDtos);
            }
            
            return dto;
        }).collect(Collectors.toList());
        
        response.setNodes(nodeList);
        
        // 查询转换关系
        List<NodeTransition> transitions = nodeTransitionRepository.findByProcessDefinitionId(id);
        
        // 转换为DTO
        List<TransitionDTO> transitionList = transitions.stream().map(transition -> {
            TransitionDTO dto = new TransitionDTO();
            BeanUtils.copyProperties(transition, dto);
            
            // 查找节点标识
            nodes.stream()
                 .filter(n -> n.getId().equals(transition.getSourceNodeId()))
                 .findFirst()
                 .ifPresent(n -> dto.setSourceNodeKey(n.getNodeKey()));
                 
            nodes.stream()
                 .filter(n -> n.getId().equals(transition.getTargetNodeId()))
                 .findFirst()
                 .ifPresent(n -> dto.setTargetNodeKey(n.getNodeKey()));
            
            return dto;
        }).collect(Collectors.toList());
        
        response.setTransitions(transitionList);
        
        // 查询重做配置
        List<ReworkConfiguration> reworks = reworkConfigRepository.findByProcessDefinitionId(id);
        
        // 转换为DTO
        List<ReworkConfigDTO> reworkList = reworks.stream().map(rework -> {
            ReworkConfigDTO dto = new ReworkConfigDTO();
            BeanUtils.copyProperties(rework, dto);
            
            // 查找节点标识
            nodes.stream()
                 .filter(n -> n.getId().equals(rework.getNodeId()))
                 .findFirst()
                 .ifPresent(n -> dto.setNodeKey(n.getNodeKey()));
            
            // 目标节点（如果有）
            if (rework.getTargetNodeId() != null) {
                nodes.stream()
                     .filter(n -> n.getId().equals(rework.getTargetNodeId()))
                     .findFirst()
                     .ifPresent(n -> dto.setTargetNodeKey(n.getNodeKey()));
            }
            
            dto.setAllowCommentRequired(rework.isCommentRequired());
            
            return dto;
        }).collect(Collectors.toList());
        
        response.setReworkConfigs(reworkList);
        
        // 查询提醒配置
        List<ReminderConfiguration> reminders = reminderConfigRepository.findByProcessDefinitionId(id);
        
        // 转换为DTO
        List<ReminderConfigDTO> reminderList = reminders.stream().map(reminder -> {
            ReminderConfigDTO dto = new ReminderConfigDTO();
            BeanUtils.copyProperties(reminder, dto);
            
            // 查找节点标识（如果有关联节点）
            if (reminder.getNodeId() != null) {
                nodes.stream()
                     .filter(n -> n.getId().equals(reminder.getNodeId()))
                     .findFirst()
                     .ifPresent(n -> dto.setNodeKey(n.getNodeKey()));
            }
            
            dto.setEnabled(reminder.isEnabled());
            
            return dto;
        }).collect(Collectors.toList());
        
        response.setReminderConfigs(reminderList);
        
        return response;
    }

    @Override
    public Page<ProcessDefinitionListResponse> getProcessDefinitionList(
            String processTypeCode, ProcessStatus status, String keyword, Pageable pageable) {
        Page<ProcessDefinition> page;
        
        // 根据查询条件调用不同的Repository方法
        if (StringUtils.hasText(keyword)) {
            page = processDefinitionRepository.searchByKeyword(keyword, pageable);
        } else if (StringUtils.hasText(processTypeCode) && status != null) {
            page = processDefinitionRepository.findByProcessTypeCodeAndStatus(processTypeCode, status, pageable);
        } else if (status != null) {
            page = processDefinitionRepository.findByStatus(status, pageable);
        } else {
            page = processDefinitionRepository.findAll(pageable);
        }
        
        // 转换为响应DTO
        return page.map(pd -> {
            ProcessDefinitionListResponse dto = new ProcessDefinitionListResponse();
            BeanUtils.copyProperties(pd, dto);
            
            // 查询节点数量
            long nodeCount = nodeDefinitionRepository.findByProcessDefinitionId(pd.getId()).size();
            dto.setNodeCount((int) nodeCount);
            
            return dto;
        });
    }

    @Override
    @Transactional
    public boolean updateProcessStatus(Long id, ProcessStatus status) {
        ProcessDefinition processDefinition = processDefinitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_DEFINITION_NOT_FOUND));
        
        // 检查状态转换是否合法
        validateStatusTransition(processDefinition.getStatus(), status);
        
        // 更新状态
        processDefinition.setStatus(status);
        
        // 设置更新元数据
        String currentUser = SecurityUtil.getCurrentUsername();
        processDefinition.setUpdatedBy(currentUser);
        processDefinition.setUpdatedTime(LocalDateTime.now());
        
        processDefinitionRepository.save(processDefinition);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteProcessDefinition(Long id) {
        ProcessDefinition processDefinition = processDefinitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_DEFINITION_NOT_FOUND));
        
        // 只有DISABLED状态的流程才能删除
        if (processDefinition.getStatus() != ProcessStatus.DISABLED) {
            throw new BusinessException(ErrorCode.PROCESS_STATUS_NOT_DELETABLE);
        }
        
        // 逻辑删除
        processDefinition.setIsDeleted(1);
        
        // 设置更新元数据
        String currentUser = SecurityUtil.getCurrentUsername();
        processDefinition.setUpdatedBy(currentUser);
        processDefinition.setUpdatedTime(LocalDateTime.now());
        
        processDefinitionRepository.save(processDefinition);
        return true;
    }

    /**
     * 创建节点和相关关系
     */
    private void createNodesAndRelationships(
            Long processDefinitionId, 
            List<NodeDefinitionDTO> nodeDtos, 
            List<TransitionDTO> transitionDtos,
            List<ReworkConfigDTO> reworkDtos,
            List<ReminderConfigDTO> reminderDtos) {
        
        String currentUser = SecurityUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();
        
        // 第一步：创建所有节点，获取ID
        Map<String, Long> nodeKeyToIdMap = new HashMap<>();
        
        for (NodeDefinitionDTO nodeDto : nodeDtos) {
            ApprovalNodeDefinition node = new ApprovalNodeDefinition();
            
            // 设置基本属性
            node.setId(idGenerator.nextId());
            node.setProcessDefinitionId(processDefinitionId);
            node.setNodeKey(nodeDto.getNodeKey());
            node.setNodeName(nodeDto.getNodeName());
            node.setNodeType(nodeDto.getNodeType());
            node.setApproverType(nodeDto.getApproverType());
            node.setApprovalStrategy(nodeDto.getApprovalStrategy());
            node.setTimeoutHours(nodeDto.getTimeoutHours());
            node.setIsStartNode(Boolean.TRUE.equals(nodeDto.getIsStartNode()) ? 1 : 0);
            node.setIsEndNode(Boolean.TRUE.equals(nodeDto.getIsEndNode()) ? 1 : 0);
            node.setAllowApproverSelection(Boolean.TRUE.equals(nodeDto.getAllowApproverSelection()) ? 1 : 0);
            node.setFormPermissions(nodeDto.getFormPermissions());
            
            // 设置元数据
            node.setCreatedBy(currentUser);
            node.setCreatedTime(now);
            
            // 保存节点获取ID
            node = nodeDefinitionRepository.save(node);
            nodeKeyToIdMap.put(nodeDto.getNodeKey(), node.getId());
            
            // 保存审批人定义
            if (nodeDto.getApprovers() != null) {
                for (NodeApproverDTO approverDto : nodeDto.getApprovers()) {
                    NodeApproverDefinition approver = new NodeApproverDefinition();
                    approver.setId(idGenerator.nextId());
                    approver.setNodeId(node.getId());
                    approver.setApproverType(approverDto.getApproverType());
                    approver.setApproverId(approverDto.getApproverId());
                    approver.setExpression(approverDto.getExpression());
                    approver.setDescription(approverDto.getDescription());
                    approver.setIsRequired(Boolean.TRUE.equals(approverDto.getIsRequired()) ? 1 : 0);
                    approver.setPriority(approverDto.getPriority());
                    
                    // 设置元数据
                    approver.setCreatedBy(currentUser);
                    approver.setCreatedTime(now);
                    
                    nodeApproverRepository.save(approver);
                }
            }
        }
        
        // 第二步：创建节点间转换关系
        if (transitionDtos != null) {
            for (TransitionDTO transitionDto : transitionDtos) {
                Long sourceNodeId = nodeKeyToIdMap.get(transitionDto.getSourceNodeKey());
                Long targetNodeId = nodeKeyToIdMap.get(transitionDto.getTargetNodeKey());
                
                if (sourceNodeId == null || targetNodeId == null) {
                    throw new BusinessException(ErrorCode.INVALID_NODE_KEY);
                }
                
                NodeTransition transition = new NodeTransition();
                transition.setId(idGenerator.nextId());
                transition.setProcessDefinitionId(processDefinitionId);
                transition.setSourceNodeId(sourceNodeId);
                transition.setTargetNodeId(targetNodeId);
                transition.setTransitionType(transitionDto.getTransitionType());
                transition.setConditionExpression(transitionDto.getConditionExpression());
                transition.setPriority(transitionDto.getPriority());
                transition.setDescription(transitionDto.getDescription());
                
                // 设置元数据
                transition.setCreatedBy(currentUser);
                transition.setCreatedTime(now);
                
                nodeTransitionRepository.save(transition);
            }
        }
        
        // 第三步：创建重做配置
        if (reworkDtos != null) {
            for (ReworkConfigDTO reworkDto : reworkDtos) {
                Long nodeId = nodeKeyToIdMap.get(reworkDto.getNodeKey());
                
                if (nodeId == null) {
                    throw new BusinessException(ErrorCode.INVALID_NODE_KEY);
                }
                
                ReworkConfiguration rework = new ReworkConfiguration();
                rework.setId(idGenerator.nextId());
                rework.setProcessDefinitionId(processDefinitionId);
                rework.setNodeId(nodeId);
                rework.setReworkType(reworkDto.getReworkType());
                
                if (reworkDto.getTargetNodeKey() != null) {
                    Long targetNodeId = nodeKeyToIdMap.get(reworkDto.getTargetNodeKey());
                    if (targetNodeId == null) {
                        throw new BusinessException(ErrorCode.INVALID_NODE_KEY);
                    }
                    rework.setTargetNodeId(targetNodeId);
                }
                
                rework.setAllowCommentRequired(Boolean.TRUE.equals(reworkDto.getAllowCommentRequired()) ? 1 : 0);
                rework.setDescription(reworkDto.getDescription());
                
                // 设置元数据
                rework.setCreatedBy(currentUser);
                rework.setCreatedTime(now);
                
                reworkConfigRepository.save(rework);
            }
        }
        
        // 第四步：创建提醒配置
        if (reminderDtos != null) {
            for (ReminderConfigDTO reminderDto : reminderDtos) {
                ReminderConfiguration reminder = new ReminderConfiguration();
                reminder.setId(idGenerator.nextId());
                reminder.setProcessDefinitionId(processDefinitionId);
                
                // 节点关联（可能为null表示全局配置）
                if (reminderDto.getNodeKey() != null) {
                    Long nodeId = nodeKeyToIdMap.get(reminderDto.getNodeKey());
                    if (nodeId == null) {
                        throw new BusinessException(ErrorCode.INVALID_NODE_KEY);
                    }
                    reminder.setNodeId(nodeId);
                }
                
                reminder.setReminderType(reminderDto.getReminderType());
                reminder.setTimeExpression(reminderDto.getTimeExpression());
                reminder.setReminderTemplateCode(reminderDto.getReminderTemplateCode());
                reminder.setEnabled(Boolean.TRUE.equals(reminderDto.getEnabled()) ? 1 : 0);
                reminder.setDescription(reminderDto.getDescription());
                
                // 设置元数据
                reminder.setCreatedBy(currentUser);
                reminder.setCreatedTime(now);
                
                reminderConfigRepository.save(reminder);
            }
        }
    }

    /**
     * 校验状态转换是否合法
     */
    private void validateStatusTransition(ProcessStatus currentStatus, ProcessStatus targetStatus) {
        // 流程状态转换规则
        switch (currentStatus) {
            case ACTIVE:
                // 激活状态只能转为禁用状态
                if (targetStatus != ProcessStatus.DISABLED) {
                    throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
                }
                break;
            case DISABLED:
                // 禁用状态可以转为激活或归档状态
                if (targetStatus != ProcessStatus.ACTIVE && targetStatus != ProcessStatus.ARCHIVED) {
                    throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
                }
                break;
            case ARCHIVED:
                // 归档状态不能转换
                throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
            default:
                throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}