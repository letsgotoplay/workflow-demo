package com.example.creditpromotion.service;

import com.example.creditpromotion.dto.response.ProcessStepperResponse;

/**
 * 流程步骤展示服务接口
 * 用于处理流程步骤的展示逻辑，包括并行节点合并、条件节点处理等
 */
public interface ProcessStepperService {
    
    /**
     * 获取流程实例的步骤展示信息
     * 
     * @param processInstanceId 流程实例ID
     * @return 流程步骤展示响应
     */
    ProcessStepperResponse getProcessSteps(Long processInstanceId);
}