# AMS 目标复刻第二阶段 Access/Task Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `权限管理` 与 `任务管理` 从通用骨架页替换为具备真实数据读取和最小可用写入动作的目标风格页面。

**Architecture:** 保持当前 `Vue 3 + Pinia + Vue Router + Element Plus` 与 `Spring Boot 3` 技术栈不变。前端按目标系统菜单拆分为 `access` 和 `task` 专用视图，复用现有 `users`、`assets`、`device-accounts`、`requests` 数据接口；后端最小新增审批动作接口，使权限申请页与任务处理页形成闭环。

**Tech Stack:** Vue 3, TypeScript, Vue Router, Pinia, Vitest, Spring Boot 3, MockMvc, JUnit 5

---

## Scope Check

本阶段只覆盖以下范围：

- `#/access/myRequest`
- `#/access/myRemove`
- `#/access/myChangepd`
- `#/access/myView`
- `#/access/accountManagement`
- `#/access/reviewCheck`
- `#/access/bedelegation`
- `#/task/accessApproval`
- `#/task/accessOperation`
- `#/task/reviewApproval`
- `#/task/revocation`

本阶段明确不做：

- 报告域、系统管理员域、HR 域的真实 CRUD
- 多动作审批流的完整业务规则
- 审核、申诉、代理的完整后端模型
- 目标系统全部弹窗细节的 1:1 还原

## File Structure

### Create

- `frontend/src/api/executions.ts`
  责任：封装执行任务完成动作接口。
- `frontend/src/api/tasks.ts`
  责任：封装审批动作接口并输出任务页需要的列表投影视图。
- `frontend/src/views/access/AccessWorkbenchView.vue`
  责任：统一承载 `myRequest`、`myRemove`、`myChangepd` 三类自助操作页。
- `frontend/src/views/access/AccessDirectoryView.vue`
  责任：统一承载 `myView`、`accountManagement` 两类账号与权限视图。
- `frontend/src/views/access/AccessAuditView.vue`
  责任：承载 `reviewCheck`、`bedelegation` 两类只读列表页。
- `frontend/src/views/task/TaskQueueView.vue`
  责任：统一承载四个任务管理页面，支持审批与执行动作。
- `frontend/src/tests/access-pages.spec.ts`
  责任：验证权限管理页路由映射、中文文案、核心交互与表格结构。
- `frontend/src/tests/task-pages.spec.ts`
  责任：验证任务页路由映射、列表过滤与动作按钮行为。
- `backend/src/test/java/com/company/ams/request/RequestControllerTest.java`
  责任：验证新增审批动作接口。

### Modify

- `frontend/src/router/index.ts`
  责任：将 `access/*` 与 `task/*` 路由切换到专用视图。
- `backend/src/main/java/com/company/ams/request/RequestController.java`
  责任：暴露审批动作接口。

## Execution Outline

### Task 1: 固化 Phase 2 路由与测试入口

**Files:**
- Modify: `frontend/src/router/index.ts`
- Test: `frontend/src/tests/access-pages.spec.ts`
- Test: `frontend/src/tests/task-pages.spec.ts`

- [ ] 写失败测试，确认 `access/*` 与 `task/*` 已从 `TargetPageView` 切到专用组件。
- [ ] 运行前端定向测试，确认失败点来自路由映射缺失。
- [ ] 最小修改路由映射，让失败测试进入组件内容断言。

### Task 2: 实现权限管理三类专用视图

**Files:**
- Create: `frontend/src/views/access/AccessWorkbenchView.vue`
- Create: `frontend/src/views/access/AccessDirectoryView.vue`
- Create: `frontend/src/views/access/AccessAuditView.vue`
- Test: `frontend/src/tests/access-pages.spec.ts`

- [ ] 先写失败测试，覆盖 `myRequest` 表单、`accountManagement` 列表、`reviewCheck` 只读表格三类页面。
- [ ] 用现有 `users`、`assets`、`device-accounts`、`requests` 接口提供真实读取。
- [ ] `myRequest` / `myRemove` / `myChangepd` 至少支持一类真实提交动作进入请求流。
- [ ] `myView` / `accountManagement` 展示账号、账号归属、可跳转动作。

### Task 3: 补齐最小任务处理 API

**Files:**
- Modify: `backend/src/main/java/com/company/ams/request/RequestController.java`
- Test: `backend/src/test/java/com/company/ams/request/RequestControllerTest.java`

- [ ] 先写失败测试，断言登录后可调用审批动作接口。
- [ ] 新增 `POST /api/requests/{requestId}/approve`。
- [ ] 返回当前状态，供前端刷新任务列表。

### Task 4: 实现任务管理专用视图

**Files:**
- Create: `frontend/src/api/executions.ts`
- Create: `frontend/src/api/tasks.ts`
- Create: `frontend/src/views/task/TaskQueueView.vue`
- Test: `frontend/src/tests/task-pages.spec.ts`

- [ ] 先写失败测试，覆盖 `accessApproval` 和 `accessOperation` 的真实动作按钮。
- [ ] 用请求列表投影出审批队列、执行队列、审核队列、申诉队列。
- [ ] `同意` 触发审批接口，`完成` 触发执行接口。
- [ ] `reviewApproval` 与 `revocation` 先落成目标风格列表页，保持真实数据占位。

### Task 5: 完整回归

**Files:**
- Modify: `frontend/src/tests/target-clone-pages.spec.ts`

- [ ] 运行新增前端测试。
- [ ] 运行前端全量测试与构建。
- [ ] 若后端修改，运行新增后端测试。

## Self-Review

### Spec coverage

- 权限管理域：Task 2
- 任务管理域：Task 3 + Task 4
- 目标路由替换：Task 1
- 验证与回归：Task 5

### Placeholder scan

- 无 `TODO` / `TBD`
- 已明确新增文件、修改文件、测试入口与业务边界

### Type consistency

- 前端任务列表统一基于 `RequestItem`
- 后端审批接口直接复用 `RequestService.advance`
- 执行动作继续复用 `ExecutionController`
