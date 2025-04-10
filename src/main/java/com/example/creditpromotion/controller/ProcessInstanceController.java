package com.example.creditpromotion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.creditpromotion.dto.request.ApprovalActionRequest;
import com.example.creditpromotion.dto.request.ProcessInstanceCreateRequest;
import com.example.creditpromotion.dto.response.ProcessInstanceDetailResponse;
import com.example.creditpromotion.dto.response.ProcessInstanceListResponse;
import com.example.creditpromotion.enums.ProcessInstanceStatus;
import com.example.creditpromotion.service.ProcessInstanceService;
import com.example.creditpromotion.util.SecurityUtil;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程实例Controller
 */
@RestController
@RequestMapping("/api/process-instances")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;

    /**
     * 创建流程实例
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProcessInstance(
            @Valid @RequestBody ProcessInstanceCreateRequest request) {
        
        Long id = processInstanceService.createProcessInstance(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "流程实例创建成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 提交流程实例
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitProcessInstance(
            @PathVariable Long id) {
        
        processInstanceService.submitProcessInstance(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "流程实例提交成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 取消流程实例
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelProcessInstance(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        
        processInstanceService.cancelProcessInstance(id, reason);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "流程实例取消成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 审批操作
     */
    @PostMapping("/approval-action")
    public ResponseEntity<Map<String, Object>> processApprovalAction(
            @Valid @RequestBody ApprovalActionRequest request) {
        
        processInstanceService.processApprovalAction(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("processInstanceId", request.getProcessInstanceId());
        response.put("message", "审批操作处理成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取流程实例详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcessInstanceDetailResponse> getProcessInstanceDetail(
            @PathVariable Long id) {
        
        ProcessInstanceDetailResponse response = processInstanceService.getProcessInstanceDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询当前用户发起的流程实例
     */
    @GetMapping("/my")
    public ResponseEntity<Page<ProcessInstanceListResponse>> getMyProcessInstances(
            @RequestParam(required = false) ProcessInstanceStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Long currentUserId = SecurityUtil.getCurrentUserId();
        
        Page<ProcessInstanceListResponse> page = processInstanceService
                .getUserProcessInstances(currentUserId, status, keyword, pageable);
        
        return ResponseEntity.ok(page);
    }

    /**
     * 分页查询流程实例列表
     */
    @GetMapping
    public ResponseEntity<Page<ProcessInstanceListResponse>> getProcessInstanceList(
            @RequestParam(required = false) Long processDefinitionId,
            @RequestParam(required = false) ProcessInstanceStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<ProcessInstanceListResponse> page = processInstanceService
                .getProcessInstanceList(processDefinitionId, status, keyword, pageable);
        
        return ResponseEntity.ok(page);
    }
}