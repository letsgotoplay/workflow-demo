package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ReworkConfiguration;

import java.util.List;

/**
 * 重做配置数据访问接口
 */
@Repository
public interface ReworkConfigurationRepository extends JpaRepository<ReworkConfiguration, Long> {

    /**
     * 根据流程定义ID查询重做配置
     * 
     * @param processDefinitionId 流程定义ID
     * @return 重做配置列表
     */
    List<ReworkConfiguration> findByProcessDefinitionId(Long processDefinitionId);

    /**
     * 根据节点ID查询重做配置
     * 
     * @param nodeId 节点ID
     * @return 重做配置列表
     */
    List<ReworkConfiguration> findByNodeId(Long nodeId);
}