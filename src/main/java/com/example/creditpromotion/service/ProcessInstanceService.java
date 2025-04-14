package com.example.creditpromotion.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.creditpromotion.dto.request.ApprovalActionRequest;
import com.example.creditpromotion.dto.request.ProcessInstanceCreateRequest;
import com.example.creditpromotion.dto.response.ProcessInstanceDetailResponse;
import com.example.creditpromotion.dto.response.ProcessInstanceListResponse;
import com.example.creditpromotion.entity.ProcessInstance;
import com.example.creditpromotion.enums.ActionType;
import com.example.creditpromotion.enums.ProcessInstanceStatus;

/**
 * 流程实例服务接口
 */
public interface ProcessInstanceService {

    /**
     * 创建流程实例（草稿）
     * 
     * @param request 创建请求
     * @return 流程实例 ID
     */
    Long createProcessInstance(ProcessInstanceCreateRequest request);

    /**
     * 提交流程实例
     * 
     * @param id 流程实例ID
     * @return 操作结果
     */
    boolean submitProcessInstance(Long id);

    /**
     * 取消流程实例
     * 
     * @param id 流程实例ID
     * @param reason 取消原因
     * @return 操作结果
     */
    boolean cancelProcessInstance(Long id, String reason);

    /**
     * 审批操作（同意、拒绝、重做、转交）
     * 
     * @param request 审批操作请求
     * @return 操作结果
     */
    boolean processApprovalAction(ApprovalActionRequest request);

    /**
     * 获取流程实例详情
     * 
     * @param id 流程实例ID
     * @return 流程实例详情
     */
    ProcessInstanceDetailResponse getProcessInstanceDetail(Long id);

    /**
     * 分页查询用户发起的流程实例
     * 
     * @param userId 用户ID
     * @param status 流程状态（可为null）
     * @param keyword 关键字（可为null）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProcessInstanceListResponse> getUserProcessInstances(
            Long userId, ProcessInstanceStatus status, String keyword, Pageable pageable);

    /**
     * 分页查询流程实例列表
     * 
     * @param processDefinitionId 流程定义ID（可为null）
     * @param status 流程状态（可为null）
     * @param keyword 关键字（可为null）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProcessInstanceListResponse> getProcessInstanceList(
            Long processDefinitionId, ProcessInstanceStatus status, String keyword, Pageable pageable);

   List<ActionType> getAllowedActionsForCurrentUser(Long processInstanceId) ;
 
        }