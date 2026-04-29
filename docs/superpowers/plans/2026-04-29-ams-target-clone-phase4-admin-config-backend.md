# AMS 目标复刻第四阶段 Admin Config Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `systemAdmin/applicationConfig` 与 `systemAdmin/mailTemplateCfg` 从前端投影/种子内容升级为真实数据库资源、后端 CRUD 接口和前端实时数据页。

**Architecture:** 延续现有 `JdbcTemplate + Repository + Service + Controller + Flyway` 模式。后端新增两组资源表与 CRUD API；前端新增 `admin-config` API 适配，并把系统管理员工作台的两个页面切到真实列表和写操作。

**Tech Stack:** Spring Boot 3, Flyway, JdbcTemplate, Vue 3, TypeScript, Vitest, JUnit 5, MockMvc

---

## Scope Check

本阶段只覆盖：

- `#/systemAdmin/applicationConfig`
- `#/systemAdmin/mailTemplateCfg`
- 对应后端真实表、种子数据、列表/新增/修改/删除 API

本阶段不覆盖：

- `admindelegation`
- `adminworkflow`
- `reviewManagement`
- `copyProfile`
- `sysAccessAdmin` 的完整持久化模型

## File Structure

### Create

- `backend/src/main/resources/db/migration/V7__admin_config_resources.sql`
- `backend/src/main/java/com/company/ams/common/persistence/ApplicationConfigRepository.java`
- `backend/src/main/java/com/company/ams/common/persistence/MailTemplateRepository.java`
- `backend/src/main/java/com/company/ams/admin/ApplicationConfigRow.java`
- `backend/src/main/java/com/company/ams/admin/ApplicationConfigUpsertCommand.java`
- `backend/src/main/java/com/company/ams/admin/ApplicationConfigService.java`
- `backend/src/main/java/com/company/ams/admin/ApplicationConfigController.java`
- `backend/src/main/java/com/company/ams/admin/MailTemplateRow.java`
- `backend/src/main/java/com/company/ams/admin/MailTemplateUpsertCommand.java`
- `backend/src/main/java/com/company/ams/admin/MailTemplateService.java`
- `backend/src/main/java/com/company/ams/admin/MailTemplateController.java`
- `backend/src/test/java/com/company/ams/admin/ApplicationConfigControllerTest.java`
- `backend/src/test/java/com/company/ams/admin/ApplicationConfigCrudIntegrationTest.java`
- `backend/src/test/java/com/company/ams/admin/MailTemplateControllerTest.java`
- `backend/src/test/java/com/company/ams/admin/MailTemplateCrudIntegrationTest.java`
- `frontend/src/api/admin-config.ts`
- `frontend/src/tests/system-admin-config.spec.ts`

### Modify

- `frontend/src/views/system-admin/SystemAdminWorkbenchView.vue`

## Execution Outline

### Task 1: 为应用配置资源写失败测试

**Files:**
- Create: `backend/src/test/java/com/company/ams/admin/ApplicationConfigControllerTest.java`
- Create: `backend/src/test/java/com/company/ams/admin/ApplicationConfigCrudIntegrationTest.java`

- [ ] 先写 WebMvc 测试，断言列表与创建响应结构。
- [ ] 再写集成测试，断言 create/update/delete 真正写库。
- [ ] 运行定向后端测试，确认因接口缺失而失败。

### Task 2: 实现应用配置后端

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__admin_config_resources.sql`
- Create: `backend/src/main/java/com/company/ams/common/persistence/ApplicationConfigRepository.java`
- Create: `backend/src/main/java/com/company/ams/admin/ApplicationConfigRow.java`
- Create: `backend/src/main/java/com/company/ams/admin/ApplicationConfigUpsertCommand.java`
- Create: `backend/src/main/java/com/company/ams/admin/ApplicationConfigService.java`
- Create: `backend/src/main/java/com/company/ams/admin/ApplicationConfigController.java`

- [ ] 最小实现 list/create/update/delete。
- [ ] 复用现有 envelope 与软删除风格。
- [ ] 跑应用配置定向后端测试至通过。

### Task 3: 为邮件模板资源写失败测试

**Files:**
- Create: `backend/src/test/java/com/company/ams/admin/MailTemplateControllerTest.java`
- Create: `backend/src/test/java/com/company/ams/admin/MailTemplateCrudIntegrationTest.java`

- [ ] 写 WebMvc 测试，断言列表与创建响应结构。
- [ ] 写集成测试，断言 create/update/delete 真正写库。
- [ ] 运行定向测试，确认因接口缺失而失败。

### Task 4: 实现邮件模板后端

**Files:**
- Create: `backend/src/main/java/com/company/ams/common/persistence/MailTemplateRepository.java`
- Create: `backend/src/main/java/com/company/ams/admin/MailTemplateRow.java`
- Create: `backend/src/main/java/com/company/ams/admin/MailTemplateUpsertCommand.java`
- Create: `backend/src/main/java/com/company/ams/admin/MailTemplateService.java`
- Create: `backend/src/main/java/com/company/ams/admin/MailTemplateController.java`

- [ ] 最小实现 list/create/update/delete。
- [ ] 跑邮件模板定向后端测试至通过。

### Task 5: 前端接入真实系统管理员配置资源

**Files:**
- Create: `frontend/src/api/admin-config.ts`
- Create: `frontend/src/tests/system-admin-config.spec.ts`
- Modify: `frontend/src/views/system-admin/SystemAdminWorkbenchView.vue`

- [ ] 先写失败测试，断言 `applicationConfig` 和 `mailTemplateCfg` 使用真实 API 数据。
- [ ] 将这两个页面从静态行切到接口数据。
- [ ] 至少支持新增、修改、删除之一的真实前端触发链路；其余可先保留按钮框架。

### Task 6: 回归验证

**Files:**
- Modify: `frontend/src/tests/report-admin-pages.spec.ts`

- [ ] 跑新增定向前后端测试。
- [ ] 跑前端全量测试与构建。
- [ ] 跑本阶段相关后端测试。

## Self-Review

### Spec coverage

- 应用配置：Task 1 + Task 2 + Task 5
- 邮件模板：Task 3 + Task 4 + Task 5
- 回归：Task 6

### Placeholder scan

- 无 `TODO` / `TBD`
- 已明确新表、新接口、新测试、新前端接入点

### Type consistency

- 后端响应继续使用 `ApiResponse` + `ListPayload`
- 前端列表继续使用 `list/total` 结构
