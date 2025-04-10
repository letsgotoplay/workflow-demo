package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ApprovalNodeDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 审批节点定义数据访问接口
 */
@Repository
public interface ApprovalNodeDefinitionRepository extends JpaRepository<ApprovalNodeDefinition, Long> {

    /**
     * 根据流程定义ID查询所有节点
     * 
     * @param processDefinitionId 流程定义ID
     * @return 节点列表
     */
    List<ApprovalNodeDefinition> findByProcessDefinitionId(Long processDefinitionId);

    /**
     * 根据流程定义ID和节点标识查询节点
     * 
     * @param processDefinitionId 流程定义ID
     * @param nodeKey 节点标识
     * @return 节点对象
     */
    Optional<ApprovalNodeDefinition> findByProcessDefinitionIdAndNodeKey(Long processDefinitionId, String nodeKey);

    /**
     * 根据流程定义ID查询开始节点
     * 
     * @param processDefinitionId 流程定义ID
     * @param isStartNode 是否开始节点 (1表示是)
     * @return 开始节点对象
     */
    Optional<ApprovalNodeDefinition> findByProcessDefinitionIdAndIsStartNode(Long processDefinitionId, Integer isStartNode);
}