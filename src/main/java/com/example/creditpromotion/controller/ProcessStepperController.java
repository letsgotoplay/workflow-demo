package com.example.creditpromotion.controller;

import com.example.creditpromotion.dto.response.ProcessStepperResponse;
import com.example.creditpromotion.service.ProcessStepperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程步骤展示控制器
 * 提供流程步骤展示相关的API
 */
@RestController
@RequestMapping("/api/process-steps")
@RequiredArgsConstructor
public class ProcessStepperController {

    private final ProcessStepperService processStepperService;

    /**
     * 获取流程实例的步骤展示信息
     * 
     * @param processInstanceId 流程实例ID
     * @return 流程步骤展示响应
     */
    @GetMapping("/{processInstanceId}")
    public ResponseEntity<ProcessStepperResponse> getProcessSteps(
            @PathVariable Long processInstanceId) {
        ProcessStepperResponse response = processStepperService.getProcessSteps(processInstanceId);
        return ResponseEntity.ok(response);
    }
}