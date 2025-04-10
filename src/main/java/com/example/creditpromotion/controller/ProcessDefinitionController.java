package com.example.creditpromotion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.creditpromotion.dto.request.ProcessDefinitionCreateRequest;
import com.example.creditpromotion.dto.request.ProcessDefinitionUpdateRequest;
import com.example.creditpromotion.dto.response.ProcessDefinitionDetailResponse;
import com.example.creditpromotion.dto.response.ProcessDefinitionListResponse;
import com.example.creditpromotion.enums.ProcessStatus;
import com.example.creditpromotion.service.ProcessDefinitionService;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程定义Controller
 */
@RestController
@RequestMapping("/api/process-definitions")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final ProcessDefinitionService processDefinitionService;

    /**
     * 创建流程定义
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProcessDefinition(
            @Valid @RequestBody ProcessDefinitionCreateRequest request) {
        
        Long id = processDefinitionService.createProcessDefinition(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "流程定义创建成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新流程定义
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProcessDefinition(
            @PathVariable Long id,
            @Valid @RequestBody ProcessDefinitionUpdateRequest request) {
        
        request.setId(id);
        processDefinitionService.updateProcessDefinition(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "流程定义更新成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取流程定义详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcessDefinitionDetailResponse> getProcessDefinitionDetail(
            @PathVariable Long id) {
        
        ProcessDefinitionDetailResponse response = processDefinitionService.getProcessDefinitionDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 分页查询流程定义列表
     */
    @GetMapping
    public ResponseEntity<Page<ProcessDefinitionListResponse>> getProcessDefinitionList(
            @RequestParam(required = false) String processTypeCode,
            @RequestParam(required = false) ProcessStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<ProcessDefinitionListResponse> page = processDefinitionService
                .getProcessDefinitionList(processTypeCode, status, keyword, pageable);
        
        return ResponseEntity.ok(page);
    }

    /**
     * 修改流程状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateProcessStatus(
            @PathVariable Long id,
            @RequestParam ProcessStatus status) {
        
        processDefinitionService.updateProcessStatus(id, status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("status", status);
        response.put("message", "流程状态更新成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除流程定义
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProcessDefinition(
            @PathVariable Long id) {
        
        processDefinitionService.deleteProcessDefinition(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "流程定义删除成功");
        
        return ResponseEntity.ok(response);
    }
}