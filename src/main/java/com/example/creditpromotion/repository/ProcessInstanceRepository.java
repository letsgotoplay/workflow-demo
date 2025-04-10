package com.example.creditpromotion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ProcessInstance;
import com.example.creditpromotion.enums.ProcessInstanceStatus;

import java.util.List;
import java.util.Optional;

/**
 * 流程实例数据访问接口
 */
@Repository
public interface ProcessInstanceRepository extends JpaRepository<ProcessInstance, Long> {

    /**
     * 根据流程实例编号查询
     * 
     * @param processNo 流程实例编号
     * @return 流程实例对象
     */
    Optional<ProcessInstance> findByProcessNo(String processNo);

    /**
     * 根据申请人ID查询流程实例分页列表
     * 
     * @param applyUserId 申请人ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProcessInstance> findByApplyUserId(Long applyUserId, Pageable pageable);

    /**
     * 根据申请人ID和状态查询流程实例分页列表
     * 
     * @param applyUserId 申请人ID
     * @param status 流程实例状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProcessInstance> findByApplyUserIdAndStatus(Long applyUserId, ProcessInstanceStatus status, Pageable pageable);

    /**
     * 根据流程定义ID查询流程实例分页列表
     * 
     * @param processDefinitionId 流程定义ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProcessInstance> findByProcessDefinitionId(Long processDefinitionId, Pageable pageable);

    /**
     * 查询用户申请的且为重做状态的流程实例
     * 
     * @param applyUserId 申请人ID
     * @return 流程实例列表
     */
    List<ProcessInstance> findByApplyUserIdAndStatus(Long applyUserId, ProcessInstanceStatus status);

    /**
     * 模糊查询流程实例
     * 
     * @param keyword 关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT pi FROM ProcessInstance pi WHERE (pi.processNo LIKE %:keyword% OR pi.employeeName LIKE %:keyword%)")
    Page<ProcessInstance> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}