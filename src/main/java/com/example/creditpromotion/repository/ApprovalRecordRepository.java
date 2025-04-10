package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ApprovalRecord;

import java.util.List;

/**
 * 审批记录数据访问接口
 */
@Repository
public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {

    /**
     * 根据流程实例ID查询审批记录
     * 
     * @param processInstanceId 流程实例ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> findByProcessInstanceId(Long processInstanceId);

    /**
     * 根据节点实例ID查询审批记录
     * 
     * @param nodeInstanceId 节点实例ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> findByNodeInstanceId(Long nodeInstanceId);

    /**
     * 根据审批人实例ID查询审批记录
     * 
     * @param approverInstanceId 审批人实例ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> findByApproverInstanceId(Long approverInstanceId);

    /**
     * 根据流程实例ID查询并按操作时间排序
     * 
     * @param processInstanceId 流程实例ID
     * @return 排序后的审批记录列表
     */
    List<ApprovalRecord> findByProcessInstanceIdOrderByActionTimeDesc(Long processInstanceId);
}