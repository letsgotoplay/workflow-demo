package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ProcessDefinition;
import com.example.creditpromotion.entity.ProcessNodeInstance;
import com.example.creditpromotion.enums.NodeInstanceStatus;

import java.util.List;
import java.util.Optional;

/**
 * 流程节点实例数据访问接口
 */
@Repository
public interface ProcessNodeInstanceRepository extends JpaRepository<ProcessNodeInstance, Long> {

    /**
     * 根据流程实例ID查询节点实例列表
     * 
     * @param processInstanceId 流程实例ID
     * @return 节点实例列表
     */
    List<ProcessNodeInstance> findByProcessInstanceId(Long processInstanceId);

    /**
     * 根据流程实例ID查询当前活动节点实例
     * 
     * @param processInstanceId 流程实例ID
     * @param nodeStatus 节点状态
     * @return 节点实例列表
     */
    List<ProcessNodeInstance> findByProcessInstanceIdAndNodeStatus(Long processInstanceId, NodeInstanceStatus nodeStatus);

    /**
     * 根据父节点实例ID查询子节点实例
     * 
     * @param parentNodeInstanceId 父节点实例ID
     * @return 子节点实例列表
     */
    List<ProcessNodeInstance> findByParentNodeInstanceId(Long parentNodeInstanceId);

    /**
     * 根据节点定义ID查询节点实例
     * 
     * @param nodeDefinitionId 节点定义ID
     * @return 节点实例列表
     */
    List<ProcessNodeInstance> findByNodeDefinitionId(Long nodeDefinitionId);

    Optional<ProcessNodeInstance> findByProcessInstanceIdAndNodeDefinitionId(Long instanceID, Long definitionID);

    Optional<ProcessNodeInstance> findByProcessInstanceIdAndNodeDefinitionIdAndNodeStatus(Long instanceID, Long currentNodeDefId,
            NodeInstanceStatus inProgress);
}