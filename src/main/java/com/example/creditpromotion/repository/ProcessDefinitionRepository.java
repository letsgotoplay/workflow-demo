package com.example.creditpromotion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ProcessDefinition;
import com.example.creditpromotion.enums.ProcessStatus;

import java.util.Optional;

/**
 * 流程定义数据访问接口
 */
@Repository
public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinition, Long> {

    /**
     * 根据流程编码查询流程定义
     * 
     * @param processCode 流程编码
     * @return 流程定义对象
     */
    Optional<ProcessDefinition> findByProcessCode(String processCode);

    /**
     * 根据状态查询流程定义分页列表
     * 
     * @param status 流程状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProcessDefinition> findByStatus(ProcessStatus status, Pageable pageable);

    /**
     * 根据流程类型和状态查询流程定义分页列表
     * 
     * @param processTypeCode 流程类型代码
     * @param status 流程状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProcessDefinition> findByProcessTypeCodeAndStatus(String processTypeCode, ProcessStatus status, Pageable pageable);

    /**
     * 模糊查询流程定义
     * 
     * @param keyword 关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT pd FROM ProcessDefinition pd WHERE (pd.processCode LIKE %:keyword% OR pd.processName LIKE %:keyword% OR pd.description LIKE %:keyword%)")
    Page<ProcessDefinition> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}