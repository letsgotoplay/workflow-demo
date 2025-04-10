package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.NodeApproverDefinition;

import java.util.List;

/**
 * 节点审批人定义数据访问接口
 */
@Repository
public interface NodeApproverDefinitionRepository extends JpaRepository<NodeApproverDefinition, Long> {

    /**
     * 根据节点ID查询审批人定义
     * 
     * @param nodeId 节点ID
     * @return 审批人定义列表
     */
    List<NodeApproverDefinition> findByNodeId(Long nodeId);

    /**
     * 根据节点ID列表批量查询审批人定义
     * 
     * @param nodeIds 节点ID列表
     * @return 审批人定义列表
     */
    List<NodeApproverDefinition> findByNodeIdIn(List<Long> nodeIds);
}