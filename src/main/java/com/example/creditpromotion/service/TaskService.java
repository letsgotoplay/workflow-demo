package com.example.creditpromotion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.creditpromotion.dto.response.TaskResponse;

/**
 * 任务服务接口
 */
public interface TaskService {

    /**
     * 查询用户待处理的任务列表（包括待审批和待重做）
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<TaskResponse> getUserTasks(Long userId, Pageable pageable);

    /**
     * 查询用户待审批的任务列表
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<TaskResponse> getUserApprovalTasks(Long userId, Pageable pageable);

    /**
     * 查询用户待重做的任务列表
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<TaskResponse> getUserReworkTasks(Long userId, Pageable pageable);

    /**
     * 查询任务详情
     * 
     * @param taskId 任务ID
     * @param taskType 任务类型
     * @return 任务详情
     */
    TaskResponse getTaskDetail(Long taskId, String taskType);
}