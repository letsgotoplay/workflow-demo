package com.example.creditpromotion.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.example.creditpromotion.enums.ProcessPriority;

/**
 * 流程实例创建请求DTO
 */
@Data
public class ProcessInstanceCreateRequest {

    @NotNull(message = "流程定义ID不能为空")
    private Long processDefinitionId;

    private Long officerId;

    private Long employeeId;

    @Size(max = 100, message = "员工姓名长度不能超过100")
    private String employeeName;

    @NotNull(message = "表单数据不能为空")
    private Map<String, Object> formData;

    private LocalDate effectiveDate;

    private String applyUserName;
    
    private Long applyUserId;

    private ProcessPriority priority = ProcessPriority.NORMAL;

    // 预选审批人信息
    private Map<String, List<Map<String, Object>>> preselectedApprovers;

}