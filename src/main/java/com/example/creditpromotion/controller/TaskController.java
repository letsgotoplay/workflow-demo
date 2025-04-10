package com.example.creditpromotion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.creditpromotion.dto.response.TaskResponse;
import com.example.creditpromotion.service.TaskService;
import com.example.creditpromotion.util.SecurityUtil;

/**
 * 任务Controller
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 查询当前用户的所有待办任务（包括待审批和待重做）
     */
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getUserTasks(
            @PageableDefault(size = 10) Pageable pageable) {
        
        Long currentUserId = SecurityUtil.getCurrentUserId();
        
        Page<TaskResponse> page = taskService.getUserTasks(currentUserId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * 查询当前用户的待审批任务
     */
    @GetMapping("/approval")
    public ResponseEntity<Page<TaskResponse>> getUserApprovalTasks(
            @PageableDefault(size = 10) Pageable pageable) {
        
        Long currentUserId = SecurityUtil.getCurrentUserId();
        
        Page<TaskResponse> page = taskService.getUserApprovalTasks(currentUserId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * 查询当前用户的待重做任务
     */
    @GetMapping("/rework")
    public ResponseEntity<Page<TaskResponse>> getUserReworkTasks(
            @PageableDefault(size = 10) Pageable pageable) {
        
        Long currentUserId = SecurityUtil.getCurrentUserId();
        
        Page<TaskResponse> page = taskService.getUserReworkTasks(currentUserId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskDetail(
            @PathVariable Long taskId,
            @RequestParam String taskType) {
        
        TaskResponse response = taskService.getTaskDetail(taskId, taskType);
        return ResponseEntity.ok(response);
    }
}