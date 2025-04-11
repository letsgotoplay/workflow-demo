package com.example.creditpromotion.service.impl;

import com.example.creditpromotion.dto.response.ProcessStepperResponse;
import com.example.creditpromotion.dto.response.ProcessStepperResponse.ApproverInfo;
import com.example.creditpromotion.dto.response.ProcessStepperResponse.StepDetail;
import com.example.creditpromotion.dto.response.ProcessStepperResponse.StepInfo;
import com.example.creditpromotion.entity.ApprovalNodeDefinition;
import com.example.creditpromotion.entity.ApprovalRecord;
import com.example.creditpromotion.entity.NodeApproverInstance;
import com.example.creditpromotion.entity.NodeTransition;
import com.example.creditpromotion.entity.ParallelNodeGroup;
import com.example.creditpromotion.entity.ProcessInstance;
import com.example.creditpromotion.entity.ProcessNodeInstance;
import com.example.creditpromotion.enums.ApprovalStatus;
import com.example.creditpromotion.enums.ApprovalStrategy;
import com.example.creditpromotion.enums.NodeInstanceStatus;
import com.example.creditpromotion.enums.NodeType;
import com.example.creditpromotion.enums.TransitionType;
import com.example.creditpromotion.exception.BusinessException;
import com.example.creditpromotion.exception.ErrorCode;
import com.example.creditpromotion.repository.ApprovalNodeDefinitionRepository;
import com.example.creditpromotion.repository.ApprovalRecordRepository;
import com.example.creditpromotion.repository.NodeApproverInstanceRepository;
import com.example.creditpromotion.repository.NodeTransitionRepository;
import com.example.creditpromotion.repository.ParallelNodeGroupRepository;
import com.example.creditpromotion.repository.ProcessDefinitionRepository;
import com.example.creditpromotion.repository.ProcessInstanceRepository;
import com.example.creditpromotion.repository.ProcessNodeInstanceRepository;
import com.example.creditpromotion.service.ProcessStepperService;
import com.example.creditpromotion.service.SpelExpressionService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程步骤展示服务实现类
 */
@Service
@RequiredArgsConstructor
public class ProcessStepperServiceImpl implements ProcessStepperService {

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessDefinitionRepository processDefinitionRepository;
    private final ProcessNodeInstanceRepository nodeInstanceRepository;
    private final ApprovalNodeDefinitionRepository nodeDefinitionRepository;
    private final NodeApproverInstanceRepository approverInstanceRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final NodeTransitionRepository nodeTransitionRepository;
    private final ParallelNodeGroupRepository parallelNodeGroupRepository;
    private final SpelExpressionService spelExpressionService;

    @Override
    public ProcessStepperResponse getProcessSteps(Long processInstanceId) {
        // 获取流程实例
        ProcessInstance instance = processInstanceRepository.findById(processInstanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));

        // 获取流程定义
        var processDefinition = processDefinitionRepository.findById(instance.getProcessDefinitionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_DEFINITION_NOT_FOUND));

        // 获取所有节点定义
        List<ApprovalNodeDefinition> allNodeDefinitions = nodeDefinitionRepository
                .findByProcessDefinitionId(instance.getProcessDefinitionId());
        Map<Long, ApprovalNodeDefinition> nodeDefinitionMap = allNodeDefinitions.stream()
                .collect(Collectors.toMap(ApprovalNodeDefinition::getId, node -> node));

        // 获取所有节点实例
        List<ProcessNodeInstance> allNodeInstances = nodeInstanceRepository.findByProcessInstanceId(processInstanceId);
        Map<Long, ProcessNodeInstance> nodeInstanceByDefIdMap = allNodeInstances.stream()
                .collect(Collectors.toMap(ProcessNodeInstance::getNodeDefinitionId, node -> node, (existing, replacement) -> existing));

        // 获取所有节点转换关系
        List<NodeTransition> transitions = nodeTransitionRepository
                .findByProcessDefinitionId(instance.getProcessDefinitionId());
        Map<Long, List<NodeTransition>> transitionsBySourceNodeId = transitions.stream()
                .collect(Collectors.groupingBy(NodeTransition::getSourceNodeId));

        // 获取所有并行节点组
        List<ParallelNodeGroup> parallelGroups = parallelNodeGroupRepository
                .findByProcessDefinitionId(instance.getProcessDefinitionId());
        Map<Long, List<ParallelNodeGroup>> parallelGroupsByParentId = parallelGroups.stream()
                .collect(Collectors.groupingBy(ParallelNodeGroup::getParentNodeId));

        // 构建步骤列表
        List<StepInfo> steps = buildStepperDisplay(
                allNodeDefinitions, 
                nodeDefinitionMap, 
                nodeInstanceByDefIdMap, 
                transitionsBySourceNodeId, 
                parallelGroupsByParentId);

        // 构建响应
        ProcessStepperResponse response = new ProcessStepperResponse();
        response.setProcessInstanceId(instance.getId());
        response.setProcessNo(instance.getProcessNo());
        response.setProcessName(processDefinition.getProcessName());
        response.setStatus(instance.getStatus().name());
        response.setSubmitTime(instance.getApplyTime());
        response.setSubmitter(instance.getApplyUserName());
        response.setSteps(steps);

        return response;
    }

    /**
     * 构建步骤展示列表
     */
    private List<StepInfo> buildStepperDisplay(
            List<ApprovalNodeDefinition> allNodeDefinitions,
            Map<Long, ApprovalNodeDefinition> nodeDefinitionMap,
            Map<Long, ProcessNodeInstance> nodeInstanceByDefIdMap,
            Map<Long, List<NodeTransition>> transitionsBySourceNodeId,
            Map<Long, List<ParallelNodeGroup>> parallelGroupsByParentId) {

        List<StepInfo> result = new ArrayList<>();
        
        // 找到开始节点（表单提交节点）
        ApprovalNodeDefinition startNodeDef = allNodeDefinitions.stream()
                .filter(ApprovalNodeDefinition::isStartNode)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_DEFINITION_NOT_FOUND, "流程定义中未找到开始节点"));

        // 创建已访问节点集合，防止循环
        Set<Long> visitedNodeDefIds = new HashSet<>();
        
        // 从开始节点开始构建步骤
        buildStepsFromNode(
                startNodeDef.getId(),
                result,
                nodeDefinitionMap,
                nodeInstanceByDefIdMap,
                transitionsBySourceNodeId,
                parallelGroupsByParentId,
                visitedNodeDefIds,
                1); // 从序号1开始
        
        return result;
    }

    /**
     * 从指定节点开始构建步骤
     * @param nodeDefId 节点定义ID
     * @param result 结果列表
     * @param nodeDefinitionMap 节点定义映射
     * @param nodeInstanceByDefIdMap 节点实例映射
     * @param transitionsBySourceNodeId 节点转换关系映射
     * @param parallelGroupsByParentId 并行节点组映射
     * @param visitedNodeDefIds 已访问节点集合
     * @param order 步骤序号
     * @return 下一个步骤序号
     */
    private int buildStepsFromNode(
            Long nodeDefId,
            List<StepInfo> result,
            Map<Long, ApprovalNodeDefinition> nodeDefinitionMap,
            Map<Long, ProcessNodeInstance> nodeInstanceByDefIdMap,
            Map<Long, List<NodeTransition>> transitionsBySourceNodeId,
            Map<Long, List<ParallelNodeGroup>> parallelGroupsByParentId,
            Set<Long> visitedNodeDefIds,
            int order) {
        
        // 防止循环
        if (visitedNodeDefIds.contains(nodeDefId)) {
            return order;
        }
        visitedNodeDefIds.add(nodeDefId);
        
        // 获取节点定义
        ApprovalNodeDefinition nodeDef = nodeDefinitionMap.get(nodeDefId);
        if (nodeDef == null) {
            return order;
        }
        
        // 获取节点实例（如果存在）
        ProcessNodeInstance nodeInstance = nodeInstanceByDefIdMap.get(nodeDefId);
        
        // 根据节点类型处理
        switch (nodeDef.getNodeType()) {
            case CONDITIONAL:
                // 条件节点不显示，直接处理后续节点
                return processConditionalNode(
                        nodeDef, 
                        nodeInstance, 
                        result, 
                        nodeDefinitionMap, 
                        nodeInstanceByDefIdMap, 
                        transitionsBySourceNodeId, 
                        parallelGroupsByParentId, 
                        visitedNodeDefIds, 
                        order);
                
            case PARALLEL:
                // 处理并行节点
                return processParallelNode(
                        nodeDef, 
                        nodeInstance, 
                        result, 
                        nodeDefinitionMap, 
                        nodeInstanceByDefIdMap, 
                        transitionsBySourceNodeId, 
                        parallelGroupsByParentId, 
                        visitedNodeDefIds, 
                        order);
                
            case NORMAL:
            default:
                // 处理普通节点
                return processNormalNode(
                        nodeDef, 
                        nodeInstance, 
                        result, 
                        nodeDefinitionMap, 
                        nodeInstanceByDefIdMap, 
                        transitionsBySourceNodeId, 
                        parallelGroupsByParentId, 
                        visitedNodeDefIds, 
                        order);
        }
    }

    /**
     * 处理条件节点
     */
    private int processConditionalNode(
            ApprovalNodeDefinition nodeDef,
            ProcessNodeInstance nodeInstance,
            List<StepInfo> result,
            Map<Long, ApprovalNodeDefinition> nodeDefinitionMap,
            Map<Long, ProcessNodeInstance> nodeInstanceByDefIdMap,
            Map<Long, List<NodeTransition>> transitionsBySourceNodeId,
            Map<Long, List<ParallelNodeGroup>> parallelGroupsByParentId,
            Set<Long> visitedNodeDefIds,
            int order) {
        
        // 创建条件节点步骤（不跳过条件节点）
        StepInfo conditionalStep = createStepInfo(nodeDef, nodeInstance, order++);
        conditionalStep.setIsConditional(true);
        result.add(conditionalStep);
        
        // 获取条件节点的所有转换
        List<NodeTransition> transitions = transitionsBySourceNodeId.getOrDefault(nodeDef.getId(), new ArrayList<>());
        
        // 按优先级排序条件转换
        transitions.sort(Comparator.comparing(NodeTransition::getPriority, Comparator.nullsLast(Comparator.naturalOrder())));
        
        // 如果有节点实例，说明条件已经执行过，找到实际执行的路径
        if (nodeInstance != null) {
            // 查找下一个已执行的节点
            for (NodeTransition transition : transitions) {
                ProcessNodeInstance nextInstance = nodeInstanceByDefIdMap.get(transition.getTargetNodeId());
                if (nextInstance != null) {
                    // 找到了已执行的下一个节点，继续构建
                    return buildStepsFromNode(
                            transition.getTargetNodeId(),
                            result,
                            nodeDefinitionMap,
                            nodeInstanceByDefIdMap,
                            transitionsBySourceNodeId,
                            parallelGroupsByParentId,
                            visitedNodeDefIds,
                            order);
                }
            }
        }
        
        // 如果没有找到已执行的路径，按条件评估选择路径
        NodeTransition selectedTransition = null;
        
        // 遍历所有条件转换
        for (NodeTransition transition : transitions) {
            if (transition.getTransitionType() == TransitionType.CONDITIONAL) {
                // 评估条件表达式
                boolean conditionResult = evaluateCondition(transition.getConditionExpression(), nodeInstance.getProcessInstanceId());
                if (conditionResult) {
                    // 找到第一个条件为true的转换
                    selectedTransition = transition;
                    break;
                }
            }
        }
        
        // 如果没有条件为true的转换，查找普通转换（通常是默认路径）
        if (selectedTransition == null) {
            selectedTransition = transitions.stream()
                    .filter(t -> t.getTransitionType() == TransitionType.NORMAL)
                    .findFirst()
                    .orElse(null);
        }
        
        // 如果找到了转换，继续构建
        if (selectedTransition != null) {
            return buildStepsFromNode(
                    selectedTransition.getTargetNodeId(),
                    result,
                    nodeDefinitionMap,
                    nodeInstanceByDefIdMap,
                    transitionsBySourceNodeId,
                    parallelGroupsByParentId,
                    visitedNodeDefIds,
                    order);
        }
        
        return order;
    }
    
    /**
     * 评估条件表达式
     * @param conditionExpression 条件表达式
     * @param processInstanceId 流程实例ID
     * @return 评估结果
     */
    private boolean evaluateCondition(String conditionExpression, Long processInstanceId) {
        if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
            return true; // 空表达式视为真
        }
        
        try {
            // 获取流程实例相关数据作为表达式变量
            Map<String, Object> variables = getProcessVariables(processInstanceId);
            
            // 使用 SpelExpressionService 评估表达式
            return spelExpressionService.evaluateBoolean(conditionExpression, variables);
        } catch (Exception e) {
            // 记录异常
            //log.error("条件表达式评估失败: {}", conditionExpression, e);
            return false;
        }
    }
    
    /**
     * 获取流程实例相关变量
     * @param processInstanceId 流程实例ID
     * @return 变量映射
     */
    private Map<String, Object> getProcessVariables(Long processInstanceId) {
        Map<String, Object> variables = new HashMap<>();
        
        // 获取流程实例
        ProcessInstance instance = processInstanceRepository.findById(processInstanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND));
        
        // 添加流程实例基本信息
        variables.put("processInstanceId", instance.getId());
        variables.put("processNo", instance.getProcessNo());
        variables.put("applyUserId", instance.getApplyUserId());
        variables.put("applyUserName", instance.getApplyUserName());
        variables.put("status", instance.getStatus());
        
        // TODO: 根据业务需求添加更多变量
        // 例如：表单数据、业务数据等
        
        return variables;
    }

    /**
     * 处理并行节点
     */
    private int processParallelNode(
            ApprovalNodeDefinition nodeDef,
            ProcessNodeInstance nodeInstance,
            List<StepInfo> result,
            Map<Long, ApprovalNodeDefinition> nodeDefinitionMap,
            Map<Long, ProcessNodeInstance> nodeInstanceByDefIdMap,
            Map<Long, List<NodeTransition>> transitionsBySourceNodeId,
            Map<Long, List<ParallelNodeGroup>> parallelGroupsByParentId,
            Set<Long> visitedNodeDefIds,
            int order) {
        
        // 创建并行节点步骤
        StepInfo parallelStep = createStepInfo(nodeDef, nodeInstance, order++);
        parallelStep.setIsParallel(true);
        parallelStep.setApprovalStrategy(nodeDef.getApprovalStrategy().name());
        
        // 设置详细信息
        StepDetail detail = parallelStep.getDetail();
        detail.setDescription("并行审批节点: " + 
                (nodeDef.getApprovalStrategy() == ApprovalStrategy.ALL ? "所有人必须通过" : "任一人通过即可"));
        
        // 添加到结果
        result.add(parallelStep);
        
        // 处理并行子节点
        List<ParallelNodeGroup> childGroups = parallelGroupsByParentId.getOrDefault(nodeDef.getId(), new ArrayList<>());
        
        // 创建临时的访问集合，避免影响主流程
        Set<Long> tempVisited = new HashSet<>(visitedNodeDefIds);
        
        // 处理每个子节点
        for (ParallelNodeGroup group : childGroups) {
            buildStepsFromNode(
                    group.getChildNodeId(),
                    result,
                    nodeDefinitionMap,
                    nodeInstanceByDefIdMap,
                    transitionsBySourceNodeId,
                    parallelGroupsByParentId,
                    tempVisited,
                    order);
        }
        
        // 处理并行节点之后的节点
        List<NodeTransition> outTransitions = transitionsBySourceNodeId.getOrDefault(nodeDef.getId(), new ArrayList<>())
                .stream()
                .filter(t -> t.getTransitionType() == TransitionType.NORMAL)
                .collect(Collectors.toList());
        
        if (!outTransitions.isEmpty()) {
            // 选择第一个正常转换
            return buildStepsFromNode(
                    outTransitions.get(0).getTargetNodeId(),
                    result,
                    nodeDefinitionMap,
                    nodeInstanceByDefIdMap,
                    transitionsBySourceNodeId,
                    parallelGroupsByParentId,
                    visitedNodeDefIds,
                    order);
        }
        
        return order;
    }

    /**
     * 处理普通节点
     */
    private int processNormalNode(
            ApprovalNodeDefinition nodeDef,
            ProcessNodeInstance nodeInstance,
            List<StepInfo> result,
            Map<Long, ApprovalNodeDefinition> nodeDefinitionMap,
            Map<Long, ProcessNodeInstance> nodeInstanceByDefIdMap,
            Map<Long, List<NodeTransition>> transitionsBySourceNodeId,
            Map<Long, List<ParallelNodeGroup>> parallelGroupsByParentId,
            Set<Long> visitedNodeDefIds,
            int order) {
        
        // 创建普通节点步骤
        StepInfo normalStep = createStepInfo(nodeDef, nodeInstance, order++);
        result.add(normalStep);
        
        // 处理后续节点
        List<NodeTransition> outTransitions = transitionsBySourceNodeId.getOrDefault(nodeDef.getId(), new ArrayList<>())
                .stream()
                .filter(t -> t.getTransitionType() == TransitionType.NORMAL)
                .collect(Collectors.toList());
        
        if (!outTransitions.isEmpty()) {
            // 选择第一个正常转换
            return buildStepsFromNode(
                    outTransitions.get(0).getTargetNodeId(),
                    result,
                    nodeDefinitionMap,
                    nodeInstanceByDefIdMap,
                    transitionsBySourceNodeId,
                    parallelGroupsByParentId,
                    visitedNodeDefIds,
                    order);
        }
        
        return order;
    }

    /**
     * 创建步骤信息
     */
    private StepInfo createStepInfo(ApprovalNodeDefinition nodeDef, ProcessNodeInstance nodeInstance, int order) {
        StepInfo step = new StepInfo();
        step.setStepId(nodeDef.getId());
        step.setStepName(nodeDef.getNodeName());
        step.setStepOrder(order);
        step.setIsParallel(nodeDef.getNodeType() == NodeType.PARALLEL);
        
        // 设置步骤状态和时间
        if (nodeInstance != null) {
            step.setStartTime(nodeInstance.getStartTime());
            step.setEndTime(nodeInstance.getEndTime());
            
            if (nodeInstance.getNodeStatus() == NodeInstanceStatus.APPROVED || 
                    nodeInstance.getNodeStatus() == NodeInstanceStatus.REJECTED) {
                step.setStatus("COMPLETED");
            } else if (nodeInstance.getNodeStatus() == NodeInstanceStatus.IN_PROGRESS) {
                step.setStatus("IN_PROGRESS");
            } else {
                step.setStatus("PENDING");
            }
        } else {
            step.setStatus("NOT_STARTED");
        }
        
        // 设置步骤详细信息
        StepDetail detail = new StepDetail();
        
        if (nodeInstance != null) {
            detail.setDueTime(nodeInstance.getDueTime());
            
            // 获取审批记录
            List<ApprovalRecord> records = approvalRecordRepository.findByNodeInstanceId(nodeInstance.getId());
            if (!records.isEmpty()) {
                // 获取最后一条审批记录的意见
                ApprovalRecord lastRecord = records.get(records.size() - 1);
                detail.setComments(lastRecord.getActionComment());
            }
            
            // 获取审批人信息
            List<NodeApproverInstance> approvers = approverInstanceRepository.findByNodeInstanceId(nodeInstance.getId());
            if (!approvers.isEmpty()) {
                detail.setApprovers(approvers.stream()
                        .map(this::convertToApproverInfo)
                        .collect(Collectors.toList()));
            }
        }
        
        // 设置描述信息
        if (nodeDef.isStartNode()) {
            detail.setDescription("表单提交");
        } else if (step.getStatus().equals("COMPLETED")) {
            detail.setDescription("已完成审批");
        } else if (step.getStatus().equals("IN_PROGRESS")) {
            detail.setDescription("正在审批中");
            detail.setDescription(detail.getDescription() + 
                    (nodeDef.getApprovalStrategy() == ApprovalStrategy.ALL ? "（所有人必须通过）" : "（任一人通过即可）"));
        } else if (step.getStatus().equals("PENDING")) {
            detail.setDescription("等待审批");
        } else {
            detail.setDescription("未开始");
        }
        
        step.setDetail(detail);
        return step;
    }

    /**
     * 将审批人实例转换为审批人信息
     */
    private ApproverInfo convertToApproverInfo(NodeApproverInstance approver) {
        ApproverInfo info = new ApproverInfo();
        info.setApproverId(approver.getApproverId());
        info.setApproverName(approver.getApproverName());
        info.setApproverRole(approver.getApproverType().name());
        info.setApprovalStatus(approver.getApprovalStatus().name());
        info.setActionTime(approver.getActionTime());
        info.setComments(approver.getComments());
        return info;
    }
}