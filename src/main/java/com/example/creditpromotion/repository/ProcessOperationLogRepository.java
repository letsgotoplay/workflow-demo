package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ProcessOperationLog;

import java.util.List;

/**
 * 流程操作日志数据访问接口
 */
@Repository
public interface ProcessOperationLogRepository extends JpaRepository<ProcessOperationLog, Long> {

    /**
     * 根据流程实例ID查询操作日志
     * 
     * @param processInstanceId 流程实例ID
     * @return 操作日志列表
     */
    List<ProcessOperationLog> findByProcessInstanceId(Long processInstanceId);

    /**
     * 根据流程实例ID查询并按操作时间排序
     * 
     * @param processInstanceId 流程实例ID
     * @return 排序后的操作日志列表
     */
    List<ProcessOperationLog> findByProcessInstanceIdOrderByOperationTimeDesc(Long processInstanceId);
}