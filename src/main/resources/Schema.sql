-- First, create all tables without foreign key constraints
CREATE TABLE process_definition (
    id NUMBER(19) PRIMARY KEY,
    process_code VARCHAR2(50) NOT NULL,
    process_name VARCHAR2(100) NOT NULL,
    process_type_code VARCHAR2(50) NOT NULL,
    description VARCHAR2(500),
    form_config CLOB,
    timeout_days NUMBER(5),
    status VARCHAR2(20) NOT NULL,
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL
);

CREATE TABLE approval_node_definition (
     id NUMBER(19) PRIMARY KEY,
     process_definition_id NUMBER(19) NOT NULL,
     node_name VARCHAR2(100) NOT NULL,
     node_type VARCHAR2(50) NOT NULL, -- 节点类型: NORMAL(普通), PARALLEL(并行), CONDITIONAL(条件)
     approver_type VARCHAR2(50) NOT NULL, -- 审批人类型: ROLE(角色), USER(用户), HIERARCHY(层级), DEPARTMENT(部门)
     approval_strategy VARCHAR2(50) NOT NULL, -- 审批策略: ALL(所有人通过), ANY(任一人通过)
     timeout_hours NUMBER(5),
     sequence_no NUMBER(3) NOT NULL,
     rework_node_id NUMBER(19), -- 重做时返回的节点ID
     condition_expression VARCHAR2(500), -- 条件表达式（针对条件节点）
     created_by VARCHAR2(50) NOT NULL,
     created_time TIMESTAMP NOT NULL,
     updated_by VARCHAR2(50),
     updated_time TIMESTAMP,
     is_deleted NUMBER(1) DEFAULT 0 NOT NULL
);

CREATE TABLE node_approver_definition (
     id NUMBER(19) PRIMARY KEY,
     node_id NUMBER(19) NOT NULL,
     approver_type VARCHAR2(50) NOT NULL, -- ROLE, USER, DEPARTMENT, EXPRESSION
     approver_id VARCHAR2(100), -- 角色ID、用户ID、部门ID
     expression VARCHAR2(500), -- 表达式，用于动态确定审批人，例如"${申请人.直接主管}"
     description VARCHAR2(255),
     created_by VARCHAR2(50) NOT NULL,
     created_time TIMESTAMP NOT NULL,
     updated_by VARCHAR2(50),
     updated_time TIMESTAMP,
     is_deleted NUMBER(1) DEFAULT 0 NOT NULL
);

CREATE TABLE process_node_instance (
     id NUMBER(19) PRIMARY KEY,
     process_instance_id NUMBER(19) NOT NULL,
     node_definition_id NUMBER(19) NOT NULL,
     node_name VARCHAR2(100) NOT NULL,
     node_status VARCHAR2(50) NOT NULL, -- PENDING, IN_PROGRESS, APPROVED, REJECTED, CANCELED, REWORK
     start_time TIMESTAMP,
     end_time TIMESTAMP,
     due_time TIMESTAMP, -- 超时时间
     prev_node_instance_id NUMBER(19), -- 前一个节点实例ID
     created_by VARCHAR2(50) NOT NULL,
     created_time TIMESTAMP NOT NULL,
     updated_by VARCHAR2(50),
     updated_time TIMESTAMP
);

CREATE TABLE process_instance (
     id NUMBER(19) PRIMARY KEY,
     process_no VARCHAR2(50) NOT NULL,
     process_definition_id NUMBER(19) NOT NULL,
     officer_id NUMBER(19),
     employee_id NUMBER(19),
     employee_name VARCHAR2(100),
     form_data CLOB,
     current_node_instance_id NUMBER(19), -- 当前节点实例ID（而不是节点定义ID）
     status VARCHAR2(20) NOT NULL, -- DRAFT, IN_PROGRESS, APPROVED, REJECTED, CANCELED, REWORK
     rework_count NUMBER(3) DEFAULT 0, -- 重做次数
     apply_user_id NUMBER(19) NOT NULL,
     apply_user_name VARCHAR2(100) NOT NULL,
     apply_time TIMESTAMP NOT NULL,
     complete_time TIMESTAMP,
     effective_date DATE,
     priority VARCHAR2(20) DEFAULT 'NORMAL',
     created_by VARCHAR2(50) NOT NULL,
     created_time TIMESTAMP NOT NULL,
     updated_by VARCHAR2(50),
     updated_time TIMESTAMP,
     is_deleted NUMBER(1) DEFAULT 0 NOT NULL
);

CREATE TABLE node_approver_instance (
     id NUMBER(19) PRIMARY KEY,
     node_instance_id NUMBER(19) NOT NULL,
     approver_id NUMBER(19) NOT NULL, -- 实际审批人ID
     approver_name VARCHAR2(100) NOT NULL, -- 实际审批人姓名
     approver_type VARCHAR2(50) NOT NULL, -- 审批人类型: ROLE, USER, DEPARTMENT
     approval_status VARCHAR2(50) NOT NULL, -- PENDING, APPROVED, REJECTED, TRANSFERRED
     assign_time TIMESTAMP NOT NULL, -- 分配时间
     action_time TIMESTAMP, -- 操作时间
     due_time TIMESTAMP, -- 超时时间
     comments VARCHAR2(500), -- 审批意见
     transferred_to_id NUMBER(19), -- 转交给谁
     transferred_to_name VARCHAR2(100), -- 转交给谁的姓名
     created_by VARCHAR2(50) NOT NULL,
     created_time TIMESTAMP NOT NULL,
     updated_by VARCHAR2(50),
     updated_time TIMESTAMP
);

CREATE TABLE process_operation_log (
     id NUMBER(19) PRIMARY KEY,
     process_instance_id NUMBER(19) NOT NULL,
     node_instance_id NUMBER(19),
     operation_type VARCHAR2(50) NOT NULL, -- CREATE, SUBMIT, APPROVE, REJECT, REWORK, TRANSFER, CANCEL, REMIND, TIMEOUT
     operator_id NUMBER(19) NOT NULL,
     operator_name VARCHAR2(100) NOT NULL,
     operation_time TIMESTAMP NOT NULL,
     operation_details CLOB, -- 操作详情（JSON格式）
     created_by VARCHAR2(50) NOT NULL,
     created_time TIMESTAMP NOT NULL
);

CREATE TABLE approval_record (
     id NUMBER(19) PRIMARY KEY,
     process_instance_id NUMBER(19) NOT NULL,
     node_instance_id NUMBER(19) NOT NULL,
     approver_instance_id NUMBER(19), -- 审批人实例ID
     approver_id NUMBER(19),
     approver_name VARCHAR2(100),
     action_type VARCHAR2(50) NOT NULL, -- APPROVE, REJECT, REWORK, TRANSFER, CANCEL, REMIND
     action_status VARCHAR2(50) NOT NULL, -- SUCCESS, FAIL
     action_comment VARCHAR2(500),
     action_time TIMESTAMP NOT NULL,
     target_approver_id NUMBER(19), -- 目标审批人ID（转交时使用）
     target_approver_name VARCHAR2(100), -- 目标审批人姓名（转交时使用）
     created_by VARCHAR2(50) NOT NULL,
     created_time TIMESTAMP NOT NULL
);

-- Now add unique constraints
ALTER TABLE process_definition ADD CONSTRAINT uk_process_code UNIQUE (process_code);
ALTER TABLE process_instance ADD CONSTRAINT uk_process_no UNIQUE (process_no);

-- Finally add foreign key constraints in the correct order
ALTER TABLE approval_node_definition ADD CONSTRAINT fk_and_process_definition_id 
    FOREIGN KEY (process_definition_id) REFERENCES process_definition(id);

ALTER TABLE node_approver_definition ADD CONSTRAINT fk_nad_node_id 
    FOREIGN KEY (node_id) REFERENCES approval_node_definition(id);

ALTER TABLE process_node_instance ADD CONSTRAINT fk_pni_node_definition_id 
    FOREIGN KEY (node_definition_id) REFERENCES approval_node_definition(id);

ALTER TABLE process_node_instance ADD CONSTRAINT fk_pni_process_instance_id 
    FOREIGN KEY (process_instance_id) REFERENCES process_instance(id);

ALTER TABLE process_instance ADD CONSTRAINT fk_pi_process_definition_id 
    FOREIGN KEY (process_definition_id) REFERENCES process_definition(id);

ALTER TABLE process_instance ADD CONSTRAINT fk_pi_current_node_instance_id 
    FOREIGN KEY (current_node_instance_id) REFERENCES process_node_instance(id);

ALTER TABLE node_approver_instance ADD CONSTRAINT fk_nai_node_instance_id 
    FOREIGN KEY (node_instance_id) REFERENCES process_node_instance(id);

ALTER TABLE process_operation_log ADD CONSTRAINT fk_pol_process_instance_id 
    FOREIGN KEY (process_instance_id) REFERENCES process_instance(id);

ALTER TABLE process_operation_log ADD CONSTRAINT fk_pol_node_instance_id 
    FOREIGN KEY (node_instance_id) REFERENCES process_node_instance(id);

ALTER TABLE approval_record ADD CONSTRAINT fk_ar_process_instance_id 
    FOREIGN KEY (process_instance_id) REFERENCES process_instance(id);

ALTER TABLE approval_record ADD CONSTRAINT fk_ar_node_instance_id 
    FOREIGN KEY (node_instance_id) REFERENCES process_node_instance(id);

ALTER TABLE approval_record ADD CONSTRAINT fk_ar_approver_instance_id 
    FOREIGN KEY (approver_instance_id) REFERENCES node_approver_instance(id);






