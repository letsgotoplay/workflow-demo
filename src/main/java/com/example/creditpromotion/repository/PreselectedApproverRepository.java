package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.PreselectedApprover;

import java.util.List;

/**
 * 申请人预选审批人数据访问接口
 */
@Repository
public interface PreselectedApproverRepository extends JpaRepository<PreselectedApprover, Long> {

    /**
     * 根据流程实例ID查询预选审批人
     * 
     * @param processInstanceId 流程实例ID
     * @return 预选审批人列表
     */
    List<PreselectedApprover> findByProcessInstanceId(Long processInstanceId);

    /**
     * 根据流程实例ID和节点定义ID查询预选审批人
     * 
     * @param processInstanceId 流程实例ID
     * @param nodeDefinitionId 节点定义ID
     * @return 预选审批人列表
     */
    List<PreselectedApprover> findByProcessInstanceIdAndNodeDefinitionId(Long processInstanceId, Long nodeDefinitionId);
}