package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ReminderConfiguration;

import java.util.List;

/**
 * 提醒配置数据访问接口
 */
@Repository
public interface ReminderConfigurationRepository extends JpaRepository<ReminderConfiguration, Long> {

    /**
     * 根据流程定义ID查询提醒配置
     * 
     * @param processDefinitionId 流程定义ID
     * @return 提醒配置列表
     */
    List<ReminderConfiguration> findByProcessDefinitionId(Long processDefinitionId);

    /**
     * 根据节点ID查询提醒配置
     * 
     * @param nodeId 节点ID
     * @return 提醒配置列表
     */
    List<ReminderConfiguration> findByNodeId(Long nodeId);

    /**
     * 查询全局提醒配置（节点ID为null）
     * 
     * @param processDefinitionId 流程定义ID
     * @return 提醒配置列表
     */
    List<ReminderConfiguration> findByProcessDefinitionIdAndNodeIdIsNull(Long processDefinitionId);
}