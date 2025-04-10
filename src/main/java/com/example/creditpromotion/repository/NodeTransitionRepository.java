package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.NodeTransition;
import com.example.creditpromotion.enums.TransitionType;

import java.util.List;

/**
 * 节点转换关系数据访问接口
 */
@Repository
public interface NodeTransitionRepository extends JpaRepository<NodeTransition, Long> {

    /**
     * 根据流程定义ID查询所有转换关系
     * 
     * @param processDefinitionId 流程定义ID
     * @return 转换关系列表
     */
    List<NodeTransition> findByProcessDefinitionId(Long processDefinitionId);

    /**
     * 根据源节点ID查询所有转换关系
     * 
     * @param sourceNodeId 源节点ID
     * @return 转换关系列表
     */
    List<NodeTransition> findBySourceNodeId(Long sourceNodeId);

    /**
     * 根据源节点ID和转换类型查询转换关系
     * 
     * @param sourceNodeId 源节点ID
     * @param transitionType 转换类型
     * @return 转换关系列表
     */
    List<NodeTransition> findBySourceNodeIdAndTransitionType(Long sourceNodeId, TransitionType transitionType);

    /**
     * 根据源节点ID查询并按优先级排序
     * 
     * @param sourceNodeId 源节点ID
     * @return 排序后的转换关系列表
     */
    List<NodeTransition> findBySourceNodeIdOrderByPriorityAsc(Long sourceNodeId);
}