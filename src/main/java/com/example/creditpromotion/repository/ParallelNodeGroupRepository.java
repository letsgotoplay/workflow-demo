package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ParallelNodeGroup;

import java.util.List;

/**
 * 并行节点组关系数据访问接口
 */
@Repository
public interface ParallelNodeGroupRepository extends JpaRepository<ParallelNodeGroup, Long> {

    /**
     * 根据流程定义ID查询并行节点组
     * 
     * @param processDefinitionId 流程定义ID
     * @return 并行节点组列表
     */
    List<ParallelNodeGroup> findByProcessDefinitionId(Long processDefinitionId);

    /**
     * 根据父节点ID查询并行节点组
     * 
     * @param parentNodeId 父节点ID
     * @return 并行节点组列表
     */
    List<ParallelNodeGroup> findByParentNodeId(Long parentNodeId);

    /**
     * 根据组标识查询并行节点组
     * 
     * @param groupKey 组标识
     * @return 并行节点组列表
     */
    List<ParallelNodeGroup> findByGroupKey(String groupKey);

    /**
     * 根据子节点ID查询并行节点组
     * 
     * @param childNodeId 子节点ID
     * @return 并行节点组列表
     */
    List<ParallelNodeGroup> findByChildNodeId(Long childNodeId);
}