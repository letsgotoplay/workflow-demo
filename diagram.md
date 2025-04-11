# Workflow Process Diagram Analysis
After scanning the ProcessInstanceServiceImpl class, I'll create Mermaid diagrams to explain how process instances, node instances, approver instances, and other entities are managed throughout the workflow lifecycle.

## 1. Process Instance Lifecycle
```mermaid
stateDiagram-v2
    [*] --> DRAFT: createProcessInstance
    DRAFT --> IN_PROGRESS: submitProcessInstance
    IN_PROGRESS --> APPROVED: processNextNode (when end node)
    IN_PROGRESS --> REJECTED: handleRejectAction
    IN_PROGRESS --> REWORK: handleReworkAction
    REWORK --> IN_PROGRESS: submitProcessInstance
    APPROVED --> [*]
    REJECTED --> [*]
```
## 2. Node Instance Lifecycle
```mermaid
stateDiagram-v2
    [*] --> IN_PROGRESS: createNodeInstance
    IN_PROGRESS --> APPROVED: checkNodeComplete (true)
    IN_PROGRESS --> REJECTED: handleRejectAction
    IN_PROGRESS --> REWORK: handleReworkAction
    APPROVED --> [*]: processNextNode/processConditionalNode/processParallelNode
```
## 3. Approver Instance Lifecycle
```mermaid
stateDiagram-v2
    [*] --> PENDING: createApproverInstances
    PENDING --> APPROVED: handleApproveAction
    PENDING --> REJECTED: handleRejectAction
    PENDING --> TRANSFERRED: handleTransferAction
    TRANSFERRED --> [*]
    APPROVED --> [*]
    REJECTED --> [*]
```

## 4. Process Flow Sequence

```mermaid
sequenceDiagram
    participant Client
    participant ProcessInstance
    participant NodeInstance
    participant ApproverInstance
    participant ApprovalRecord
    participant OperationLog

    Client->>ProcessInstance: createProcessInstance
    ProcessInstance->>NodeInstance: Create start node instance
    NodeInstance->>ApproverInstance: Create approver instance
    ProcessInstance->>OperationLog: Log CREATE operation

    Client->>ProcessInstance: submitProcessInstance
    ProcessInstance->>ApproverInstance: Update start node approver status to APPROVED
    ProcessInstance->>ApprovalRecord: Create approval record
    ProcessInstance->>NodeInstance: Update node status to APPROVED
    ProcessInstance->>NodeInstance: Process next node
    ProcessInstance->>OperationLog: Log SUBMIT operation

    loop Until process completes
        alt Conditional Node
            NodeInstance->>NodeInstance: Evaluate conditions
            NodeInstance->>NodeInstance: Create next node based on condition
        else Parallel Node
            NodeInstance->>NodeInstance: Create all child nodes
        else Regular Node
            NodeInstance->>NodeInstance: Create next node
        end

        NodeInstance->>ApproverInstance: Create approver instances
        
        alt User Approver
            ApproverInstance->>ApproverInstance: Create user approver
        else Role Approver
            ApproverInstance->>ApproverInstance: Create role approvers
        else Department Approver
            ApproverInstance->>ApproverInstance: Create department approvers
        else Expression Approver
            ApproverInstance->>ApproverInstance: Evaluate expression and create approver
        end

        Client->>ApproverInstance: Process approval action
        
        alt Approve
            ApproverInstance->>ApproverInstance: Update status to APPROVED
            ApproverInstance->>ApprovalRecord: Create approval record
            ApproverInstance->>NodeInstance: Check if node complete
            NodeInstance->>NodeInstance: If complete, update status to APPROVED
            NodeInstance->>NodeInstance: Process next node
        else Reject
            ApproverInstance->>ApproverInstance: Update status to REJECTED
            ApproverInstance->>NodeInstance: Update status to REJECTED
            ApproverInstance->>ProcessInstance: Update status to REJECTED
            ApproverInstance->>ApprovalRecord: Create approval record
        else Rework
            ApproverInstance->>ApproverInstance: Update status to APPROVED
            ApproverInstance->>NodeInstance: Update status to REWORK
            ApproverInstance->>ProcessInstance: Update status to REWORK or IN_PROGRESS
            ApproverInstance->>ApprovalRecord: Create approval record
        else Transfer
            ApproverInstance->>ApproverInstance: Update status to TRANSFERRED
            ApproverInstance->>ApproverInstance: Create new approver instance
            ApproverInstance->>ApprovalRecord: Create approval record
        end
        
        ApproverInstance->>OperationLog: Log operation
    end

    ProcessInstance->>ProcessInstance: Update status to APPROVED/REJECTED

```
## 5. Parallel Node Processing
```mermaid
flowchart TD
    A[Process Parallel Node] --> B{Is Child Node?}
    B -->|Yes| C[Check if all siblings complete]
    B -->|No| D[Create all child nodes]
    
    C -->|All Complete| E[Update parent node status]
    C -->|Not All Complete| F[Wait for other nodes]
    
    E --> G[Process parent's next node]
    D --> H[Keep process instance at parent node]
```
## 6. Conditional Node Processing

```mermaid
flowchart TD
    A[Process Conditional Node] --> B[Get all transitions]
    B --> C[Deserialize form data]
    C --> D[Evaluate conditions in priority order]
    D --> E{Match found?}
    E -->|Yes| F[Create target node instance]
    E -->|No| G[Use default transition]
    G --> F
    F --> H[Update process instance current node]
```

##7. Rework Processing
```mermaid
flowchart TD
    A[Handle Rework Action] --> B[Update approver status]
    B --> C[Update node status to REWORK]
    C --> D[Get rework configuration]
    D --> E{Rework Type?}
    
    E -->|TO_INITIATOR| F[Set process status to REWORK]
    E -->|TO_PREV_NODE| G[Find previous node]
    E -->|TO_SPECIFIC_NODE| H[Use configured target node]
    
    G --> I[Create new node instance]
    H --> I
    I --> J[Update process instance]
```

## 8. Entity Relationship
```mermaid
erDiagram
    ProcessInstance ||--o{ ProcessNodeInstance : "has"
    ProcessNodeInstance ||--o{ NodeApproverInstance : "has"
    ProcessNodeInstance ||--o{ ProcessNodeInstance : "parent-child"
    ProcessInstance ||--o{ PreselectedApprover : "has"
    ProcessInstance ||--o{ ApprovalRecord : "has"
    ProcessInstance ||--o{ ProcessOperationLog : "has"
    ProcessNodeInstance ||--o{ ApprovalRecord : "has"
    NodeApproverInstance ||--o{ ApprovalRecord : "has"
    
    ProcessDefinition ||--o{ ProcessInstance : "defines"
    ApprovalNodeDefinition ||--o{ ProcessNodeInstance : "defines"
    NodeApproverDefinition ||--o{ NodeApproverInstance : "defines"
    NodeTransition ||--o{ ProcessNodeInstance : "connects"
    ParallelNodeGroup ||--o{ ProcessNodeInstance : "groups"
```

These diagrams capture the key workflows and state transitions in the process management system, showing how entities are created, updated, and related throughout the process lifecycle.