# DB Design

## 完整数据库表结构设计

以下是根据核心需求优化后的完整表结构设计，包含字段注释、数据类型、示例值和枚举说明。

### 1. 流程定义表 (PROCESS_DEFINITION)

```sql
CREATE TABLE process_definition (
    id NUMBER(19) PRIMARY KEY,
    process_code VARCHAR2(50) NOT NULL,
    process_name VARCHAR2(100) NOT NULL,
    process_type_code VARCHAR2(50) NOT NULL,
    description VARCHAR2(500),
    form_config CLOB,
    form_version NUMBER(10) DEFAULT 1 NOT NULL,  -- 新增：表单版本号
    timeout_days NUMBER(5),
    status VARCHAR2(20) NOT NULL,  -- ACTIVE, DISABLED, ARCHIVED
    priority VARCHAR2(20) DEFAULT 'NORMAL',  -- HIGH, NORMAL, LOW
    version NUMBER(10) DEFAULT 1 NOT NULL,  -- 新增：乐观锁版本号
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT uk_process_code UNIQUE (process_code)
);

COMMENT ON TABLE process_definition IS '流程定义表';
COMMENT ON COLUMN process_definition.id IS '主键ID';
COMMENT ON COLUMN process_definition.process_code IS '流程编码，如CREDIT_OFFICER_PROMOTION';
COMMENT ON COLUMN process_definition.process_name IS '流程名称，如信贷员晋升流程';
COMMENT ON COLUMN process_definition.process_type_code IS '流程类型代码，如HR、FINANCE、RISK';
COMMENT ON COLUMN process_definition.description IS '流程描述';
COMMENT ON COLUMN process_definition.form_config IS '表单配置JSON，包含字段定义、验证规则等';
COMMENT ON COLUMN process_definition.form_version IS '表单版本号，每次表单变更时递增';
COMMENT ON COLUMN process_definition.timeout_days IS '整体流程超时天数';
COMMENT ON COLUMN process_definition.status IS '状态：ACTIVE-激活，DISABLED-禁用，ARCHIVED-归档';
COMMENT ON COLUMN process_definition.priority IS '流程优先级：HIGH-高，NORMAL-中，LOW-低';
COMMENT ON COLUMN process_definition.version IS '乐观锁版本号';
COMMENT ON COLUMN process_definition.created_by IS '创建人';
COMMENT ON COLUMN process_definition.created_time IS '创建时间';
COMMENT ON COLUMN process_definition.updated_by IS '更新人';
COMMENT ON COLUMN process_definition.updated_time IS '更新时间';
COMMENT ON COLUMN process_definition.is_deleted IS '是否删除：0-否，1-是';
```

### 2. 节点定义表 (APPROVAL_NODE_DEFINITION)

```sql
CREATE TABLE approval_node_definition (
    id NUMBER(19) PRIMARY KEY,
    process_definition_id NUMBER(19) NOT NULL,
    node_key VARCHAR2(100) NOT NULL,  -- 新增：节点业务标识
    node_name VARCHAR2(100) NOT NULL,
    node_type VARCHAR2(50) NOT NULL,  -- NORMAL, PARALLEL, CONDITIONAL
    approver_type VARCHAR2(50) NOT NULL,  -- ROLE, USER, DEPARTMENT, HIERARCHY, EXPRESSION, SYSTEM
    approval_strategy VARCHAR2(50) NOT NULL,  -- ALL, ANY
    timeout_hours NUMBER(5),
    is_start_node NUMBER(1) DEFAULT 0 NOT NULL,  -- 新增：是否开始节点
    is_end_node NUMBER(1) DEFAULT 0 NOT NULL,  -- 新增：是否结束节点
    allow_approver_selection NUMBER(1) DEFAULT 0 NOT NULL,  -- 新增：是否允许申请人预选审批人
    form_permissions CLOB,  -- 新增：表单字段权限配置JSON
    version NUMBER(10) DEFAULT 1 NOT NULL,  -- 新增：乐观锁版本号
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_and_process_definition_id FOREIGN KEY (process_definition_id) REFERENCES process_definition(id),
    CONSTRAINT uk_node_key_process UNIQUE (process_definition_id, node_key)
);

COMMENT ON TABLE approval_node_definition IS '审批节点定义表';
COMMENT ON COLUMN approval_node_definition.id IS '主键ID';
COMMENT ON COLUMN approval_node_definition.process_definition_id IS '流程定义ID';
COMMENT ON COLUMN approval_node_definition.node_key IS '节点标识，如dept_review、parallel_review';
COMMENT ON COLUMN approval_node_definition.node_name IS '节点名称，如部门初审、并行评审';
COMMENT ON COLUMN approval_node_definition.node_type IS '节点类型：NORMAL-普通节点, PARALLEL-并行节点, CONDITIONAL-条件节点';
COMMENT ON COLUMN approval_node_definition.approver_type IS '审批人类型：ROLE-角色, USER-用户, DEPARTMENT-部门, HIERARCHY-层级, EXPRESSION-表达式, SYSTEM-系统';
COMMENT ON COLUMN approval_node_definition.approval_strategy IS '审批策略：ALL-所有人通过, ANY-任一人通过';
COMMENT ON COLUMN approval_node_definition.timeout_hours IS '节点超时小时数';
COMMENT ON COLUMN approval_node_definition.is_start_node IS '是否开始节点：0-否, 1-是';
COMMENT ON COLUMN approval_node_definition.is_end_node IS '是否结束节点：0-否, 1-是';
COMMENT ON COLUMN approval_node_definition.allow_approver_selection IS '是否允许申请人预选审批人：0-否, 1-是';
COMMENT ON COLUMN approval_node_definition.form_permissions IS '表单字段权限配置JSON，如{"field1":"READ","field2":"EDIT"}';
COMMENT ON COLUMN approval_node_definition.version IS '乐观锁版本号';
COMMENT ON COLUMN approval_node_definition.created_by IS '创建人';
COMMENT ON COLUMN approval_node_definition.created_time IS '创建时间';
COMMENT ON COLUMN approval_node_definition.updated_by IS '更新人';
COMMENT ON COLUMN approval_node_definition.updated_time IS '更新时间';
COMMENT ON COLUMN approval_node_definition.is_deleted IS '是否删除：0-否，1-是';
```

### 3. 节点审批人定义表 (NODE_APPROVER_DEFINITION)

```sql
CREATE TABLE node_approver_definition (
    id NUMBER(19) PRIMARY KEY,
    node_id NUMBER(19) NOT NULL,
    approver_type VARCHAR2(50) NOT NULL,  -- ROLE, USER, DEPARTMENT, EXPRESSION
    approver_id VARCHAR2(100),  -- 角色ID、用户ID、部门ID
    expression VARCHAR2(500),  -- SpEL表达式
    description VARCHAR2(255),
    is_required NUMBER(1) DEFAULT 1 NOT NULL,  -- 新增：是否必须
    priority NUMBER(3) DEFAULT 1 NOT NULL,  -- 新增：优先级
    version NUMBER(10) DEFAULT 1 NOT NULL,  -- 新增：乐观锁版本号
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_nad_node_id FOREIGN KEY (node_id) REFERENCES approval_node_definition(id)
);

COMMENT ON TABLE node_approver_definition IS '节点审批人定义表';
COMMENT ON COLUMN node_approver_definition.id IS '主键ID';
COMMENT ON COLUMN node_approver_definition.node_id IS '节点定义ID';
COMMENT ON COLUMN node_approver_definition.approver_type IS '审批人类型：ROLE-角色, USER-用户, DEPARTMENT-部门, EXPRESSION-表达式';
COMMENT ON COLUMN node_approver_definition.approver_id IS '审批人ID，当approver_type为ROLE/USER/DEPARTMENT时使用';
COMMENT ON COLUMN node_approver_definition.expression IS 'SpEL表达式，如#{applicant.directManager}，当approver_type为EXPRESSION时使用';
COMMENT ON COLUMN node_approver_definition.description IS '审批人描述';
COMMENT ON COLUMN node_approver_definition.is_required IS '是否必须：0-否, 1-是';
COMMENT ON COLUMN node_approver_definition.priority IS '优先级，数字越小优先级越高';
COMMENT ON COLUMN node_approver_definition.version IS '乐观锁版本号';
COMMENT ON COLUMN node_approver_definition.created_by IS '创建人';
COMMENT ON COLUMN node_approver_definition.created_time IS '创建时间';
COMMENT ON COLUMN node_approver_definition.updated_by IS '更新人';
COMMENT ON COLUMN node_approver_definition.updated_time IS '更新时间';
COMMENT ON COLUMN node_approver_definition.is_deleted IS '是否删除：0-否，1-是';
```

### 4. 节点转换关系表 (NODE_TRANSITION)

```sql
CREATE TABLE node_transition (
    id NUMBER(19) PRIMARY KEY,
    process_definition_id NUMBER(19) NOT NULL,
    source_node_id NUMBER(19) NOT NULL,
    target_node_id NUMBER(19) NOT NULL,
    transition_type VARCHAR2(50) NOT NULL,  -- NORMAL, CONDITIONAL, PARALLEL_START, PARALLEL_END
    condition_expression VARCHAR2(500),  -- SpEL条件表达式
    priority NUMBER(3) DEFAULT 1 NOT NULL,  -- 条件优先级
    description VARCHAR2(255),
    version NUMBER(10) DEFAULT 1 NOT NULL,
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_nt_process_def_id FOREIGN KEY (process_definition_id) REFERENCES process_definition(id),
    CONSTRAINT fk_nt_source_node_id FOREIGN KEY (source_node_id) REFERENCES approval_node_definition(id),
    CONSTRAINT fk_nt_target_node_id FOREIGN KEY (target_node_id) REFERENCES approval_node_definition(id)
);

COMMENT ON TABLE node_transition IS '节点转换关系表';
COMMENT ON COLUMN node_transition.id IS '主键ID';
COMMENT ON COLUMN node_transition.process_definition_id IS '流程定义ID';
COMMENT ON COLUMN node_transition.source_node_id IS '源节点ID';
COMMENT ON COLUMN node_transition.target_node_id IS '目标节点ID';
COMMENT ON COLUMN node_transition.transition_type IS '转换类型：NORMAL-普通, CONDITIONAL-条件, PARALLEL_START-并行开始, PARALLEL_END-并行结束';
COMMENT ON COLUMN node_transition.condition_expression IS 'SpEL条件表达式，如#{promotion.level == "SENIOR"}';
COMMENT ON COLUMN node_transition.priority IS '条件优先级，数字越小优先级越高';
COMMENT ON COLUMN node_transition.description IS '转换描述';
COMMENT ON COLUMN node_transition.version IS '乐观锁版本号';
COMMENT ON COLUMN node_transition.created_by IS '创建人';
COMMENT ON COLUMN node_transition.created_time IS '创建时间';
COMMENT ON COLUMN node_transition.updated_by IS '更新人';
COMMENT ON COLUMN node_transition.updated_time IS '更新时间';
COMMENT ON COLUMN node_transition.is_deleted IS '是否删除：0-否，1-是';
```

### 5. 并行节点组关系表 (PARALLEL_NODE_GROUP)

```sql
CREATE TABLE parallel_node_group (
    id NUMBER(19) PRIMARY KEY,
    process_definition_id NUMBER(19) NOT NULL,
    group_key VARCHAR2(100) NOT NULL,
    parent_node_id NUMBER(19) NOT NULL,  -- 并行父节点
    child_node_id NUMBER(19) NOT NULL,  -- 并行子节点
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_png_process_def_id FOREIGN KEY (process_definition_id) REFERENCES process_definition(id),
    CONSTRAINT fk_png_parent_node_id FOREIGN KEY (parent_node_id) REFERENCES approval_node_definition(id),
    CONSTRAINT fk_png_child_node_id FOREIGN KEY (child_node_id) REFERENCES approval_node_definition(id),
    CONSTRAINT uk_group_parent_child UNIQUE (group_key, parent_node_id, child_node_id)
);

COMMENT ON TABLE parallel_node_group IS '并行节点组关系表';
COMMENT ON COLUMN parallel_node_group.id IS '主键ID';
COMMENT ON COLUMN parallel_node_group.process_definition_id IS '流程定义ID';
COMMENT ON COLUMN parallel_node_group.group_key IS '并行组标识，如parallel_review_group';
COMMENT ON COLUMN parallel_node_group.parent_node_id IS '并行父节点ID';
COMMENT ON COLUMN parallel_node_group.child_node_id IS '并行子节点ID';
COMMENT ON COLUMN parallel_node_group.created_by IS '创建人';
COMMENT ON COLUMN parallel_node_group.created_time IS '创建时间';
COMMENT ON COLUMN parallel_node_group.updated_by IS '更新人';
COMMENT ON COLUMN parallel_node_group.updated_time IS '更新时间';
COMMENT ON COLUMN parallel_node_group.is_deleted IS '是否删除：0-否，1-是';
```

### 6. 重做配置表 (REWORK_CONFIGURATION)

```sql
CREATE TABLE rework_configuration (
    id NUMBER(19) PRIMARY KEY,
    process_definition_id NUMBER(19) NOT NULL,
    node_id NUMBER(19) NOT NULL,
    rework_type VARCHAR2(50) NOT NULL,  -- TO_INITIATOR, TO_PREV_NODE, TO_SPECIFIC_NODE
    target_node_id NUMBER(19),  -- 目标节点ID，仅当rework_type为TO_SPECIFIC_NODE时有值
    allow_comment_required NUMBER(1) DEFAULT 1 NOT NULL,  -- 退回是否必须填写意见
    description VARCHAR2(255),
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_rc_process_definition_id FOREIGN KEY (process_definition_id) REFERENCES process_definition(id),
    CONSTRAINT fk_rc_node_id FOREIGN KEY (node_id) REFERENCES approval_node_definition(id),
    CONSTRAINT fk_rc_target_node_id FOREIGN KEY (target_node_id) REFERENCES approval_node_definition(id)
);

COMMENT ON TABLE rework_configuration IS '重做配置表';
COMMENT ON COLUMN rework_configuration.id IS '主键ID';
COMMENT ON COLUMN rework_configuration.process_definition_id IS '流程定义ID';
COMMENT ON COLUMN rework_configuration.node_id IS '节点ID（从哪个节点发起重做）';
COMMENT ON COLUMN rework_configuration.rework_type IS '重做类型：TO_INITIATOR-退回发起人, TO_PREV_NODE-退回上一节点, TO_SPECIFIC_NODE-退回指定节点';
COMMENT ON COLUMN rework_configuration.target_node_id IS '目标节点ID，仅当rework_type为TO_SPECIFIC_NODE时有值';
COMMENT ON COLUMN rework_configuration.allow_comment_required IS '退回是否必须填写意见：0-否, 1-是';
COMMENT ON COLUMN rework_configuration.description IS '重做配置描述';
COMMENT ON COLUMN rework_configuration.created_by IS '创建人';
COMMENT ON COLUMN rework_configuration.created_time IS '创建时间';
COMMENT ON COLUMN rework_configuration.updated_by IS '更新人';
COMMENT ON COLUMN rework_configuration.updated_time IS '更新时间';
COMMENT ON COLUMN rework_configuration.is_deleted IS '是否删除：0-否，1-是';
```

### 7. 提醒配置表 (REMINDER_CONFIGURATION)

```sql
CREATE TABLE reminder_configuration (
    id NUMBER(19) PRIMARY KEY,
    process_definition_id NUMBER(19) NOT NULL,
    node_id NUMBER(19),  -- 为NULL时表示全局配置
    reminder_type VARCHAR2(50) NOT NULL,  -- TIMEOUT, APPROACHING_TIMEOUT, PERIODIC
    time_expression VARCHAR2(100) NOT NULL,  -- cron表达式或相对时间表达式
    reminder_template_code VARCHAR2(100) NOT NULL,  -- 提醒模板代码
    enabled NUMBER(1) DEFAULT 1 NOT NULL,  -- 是否启用
    description VARCHAR2(255),
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_reminderconf_process_def_id FOREIGN KEY (process_definition_id) REFERENCES process_definition(id),
    CONSTRAINT fk_reminderconf_node_id FOREIGN KEY (node_id) REFERENCES approval_node_definition(id)
);

COMMENT ON TABLE reminder_configuration IS '提醒配置表';
COMMENT ON COLUMN reminder_configuration.id IS '主键ID';
COMMENT ON COLUMN reminder_configuration.process_definition_id IS '流程定义ID';
COMMENT ON COLUMN reminder_configuration.node_id IS '节点ID，为NULL时表示全局配置';
COMMENT ON COLUMN reminder_configuration.reminder_type IS '提醒类型：TIMEOUT-超时提醒, APPROACHING_TIMEOUT-即将超时提醒, PERIODIC-周期性提醒';
COMMENT ON COLUMN reminder_configuration.time_expression IS '时间表达式，可以是cron表达式或相对时间表达式，如"0 0 9 * * ?"(每天9点)或"-2h"(超时前2小时)';
COMMENT ON COLUMN reminder_configuration.reminder_template_code IS '提醒模板代码';
COMMENT ON COLUMN reminder_configuration.enabled IS '是否启用：0-否, 1-是';
COMMENT ON COLUMN reminder_configuration.description IS '提醒配置描述';
COMMENT ON COLUMN reminder_configuration.created_by IS '创建人';
COMMENT ON COLUMN reminder_configuration.created_time IS '创建时间';
COMMENT ON COLUMN reminder_configuration.updated_by IS '更新人';
COMMENT ON COLUMN reminder_configuration.updated_time IS '更新时间';
COMMENT ON COLUMN reminder_configuration.is_deleted IS '是否删除：0-否，1-是';
```

### 8. 流程实例表 (PROCESS_INSTANCE)

```sql
CREATE TABLE process_instance (
    id NUMBER(19) PRIMARY KEY,
    process_no VARCHAR2(50) NOT NULL,
    process_definition_id NUMBER(19) NOT NULL,
    officer_id NUMBER(19),  -- 信贷员ID
    employee_id NUMBER(19),
    employee_name VARCHAR2(100),
    form_data CLOB,
    form_version NUMBER(10) NOT NULL,  -- 表单版本号，与定义中的版本保持一致
    current_node_id NUMBER(19),  -- 当前节点定义ID（非实例ID）
    status VARCHAR2(20) NOT NULL,  -- DRAFT, IN_PROGRESS, APPROVED, REJECTED, CANCELED, REWORK
    rework_count NUMBER(3) DEFAULT 0,  -- 重做次数
    apply_user_id NUMBER(19) NOT NULL,
    apply_user_name VARCHAR2(100) NOT NULL,
    apply_time TIMESTAMP NOT NULL,
    complete_time TIMESTAMP,
    effective_date DATE,
    due_time TIMESTAMP,  -- 整体流程超时时间
    priority VARCHAR2(20) DEFAULT 'NORMAL',  -- HIGH, NORMAL, LOW
    version NUMBER(10) DEFAULT 1 NOT NULL,  -- 乐观锁版本号
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT uk_process_no UNIQUE (process_no),
    CONSTRAINT fk_pi_process_definition_id FOREIGN KEY (process_definition_id) REFERENCES process_definition(id),
    CONSTRAINT fk_pi_officer_id FOREIGN KEY (officer_id) REFERENCES credit_officer(id),
    CONSTRAINT fk_pi_current_node_id FOREIGN KEY (current_node_id) REFERENCES approval_node_definition(id)
);

COMMENT ON TABLE process_instance IS '流程实例表';
COMMENT ON COLUMN process_instance.id IS '主键ID';
COMMENT ON COLUMN process_instance.process_no IS '流程实例编号，如PROMOTION-202504-00001';
COMMENT ON COLUMN process_instance.process_definition_id IS '流程定义ID';
COMMENT ON COLUMN process_instance.officer_id IS '信贷员ID';
COMMENT ON COLUMN process_instance.employee_id IS '员工ID';
COMMENT ON COLUMN process_instance.employee_name IS '员工姓名';
COMMENT ON COLUMN process_instance.form_data IS '表单数据JSON';
COMMENT ON COLUMN process_instance.form_version IS '表单版本号';
COMMENT ON COLUMN process_instance.current_node_id IS '当前节点定义ID';
COMMENT ON COLUMN process_instance.status IS '状态：DRAFT-草稿, IN_PROGRESS-进行中, APPROVED-已批准, REJECTED-已拒绝, CANCELED-已取消, REWORK-重做';
COMMENT ON COLUMN process_instance.rework_count IS '重做次数';
COMMENT ON COLUMN process_instance.apply_user_id IS '申请人ID';
COMMENT ON COLUMN process_instance.apply_user_name IS '申请人姓名';
COMMENT ON COLUMN process_instance.apply_time IS '申请时间';
COMMENT ON COLUMN process_instance.complete_time IS '完成时间';
COMMENT ON COLUMN process_instance.effective_date IS '生效日期';
COMMENT ON COLUMN process_instance.due_time IS '整体流程超时时间';
COMMENT ON COLUMN process_instance.priority IS '优先级：HIGH-高, NORMAL-中, LOW-低';
COMMENT ON COLUMN process_instance.version IS '乐观锁版本号';
COMMENT ON COLUMN process_instance.created_by IS '创建人';
COMMENT ON COLUMN process_instance.created_time IS '创建时间';
COMMENT ON COLUMN process_instance.updated_by IS '更新人';
COMMENT ON COLUMN process_instance.updated_time IS '更新时间';
COMMENT ON COLUMN process_instance.is_deleted IS '是否删除：0-否，1-是';
```

### 9. 流程节点实例表 (PROCESS_NODE_INSTANCE)

```sql
CREATE TABLE process_node_instance (
    id NUMBER(19) PRIMARY KEY,
    process_instance_id NUMBER(19) NOT NULL,
    node_definition_id NUMBER(19) NOT NULL,
    node_name VARCHAR2(100) NOT NULL,
    node_status VARCHAR2(50) NOT NULL,  -- PENDING, IN_PROGRESS, APPROVED, REJECTED, CANCELED, REWORK
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    due_time TIMESTAMP,  -- 节点超时时间
    prev_node_instance_id NUMBER(19),  -- 前一个节点实例ID
    parent_node_instance_id NUMBER(19),  -- 父节点实例ID（用于并行节点）
    version NUMBER(10) DEFAULT 1 NOT NULL,  -- 乐观锁版本号
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    CONSTRAINT fk_pni_process_instance_id FOREIGN KEY (process_instance_id) REFERENCES process_instance(id),
    CONSTRAINT fk_pni_node_definition_id FOREIGN KEY (node_definition_id) REFERENCES approval_node_definition(id),
    CONSTRAINT fk_pni_prev_node_instance_id FOREIGN KEY (prev_node_instance_id) REFERENCES process_node_instance(id),
    CONSTRAINT fk_pni_parent_node_instance_id FOREIGN KEY (parent_node_instance_id) REFERENCES process_node_instance(id)
);

CREATE INDEX idx_pni_process_instance_id ON process_node_instance(process_instance_id);
CREATE INDEX idx_pni_node_definition_id ON process_node_instance(node_definition_id);
CREATE INDEX idx_pni_node_status ON process_node_instance(node_status);

COMMENT ON TABLE process_node_instance IS '流程节点实例表';
COMMENT ON COLUMN process_node_instance.id IS '主键ID';
COMMENT ON COLUMN process_node_instance.process_instance_id IS '流程实例ID';
COMMENT ON COLUMN process_node_instance.node_definition_id IS '节点定义ID';
COMMENT ON COLUMN process_node_instance.node_name IS '节点名称';
COMMENT ON COLUMN process_node_instance.node_status IS '节点状态：PENDING-待处理, IN_PROGRESS-处理中, APPROVED-已批准, REJECTED-已拒绝, CANCELED-已取消, REWORK-重做';
COMMENT ON COLUMN process_node_instance.start_time IS '开始时间';
COMMENT ON COLUMN process_node_instance.end_time IS '结束时间';
COMMENT ON COLUMN process_node_instance.due_time IS '节点超时时间';
COMMENT ON COLUMN process_node_instance.prev_node_instance_id IS '前一个节点实例ID';
COMMENT ON COLUMN process_node_instance.parent_node_instance_id IS '父节点实例ID，用于并行节点';
COMMENT ON COLUMN process_node_instance.version IS '乐观锁版本号';
COMMENT ON COLUMN process_node_instance.created_by IS '创建人';
COMMENT ON COLUMN process_node_instance.created_time IS '创建时间';
COMMENT ON COLUMN process_node_instance.updated_by IS '更新人';
COMMENT ON COLUMN process_node_instance.updated_time IS '更新时间';
```

### 10. 节点审批人实例表 (NODE_APPROVER_INSTANCE)

```sql
CREATE TABLE node_approver_instance (
    id NUMBER(19) PRIMARY KEY,
    node_instance_id NUMBER(19) NOT NULL,
    approver_id NUMBER(19) NOT NULL,  -- 实际审批人ID
    approver_name VARCHAR2(100) NOT NULL,  -- 实际审批人姓名
    approver_type VARCHAR2(50) NOT NULL,  -- 审批人类型: ROLE, USER, DEPARTMENT
    approval_status VARCHAR2(50) NOT NULL,  -- PENDING, APPROVED, REJECTED, TRANSFERRED
    assign_time TIMESTAMP NOT NULL,  -- 分配时间
    action_time TIMESTAMP,  -- 操作时间
    due_time TIMESTAMP,  -- 超时时间
    comments VARCHAR2(1000),  -- 审批意见（扩大长度）
    transferred_to_id NUMBER(19),  -- 转交给谁
    transferred_to_name VARCHAR2(100),  -- 转交给谁的姓名
    is_preselected NUMBER(1) DEFAULT 0 NOT NULL,  -- 是否申请人预选的审批人
    version NUMBER(10) DEFAULT 1 NOT NULL,  -- 乐观锁版本号
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    CONSTRAINT fk_nai_node_instance_id FOREIGN KEY (node_instance_id) REFERENCES process_node_instance(id)
);

CREATE INDEX idx_nai_node_instance_id ON node_approver_instance(node_instance_id);
CREATE INDEX idx_nai_approver_id ON node_approver_instance(approver_id);
CREATE INDEX idx_nai_approval_status ON node_approver_instance(approval_status);

COMMENT ON TABLE node_approver_instance IS '节点审批人实例表';
COMMENT ON COLUMN node_approver_instance.id IS '主键ID';
COMMENT ON COLUMN node_approver_instance.node_instance_id IS '节点实例ID';
COMMENT ON COLUMN node_approver_instance.approver_id IS '审批人ID';
COMMENT ON COLUMN node_approver_instance.approver_name IS '审批人姓名';
COMMENT ON COLUMN node_approver_instance.approver_type IS '审批人类型：ROLE-角色, USER-用户, DEPARTMENT-部门';
COMMENT ON COLUMN node_approver_instance.approval_status IS '审批状态：PENDING-待处理, APPROVED-已批准, REJECTED-已拒绝, TRANSFERRED-已转交';
COMMENT ON COLUMN node_approver_instance.assign_time IS '分配时间';
COMMENT ON COLUMN node_approver_instance.action_time IS '操作时间';
COMMENT ON COLUMN node_approver_instance.due_time IS '超时时间';
COMMENT ON COLUMN node_approver_instance.comments IS '审批意见';
COMMENT ON COLUMN node_approver_instance.transferred_to_id IS '转交目标人ID';
COMMENT ON COLUMN node_approver_instance.transferred_to_name IS '转交目标人姓名';
COMMENT ON COLUMN node_approver_instance.is_preselected IS '是否申请人预选的审批人：0-否, 1-是';
COMMENT ON COLUMN node_approver_instance.version IS '乐观锁版本号';
COMMENT ON COLUMN node_approver_instance.created_by IS '创建人';
COMMENT ON COLUMN node_approver_instance.created_time IS '创建时间';
COMMENT ON COLUMN node_approver_instance.updated_by IS '更新人';
COMMENT ON COLUMN node_approver_instance.updated_time IS '更新时间';
```

### 11. 审批记录表 (APPROVAL_RECORD)

```sql
CREATE TABLE approval_record (
    id NUMBER(19) PRIMARY KEY,
    process_instance_id NUMBER(19) NOT NULL,
    node_instance_id NUMBER(19) NOT NULL,
    approver_instance_id NUMBER(19),  -- 审批人实例ID
    approver_id NUMBER(19),
    approver_name VARCHAR2(100),
    action_type VARCHAR2(50) NOT NULL,  -- APPROVE, REJECT, REWORK, TRANSFER, CANCEL, REMIND
    action_status VARCHAR2(50) NOT NULL,  -- SUCCESS, FAIL
    action_comment VARCHAR2(1000),  -- 扩大长度
    action_time TIMESTAMP NOT NULL,
    target_approver_id NUMBER(19),  -- 目标审批人ID（转交时使用）
    target_approver_name VARCHAR2(100),  -- 目标审批人姓名（转交时使用）
    target_node_id NUMBER(19),  -- 目标节点ID（重做时使用）
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_ar_process_instance_id FOREIGN KEY (process_instance_id) REFERENCES process_instance(id),
    CONSTRAINT fk_ar_node_instance_id FOREIGN KEY (node_instance_id) REFERENCES process_node_instance(id),
    CONSTRAINT fk_ar_approver_instance_id FOREIGN KEY (approver_instance_id) REFERENCES node_approver_instance(id)
);

CREATE INDEX idx_ar_process_instance_id ON approval_record(process_instance_id);
CREATE INDEX idx_ar_node_instance_id ON approval_record(node_instance_id);
CREATE INDEX idx_ar_approver_id ON approval_record(approver_id);
CREATE INDEX idx_ar_action_time ON approval_record(action_time);

COMMENT ON TABLE approval_record IS '审批记录表';
COMMENT ON COLUMN approval_record.id IS '主键ID';
COMMENT ON COLUMN approval_record.process_instance_id IS '流程实例ID';
COMMENT ON COLUMN approval_record.node_instance_id IS '节点实例ID';
COMMENT ON COLUMN approval_record.approver_instance_id IS '审批人实例ID';
COMMENT ON COLUMN approval_record.approver_id IS '审批人ID';
COMMENT ON COLUMN approval_record.approver_name IS '审批人姓名';
COMMENT ON COLUMN approval_record.action_type IS '操作类型：APPROVE-批准, REJECT-拒绝, REWORK-重做, TRANSFER-转交, CANCEL-取消, REMIND-提醒';
COMMENT ON COLUMN approval_record.action_status IS '操作状态：SUCCESS-成功, FAIL-失败';
COMMENT ON COLUMN approval_record.action_comment IS '操作意见';
COMMENT ON COLUMN approval_record.action_time IS '操作时间';
COMMENT ON COLUMN approval_record.target_approver_id IS '目标审批人ID（转交时使用）';
COMMENT ON COLUMN approval_record.target_approver_name IS '目标审批人姓名（转交时使用）';
COMMENT ON COLUMN approval_record.target_node_id IS '目标节点ID（重做时使用）';
COMMENT ON COLUMN approval_record.created_by IS '创建人';
COMMENT ON COLUMN approval_record.created_time IS '创建时间';
```

### 12. 流程操作日志表 (PROCESS_OPERATION_LOG)

```sql
CREATE TABLE process_operation_log (
    id NUMBER(19) PRIMARY KEY,
    process_instance_id NUMBER(19) NOT NULL,
    node_instance_id NUMBER(19),
    operation_type VARCHAR2(50) NOT NULL,  -- CREATE, SUBMIT, APPROVE, REJECT, REWORK, TRANSFER, CANCEL, REMIND, TIMEOUT
    operator_id NUMBER(19) NOT NULL,
    operator_name VARCHAR2(100) NOT NULL,
    operation_time TIMESTAMP NOT NULL,
    operation_details CLOB,  -- 操作详情（JSON格式）
    ip_address VARCHAR2(50),  -- 新增：操作IP地址
    device_info VARCHAR2(255),  -- 新增：设备信息
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_pol_process_instance_id FOREIGN KEY (process_instance_id) REFERENCES process_instance(id),
    CONSTRAINT fk_pol_node_instance_id FOREIGN KEY (node_instance_id) REFERENCES process_node_instance(id)
);

CREATE INDEX idx_pol_process_instance_id ON process_operation_log(process_instance_id);
CREATE INDEX idx_pol_operation_time ON process_operation_log(operation_time);
CREATE INDEX idx_pol_operator_id ON process_operation_log(operator_id);

COMMENT ON TABLE process_operation_log IS '流程操作日志表';
COMMENT ON COLUMN process_operation_log.id IS '主键ID';
COMMENT ON COLUMN process_operation_log.process_instance_id IS '流程实例ID';
COMMENT ON COLUMN process_operation_log.node_instance_id IS '节点实例ID';
COMMENT ON COLUMN process_operation_log.operation_type IS '操作类型：CREATE-创建, SUBMIT-提交, APPROVE-批准, REJECT-拒绝, REWORK-重做, TRANSFER-转交, CANCEL-取消, REMIND-提醒, TIMEOUT-超时';
COMMENT ON COLUMN process_operation_log.operator_id IS '操作人ID';
COMMENT ON COLUMN process_operation_log.operator_name IS '操作人姓名';
COMMENT ON COLUMN process_operation_log.operation_time IS '操作时间';
COMMENT ON COLUMN process_operation_log.operation_details IS '操作详情（JSON格式）';
COMMENT ON COLUMN process_operation_log.ip_address IS '操作IP地址';
COMMENT ON COLUMN process_operation_log.device_info IS '设备信息';
COMMENT ON COLUMN process_operation_log.created_by IS '创建人';
COMMENT ON COLUMN process_operation_log.created_time IS '创建时间';
```

### 13. 提醒记录表 (REMINDER_RECORD)

```sql
CREATE TABLE reminder_record (
    id NUMBER(19) PRIMARY KEY,
    reminder_config_id NUMBER(19) NOT NULL,
    process_instance_id NUMBER(19) NOT NULL,
    node_instance_id NUMBER(19),
    approver_instance_id NUMBER(19),
    reminder_type VARCHAR2(50) NOT NULL,  -- TIMEOUT, APPROACHING_TIMEOUT, PERIODIC
    reminder_time TIMESTAMP NOT NULL,
    reminder_content CLOB,
    status VARCHAR2(50) NOT NULL,  -- PENDING, SENT, FAILED
    retry_count NUMBER(3) DEFAULT 0,
    error_message VARCHAR2(500),
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    CONSTRAINT fk_rr_reminder_config_id FOREIGN KEY (reminder_config_id) REFERENCES reminder_configuration(id),
    CONSTRAINT fk_rr_process_instance_id FOREIGN KEY (process_instance_id) REFERENCES process_instance(id),
    CONSTRAINT fk_rr_node_instance_id FOREIGN KEY (node_instance_id) REFERENCES process_node_instance(id),
    CONSTRAINT fk_rr_approver_instance_id FOREIGN KEY (approver_instance_id) REFERENCES node_approver_instance(id)
);

CREATE INDEX idx_rr_process_instance_id ON reminder_record(process_instance_id);
CREATE INDEX idx_rr_reminder_time ON reminder_record(reminder_time);
CREATE INDEX idx_rr_status ON reminder_record(status);

COMMENT ON TABLE reminder_record IS '提醒记录表';
COMMENT ON COLUMN reminder_record.id IS '主键ID';
COMMENT ON COLUMN reminder_record.reminder_config_id IS '提醒配置ID';
COMMENT ON COLUMN reminder_record.process_instance_id IS '流程实例ID';
COMMENT ON COLUMN reminder_record.node_instance_id IS '节点实例ID';
COMMENT ON COLUMN reminder_record.approver_instance_id IS '审批人实例ID';
COMMENT ON COLUMN reminder_record.reminder_type IS '提醒类型：TIMEOUT-超时提醒, APPROACHING_TIMEOUT-即将超时提醒, PERIODIC-周期性提醒';
COMMENT ON COLUMN reminder_record.reminder_time IS '提醒时间';
COMMENT ON COLUMN reminder_record.reminder_content IS '提醒内容';
COMMENT ON COLUMN reminder_record.status IS '状态：PENDING-待发送, SENT-已发送, FAILED-发送失败';
COMMENT ON COLUMN reminder_record.retry_count IS '重试次数';
COMMENT ON COLUMN reminder_record.error_message IS '错误信息';
COMMENT ON COLUMN reminder_record.created_by IS '创建人';
COMMENT ON COLUMN reminder_record.created_time IS '创建时间';
COMMENT ON COLUMN reminder_record.updated_by IS '更新人';
COMMENT ON COLUMN reminder_record.updated_time IS '更新时间';
```

### 14. 表单版本表 (FORM_VERSION)

```sql
CREATE TABLE form_version (
    id NUMBER(19) PRIMARY KEY,
    process_definition_id NUMBER(19) NOT NULL,
    version NUMBER(10) NOT NULL,
    form_config CLOB NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_fv_process_definition_id FOREIGN KEY (process_definition_id) REFERENCES process_definition(id),
    CONSTRAINT uk_process_def_version UNIQUE (process_definition_id, version)
);

COMMENT ON TABLE form_version IS '表单版本表';
COMMENT ON COLUMN form_version.id IS '主键ID';
COMMENT ON COLUMN form_version.process_definition_id IS '流程定义ID';
COMMENT ON COLUMN form_version.version IS '版本号';
COMMENT ON COLUMN form_version.form_config IS '表单配置JSON';
COMMENT ON COLUMN form_version.effective_from IS '生效开始时间';
COMMENT ON COLUMN form_version.effective_to IS '生效结束时间，为NULL表示一直有效';
COMMENT ON COLUMN form_version.created_by IS '创建人';
COMMENT ON COLUMN form_version.created_time IS '创建时间';
COMMENT ON COLUMN form_version.updated_by IS '更新人';
COMMENT ON COLUMN form_version.updated_time IS '更新时间';
COMMENT ON COLUMN form_version.is_deleted IS '是否删除：0-否，1-是';
```

### 15. 申请人预选审批人表 (PRESELECTED_APPROVER)

```sql
CREATE TABLE preselected_approver (
    id NUMBER(19) PRIMARY KEY,
    process_instance_id NUMBER(19) NOT NULL,
    node_definition_id NUMBER(19) NOT NULL,
    approver_id NUMBER(19) NOT NULL,
    approver_name VARCHAR2(100) NOT NULL,
    created_by VARCHAR2(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_by VARCHAR2(50),
    updated_time TIMESTAMP,
    CONSTRAINT fk_pa_process_instance_id FOREIGN KEY (process_instance_id) REFERENCES process_instance(id),
    CONSTRAINT fk_pa_node_definition_id FOREIGN KEY (node_definition_id) REFERENCES approval_node_definition(id)
);

CREATE INDEX idx_pa_process_instance_id ON preselected_approver(process_instance_id);
CREATE INDEX idx_pa_node_definition_id ON preselected_approver(node_definition_id);

COMMENT ON TABLE preselected_approver IS '申请人预选审批人表';
COMMENT ON COLUMN preselected_approver.id IS '主键ID';
COMMENT ON COLUMN preselected_approver.process_instance_id IS '流程实例ID';
COMMENT ON COLUMN preselected_approver.node_definition_id IS '节点定义ID';
COMMENT ON COLUMN preselected_approver.approver_id IS '审批人ID';
COMMENT ON COLUMN preselected_approver.approver_name IS '审批人姓名';
COMMENT ON COLUMN preselected_approver.created_by IS '创建人';
COMMENT ON COLUMN preselected_approver.created_time IS '创建时间';
COMMENT ON COLUMN preselected_approver.updated_by IS '更新人';
COMMENT ON COLUMN preselected_approver.updated_time IS '更新时间';
```

## 优化后的枚举定义

以下是完整的枚举定义，供代码开发使用：

```java
// 流程状态枚举
public enum ProcessStatus {
    ACTIVE,      // 激活状态
    DISABLED,    // 禁用状态
    ARCHIVED     // 归档状态
}

// 流程实例状态枚举
public enum ProcessInstanceStatus {
    DRAFT,       // 草稿
    IN_PROGRESS, // 进行中
    APPROVED,    // 已批准
    REJECTED,    // 已拒绝
    CANCELED,    // 已取消
    REWORK       // 重做
}

// 节点类型枚举
public enum NodeType {
    NORMAL,      // 普通节点
    PARALLEL,    // 并行节点
    CONDITIONAL  // 条件节点
}

// 审批人类型枚举
public enum ApproverType {
    ROLE,        // 角色
    USER,        // 用户
    DEPARTMENT,  // 部门
    HIERARCHY,   // 层级
    EXPRESSION,  // 表达式（SpEL）
    SYSTEM       // 系统
}

// 审批策略枚举
public enum ApprovalStrategy {
    ALL,         // 所有人通过
    ANY          // 任一人通过
}

// 节点实例状态枚举
public enum NodeInstanceStatus {
    PENDING,     // 待处理
    IN_PROGRESS, // 处理中
    APPROVED,    // 已批准
    REJECTED,    // 已拒绝
    CANCELED,    // 已取消
    REWORK       // 重做
}

// 审批状态枚举
public enum ApprovalStatus {
    PENDING,     // 待处理
    APPROVED,    // 已批准
    REJECTED,    // 已拒绝
    TRANSFERRED  // 已转交
}

// 操作类型枚举
public enum ActionType {
    APPROVE,     // 批准
    REJECT,      // 拒绝
    REWORK,      // 重做
    TRANSFER,    // 转交
    CANCEL,      // 取消
    REMIND       // 提醒
}

// 操作状态枚举
public enum ActionStatus {
    SUCCESS,     // 成功
    FAIL         // 失败
}

// 操作类型枚举
public enum OperationType {
    CREATE,      // 创建
    SUBMIT,      // 提交
    APPROVE,     // 批准
    REJECT,      // 拒绝
    REWORK,      // 重做
    TRANSFER,    // 转交
    CANCEL,      // 取消
    REMIND,      // 提醒
    TIMEOUT      // 超时
}

// 流程优先级枚举
public enum ProcessPriority {
    HIGH,        // 高优先级
    NORMAL,      // 正常优先级
    LOW          // 低优先级
}

// 转换类型枚举
public enum TransitionType {
    NORMAL,          // 普通转换
    CONDITIONAL,     // 条件转换
    PARALLEL_START,  // 并行开始
    PARALLEL_END     // 并行结束
}

// 重做类型枚举
public enum ReworkType {
    TO_INITIATOR,     // 退回发起人
    TO_PREV_NODE,     // 退回上一节点
    TO_SPECIFIC_NODE  // 退回指定节点
}

// 提醒类型枚举
public enum ReminderType {
    TIMEOUT,             // 超时提醒
    APPROACHING_TIMEOUT, // 即将超时提醒
    PERIODIC             // 周期性提醒
}

// 提醒状态枚举
public enum ReminderStatus {
    PENDING,  // 待发送
    SENT,     // 已发送
    FAILED    // 发送失败
}
```

## SpEL表达式示例

结合案例需求，以下是几个关键场景的SpEL表达式示例：

### 1. 直接主管审批配置
```java
// 表达式：获取申请人的直接主管
"#{applicant.directManager}"
```

### 2. 条件分支表达式
```java
// 高级晋升路径条件
"#{promotion.level == 'SENIOR'}"

// 初级晋升路径条件
"#{promotion.level == 'JUNIOR'}"

// 默认条件（始终为真）
"true"
```

### 3. 动态角色表达式
```java
// 获取申请人所在部门的经理
"#{applicant.department.manager}"

// 获取申请人所在业务线的风控负责人
"#{applicant.businessLine.riskManager}"
```

## 完整示例 - 信贷员晋升流程配置

以下是一个完整的"信贷员晋升流程"配置示例（JSON格式）：

```json
{
  "processCode": "CREDIT_OFFICER_PROMOTION",
  "processName": "信贷员晋升流程",
  "processTypeCode": "HR",
  "description": "信贷员职级晋升审批流程",
  "formConfig": {
    "fields": [
      {
        "id": "employeeId",
        "label": "员工编号",
        "type": "text",
        "required": true,
        "readOnly": false
      },
      {
        "id": "employeeName",
        "label": "员工姓名",
        "type": "text",
        "required": true,
        "readOnly": false
      },
      {
        "id": "department",
        "label": "所属部门",
        "type": "text",
        "required": true,
        "readOnly": false
      },
      {
        "id": "currentLevel",
        "label": "当前级别",
        "type": "select",
        "options": ["JUNIOR", "MIDDLE", "SENIOR", "EXPERT"],
        "required": true,
        "readOnly": false
      },
      {
        "id": "targetLevel",
        "label": "目标级别",
        "type": "select",
        "options": ["JUNIOR", "MIDDLE", "SENIOR", "EXPERT"],
        "required": true,
        "readOnly": false
      },
      {
        "id": "promotionReason",
        "label": "晋升理由",
        "type": "textarea",
        "required": true,
        "readOnly": false
      },
      {
        "id": "achievements",
        "label": "业绩表现",
        "type": "textarea",
        "required": true,
        "readOnly": false
      },
      {
        "id": "effectiveDate",
        "label": "生效日期",
        "type": "date",
        "required": true,
        "readOnly": false
      },
      {
        "id": "attachments",
        "label": "附件",
        "type": "file",
        "required": false,
        "readOnly": false,
        "multiple": true
      }
    ],
    "layout": {
      "sections": [
        {
          "title": "基本信息",
          "fields": ["employeeId", "employeeName", "department"]
        },
        {
          "title": "晋升信息",
          "fields": ["currentLevel", "targetLevel", "effectiveDate"]
        },
        {
          "title": "申请说明",
          "fields": ["promotionReason", "achievements", "attachments"]
        }
      ]
    }
  },
  "timeoutDays": 7,
  "status": "ACTIVE",
  "priority": "NORMAL",
  "nodes": [
    {
      "nodeKey": "dept_review",
      "nodeName": "部门初审",
      "nodeType": "NORMAL",
      "approverType": "EXPRESSION",
      "approvalStrategy": "ALL",
      "timeoutHours": 24,
      "isStartNode": true,
      "isEndNode": false,
      "allowApproverSelection": false,
      "formPermissions": {
        "employeeId": "READ",
        "employeeName": "READ",
        "department": "READ",
        "currentLevel": "READ",
        "targetLevel": "READ",
        "promotionReason": "READ",
        "achievements": "READ",
        "effectiveDate": "READ",
        "attachments": "READ",
        "deptManagerComment": "EDIT"
      }
    },
    {
      "nodeKey": "business_review",
      "nodeName": "业务部门审批",
      "nodeType": "NORMAL",
      "approverType": "ROLE",
      "approvalStrategy": "ALL",
      "timeoutHours": 48,
      "isStartNode": false,
      "isEndNode": false,
      "allowApproverSelection": false,
      "formPermissions": {
        "employeeId": "READ",
        "employeeName": "READ",
        "department": "READ",
        "currentLevel": "READ",
        "targetLevel": "READ",
        "promotionReason": "READ",
        "achievements": "READ",
        "effectiveDate": "READ",
        "attachments": "READ",
        "businessComment": "EDIT"
      }
    },
    {
      "nodeKey": "risk_review",
      "nodeName": "风控部门审批",
      "nodeType": "NORMAL",
      "approverType": "ROLE",
      "approvalStrategy": "ALL",
      "timeoutHours": 48,
      "isStartNode": false,
      "isEndNode": false,
      "allowApproverSelection": false,
      "formPermissions": {
        "employeeId": "READ",
        "employeeName": "READ",
        "department": "READ",
        "currentLevel": "READ",
        "targetLevel": "READ",
        "promotionReason": "READ",
        "achievements": "READ",
        "effectiveDate": "READ",
        "attachments": "READ",
        "riskComment": "EDIT"
      }
    },
    {
      "nodeKey": "level_check",
      "nodeName": "级别检查",
      "nodeType": "CONDITIONAL",
      "approverType": "SYSTEM",
      "approvalStrategy": "ALL",
      "timeoutHours": 1,
      "isStartNode": false,
      "isEndNode": false,
      "allowApproverSelection": false
    },
    {
      "nodeKey": "director_approval",
      "nodeName": "总监审批",
      "nodeType": "NORMAL",
      "approverType": "ROLE",
      "approvalStrategy": "ANY",
      "timeoutHours": 72,
      "isStartNode": false,
      "isEndNode": false,
      "allowApproverSelection": true,
      "formPermissions": {
        "employeeId": "READ",
        "employeeName": "READ",
        "department": "READ",
        "currentLevel": "READ",
        "targetLevel": "READ",
        "promotionReason": "READ",
        "achievements": "READ",
        "effectiveDate": "READ",
        "attachments": "READ",
        "directorComment": "EDIT"
      }
    },
    {
      "nodeKey": "hr_confirm",
      "nodeName": "人力资源确认",
      "nodeType": "NORMAL",
      "approverType": "DEPARTMENT",
      "approvalStrategy": "ALL",
      "timeoutHours": 48,
      "isStartNode": false,
      "isEndNode": true,
      "allowApproverSelection": false,
      "formPermissions": {
        "employeeId": "READ",
        "employeeName": "READ",
        "department": "READ",
        "currentLevel": "READ",
        "targetLevel": "READ",
        "promotionReason": "READ",
        "achievements": "READ",
        "effectiveDate": "EDIT",
        "attachments": "READ",
        "hrComment": "EDIT"
      }
    }
  ],
  "nodeApprovers": [
    {
      "nodeKey": "dept_review",
      "approvers": [
        {
          "approverType": "EXPRESSION",
          "expression": "#{applicant.directManager}",
          "description": "申请人直接主管"
        }
      ]
    },
    {
      "nodeKey": "business_review",
      "approvers": [
        {
          "approverType": "ROLE",
          "approverId": "BUSINESS_MANAGER",
          "description": "业务部门经理"
        }
      ]
    },
    {
      "nodeKey": "risk_review",
      "approvers": [
        {
          "approverType": "ROLE",
          "approverId": "RISK_MANAGER",
          "description": "风控部门经理"
        }
      ]
    },
    {
      "nodeKey": "director_approval",
      "approvers": [
        {
          "approverType": "ROLE",
          "approverId": "DIRECTOR",
          "description": "总监",
          "isRequired": false
        }
      ]
    },
    {
      "nodeKey": "hr_confirm",
      "approvers": [
        {
          "approverType": "DEPARTMENT",
          "approverId": "HR_DEPARTMENT",
          "description": "人力资源部门"
        }
      ]
    }
  ],
  "transitions": [
    {
      "sourceNodeKey": "dept_review",
      "targetNodeKey": "business_review",
      "transitionType": "NORMAL"
    },
    {
      "sourceNodeKey": "business_review",
      "targetNodeKey": "risk_review",
      "transitionType": "NORMAL"
    },
    {
      "sourceNodeKey": "risk_review",
      "targetNodeKey": "level_check",
      "transitionType": "NORMAL"
    },
    {
      "sourceNodeKey": "level_check",
      "targetNodeKey": "director_approval",
      "transitionType": "CONDITIONAL",
      "conditionExpression": "#{form.targetLevel == 'SENIOR' || form.targetLevel == 'EXPERT'}",
      "priority": 1,
      "description": "高级晋升路径"
    },
    {
      "sourceNodeKey": "level_check",
      "targetNodeKey": "hr_confirm",
      "transitionType": "CONDITIONAL",
      "conditionExpression": "#{form.targetLevel == 'JUNIOR' || form.targetLevel == 'MIDDLE'}",
      "priority": 2,
      "description": "初级晋升路径"
    },
    {
      "sourceNodeKey": "director_approval",
      "targetNodeKey": "hr_confirm",
      "transitionType": "NORMAL"
    }
  ],
  "parallelGroups": [
    {
      "groupKey": "business_risk_review",
      "parentNodeKey": "parallel_review",
      "childNodeKeys": ["business_review", "risk_review"]
    }
  ],
  "reworkConfigs": [
    {
      "nodeKey": "hr_confirm",
      "reworkType": "TO_SPECIFIC_NODE",
      "targetNodeKey": "dept_review",
      "allowCommentRequired": true,
      "description": "HR可退回至部门初审"
    },
    {
      "nodeKey": "director_approval",
      "reworkType": "TO_PREV_NODE",
      "allowCommentRequired": true,
      "description": "总监可退回至上一节点"
    },
    {
      "nodeKey": "business_review",
      "reworkType": "TO_INITIATOR",
      "allowCommentRequired": true,
      "description": "业务部门可退回至发起人"
    },
    {
      "nodeKey": "risk_review",
      "reworkType": "TO_INITIATOR",
      "allowCommentRequired": true,
      "description": "风控部门可退回至发起人"
    }
  ],
  "reminderConfigs": [
    {
      "nodeKey": null,
      "reminderType": "APPROACHING_TIMEOUT",
      "timeExpression": "-4h",
      "reminderTemplateCode": "NODE_APPROACHING_TIMEOUT",
      "enabled": true,
      "description": "节点即将超时提醒（4小时前）"
    },
    {
      "nodeKey": null,
      "reminderType": "TIMEOUT",
      "timeExpression": "0",
      "reminderTemplateCode": "NODE_TIMEOUT",
      "enabled": true,
      "description": "节点超时提醒"
    },
    {
      "nodeKey": "director_approval",
      "reminderType": "PERIODIC",
      "timeExpression": "0 0 9 * * ?",
      "reminderTemplateCode": "SENIOR_PROMOTION_REMINDER",
      "enabled": true,
      "description": "高级晋升每天9点提醒总监"
    }
  ]
}
```

## 支持核心需求的实现方式

已优化的数据库设计完整支持所有核心需求，以下是各需求的具体实现方式：

1. **支持信贷员晋升流程定义及管理**
   - `process_definition`表存储流程定义
   - `form_version`表支持表单版本控制
   - 支持流程状态管理（激活、禁用、归档）

2. **支持多种类型的审批节点**
   - `approval_node_definition`表的`node_type`字段支持普通节点、并行节点和条件节点
   - `node_transition`表定义节点间关系，支持条件路由
   - `parallel_node_group`表支持并行节点组织

3. **灵活的审批人配置**
   - `node_approver_definition`表支持多种审批人类型（角色、用户、部门、表达式）
   - `expression`字段存储SpEL表达式，支持动态计算审批人

4. **申请人预选审批人**
   - `approval_node_definition`表中的`allow_approver_selection`标识是否允许预选
   - `preselected_approver`表存储预选的审批人
   - `node_approver_instance`表中的`is_preselected`标识是否预选审批人

5. **流程实例创建和执行**
   - `process_instance`表存储流程实例
   - `process_node_instance`表存储节点实例
   - `node_approver_instance`表存储审批人实例

6. **审批流转及记录**
   - `approval_record`表记录所有审批操作
   - `process_operation_log`表记录详细操作日志

7. **退回重做机制**
   - `rework_configuration`表支持灵活的重做配置
   - 支持退回至发起人、上一节点或指定节点

8. **审批转交功能**
   - `node_approver_instance`表中的`transferred_to_id`和`transferred_to_name`字段
   - `approval_record`表记录转交操作

9. **超时提醒和处理**
   - `reminder_configuration`表配置提醒规则
   - `reminder_record`表记录提醒历史
   - 支持即将超时、已超时和周期性提醒

10. **展示进度图**
    - 通过`node_transition`表的节点关系数据可生成流程图
    - `process_node_instance`和`node_approver_instance`提供当前状态数据

11. **电子表单设计**
    - `form_config`字段存储表单设计
    - `form_version`表支持表单版本控制
    - `form_permissions`支持节点级表单权限控制

