# AMS 目标复刻第三阶段 Report/Admin/HR Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将剩余 `报告`、`系统/权限经理`、`系统管理员`、`HR` 路由从通用骨架页替换为目标风格的真实业务页，覆盖现有 31 页地图的最后一批空白。

**Architecture:** 继续保持 `Vue 3 + Vue Router + Pinia + Spring Boot 3`。优先复用已有 `requests`、`users`、`departments`、`device-accounts`、`assets`、`audit`、`query` 接口，在前端按域构建 4 个专用工作台视图；对现有后端尚不支持的管理类页面，优先做真实数据投影与只读/轻操作态，而不是继续停留在静态骨架。

**Tech Stack:** Vue 3, TypeScript, Vitest, Spring Boot 3 existing APIs

---

## Scope Check

本阶段覆盖：

- `#/report/accountHistory`
- `#/report/reviewReport`
- `#/report/empAccess`
- `#/report/systemAccess`
- `#/report/sysconfig`
- `#/report/variationReport`
- `#/sysAccessManager/empAccessManagement`
- `#/sysAccessManager/reviewCheck`
- `#/systemAdmin/userAccessManagement`
- `#/systemAdmin/empAccessManagement`
- `#/systemAdmin/copyProfile`
- `#/systemAdmin/sysAccessAdmin`
- `#/systemAdmin/reviewManagement`
- `#/systemAdmin/admindelegation`
- `#/systemAdmin/adminworkflow`
- `#/systemAdmin/applicationConfig`
- `#/systemAdmin/mailTemplateCfg`
- `#/hrAdmin/hrManagement`
- `#/hrAdmin/enterprisemaintain`

本阶段不做：

- 新增复杂数据库模型去完整实现邮件模板、工作流、代理、审核计划的持久化
- 多角色权限控制的完整细粒度授权
- 导出 PDF / Excel 的真实文件生成

## File Structure

### Create

- `frontend/src/views/report/ReportCenterView.vue`
  责任：承载六类报告页，用现有接口投影真实数据。
- `frontend/src/views/manager/ManagerWorkbenchView.vue`
  责任：承载系统/权限经理两页。
- `frontend/src/views/system-admin/SystemAdminWorkbenchView.vue`
  责任：承载系统管理员九页。
- `frontend/src/views/hr-admin/HrWorkbenchView.vue`
  责任：承载 HR 两页。
- `frontend/src/tests/report-admin-pages.spec.ts`
  责任：验证代表性路由已从骨架页切换、页面文案正确、真实数据已落地。

### Modify

- `frontend/src/router/index.ts`
  责任：将剩余四个域的路由切换到专用视图。

## Execution Outline

### Task 1: 用失败测试锁定剩余四个域的路由切换

**Files:**
- Test: `frontend/src/tests/report-admin-pages.spec.ts`
- Modify: `frontend/src/router/index.ts`

- [ ] 先写失败测试，覆盖至少 4 条代表性路由：
  - `report/accountHistory`
  - `sysAccessManager/empAccessManagement`
  - `systemAdmin/applicationConfig`
  - `hrAdmin/hrManagement`
- [ ] 跑测试，确认失败点来自当前还在用 `TargetPageView`。
- [ ] 修改路由映射，让测试进入真实视图断言。

### Task 2: 实现报告域工作台

**Files:**
- Create: `frontend/src/views/report/ReportCenterView.vue`
- Test: `frontend/src/tests/report-admin-pages.spec.ts`

- [ ] 先写失败测试，断言 `accountHistory` / `empAccess` / `variationReport` 渲染真实数据行。
- [ ] 用 `fetchRequests`、`fetchUsers`、`fetchDeviceAccounts`、`fetchAssetTree`、`fetchAuditLogs`、`fetchDevicePermissions` 组合出六类报告视图。
- [ ] 保留目标风格按钮与表头，不回退到通用占位表格。

### Task 3: 实现经理与系统管理员域

**Files:**
- Create: `frontend/src/views/manager/ManagerWorkbenchView.vue`
- Create: `frontend/src/views/system-admin/SystemAdminWorkbenchView.vue`
- Test: `frontend/src/tests/report-admin-pages.spec.ts`

- [ ] 先写失败测试，断言经理页和管理员页出现真实账号/系统/配置数据。
- [ ] `empAccessManagement` 与 `userAccessManagement` 基于账号和权限数据渲染。
- [ ] `sysAccessAdmin`、`applicationConfig`、`adminworkflow`、`mailTemplateCfg` 至少落成可读且有操作入口的目标风格列表。
- [ ] `copyProfile`、`admindelegation`、`reviewManagement` 先落成真实数据投影视图，不再是静态空表。

### Task 4: 实现 HR 域

**Files:**
- Create: `frontend/src/views/hr-admin/HrWorkbenchView.vue`
- Test: `frontend/src/tests/report-admin-pages.spec.ts`

- [ ] 先写失败测试，断言 `hrManagement` 和 `enterprisemaintain` 有真实用户/部门数据。
- [ ] 用 `fetchUsers` 和 `fetchDepartments` 实现目标风格 HR 表格页。

### Task 5: 回归验证

**Files:**
- Modify: `frontend/src/tests/target-clone-pages.spec.ts`

- [ ] 跑新增定向测试。
- [ ] 跑前端全量测试。
- [ ] 跑前端构建。

## Self-Review

### Spec coverage

- 报告域：Task 2
- 经理域：Task 3
- 系统管理员域：Task 3
- HR 域：Task 4
- 回归：Task 5

### Placeholder scan

- 无 `TODO` / `TBD`
- 已明确目标文件与测试路径

### Type consistency

- 继续以现有前端 API 类型为准
- 路由切换仍统一走 `resolveTargetCloneComponent`
