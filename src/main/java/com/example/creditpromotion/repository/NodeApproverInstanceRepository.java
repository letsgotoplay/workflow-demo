package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.NodeApproverInstance;
import com.example.creditpromotion.entity.ProcessDefinition;
import com.example.creditpromotion.enums.ApprovalStatus;

import java.util.List;
import java.util.Optional;

/**
 * 节点审批人实例数据访问接口
 */
@Repository
public interface NodeApproverInstanceRepository extends JpaRepository<NodeApproverInstance, Long> {

    /**
     * 根据节点实例ID查询审批人实例
     * 
     * @param nodeInstanceId 节点实例ID
     * @return 审批人实例列表
     */
    List<NodeApproverInstance> findByNodeInstanceId(Long nodeInstanceId);

    /**
     * 根据审批人ID查询待处理的审批任务
     * 
     * @param approverId 审批人ID
     * @param approvalStatus 审批状态
     * @return 审批人实例列表
     */
    List<NodeApproverInstance> findByApproverIdAndApprovalStatus(Long approverId, ApprovalStatus approvalStatus);

    /**
     * 根据节点实例ID列表批量查询审批人实例
     * 
     * @param nodeInstanceIds 节点实例ID列表
     * @return 审批人实例列表
     */
    List<NodeApproverInstance> findByNodeInstanceIdIn(List<Long> nodeInstanceIds);

    /**
     * 查询用户待处理的审批任务
     * 
     * @param approverId 审批人ID
     * @return 审批人实例列表
     */
    @Query("SELECT nai FROM NodeApproverInstance nai JOIN ProcessNodeInstance pni ON nai.nodeInstanceId = pni.id " +
           "WHERE nai.approverId = :approverId AND nai.approvalStatus = 'PENDING' AND pni.nodeStatus = 'IN_PROGRESS'")
    List<NodeApproverInstance> findPendingTasksByApproverId(@Param("approverId") Long approverId);

    Optional<NodeApproverInstance> findByNodeInstanceIdAndApproverId(Long id, Long currentUserId);

    Optional<ProcessDefinition> findByNodeInstanceIdAndApproverIdAndApprovalStatus(Long id, Long currentUserId,
            ApprovalStatus pending);

    List<NodeApproverInstance> findByNodeInstanceIdAndApprovalStatus(Long id, ApprovalStatus status);
}