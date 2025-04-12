package com.example.creditpromotion.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    
    // 通用错误
    SYSTEM_ERROR(10000, "系统错误"),
    PARAMETER_ERROR(10001, "参数错误"),
    UNAUTHORIZED(10002, "未授权"),
    FORBIDDEN(10003, "拒绝访问"),
    RESOURCE_NOT_FOUND(10004, "资源不存在"),
    
    // 流程定义相关错误
    PROCESS_DEFINITION_NOT_FOUND(20001, "流程定义不存在"),
    DUPLICATE_PROCESS_CODE(20002, "流程编码已存在"),
    PROCESS_STATUS_NOT_EDITABLE(20003, "当前状态不允许编辑"),
    PROCESS_STATUS_NOT_DELETABLE(20004, "当前状态不允许删除"),
    INVALID_STATUS_TRANSITION(20005, "无效的状态转换"),
    INVALID_NODE_KEY(20006, "无效的节点标识"),
    
    // 节点相关错误
    NODE_DEFINITION_NOT_FOUND(21001, "节点定义不存在"),
    START_NODE_NOT_FOUND(21002, "开始节点不存在"),
    TRANSITION_NOT_FOUND(21003, "转换关系不存在"),
    NO_MATCHING_TRANSITION(21004, "没有匹配的转换路径"),
    MULTIPLE_TRANSITIONS(21005, "存在多个转换路径"),
    //
    INVALID_APPROVER_DEFINITION(21006, "无效的审批人定义"),
    INVALID_APPROVER_TYPE(21007, "无效的审批人类型"),
    INVALID_APPROVER_EXPRESSION(21008, "无效的审批人表达式"),
    INVALID_APPROVER_TARGET(21009, "无效的审批人目标"),
    // 流程实例相关错误
    PROCESS_INSTANCE_NOT_FOUND(30001, "流程实例不存在"),
    PROCESS_NOT_ACTIVE(30002, "流程未激活"),
    PROCESS_STATUS_NOT_SUBMITTABLE(30003, "当前状态不允许提交"),
    PROCESS_STATUS_NOT_CANCELABLE(30004, "当前状态不允许取消"),
    PROCESS_NOT_IN_PROGRESS(30005, "流程未在进行中"),
    INVALID_FORM_DATA(30006, "无效的表单数据"),
    
    // 节点实例相关错误
    NODE_INSTANCE_NOT_FOUND(31001, "节点实例不存在"),
    NODE_NOT_IN_PROGRESS(31002, "节点未在处理中"),
    
    // 审批相关错误
    APPROVER_INSTANCE_NOT_FOUND(32001, "审批人实例不存在"),
    NO_PERMISSION(32002, "无权限执行此操作"),
    APPROVAL_ALREADY_PROCESSED(32003, "审批已处理"),
    INVALID_ACTION_TYPE(32004, "无效的操作类型"),
    
    // 重做相关错误
    REWORK_CONFIG_NOT_FOUND(33001, "重做配置不存在"),
    INVALID_REWORK_TARGET(33002, "无效的重做目标"),
    PREV_NODE_NOT_FOUND(33003, "前一节点不存在"),
    REWORK_TARGET_NOT_SPECIFIED(33004, "未指定重做目标"),
    INVALID_REWORK_TYPE(33005, "无效的重做类型"),
    
    // 转交相关错误
    INVALID_TARGET_APPROVER(34001, "无效的目标审批人"),
    
    // 表达式相关错误
    EXPRESSION_EVALUATION_ERROR(40001, "表达式评估错误"),
    
    // 任务相关错误
    TASK_NOT_FOUND(50001, "任务不存在"),
    INVALID_TASK_TYPE(50002, "无效的任务类型");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}