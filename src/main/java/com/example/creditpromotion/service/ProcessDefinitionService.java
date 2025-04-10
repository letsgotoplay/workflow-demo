package com.example.creditpromotion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.creditpromotion.dto.request.ProcessDefinitionCreateRequest;
import com.example.creditpromotion.dto.request.ProcessDefinitionUpdateRequest;
import com.example.creditpromotion.dto.response.ProcessDefinitionDetailResponse;
import com.example.creditpromotion.dto.response.ProcessDefinitionListResponse;
import com.example.creditpromotion.enums.ProcessStatus;

/**
 * 流程定义服务接口
 */
public interface ProcessDefinitionService {

    /**
     * 创建流程定义
     * 
     * @param request 创建请求
     * @return 流程定义ID
     */
    Long createProcessDefinition(ProcessDefinitionCreateRequest request);

    /**
     * 更新流程定义
     * 
     * @param request 更新请求
     * @return 流程定义ID
     */
    Long updateProcessDefinition(ProcessDefinitionUpdateRequest request);

    /**
     * 获取流程定义详情
     * 
     * @param id 流程定义ID
     * @return 流程定义详情
     */
    ProcessDefinitionDetailResponse getProcessDefinitionDetail(Long id);

    /**
     * 分页查询流程定义列表
     * 
     * @param processTypeCode 流程类型代码（可为null）
     * @param status 流程状态（可为null）
     * @param keyword 关键字（可为null）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProcessDefinitionListResponse> getProcessDefinitionList(
            String processTypeCode, ProcessStatus status, String keyword, Pageable pageable);

    /**
     * 修改流程状态
     * 
     * @param id 流程定义ID
     * @param status 目标状态
     * @return 操作结果
     */
    boolean updateProcessStatus(Long id, ProcessStatus status);

    /**
     * 删除流程定义
     * 
     * @param id 流程定义ID
     * @return 操作结果
     */
    boolean deleteProcessDefinition(Long id);
}