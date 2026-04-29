# AMS2.0 UI Cockpit and CRUD Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 AMS2.0 工程上完成业务驾驶舱式 UI 重构，并交付用户、部门、设备账号三类基础资料的可用 CRUD 与审批联动闭环。

**Architecture:** 以现有 Spring Boot 3 + JdbcTemplate + Flyway + Vue 3 + Element Plus 工程为基础，先补齐后端业务错误语义、统计接口、基础资料 CRUD 和 `device_account` 迁移，再在前端建立统一 cockpit 壳层、指标区、工具栏和右侧抽屉式编辑模型。敏感权限动作不在管理页直接改数据，而是在设备账号页生成预填请求并跳转到现有申请流程。

**Tech Stack:** Vue 3, TypeScript, Vite, Element Plus, Pinia, Vue Router, Axios, Java 17, Spring Boot 3, Spring Security, JdbcTemplate, Flyway, H2/MySQL, JUnit 5, Vitest, Vue Test Utils, Playwright, Docker Compose

---

## Workspace Notes

- 当前目录 `E:\onedriver\OneDrive\work\山寨ams` 没有 `.git`，所以每个任务最后的“提交”步骤统一改为“checkpoint”。如果后续初始化 git，把同一步里的 `git add` 和 `git commit` 命令恢复执行即可。
- 所有命令默认在仓库根目录 `E:\onedriver\OneDrive\work\山寨ams` 执行。
- 前端单测使用 `npm --prefix frontend test -- <spec>`，后端单测使用 `mvn -f backend/pom.xml -Dtest=<ClassName> test`。
- 最终联调和上线验收使用现有 `docker-compose.yml`、`ops/backend/Dockerfile`、`ops/frontend/Dockerfile`，目标访问地址固定为 `http://127.0.0.1:5173/login`。

## File Map

### Backend

- Create: `backend/src/main/java/com/company/ams/common/api/BusinessException.java`
  责任：表达可预期业务错误，避免 CRUD 保护规则回退成 500。
- Create: `backend/src/main/java/com/company/ams/common/api/ApiExceptionHandler.java`
  责任：统一把 `BusinessException` 和 `IllegalArgumentException` 包装成结构化 `ApiResponse`。
- Create: `backend/src/main/java/com/company/ams/common/api/ListPayload.java`
  责任：统一列表接口返回结构，替换当前散落在 controller 内的局部 record。
- Create: `backend/src/main/java/com/company/ams/common/persistence/DashboardRepository.java`
  责任：聚合工作台指标、异常提醒和最近申请。
- Create: `backend/src/main/java/com/company/ams/dashboard/DashboardController.java`
  责任：提供 `GET /api/dashboard/summary`。
- Create: `backend/src/main/java/com/company/ams/dashboard/DashboardService.java`
  责任：组装 dashboard DTO，不让 controller 直接拼接数据。
- Create: `backend/src/main/java/com/company/ams/dashboard/DashboardSummary.java`
  责任：定义前端工作台需要的 metrics、alerts、recentRequests、quickActions。
- Create: `backend/src/main/java/com/company/ams/user/UserUpsertCommand.java`
  责任：定义用户新增/编辑入参。
- Create: `backend/src/main/java/com/company/ams/user/UserStatusCommand.java`
  责任：定义用户启停用入参。
- Create: `backend/src/main/java/com/company/ams/user/DepartmentUpsertCommand.java`
  责任：定义部门新增/编辑入参。
- Create: `backend/src/main/java/com/company/ams/account/DeviceAccountUpsertCommand.java`
  责任：定义设备账号新增/编辑入参。
- Modify: `backend/src/main/java/com/company/ams/user/UserController.java`
  责任：补齐用户列表、新增、编辑、状态切换、删除接口。
- Modify: `backend/src/main/java/com/company/ams/user/UserService.java`
  责任：承接用户业务校验和删除保护。
- Modify: `backend/src/main/java/com/company/ams/user/UserRow.java`
  责任：补足 `departmentId` 等前端编辑必需字段。
- Modify: `backend/src/main/java/com/company/ams/common/persistence/UserRepository.java`
  责任：实现用户 CRUD、重复校验和关联引用检查。
- Modify: `backend/src/main/java/com/company/ams/user/DepartmentController.java`
  责任：补齐部门列表、新增、编辑、删除接口。
- Modify: `backend/src/main/java/com/company/ams/user/DepartmentService.java`
  责任：实现成员数保护和负责人管理规则。
- Modify: `backend/src/main/java/com/company/ams/user/DepartmentRow.java`
  责任：补足 `managerUserId`、`description`、`memberCount`、`updatedAt`。
- Modify: `backend/src/main/java/com/company/ams/common/persistence/DepartmentRepository.java`
  责任：实现部门 CRUD 和删除保护查询。
- Modify: `backend/src/main/java/com/company/ams/account/DeviceAccountController.java`
  责任：增加设备账号统一列表、新增、编辑、删除接口，同时保留 `/by-device` 兼容请求单。
- Modify: `backend/src/main/java/com/company/ams/account/DeviceAccountService.java`
  责任：实现设备账号 CRUD、绑定/解绑人员和审批跳转前置校验。
- Modify: `backend/src/main/java/com/company/ams/account/DeviceAccountRow.java`
  责任：补足 `id`、`deviceName`、`userId`、`accountStatus`、`sourceType`、`remark` 等字段。
- Modify: `backend/src/main/java/com/company/ams/common/persistence/DeviceAccountRepository.java`
  责任：实现设备账号列表聚合、CRUD、可空绑定、角色关联检查。
- Modify: `backend/src/main/java/com/company/ams/common/persistence/RequestWorkflowPersistence.java`
  责任：把申请单目标账号校验和执行绑定逻辑切到 `device_node_id + account_name` 模型，兼容未绑定人员的账号。
- Create: `backend/src/main/resources/db/migration/V4__department_description.sql`
  责任：为部门补充 `description` 字段。
- Create: `backend/src/main/resources/db/migration/V5__device_account_nullable_binding.sql`
  责任：把 `device_account.user_id` 改成可空，并把唯一性调整为 `(device_node_id, account_name)`。

### Frontend

- Create: `frontend/src/styles/theme.css`
  责任：定义 cockpit 主题变量、深色侧栏、命令栏、表格、抽屉和状态标签视觉体系。
- Create: `frontend/src/config/navigation.ts`
  责任：集中定义左侧导航、顶部快捷动作和路由标题元数据。
- Create: `frontend/src/components/shell/AppSidebar.vue`
  责任：渲染固定左侧导航。
- Create: `frontend/src/components/shell/AppTopbar.vue`
  责任：渲染命令栏、全局搜索、快捷新建、当前用户和退出入口。
- Create: `frontend/src/components/shell/PageHero.vue`
  责任：统一页面标题、描述、指标卡区域。
- Create: `frontend/src/components/shell/FilterPanel.vue`
  责任：统一筛选区视觉和折叠布局。
- Create: `frontend/src/components/users/UserDrawerForm.vue`
  责任：封装用户新增/编辑抽屉表单。
- Create: `frontend/src/components/departments/DepartmentDrawerForm.vue`
  责任：封装部门新增/编辑抽屉表单。
- Create: `frontend/src/components/device-accounts/DeviceAccountDrawerForm.vue`
  责任：封装设备账号新增/编辑/绑定人员抽屉表单。
- Create: `frontend/src/api/dashboard.ts`
  责任：拉取工作台 summary。
- Modify: `frontend/src/main.ts`
  责任：注入统一主题样式。
- Modify: `frontend/src/layouts/AppLayout.vue`
  责任：从极简头部升级为业务驾驶舱壳层。
- Modify: `frontend/src/router/index.ts`
  责任：增加 page meta、设备账号管理入口和请求预填路由参数处理。
- Modify: `frontend/src/views/LoginView.vue`
  责任：重做为品牌说明 + 登录卡片的正式入口页。
- Modify: `frontend/src/views/DashboardView.vue`
  责任：展示指标卡、待处理申请、异常提醒和快捷动作。
- Modify: `frontend/src/api/users.ts`
  责任：补齐用户 CRUD 接口和类型。
- Modify: `frontend/src/views/users/UserListView.vue`
  责任：实现用户管理页 table + right drawer 模式。
- Modify: `frontend/src/api/departments.ts`
  责任：补齐部门 CRUD 接口和类型。
- Modify: `frontend/src/views/departments/DepartmentListView.vue`
  责任：实现部门管理页 table + right drawer 模式。
- Modify: `frontend/src/api/device-accounts.ts`
  责任：补齐设备账号统一列表、CRUD 和筛选类型。
- Create: `frontend/src/views/device-accounts/DeviceAccountListView.vue`
  责任：实现设备账号管理主页面。
- Modify: `frontend/src/views/requests/RequestListView.vue`
  责任：统一视觉、增强状态筛选和摘要信息。
- Modify: `frontend/src/views/requests/RequestFormView.vue`
  责任：支持预填 query、步骤感、审批说明和提交反馈。
- Modify: `frontend/src/views/queries/DevicePermissionView.vue`
  责任：统一视觉骨架和筛选区。
- Modify: `frontend/src/views/audit/AuditLogView.vue`
  责任：统一视觉骨架和筛选区。

### Tests and Ops

- Create: `frontend/src/tests/app-layout.spec.ts`
- Create: `frontend/src/tests/dashboard-view.spec.ts`
- Create: `frontend/src/tests/user-list-view.spec.ts`
- Create: `frontend/src/tests/department-list-view.spec.ts`
- Create: `frontend/src/tests/device-account-list-view.spec.ts`
- Create: `frontend/src/tests/request-list-view.spec.ts`
- Modify: `frontend/src/tests/request-form.spec.ts`
- Modify: `frontend/src/tests/device-permission-view.spec.ts`
- Modify: `frontend/src/tests/audit-log-view.spec.ts`
- Create: `backend/src/test/java/com/company/ams/dashboard/DashboardControllerTest.java`
- Create: `backend/src/test/java/com/company/ams/user/UserCrudIntegrationTest.java`
- Create: `backend/src/test/java/com/company/ams/user/DepartmentCrudIntegrationTest.java`
- Modify: `backend/src/test/java/com/company/ams/user/UserControllerTest.java`
- Modify: `backend/src/test/java/com/company/ams/user/DepartmentControllerTest.java`
- Modify: `backend/src/test/java/com/company/ams/account/DeviceAccountControllerTest.java`
- Modify: `backend/src/test/java/com/company/ams/e2e/ApprovalFlowIntegrationTest.java`
- Create: `frontend/tests/e2e/cockpit-navigation.spec.ts`
- Modify: `frontend/tests/e2e/login.spec.ts`
- Create: `docs/test/2026-04-24-ams2-ui-crud-smoke.md`

## Task 1: Build the Cockpit Visual System and Shared Shell

**Files:**
- Create: `frontend/src/styles/theme.css`
- Create: `frontend/src/config/navigation.ts`
- Create: `frontend/src/components/shell/AppSidebar.vue`
- Create: `frontend/src/components/shell/AppTopbar.vue`
- Create: `frontend/src/components/shell/PageHero.vue`
- Create: `frontend/src/components/shell/FilterPanel.vue`
- Modify: `frontend/src/main.ts`
- Modify: `frontend/src/layouts/AppLayout.vue`
- Modify: `frontend/src/router/index.ts`
- Test: `frontend/src/tests/app-layout.spec.ts`

- [ ] **Step 1: Write the failing shell test**

```ts
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import AppLayout from '../layouts/AppLayout.vue'

describe('AppLayout', () => {
  it('renders cockpit navigation and top command bar', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>dashboard body</div>' } }]
    })

    await router.push('/')
    await router.isReady()

    const wrapper = mount(AppLayout, {
      global: {
        plugins: [createPinia(), router]
      }
    })

    expect(wrapper.text()).toContain('工作台')
    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.text()).toContain('申请管理')
    expect(wrapper.find('[data-testid="global-search"]').exists()).toBe(true)
  })
})
```

- [ ] **Step 2: Run the shell test to verify it fails**

Run: `npm --prefix frontend test -- src/tests/app-layout.spec.ts`

Expected: FAIL because the current `AppLayout.vue` only renders a thin header and does not contain the cockpit nav or command bar.

- [ ] **Step 3: Implement theme tokens, navigation config, and the cockpit shell**

```ts
// frontend/src/config/navigation.ts
export interface NavigationItem {
  label: string
  routeName: string
  to: string
  badge?: string
}

export const primaryNavigation: NavigationItem[] = [
  { label: '工作台', routeName: 'dashboard', to: '/' },
  { label: '用户管理', routeName: 'users', to: '/users' },
  { label: '部门管理', routeName: 'departments', to: '/departments' },
  { label: '设备账号管理', routeName: 'assets', to: '/assets' },
  { label: '申请管理', routeName: 'requests', to: '/requests' },
  { label: '查询管理', routeName: 'device-permissions', to: '/queries/device-permissions' },
  { label: '审计追踪', routeName: 'audit-logs', to: '/audit/logs' }
]

export const quickActions = [
  { label: '新建申请', to: '/requests/new' },
  { label: '新建用户', to: '/users?mode=create' }
]
```

```css
/* frontend/src/styles/theme.css */
:root {
  --ams-bg: #f3f6fb;
  --ams-panel: rgba(255, 255, 255, 0.88);
  --ams-panel-strong: #ffffff;
  --ams-sidebar: #0f1b2d;
  --ams-sidebar-muted: #7f90ab;
  --ams-primary: #1f6feb;
  --ams-primary-soft: #dce9ff;
  --ams-success: #0f9f6e;
  --ams-warning: #d38b15;
  --ams-danger: #d94c4c;
  --ams-text: #132238;
  --ams-text-muted: #5c6b82;
  --ams-border: rgba(18, 34, 56, 0.1);
  --ams-shadow: 0 18px 48px rgba(15, 27, 45, 0.12);
  --ams-radius-xl: 28px;
  --ams-radius-lg: 20px;
  --ams-radius-md: 14px;
  --ams-font-sans: 'Source Han Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

body {
  margin: 0;
  font-family: var(--ams-font-sans);
  background:
    radial-gradient(circle at top left, rgba(31, 111, 235, 0.18), transparent 36%),
    linear-gradient(180deg, #edf3fb 0%, #f7f9fc 100%);
  color: var(--ams-text);
}
```

```vue
<!-- frontend/src/layouts/AppLayout.vue -->
<template>
  <div class="app-shell">
    <AppSidebar />
    <div class="app-shell__main">
      <AppTopbar data-testid="global-search" />
      <main class="app-shell__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from '../components/shell/AppSidebar.vue'
import AppTopbar from '../components/shell/AppTopbar.vue'
</script>
```

```ts
// frontend/src/main.ts
import './styles/theme.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import { createApp, h } from 'vue'
import { RouterView } from 'vue-router'
import router from './router'

createApp({
  render: () => h(RouterView)
})
  .use(createPinia())
  .use(router)
  .use(ElementPlus)
  .mount('#app')
```

- [ ] **Step 4: Run the shell test again**

Run: `npm --prefix frontend test -- src/tests/app-layout.spec.ts`

Expected: PASS with `1 passed`, and the rendered shell contains sidebar navigation plus the top command bar.

- [ ] **Step 5: Checkpoint the shell slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/styles/theme.css frontend/src/config/navigation.ts frontend/src/components/shell frontend/src/layouts/AppLayout.vue frontend/src/main.ts frontend/src/router/index.ts frontend/src/tests/app-layout.spec.ts
  git commit -m "feat: add cockpit shell and theme system"
} else {
  Write-Host "No git repo detected; checkpoint shell slice after verification."
  Get-Item frontend/src/styles/theme.css, frontend/src/config/navigation.ts, frontend/src/layouts/AppLayout.vue
}
```

Expected: Either a normal git commit, or a verified file checkpoint confirming the shell files were created and updated.

## Task 2: Add the Backend Dashboard Summary Endpoint

**Files:**
- Create: `backend/src/main/java/com/company/ams/common/persistence/DashboardRepository.java`
- Create: `backend/src/main/java/com/company/ams/dashboard/DashboardController.java`
- Create: `backend/src/main/java/com/company/ams/dashboard/DashboardService.java`
- Create: `backend/src/main/java/com/company/ams/dashboard/DashboardSummary.java`
- Test: `backend/src/test/java/com/company/ams/dashboard/DashboardControllerTest.java`

- [ ] **Step 1: Write the failing dashboard API test**

```java
package com.company.ams.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard-summary;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DashboardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void summaryReturnsMetricsAlertsAndRecentRequests() throws Exception {
        MockHttpSession session = login("zhangsan", "zhangsan123");

        mockMvc.perform(get("/api/dashboard/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.metrics.userTotal").value(4))
                .andExpect(jsonPath("$.data.metrics.departmentTotal").value(2))
                .andExpect(jsonPath("$.data.metrics.deviceAccountTotal").value(3))
                .andExpect(jsonPath("$.data.metrics.pendingRequestTotal").exists())
                .andExpect(jsonPath("$.data.alerts").isArray())
                .andExpect(jsonPath("$.data.recentRequests").isArray())
                .andExpect(jsonPath("$.data.quickActions[0].actionKey").value("create-request"));
    }

    private MockHttpSession login(String loginName, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"loginName":"%s","password":"%s"}
                            """.formatted(loginName, password)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
```

- [ ] **Step 2: Run the dashboard API test to verify it fails**

Run: `mvn -f backend/pom.xml -Dtest=DashboardControllerTest test`

Expected: FAIL with `404` or `No mapping for GET /api/dashboard/summary`.

- [ ] **Step 3: Implement the dashboard repository, DTO, service, and controller**

```java
// backend/src/main/java/com/company/ams/dashboard/DashboardSummary.java
package com.company.ams.dashboard;

import java.util.List;

public record DashboardSummary(
        Metrics metrics,
        List<AlertCard> alerts,
        List<RecentRequestCard> recentRequests,
        List<QuickActionCard> quickActions) {
    public record Metrics(long userTotal, long departmentTotal, long deviceAccountTotal, long pendingRequestTotal) {}
    public record AlertCard(String alertKey, String title, String description, String level, long count) {}
    public record RecentRequestCard(long id, String requestNo, String requestType, String status, String createdAt) {}
    public record QuickActionCard(String actionKey, String label, String route) {}
}
```

```java
// backend/src/main/java/com/company/ams/common/persistence/DashboardRepository.java
package com.company.ams.common.persistence;

import com.company.ams.dashboard.DashboardSummary;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {
    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummary.Metrics loadMetrics() {
        long userTotal = count("select count(*) from sys_user where deleted = 0");
        long departmentTotal = count("select count(*) from sys_department where deleted = 0");
        long deviceAccountTotal = count("select count(*) from device_account where deleted = 0");
        long pendingRequestTotal = count("select count(*) from request_order where current_status <> 'COMPLETED'");
        return new DashboardSummary.Metrics(userTotal, departmentTotal, deviceAccountTotal, pendingRequestTotal);
    }

    public List<DashboardSummary.AlertCard> loadAlerts() {
        long unboundCount = count("select count(*) from device_account where deleted = 0 and user_id is null");
        long noRoleCount = count("""
                select count(*)
                from device_account da
                where da.deleted = 0
                  and not exists (
                    select 1
                    from device_account_role dar
                    where dar.device_account_id = da.id and dar.relation_status = 'ACTIVE'
                  )
                """);
        return List.of(
                new DashboardSummary.AlertCard("unbound-account", "未绑定人员账号", "需要补齐归属人员后再发起权限动作", "warning", unboundCount),
                new DashboardSummary.AlertCard("no-role-account", "无角色账号", "账号存在但没有任何有效角色", "danger", noRoleCount));
    }

    public List<DashboardSummary.RecentRequestCard> loadRecentRequests() {
        return jdbcTemplate.query(
                """
                select id, request_no, request_type, current_status, created_at
                from request_order
                order by created_at desc, id desc
                limit 5
                """,
                (rs, rowNum) -> new DashboardSummary.RecentRequestCard(
                        rs.getLong("id"),
                        rs.getString("request_no"),
                        rs.getString("request_type"),
                        rs.getString("current_status"),
                        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC).toString()));
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
```

```java
// backend/src/main/java/com/company/ams/dashboard/DashboardService.java
package com.company.ams.dashboard;

import com.company.ams.common.persistence.DashboardRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardSummary summary() {
        return new DashboardSummary(
                dashboardRepository.loadMetrics(),
                dashboardRepository.loadAlerts(),
                dashboardRepository.loadRecentRequests(),
                List.of(
                        new DashboardSummary.QuickActionCard("create-request", "新建申请", "/requests/new"),
                        new DashboardSummary.QuickActionCard("create-user", "新建用户", "/users?mode=create")));
    }
}
```

```java
// backend/src/main/java/com/company/ams/dashboard/DashboardController.java
package com.company.ams.dashboard;

import com.company.ams.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary() {
        return ApiResponse.success(dashboardService.summary());
    }
}
```

- [ ] **Step 4: Run the dashboard API test again**

Run: `mvn -f backend/pom.xml -Dtest=DashboardControllerTest test`

Expected: PASS with `summaryReturnsMetricsAlertsAndRecentRequests`.

- [ ] **Step 5: Checkpoint the dashboard backend slice**

Run:

```powershell
if (Test-Path .git) {
  git add backend/src/main/java/com/company/ams/common/persistence/DashboardRepository.java backend/src/main/java/com/company/ams/dashboard backend/src/test/java/com/company/ams/dashboard/DashboardControllerTest.java
  git commit -m "feat: add dashboard summary endpoint"
} else {
  Write-Host "No git repo detected; checkpoint dashboard backend slice after verification."
  Get-Item backend/src/main/java/com/company/ams/dashboard/DashboardController.java, backend/src/main/java/com/company/ams/dashboard/DashboardSummary.java
}
```

Expected: Dashboard API files are checkpointed and ready for the redesigned workbench.

## Task 3: Redesign the Login Page and Dashboard Workbench

**Files:**
- Create: `frontend/src/api/dashboard.ts`
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/views/DashboardView.vue`
- Modify: `frontend/src/tests/login-view.spec.ts`
- Create: `frontend/src/tests/dashboard-view.spec.ts`

- [ ] **Step 1: Write the failing dashboard UI test**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchDashboardSummary } from '../api/dashboard'
import DashboardView from '../views/DashboardView.vue'

vi.mock('../api/dashboard', () => ({
  fetchDashboardSummary: vi.fn()
}))

describe('DashboardView', () => {
  beforeEach(() => {
    vi.mocked(fetchDashboardSummary).mockReset()
    vi.mocked(fetchDashboardSummary).mockResolvedValue({
      metrics: {
        userTotal: 4,
        departmentTotal: 2,
        deviceAccountTotal: 3,
        pendingRequestTotal: 1
      },
      alerts: [
        {
          alertKey: 'no-role-account',
          title: '无角色账号',
          description: '仍有账号未绑定任何角色',
          level: 'danger',
          count: 1
        }
      ],
      recentRequests: [],
      quickActions: [{ actionKey: 'create-request', label: '新建申请', route: '/requests/new' }]
    })
  })

  it('renders metric cards, alert cards and quick actions', async () => {
    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('用户总量')
    expect(wrapper.text()).toContain('待处理申请')
    expect(wrapper.text()).toContain('无角色账号')
    expect(wrapper.text()).toContain('新建申请')
  })
})
```

- [ ] **Step 2: Run the dashboard UI test to verify it fails**

Run: `npm --prefix frontend test -- src/tests/dashboard-view.spec.ts`

Expected: FAIL because the current `DashboardView.vue` only renders a text list of links and does not fetch or display dashboard summary data.

- [ ] **Step 3: Implement the summary API client, redesigned login page, and workbench**

```ts
// frontend/src/api/dashboard.ts
import axios from 'axios'

export interface DashboardSummary {
  metrics: {
    userTotal: number
    departmentTotal: number
    deviceAccountTotal: number
    pendingRequestTotal: number
  }
  alerts: Array<{
    alertKey: string
    title: string
    description: string
    level: string
    count: number
  }>
  recentRequests: Array<{
    id: number
    requestNo: string
    requestType: string
    status: string
    createdAt: string
  }>
  quickActions: Array<{
    actionKey: string
    label: string
    route: string
  }>
}

export async function fetchDashboardSummary(): Promise<DashboardSummary> {
  const { data } = await axios.get('/api/dashboard/summary')
  return data.data
}
```

```vue
<!-- frontend/src/views/LoginView.vue -->
<template>
  <section class="login-page">
    <div class="login-page__hero">
      <p class="login-page__eyebrow">AMS2.0 Operations Cockpit</p>
      <h1>审批驱动的账号与权限管理中台</h1>
      <p class="login-page__description">
        在一个统一入口里管理人员、部门、设备账号和审批申请，重点提醒异常账号与待办事项。
      </p>
    </div>
    <form class="login-card" @submit.prevent="handleSubmit">
      <label>
        <span>登录名</span>
        <input v-model="loginName" name="loginName" autocomplete="username" />
      </label>
      <label>
        <span>密码</span>
        <input v-model="password" name="password" type="password" autocomplete="current-password" />
      </label>
      <button type="submit">进入系统</button>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loginName = ref('')
const password = ref('')

async function handleSubmit() {
  await authStore.login(loginName.value, password.value)
  await router.push('/')
}
</script>
```

```vue
<!-- frontend/src/views/DashboardView.vue -->
<template>
  <section class="dashboard-view">
    <PageHero
      title="业务驾驶舱"
      description="优先看到今天需要处理的请求、异常账号和快速入口。"
      :metrics="metricCards"
    />

    <div class="dashboard-grid">
      <article class="dashboard-panel">
        <h2>待处理异常</h2>
        <ul>
          <li v-for="alert in summary?.alerts ?? []" :key="alert.alertKey">
            <strong>{{ alert.title }}</strong>
            <span>{{ alert.count }}</span>
            <p>{{ alert.description }}</p>
          </li>
        </ul>
      </article>

      <article class="dashboard-panel">
        <h2>快捷动作</h2>
        <RouterLink
          v-for="action in summary?.quickActions ?? []"
          :key="action.actionKey"
          class="dashboard-quick-link"
          :to="action.route"
        >
          {{ action.label }}
        </RouterLink>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHero from '../components/shell/PageHero.vue'
import { fetchDashboardSummary, type DashboardSummary } from '../api/dashboard'

const summary = ref<DashboardSummary | null>(null)

const metricCards = computed(() => {
  if (!summary.value) {
    return []
  }
  return [
    { label: '用户总量', value: summary.value.metrics.userTotal },
    { label: '部门总量', value: summary.value.metrics.departmentTotal },
    { label: '设备账号', value: summary.value.metrics.deviceAccountTotal },
    { label: '待处理申请', value: summary.value.metrics.pendingRequestTotal }
  ]
})

onMounted(async () => {
  summary.value = await fetchDashboardSummary()
})
</script>
```

- [ ] **Step 4: Run the login and dashboard tests**

Run: `npm --prefix frontend test -- src/tests/login-view.spec.ts src/tests/dashboard-view.spec.ts`

Expected: PASS with the existing login submit flow still working and the new dashboard test verifying metric cards, alert cards, and quick actions.

- [ ] **Step 5: Checkpoint the login/workbench slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/api/dashboard.ts frontend/src/views/LoginView.vue frontend/src/views/DashboardView.vue frontend/src/tests/login-view.spec.ts frontend/src/tests/dashboard-view.spec.ts
  git commit -m "feat: redesign login page and workbench"
} else {
  Write-Host "No git repo detected; checkpoint login/workbench slice after verification."
  Get-Item frontend/src/views/LoginView.vue, frontend/src/views/DashboardView.vue
}
```

Expected: The new login page and dashboard workbench are checkpointed with passing tests.

## Task 4: Add Common Backend Error Handling and User CRUD

**Files:**
- Create: `backend/src/main/java/com/company/ams/common/api/BusinessException.java`
- Create: `backend/src/main/java/com/company/ams/common/api/ApiExceptionHandler.java`
- Create: `backend/src/main/java/com/company/ams/common/api/ListPayload.java`
- Create: `backend/src/main/java/com/company/ams/user/UserUpsertCommand.java`
- Create: `backend/src/main/java/com/company/ams/user/UserStatusCommand.java`
- Modify: `backend/src/main/java/com/company/ams/user/UserController.java`
- Modify: `backend/src/main/java/com/company/ams/user/UserService.java`
- Modify: `backend/src/main/java/com/company/ams/user/UserRow.java`
- Modify: `backend/src/main/java/com/company/ams/common/persistence/UserRepository.java`
- Modify: `backend/src/test/java/com/company/ams/user/UserControllerTest.java`
- Create: `backend/src/test/java/com/company/ams/user/UserCrudIntegrationTest.java`

- [ ] **Step 1: Write the failing user CRUD tests**

```java
package com.company.ams.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ams.auth.SecurityConfig;
import com.company.ams.common.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void createUserReturnsCreatedPayload() throws Exception {
        given(userService.create(any())).willReturn(
                new UserRow(9L, "EMP009", "赵六", 1L, "Assembly Dept", "ACTIVE", "zhaoliu", "ENABLED"));

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("""
                            {
                              "userCode":"EMP009",
                              "userName":"赵六",
                              "departmentId":1,
                              "employmentStatus":"ACTIVE",
                              "loginName":"zhaoliu",
                              "accountStatus":"ENABLED"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userCode").value("EMP009"))
                .andExpect(jsonPath("$.data.departmentId").value(1));
    }

    @Test
    @WithMockUser
    void patchStatusReturnsUpdatedUser() throws Exception {
        given(userService.updateStatus(2L, "DISABLED")).willReturn(
                new UserRow(2L, "EMP001", "张三", 1L, "Assembly Dept", "ACTIVE", "zhangsan", "DISABLED"));

        mockMvc.perform(patch("/api/users/2/status")
                        .contentType("application/json")
                        .content("""
                            {"accountStatus":"DISABLED"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("DISABLED"));
    }
}
```

```java
package com.company.ams.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-crud;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserCrudIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void deleteRejectsUserWithBoundDeviceAccount() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(delete("/api/users/2").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User still owns device accounts"));
    }

    private MockHttpSession login(String loginName, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"loginName":"%s","password":"%s"}
                            """.formatted(loginName, password)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
```

- [ ] **Step 2: Run the user CRUD tests to verify they fail**

Run: `mvn -f backend/pom.xml -Dtest=UserControllerTest,UserCrudIntegrationTest test`

Expected: FAIL because the current controller only supports `GET /api/users`, there is no exception handler, and delete protection does not exist.

- [ ] **Step 3: Implement common API errors, user DTOs, service rules, and repository mutations**

```java
// backend/src/main/java/com/company/ams/common/api/BusinessException.java
package com.company.ams.common.api;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        this(4001, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
```

```java
// backend/src/main/java/com/company/ams/common/api/ApiExceptionHandler.java
package com.company.ams.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusiness(BusinessException exception) {
        return ApiResponse.error(exception.code(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.error(4000, exception.getMessage());
    }
}
```

```java
// backend/src/main/java/com/company/ams/common/api/ListPayload.java
package com.company.ams.common.api;

import java.util.List;

public record ListPayload<T>(List<T> list, int total) {}
```

```java
// backend/src/main/java/com/company/ams/user/UserUpsertCommand.java
package com.company.ams.user;

public record UserUpsertCommand(
        String userCode,
        String userName,
        Long departmentId,
        String employmentStatus,
        String loginName,
        String accountStatus) {}
```

```java
// backend/src/main/java/com/company/ams/user/UserService.java
public UserRow create(UserUpsertCommand command) {
    validate(command, null);
    return userRepository.create(command);
}

public UserRow update(long userId, UserUpsertCommand command) {
    validate(command, userId);
    return userRepository.update(userId, command);
}

public UserRow updateStatus(long userId, String accountStatus) {
    if (!"ENABLED".equals(accountStatus) && !"DISABLED".equals(accountStatus)) {
        throw new IllegalArgumentException("accountStatus must be ENABLED or DISABLED");
    }
    return userRepository.updateStatus(userId, accountStatus);
}

public void delete(long userId) {
    if (userRepository.countBoundDeviceAccounts(userId) > 0) {
        throw new BusinessException("User still owns device accounts");
    }
    if (userRepository.countOpenRequests(userId) > 0) {
        throw new BusinessException("User still has unfinished requests");
    }
    userRepository.softDelete(userId);
}

private void validate(UserUpsertCommand command, Long currentUserId) {
    if (command.userCode() == null || command.userCode().isBlank()) {
        throw new IllegalArgumentException("userCode is required");
    }
    if (command.userName() == null || command.userName().isBlank()) {
        throw new IllegalArgumentException("userName is required");
    }
    if (command.departmentId() == null) {
        throw new IllegalArgumentException("departmentId is required");
    }
    if (command.loginName() == null || command.loginName().isBlank()) {
        throw new IllegalArgumentException("loginName is required");
    }
    userRepository.assertUniqueLoginName(command.loginName(), currentUserId);
    userRepository.assertUniqueUserCode(command.userCode(), currentUserId);
}
```

```java
// backend/src/main/java/com/company/ams/common/persistence/UserRepository.java
public UserRow create(UserUpsertCommand command) {
    assertUniqueLoginName(command.loginName(), null);
    assertUniqueUserCode(command.userCode(), null);
    jdbcTemplate.update(
            """
            insert into sys_user (
              user_code, user_name, department_id, employment_status,
              login_name, password_hash, account_status, created_at, updated_at, deleted
            ) values (?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
            """,
            command.userCode(),
            command.userName(),
            command.departmentId(),
            command.employmentStatus(),
            command.loginName(),
            "$2a$10$heYFmopHDIcxp1j/H3ahGuTdkKyVrmFwx.rrhSm6ls7KIfn6bTbki",
            command.accountStatus());
    return findRequiredByLoginName(command.loginName());
}

public int countBoundDeviceAccounts(long userId) {
    Integer count = jdbcTemplate.queryForObject(
            "select count(*) from device_account where deleted = 0 and user_id = ?",
            Integer.class,
            userId);
    return count == null ? 0 : count;
}

public int countOpenRequests(long userId) {
    Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from request_order
            where (applicant_user_id = ? or target_user_id = ?)
              and current_status <> 'COMPLETED'
            """,
            Integer.class,
            userId,
            userId);
    return count == null ? 0 : count;
}

public void softDelete(long userId) {
    jdbcTemplate.update(
            "update sys_user set deleted = 1, updated_at = current_timestamp where id = ?",
            userId);
}

public void assertUniqueLoginName(String loginName, Long currentUserId) {
    Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from sys_user
            where deleted = 0 and login_name = ? and (? is null or id <> ?)
            """,
            Integer.class,
            loginName,
            currentUserId,
            currentUserId);
    if (count != null && count > 0) {
        throw new BusinessException("Login name already exists");
    }
}

public void assertUniqueUserCode(String userCode, Long currentUserId) {
    Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from sys_user
            where deleted = 0 and user_code = ? and (? is null or id <> ?)
            """,
            Integer.class,
            userCode,
            currentUserId,
            currentUserId);
    if (count != null && count > 0) {
        throw new BusinessException("User code already exists");
    }
}

private UserRow findRequiredByLoginName(String loginName) {
    return jdbcTemplate.query(
            """
            select u.id, u.user_code, u.user_name, u.department_id, d.department_name,
                   u.employment_status, u.login_name, u.account_status
            from sys_user u
            join sys_department d on d.id = u.department_id and d.deleted = 0
            where u.deleted = 0 and u.login_name = ?
            """,
            rs -> rs.next()
                    ? new UserRow(
                            rs.getLong("id"),
                            rs.getString("user_code"),
                            rs.getString("user_name"),
                            rs.getLong("department_id"),
                            rs.getString("department_name"),
                            rs.getString("employment_status"),
                            rs.getString("login_name"),
                            rs.getString("account_status"))
                    : null,
            loginName);
}
```

- [ ] **Step 4: Run the user CRUD tests again**

Run: `mvn -f backend/pom.xml -Dtest=UserControllerTest,UserCrudIntegrationTest test`

Expected: PASS with new create/status endpoints mapped and delete protection returning a 400 `ApiResponse` message instead of a 500.

- [ ] **Step 5: Checkpoint the user CRUD slice**

Run:

```powershell
if (Test-Path .git) {
  git add backend/src/main/java/com/company/ams/common/api backend/src/main/java/com/company/ams/user backend/src/main/java/com/company/ams/common/persistence/UserRepository.java backend/src/test/java/com/company/ams/user/UserControllerTest.java backend/src/test/java/com/company/ams/user/UserCrudIntegrationTest.java
  git commit -m "feat: add user crud and business error handling"
} else {
  Write-Host "No git repo detected; checkpoint user CRUD slice after verification."
  Get-Item backend/src/main/java/com/company/ams/common/api/ApiExceptionHandler.java, backend/src/main/java/com/company/ams/user/UserUpsertCommand.java
}
```

Expected: Common API error handling and user CRUD are checkpointed for frontend integration.

## Task 5: Build the User Management Page with Drawer-Based CRUD

**Files:**
- Create: `frontend/src/components/users/UserDrawerForm.vue`
- Modify: `frontend/src/api/users.ts`
- Modify: `frontend/src/views/users/UserListView.vue`
- Create: `frontend/src/tests/user-list-view.spec.ts`

- [ ] **Step 1: Write the failing user management page test**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchDepartments } from '../api/departments'
import { createUser, fetchUsers, updateUserStatus } from '../api/users'
import UserListView from '../views/users/UserListView.vue'

vi.mock('../api/users', () => ({
  fetchUsers: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  updateUserStatus: vi.fn(),
  deleteUser: vi.fn()
}))

vi.mock('../api/departments', () => ({
  fetchDepartments: vi.fn()
}))

describe('UserListView', () => {
  beforeEach(() => {
    vi.mocked(fetchUsers).mockResolvedValue([
      {
        id: '2',
        userCode: 'EMP001',
        userName: '张三',
        departmentId: '1',
        departmentName: 'Assembly Dept',
        employmentStatus: 'ACTIVE',
        loginName: 'zhangsan',
        accountStatus: 'ENABLED'
      }
    ])
    vi.mocked(fetchDepartments).mockResolvedValue([
      { id: '1', departmentName: 'Assembly Dept', managerUserId: '2', managerUserName: '张三', description: '', memberCount: 2, status: 'ENABLED', updatedAt: '' }
    ])
    vi.mocked(createUser).mockResolvedValue({
      id: '9',
      userCode: 'EMP009',
      userName: '赵六',
      departmentId: '1',
      departmentName: 'Assembly Dept',
      employmentStatus: 'ACTIVE',
      loginName: 'zhaoliu',
      accountStatus: 'ENABLED'
    })
    vi.mocked(updateUserStatus).mockResolvedValue({
      id: '2',
      userCode: 'EMP001',
      userName: '张三',
      departmentId: '1',
      departmentName: 'Assembly Dept',
      employmentStatus: 'ACTIVE',
      loginName: 'zhangsan',
      accountStatus: 'DISABLED'
    })
  })

  it('opens the create drawer and submits a new user payload', async () => {
    const wrapper = mount(UserListView)
    await flushPromises()

    await wrapper.get('[data-testid="create-user"]').trigger('click')
    await wrapper.get('input[name="userCode"]').setValue('EMP009')
    await wrapper.get('input[name="userName"]').setValue('赵六')
    await wrapper.get('input[name="loginName"]').setValue('zhaoliu')
    await wrapper.get('[data-testid="submit-user"]').trigger('click')

    expect(createUser).toHaveBeenCalledWith({
      userCode: 'EMP009',
      userName: '赵六',
      departmentId: 1,
      employmentStatus: 'ACTIVE',
      loginName: 'zhaoliu',
      accountStatus: 'ENABLED'
    })
  })
})
```

- [ ] **Step 2: Run the user page test to verify it fails**

Run: `npm --prefix frontend test -- src/tests/user-list-view.spec.ts`

Expected: FAIL because the current user page is read-only and has no drawer, no create button, and no CRUD API methods.

- [ ] **Step 3: Implement user CRUD API bindings and the drawer-based management page**

```ts
// frontend/src/api/users.ts
import axios from 'axios'

export interface UserItem {
  id: string
  userCode: string
  userName: string
  departmentId: string
  departmentName: string
  employmentStatus: string
  loginName: string
  accountStatus: string
}

export async function fetchUsers(): Promise<UserItem[]> {
  const { data } = await axios.get('/api/users')
  return data.data.list.map((row: any) => ({
    id: String(row.id),
    userCode: row.userCode,
    userName: row.userName,
    departmentId: String(row.departmentId),
    departmentName: row.departmentName,
    employmentStatus: row.employmentStatus,
    loginName: row.loginName,
    accountStatus: row.accountStatus
  }))
}

export async function createUser(payload: Record<string, unknown>): Promise<UserItem> {
  const { data } = await axios.post('/api/users', payload)
  return data.data
}

export async function updateUserStatus(userId: string, accountStatus: string): Promise<UserItem> {
  const { data } = await axios.patch(`/api/users/${userId}/status`, { accountStatus })
  return data.data
}
```

```vue
<!-- frontend/src/components/users/UserDrawerForm.vue -->
<template>
  <div class="drawer-form">
    <input v-model="form.userCode" name="userCode" />
    <input v-model="form.userName" name="userName" />
    <input v-model="form.loginName" name="loginName" />
    <button data-testid="submit-user" type="button" @click="$emit('submit', { ...form, departmentId: Number(form.departmentId) })">
      保存
    </button>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

const form = reactive({
  userCode: '',
  userName: '',
  departmentId: '1',
  employmentStatus: 'ACTIVE',
  loginName: '',
  accountStatus: 'ENABLED'
})
</script>
```

```vue
<!-- frontend/src/views/users/UserListView.vue -->
<template>
  <section class="management-page">
    <PageHero title="用户管理" description="维护人员主数据、登录账号和启停状态。" :metrics="metrics" />

    <div class="management-toolbar">
      <button data-testid="create-user" type="button" @click="openCreateDrawer">新增用户</button>
    </div>

    <table class="management-table">
      <thead>
        <tr>
          <th>人员编码</th>
          <th>姓名</th>
          <th>登录名</th>
          <th>部门</th>
          <th>账号状态</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in filteredUsers" :key="user.id">
          <td>{{ user.userCode }}</td>
          <td>{{ user.userName }}</td>
          <td>{{ user.loginName }}</td>
          <td>{{ user.departmentName }}</td>
          <td>{{ user.accountStatus }}</td>
        </tr>
      </tbody>
    </table>

    <aside v-if="drawerOpen">
      <UserDrawerForm @submit="handleCreate" />
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHero from '../../components/shell/PageHero.vue'
import UserDrawerForm from '../../components/users/UserDrawerForm.vue'
import { createUser, fetchUsers, type UserItem } from '../../api/users'

const users = ref<UserItem[]>([])
const drawerOpen = ref(false)

const metrics = computed(() => [
  { label: '总用户数', value: users.value.length },
  { label: '启用账号', value: users.value.filter((item) => item.accountStatus === 'ENABLED').length }
])

const filteredUsers = computed(() => users.value)

function openCreateDrawer() {
  drawerOpen.value = true
}

async function handleCreate(payload: Record<string, unknown>) {
  await createUser(payload)
  users.value = await fetchUsers()
  drawerOpen.value = false
}

onMounted(async () => {
  users.value = await fetchUsers()
})
</script>
```

- [ ] **Step 4: Run the user page test again**

Run: `npm --prefix frontend test -- src/tests/user-list-view.spec.ts`

Expected: PASS with the create button, drawer form, and create payload behavior working against mocked APIs.

- [ ] **Step 5: Checkpoint the user page slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/api/users.ts frontend/src/components/users/UserDrawerForm.vue frontend/src/views/users/UserListView.vue frontend/src/tests/user-list-view.spec.ts
  git commit -m "feat: add user management drawer crud ui"
} else {
  Write-Host "No git repo detected; checkpoint user page slice after verification."
  Get-Item frontend/src/views/users/UserListView.vue, frontend/src/components/users/UserDrawerForm.vue
}
```

Expected: User management page is checkpointed with a working create flow and room for edit/delete/status actions in the same drawer pattern.

## Task 6: Add Department CRUD, Description Support, and Delete Guards

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__department_description.sql`
- Create: `backend/src/main/java/com/company/ams/user/DepartmentUpsertCommand.java`
- Modify: `backend/src/main/java/com/company/ams/user/DepartmentController.java`
- Modify: `backend/src/main/java/com/company/ams/user/DepartmentService.java`
- Modify: `backend/src/main/java/com/company/ams/user/DepartmentRow.java`
- Modify: `backend/src/main/java/com/company/ams/common/persistence/DepartmentRepository.java`
- Modify: `backend/src/test/java/com/company/ams/user/DepartmentControllerTest.java`
- Create: `backend/src/test/java/com/company/ams/user/DepartmentCrudIntegrationTest.java`

- [ ] **Step 1: Write the failing department CRUD tests**

```java
package com.company.ams.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ams.auth.SecurityConfig;
import com.company.ams.common.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DepartmentController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class DepartmentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentService departmentService;

    @Test
    @WithMockUser
    void updateDepartmentReturnsDescriptionAndMemberCount() throws Exception {
        given(departmentService.update(anyLong(), any())).willReturn(
                new DepartmentRow(2L, "Quality Dept", 3L, "李四", "负责质量检验", 2, "ENABLED", "2026-04-24T09:00:00Z"));

        mockMvc.perform(put("/api/departments/2")
                        .contentType("application/json")
                        .content("""
                            {
                              "departmentName":"Quality Dept",
                              "managerUserId":3,
                              "description":"负责质量检验",
                              "status":"ENABLED"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managerUserId").value(3))
                .andExpect(jsonPath("$.data.description").value("负责质量检验"));
    }
}
```

```java
package com.company.ams.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:department-crud;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DepartmentCrudIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void deleteRejectsDepartmentWithMembers() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(delete("/api/departments/1").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Department still has active members"));
    }

    private MockHttpSession login(String loginName, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"loginName":"%s","password":"%s"}
                            """.formatted(loginName, password)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
```

- [ ] **Step 2: Run the department CRUD tests to verify they fail**

Run: `mvn -f backend/pom.xml -Dtest=DepartmentControllerTest,DepartmentCrudIntegrationTest test`

Expected: FAIL because the current backend only supports department listing, the `description` column does not exist, and delete protection is not implemented.

- [ ] **Step 3: Add the migration, department DTOs, and service/repository logic**

```sql
-- backend/src/main/resources/db/migration/V4__department_description.sql
alter table sys_department add column description varchar(255) null;
```

```java
// backend/src/main/java/com/company/ams/user/DepartmentUpsertCommand.java
package com.company.ams.user;

public record DepartmentUpsertCommand(
        String departmentName,
        Long managerUserId,
        String description,
        String status) {}
```

```java
// backend/src/main/java/com/company/ams/user/DepartmentRow.java
package com.company.ams.user;

public record DepartmentRow(
        Long id,
        String departmentName,
        Long managerUserId,
        String managerUserName,
        String description,
        Integer memberCount,
        String status,
        String updatedAt) {}
```

```java
// backend/src/main/java/com/company/ams/user/DepartmentService.java
public void delete(long departmentId) {
    if (departmentRepository.countMembers(departmentId) > 0) {
        throw new BusinessException("Department still has active members");
    }
    departmentRepository.softDelete(departmentId);
}
```

```java
// backend/src/main/java/com/company/ams/common/persistence/DepartmentRepository.java
public int countMembers(long departmentId) {
    Integer count = jdbcTemplate.queryForObject(
            "select count(*) from sys_user where deleted = 0 and department_id = ?",
            Integer.class,
            departmentId);
    return count == null ? 0 : count;
}

public void softDelete(long departmentId) {
    jdbcTemplate.update(
            "update sys_department set deleted = 1, updated_at = current_timestamp where id = ?",
            departmentId);
}
```

- [ ] **Step 4: Run the department CRUD tests again**

Run: `mvn -f backend/pom.xml -Dtest=DepartmentControllerTest,DepartmentCrudIntegrationTest test`

Expected: PASS with CRUD endpoints returning `description/memberCount` data and delete protection returning a 400 business message.

- [ ] **Step 5: Checkpoint the department backend slice**

Run:

```powershell
if (Test-Path .git) {
  git add backend/src/main/resources/db/migration/V4__department_description.sql backend/src/main/java/com/company/ams/user backend/src/main/java/com/company/ams/common/persistence/DepartmentRepository.java backend/src/test/java/com/company/ams/user/DepartmentControllerTest.java backend/src/test/java/com/company/ams/user/DepartmentCrudIntegrationTest.java
  git commit -m "feat: add department crud and delete guards"
} else {
  Write-Host "No git repo detected; checkpoint department backend slice after verification."
  Get-Item backend/src/main/resources/db/migration/V4__department_description.sql, backend/src/main/java/com/company/ams/user/DepartmentUpsertCommand.java
}
```

Expected: Department CRUD backend is checkpointed with schema support for optional descriptions.

## Task 7: Build the Department Management Page

**Files:**
- Create: `frontend/src/components/departments/DepartmentDrawerForm.vue`
- Modify: `frontend/src/api/departments.ts`
- Modify: `frontend/src/views/departments/DepartmentListView.vue`
- Create: `frontend/src/tests/department-list-view.spec.ts`

- [ ] **Step 1: Write the failing department page test**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createDepartment, fetchDepartments } from '../api/departments'
import DepartmentListView from '../views/departments/DepartmentListView.vue'

vi.mock('../api/departments', () => ({
  fetchDepartments: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn()
}))

describe('DepartmentListView', () => {
  beforeEach(() => {
    vi.mocked(fetchDepartments).mockResolvedValue([
      {
        id: '1',
        departmentName: 'Assembly Dept',
        managerUserId: '2',
        managerUserName: '张三',
        description: '装配主线',
        memberCount: 2,
        status: 'ENABLED',
        updatedAt: '2026-04-24T09:00:00Z'
      }
    ])
    vi.mocked(createDepartment).mockResolvedValue({
      id: '9',
      departmentName: 'Process Dept',
      managerUserId: '2',
      managerUserName: '张三',
      description: '工艺支持',
      memberCount: 0,
      status: 'ENABLED',
      updatedAt: '2026-04-24T09:20:00Z'
    })
  })

  it('opens create drawer and submits department payload', async () => {
    const wrapper = mount(DepartmentListView)
    await flushPromises()

    await wrapper.get('[data-testid="create-department"]').trigger('click')
    await wrapper.get('input[name="departmentName"]').setValue('Process Dept')
    await wrapper.get('textarea[name="description"]').setValue('工艺支持')
    await wrapper.get('[data-testid="submit-department"]').trigger('click')

    expect(createDepartment).toHaveBeenCalledWith({
      departmentName: 'Process Dept',
      managerUserId: 2,
      description: '工艺支持',
      status: 'ENABLED'
    })
  })
})
```

- [ ] **Step 2: Run the department page test to verify it fails**

Run: `npm --prefix frontend test -- src/tests/department-list-view.spec.ts`

Expected: FAIL because the current department page is still read-only and does not expose a drawer or CRUD actions.

- [ ] **Step 3: Implement department CRUD API bindings and the management page**

```ts
// frontend/src/api/departments.ts
import axios from 'axios'

export interface DepartmentItem {
  id: string
  departmentName: string
  managerUserId: string
  managerUserName: string
  description: string
  memberCount: number
  status: string
  updatedAt: string
}

export async function fetchDepartments(): Promise<DepartmentItem[]> {
  const { data } = await axios.get('/api/departments')
  return data.data.list.map((row: any) => ({
    id: String(row.id),
    departmentName: row.departmentName,
    managerUserId: String(row.managerUserId ?? ''),
    managerUserName: row.managerUserName ?? '',
    description: row.description ?? '',
    memberCount: Number(row.memberCount ?? 0),
    status: row.status ?? 'ENABLED',
    updatedAt: row.updatedAt ?? ''
  }))
}

export async function createDepartment(payload: Record<string, unknown>): Promise<DepartmentItem> {
  const { data } = await axios.post('/api/departments', payload)
  return data.data
}
```

```vue
<!-- frontend/src/components/departments/DepartmentDrawerForm.vue -->
<template>
  <div class="drawer-form">
    <input v-model="form.departmentName" name="departmentName" />
    <textarea v-model="form.description" name="description" />
    <button data-testid="submit-department" type="button" @click="$emit('submit', { ...form, managerUserId: Number(form.managerUserId) })">
      保存
    </button>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

const form = reactive({
  departmentName: '',
  managerUserId: '2',
  description: '',
  status: 'ENABLED'
})
</script>
```

```vue
<!-- frontend/src/views/departments/DepartmentListView.vue -->
<template>
  <section class="management-page">
    <PageHero title="部门管理" description="维护组织结构、负责人和成员规模。" :metrics="metrics" />

    <div class="management-toolbar">
      <button data-testid="create-department" type="button" @click="drawerOpen = true">新增部门</button>
    </div>

    <table class="management-table">
      <thead>
        <tr>
          <th>部门名称</th>
          <th>负责人</th>
          <th>成员数</th>
          <th>最近更新</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="department in departments" :key="department.id">
          <td>{{ department.departmentName }}</td>
          <td>{{ department.managerUserName || '-' }}</td>
          <td>{{ department.memberCount }}</td>
          <td>{{ department.updatedAt || '-' }}</td>
        </tr>
      </tbody>
    </table>

    <aside v-if="drawerOpen">
      <DepartmentDrawerForm @submit="handleCreate" />
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHero from '../../components/shell/PageHero.vue'
import DepartmentDrawerForm from '../../components/departments/DepartmentDrawerForm.vue'
import { createDepartment, fetchDepartments, type DepartmentItem } from '../../api/departments'

const departments = ref<DepartmentItem[]>([])
const drawerOpen = ref(false)

const metrics = computed(() => [
  { label: '部门总数', value: departments.value.length },
  {
    label: '在编成员',
    value: departments.value.reduce((sum, item) => sum + item.memberCount, 0)
  }
])

async function handleCreate(payload: Record<string, unknown>) {
  await createDepartment(payload)
  departments.value = await fetchDepartments()
  drawerOpen.value = false
}

onMounted(async () => {
  departments.value = await fetchDepartments()
})
</script>
```

- [ ] **Step 4: Run the department page test again**

Run: `npm --prefix frontend test -- src/tests/department-list-view.spec.ts`

Expected: PASS with the department drawer create flow verified against mocked APIs.

- [ ] **Step 5: Checkpoint the department page slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/api/departments.ts frontend/src/components/departments/DepartmentDrawerForm.vue frontend/src/views/departments/DepartmentListView.vue frontend/src/tests/department-list-view.spec.ts
  git commit -m "feat: add department management drawer crud ui"
} else {
  Write-Host "No git repo detected; checkpoint department page slice after verification."
  Get-Item frontend/src/views/departments/DepartmentListView.vue, frontend/src/components/departments/DepartmentDrawerForm.vue
}
```

Expected: Department management page is checkpointed and visually aligned with the cockpit shell.

## Task 8: Migrate Device Accounts to Nullable Binding and Add Backend CRUD

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__device_account_nullable_binding.sql`
- Create: `backend/src/main/java/com/company/ams/account/DeviceAccountUpsertCommand.java`
- Modify: `backend/src/main/java/com/company/ams/account/DeviceAccountController.java`
- Modify: `backend/src/main/java/com/company/ams/account/DeviceAccountService.java`
- Modify: `backend/src/main/java/com/company/ams/account/DeviceAccountRow.java`
- Modify: `backend/src/main/java/com/company/ams/common/persistence/DeviceAccountRepository.java`
- Modify: `backend/src/main/java/com/company/ams/common/persistence/RequestWorkflowPersistence.java`
- Modify: `backend/src/test/java/com/company/ams/account/DeviceAccountControllerTest.java`
- Modify: `backend/src/test/java/com/company/ams/e2e/ApprovalFlowIntegrationTest.java`

- [ ] **Step 1: Write the failing device-account migration and request-compatibility tests**

```java
package com.company.ams.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ams.auth.SecurityConfig;
import com.company.ams.common.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeviceAccountController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class DeviceAccountControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceAccountService deviceAccountService;

    @Test
    @WithMockUser
    void createDeviceAccountAllowsNullUserBinding() throws Exception {
        given(deviceAccountService.create(any())).willReturn(
                new DeviceAccountRow(11L, 100L, "Device A", null, "", "device_a_spare", "ENABLED", "MANUAL", "待分配", java.util.List.of()));

        mockMvc.perform(post("/api/device-accounts")
                        .contentType("application/json")
                        .content("""
                            {
                              "deviceNodeId":100,
                              "userId":null,
                              "accountName":"device_a_spare",
                              "accountStatus":"ENABLED",
                              "sourceType":"MANUAL",
                              "remark":"待分配"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").isEmpty())
                .andExpect(jsonPath("$.data.accountName").value("device_a_spare"));
    }
}
```

```java
package com.company.ams.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ams.execution.ExecutionService;
import com.company.ams.request.RequestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approval-flow-device-nullable;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApprovalFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void requestAgainstUnboundAccountBindsUserDuringExecution() throws Exception {
        jdbcTemplate.update(
                """
                insert into device_account (
                  id, user_id, device_node_id, account_name, account_status, source_type, remark, deleted
                ) values (99, null, 100, 'device_a_spare', 'ENABLED', 'MANUAL', '待分配', 0)
                """);

        MockHttpSession session = login("zhangsan", "zhangsan123");

        MvcResult createResult = mockMvc.perform(post("/api/requests")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                            {
                              "requestType":"ROLE_ADD",
                              "targetUserId":4,
                              "targetDeviceNodeId":100,
                              "targetAccountName":"device_a_spare",
                              "reason":"新增备用账号权限",
                              "items":[{"roleNodeId":302}]
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long requestId = body.path("data").path("id").asLong();

        requestService.advance(requestId, "APPROVE");
        requestService.advance(requestId, "APPROVE");
        requestService.advance(requestId, "APPROVE");
        executionService.submit(requestId);

        Integer boundUserId = jdbcTemplate.queryForObject(
                "select user_id from device_account where account_name = 'device_a_spare'",
                Integer.class);

        Integer grantedRoleCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from device_account_role dar
                join device_account da on da.id = dar.device_account_id
                where da.account_name = 'device_a_spare'
                  and da.user_id = 4
                  and dar.role_node_id = 302
                """,
                Integer.class);

        assertThat(boundUserId).isEqualTo(4);
        assertThat(grantedRoleCount).isEqualTo(1);
    }

    private MockHttpSession login(String loginName, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"loginName":"%s","password":"%s"}
                            """.formatted(loginName, password)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
```

- [ ] **Step 2: Run the device-account tests to verify they fail**

Run: `mvn -f backend/pom.xml -Dtest=DeviceAccountControllerTest,ApprovalFlowIntegrationTest test`

Expected: FAIL because the schema still requires `user_id not null`, CRUD endpoints do not exist, and request execution still resolves accounts by `(user_id, device_node_id)`.

- [ ] **Step 3: Add the migration, CRUD API, and request-account compatibility logic**

```sql
-- backend/src/main/resources/db/migration/V5__device_account_nullable_binding.sql
alter table device_account drop foreign key fk_device_account_user;
alter table device_account drop index uk_device_account_user_device;
alter table device_account modify column user_id bigint null;
alter table device_account add unique key uk_device_account_device_account (device_node_id, account_name);
alter table device_account add key idx_device_account_user (user_id);
alter table device_account add constraint fk_device_account_user foreign key (user_id) references sys_user (id);
```

```java
// backend/src/main/java/com/company/ams/account/DeviceAccountUpsertCommand.java
package com.company.ams.account;

public record DeviceAccountUpsertCommand(
        Long deviceNodeId,
        Long userId,
        String accountName,
        String accountStatus,
        String sourceType,
        String remark) {}
```

```java
// backend/src/main/java/com/company/ams/account/DeviceAccountRow.java
package com.company.ams.account;

import java.util.List;

public record DeviceAccountRow(
        Long id,
        Long deviceNodeId,
        String deviceName,
        Long userId,
        String userName,
        String accountName,
        String accountStatus,
        String sourceType,
        String remark,
        List<String> roles) {}
```

```java
// backend/src/main/java/com/company/ams/common/persistence/RequestWorkflowPersistence.java
private void validateDeviceAccount(long targetUserId, long targetDeviceNodeId, String targetAccountName) {
    DeviceAccountRepository.RequestTargetAccount account =
            deviceAccountRepository.findRequestTargetAccount(targetDeviceNodeId, targetAccountName)
                    .orElseThrow(() -> new IllegalArgumentException("Target device account does not exist"));
    if (account.userId() != null && account.userId() != targetUserId) {
        throw new BusinessException("Target device account is already bound to another user");
    }
}

String targetAccountName = jdbcTemplate.query(
        "select target_account_name from request_order where id = ?",
        rs -> rs.next() ? rs.getString("target_account_name") : null,
        requestId);
if (targetAccountName == null || targetAccountName.isBlank()) {
    throw new IllegalStateException("Request target account is required");
}

for (Long roleNodeId : roleNodeIds) {
    deviceAccountRepository.bindRoleToRequestTarget(
            requestId,
            target.targetUserId(),
            target.targetDeviceNodeId(),
            targetAccountName,
            roleNodeId);
}
```

```java
// backend/src/main/java/com/company/ams/common/persistence/DeviceAccountRepository.java
public Optional<RequestTargetAccount> findRequestTargetAccount(long deviceNodeId, String accountName) {
    List<RequestTargetAccount> rows = jdbcTemplate.query(
            """
            select id, user_id
            from device_account
            where deleted = 0 and device_node_id = ? and account_name = ?
            """,
            (rs, rowNum) -> new RequestTargetAccount(
                    rs.getLong("id"),
                    (Long) rs.getObject("user_id")),
            deviceNodeId,
            accountName);
    return rows.stream().findFirst();
}

@Transactional
public void bindRoleToRequestTarget(long requestId, long targetUserId, long targetDeviceNodeId, String targetAccountName, long roleNodeId) {
    RequestTargetAccount account = findRequestTargetAccount(targetDeviceNodeId, targetAccountName)
            .orElseThrow(() -> new IllegalStateException("Missing target device account"));
    if (account.userId() == null) {
        jdbcTemplate.update(
                "update device_account set user_id = ?, updated_at = current_timestamp where id = ?",
                targetUserId,
                account.id());
    }
    if (account.userId() != null && account.userId() != targetUserId) {
        throw new IllegalStateException("Target device account bound to another user");
    }
    upsertRoleRelation(account.id(), roleNodeId, requestId);
}

private void upsertRoleRelation(long deviceAccountId, long roleNodeId, long requestId) {
    Integer count = jdbcTemplate.queryForObject(
            "select count(*) from device_account_role where device_account_id = ? and role_node_id = ?",
            Integer.class,
            deviceAccountId,
            roleNodeId);
    if (count != null && count > 0) {
        jdbcTemplate.update(
                """
                update device_account_role
                set relation_status = 'ACTIVE', source_request_id = ?, updated_at = current_timestamp
                where device_account_id = ? and role_node_id = ?
                """,
                requestId,
                deviceAccountId,
                roleNodeId);
        return;
    }
    jdbcTemplate.update(
            """
            insert into device_account_role (
              device_account_id, role_node_id, relation_status, effective_at, source_request_id, created_at, updated_at
            ) values (?, ?, 'ACTIVE', current_timestamp, ?, current_timestamp, current_timestamp)
            """,
            deviceAccountId,
            roleNodeId,
            requestId);
}

public record RequestTargetAccount(long id, Long userId) {}
```

- [ ] **Step 4: Run the device-account tests again**

Run: `mvn -f backend/pom.xml -Dtest=DeviceAccountControllerTest,ApprovalFlowIntegrationTest test`

Expected: PASS with nullable binding supported, CRUD endpoints available, and request execution compatible with unbound accounts.

- [ ] **Step 5: Checkpoint the device-account backend slice**

Run:

```powershell
if (Test-Path .git) {
  git add backend/src/main/resources/db/migration/V5__device_account_nullable_binding.sql backend/src/main/java/com/company/ams/account backend/src/main/java/com/company/ams/common/persistence/DeviceAccountRepository.java backend/src/main/java/com/company/ams/common/persistence/RequestWorkflowPersistence.java backend/src/test/java/com/company/ams/account/DeviceAccountControllerTest.java backend/src/test/java/com/company/ams/e2e/ApprovalFlowIntegrationTest.java
  git commit -m "feat: add nullable device-account binding and crud"
} else {
  Write-Host "No git repo detected; checkpoint device-account backend slice after verification."
  Get-Item backend/src/main/resources/db/migration/V5__device_account_nullable_binding.sql, backend/src/main/java/com/company/ams/account/DeviceAccountUpsertCommand.java
}
```

Expected: Device-account schema and backend CRUD are checkpointed, including request-flow compatibility.

## Task 9: Build the Device Account Management Page and Request Prefill Actions

**Files:**
- Create: `frontend/src/components/device-accounts/DeviceAccountDrawerForm.vue`
- Create: `frontend/src/views/device-accounts/DeviceAccountListView.vue`
- Modify: `frontend/src/api/device-accounts.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/requests/RequestFormView.vue`
- Create: `frontend/src/tests/device-account-list-view.spec.ts`
- Modify: `frontend/src/tests/request-form.spec.ts`

- [ ] **Step 1: Write the failing device-account UI tests**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { fetchDeviceAccounts } from '../api/device-accounts'
import DeviceAccountListView from '../views/device-accounts/DeviceAccountListView.vue'

const pushMock = vi.fn()

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push: pushMock })
  }
})

vi.mock('../api/device-accounts', () => ({
  fetchDeviceAccounts: vi.fn(),
  createDeviceAccount: vi.fn(),
  updateDeviceAccount: vi.fn(),
  deleteDeviceAccount: vi.fn()
}))

describe('DeviceAccountListView', () => {
  beforeEach(() => {
    vi.mocked(fetchDeviceAccounts).mockResolvedValue([
      {
        id: '3',
        deviceNodeId: 100,
        deviceName: 'Device A',
        userId: '4',
        userName: 'Wang Wu',
        accountName: 'device_a_wangwu',
        accountStatus: 'ENABLED',
        sourceType: 'MANUAL',
        remark: '',
        roles: ['Inspector']
      }
    ])
    pushMock.mockReset()
  })

  it('navigates to request form with prefilled account context', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: DeviceAccountListView }]
    })

    await router.push('/')
    await router.isReady()

    const wrapper = mount(DeviceAccountListView, {
      global: { plugins: [router] }
    })
    await flushPromises()

    await wrapper.get('[data-testid="action-request-role-add"]').trigger('click')

    expect(pushMock).toHaveBeenCalledWith({
      name: 'request-form',
      query: {
        requestType: 'ROLE_ADD',
        targetUserId: '4',
        targetDeviceNodeId: '100',
        targetAccountName: 'device_a_wangwu'
      }
    })
  })
})
```

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useRoute } from 'vue-router'
import RequestFormView from '../views/requests/RequestFormView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {
      requestType: 'ROLE_ADD',
      targetUserId: '4',
      targetDeviceNodeId: '100',
      targetAccountName: 'device_a_wangwu'
    }
  })
}))

describe('RequestFormView prefill', () => {
  beforeEach(() => {
    void useRoute()
  })

  it('applies route query values as initial request form state', async () => {
    const wrapper = mount(RequestFormView)
    await flushPromises()

    expect(wrapper.get('select[name="targetUserId"]').element.value).toBe('4')
    expect(wrapper.get('select[name="targetDeviceNodeId"]').element.value).toBe('100')
    expect(wrapper.get('select[name="targetAccountName"]').element.value).toBe('device_a_wangwu')
  })
})
```

- [ ] **Step 2: Run the device-account UI tests to verify they fail**

Run: `npm --prefix frontend test -- src/tests/device-account-list-view.spec.ts src/tests/request-form.spec.ts`

Expected: FAIL because there is no dedicated device-account management page, no prefill navigation action, and `RequestFormView` does not read route query parameters.

- [ ] **Step 3: Implement device-account CRUD bindings, management page, and request prefills**

```ts
// frontend/src/api/device-accounts.ts
import axios from 'axios'

export interface DeviceAccountItem {
  id: string
  deviceNodeId: number
  deviceName: string
  userId: string
  userName: string
  accountName: string
  accountStatus: string
  sourceType: string
  remark: string
  roles: string[]
}

export async function fetchDeviceAccounts(): Promise<DeviceAccountItem[]> {
  const { data } = await axios.get('/api/device-accounts')
  return data.data.list.map((row: any) => ({
    id: String(row.id),
    deviceNodeId: Number(row.deviceNodeId),
    deviceName: row.deviceName,
    userId: row.userId == null ? '' : String(row.userId),
    userName: row.userName ?? '',
    accountName: row.accountName,
    accountStatus: row.accountStatus,
    sourceType: row.sourceType,
    remark: row.remark ?? '',
    roles: Array.isArray(row.roles) ? row.roles : []
  }))
}

export async function createDeviceAccount(payload: Record<string, unknown>): Promise<DeviceAccountItem> {
  const { data } = await axios.post('/api/device-accounts', payload)
  return data.data
}
```

```vue
<!-- frontend/src/views/device-accounts/DeviceAccountListView.vue -->
<template>
  <section class="management-page">
    <PageHero title="设备账号管理" description="维护账号资料、绑定人员并从这里发起敏感审批动作。" :metrics="metrics" />

    <div class="management-toolbar">
      <button data-testid="create-device-account" type="button" @click="drawerOpen = true">新增账号</button>
    </div>

    <table class="management-table">
      <thead>
        <tr>
          <th>设备</th>
          <th>账号</th>
          <th>绑定人员</th>
          <th>角色</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id">
          <td>{{ row.deviceName }}</td>
          <td>{{ row.accountName }}</td>
          <td>{{ row.userName || '未绑定' }}</td>
          <td>{{ row.roles.join(' / ') || '无角色' }}</td>
          <td>
            <button
              data-testid="action-request-role-add"
              type="button"
              :disabled="!row.userId"
              @click="goToRequest('ROLE_ADD', row)"
            >
              发起加权申请
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <aside v-if="drawerOpen">
      <DeviceAccountDrawerForm @submit="handleCreate" />
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import DeviceAccountDrawerForm from '../../components/device-accounts/DeviceAccountDrawerForm.vue'
import PageHero from '../../components/shell/PageHero.vue'
import { createDeviceAccount, fetchDeviceAccounts, type DeviceAccountItem } from '../../api/device-accounts'

const router = useRouter()
const rows = ref<DeviceAccountItem[]>([])
const drawerOpen = ref(false)

const metrics = computed(() => [
  { label: '账号总数', value: rows.value.length },
  { label: '未绑定人员', value: rows.value.filter((item) => !item.userId).length },
  { label: '无角色账号', value: rows.value.filter((item) => item.roles.length === 0).length }
])

onMounted(async () => {
  rows.value = await fetchDeviceAccounts()
})

async function handleCreate(payload: Record<string, unknown>) {
  await createDeviceAccount(payload)
  rows.value = await fetchDeviceAccounts()
  drawerOpen.value = false
}

function goToRequest(requestType: string, row: DeviceAccountItem) {
  void router.push({
    name: 'request-form',
    query: {
      requestType,
      targetUserId: row.userId,
      targetDeviceNodeId: String(row.deviceNodeId),
      targetAccountName: row.accountName
    }
  })
}
</script>
```

```ts
// frontend/src/views/requests/RequestFormView.vue
import { watchEffect } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

watchEffect(() => {
  if (typeof route.query.requestType === 'string') {
    requestType.value = route.query.requestType
  }
  if (typeof route.query.targetUserId === 'string') {
    targetUserId.value = route.query.targetUserId
  }
  if (typeof route.query.targetDeviceNodeId === 'string') {
    targetDeviceNodeId.value = route.query.targetDeviceNodeId
  }
  if (typeof route.query.targetAccountName === 'string') {
    targetAccountName.value = route.query.targetAccountName
  }
})
```

- [ ] **Step 4: Run the device-account UI tests again**

Run: `npm --prefix frontend test -- src/tests/device-account-list-view.spec.ts src/tests/request-form.spec.ts`

Expected: PASS with the new management page navigating to a prefilled request form and the request form honoring route query defaults.

- [ ] **Step 5: Checkpoint the device-account page slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/api/device-accounts.ts frontend/src/components/device-accounts/DeviceAccountDrawerForm.vue frontend/src/views/device-accounts/DeviceAccountListView.vue frontend/src/router/index.ts frontend/src/views/requests/RequestFormView.vue frontend/src/tests/device-account-list-view.spec.ts frontend/src/tests/request-form.spec.ts
  git commit -m "feat: add device account management and request prefills"
} else {
  Write-Host "No git repo detected; checkpoint device-account page slice after verification."
  Get-Item frontend/src/views/device-accounts/DeviceAccountListView.vue, frontend/src/views/requests/RequestFormView.vue
}
```

Expected: Device-account management page is checkpointed with approval entrypoints routed through the request form.

## Task 10: Unify Request, Query, and Audit Pages Under the Cockpit Visual Language

**Files:**
- Modify: `frontend/src/views/requests/RequestListView.vue`
- Modify: `frontend/src/views/requests/RequestFormView.vue`
- Modify: `frontend/src/views/queries/DevicePermissionView.vue`
- Modify: `frontend/src/views/audit/AuditLogView.vue`
- Create: `frontend/src/tests/request-list-view.spec.ts`
- Modify: `frontend/src/tests/device-permission-view.spec.ts`
- Modify: `frontend/src/tests/audit-log-view.spec.ts`

- [ ] **Step 1: Write the failing view-unification tests**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchRequests } from '../api/requests'
import RequestListView from '../views/requests/RequestListView.vue'

vi.mock('../api/requests', () => ({
  fetchRequests: vi.fn()
}))

describe('RequestListView', () => {
  beforeEach(() => {
    vi.mocked(fetchRequests).mockResolvedValue([
      {
        id: '1',
        requestNo: 'REQ1',
        requestType: 'ROLE_ADD',
        targetAccountName: 'device_a_wangwu',
        status: 'WAIT_QA',
        createdAt: '2026-04-24 09:00:00'
      }
    ])
  })

  it('renders cockpit header, filters and table summary text', async () => {
    const wrapper = mount(RequestListView)
    await flushPromises()

    expect(wrapper.text()).toContain('申请管理')
    expect(wrapper.text()).toContain('状态筛选')
    expect(wrapper.text()).toContain('REQ1')
  })
})
```

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import AuditLogView from '../views/audit/AuditLogView.vue'

vi.mock('axios', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('AuditLogView cockpit shell', () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: {
          total: 1,
          list: [
            { id: '1', operatorName: 'admin', actionType: 'GRANT_PERMISSION', objectType: 'DEVICE_ACCOUNT', createdAt: '2026-04-24 10:00:00' }
          ]
        }
      }
    })
  })

  it('renders filter labels and dense result headings', async () => {
    const wrapper = mount(AuditLogView)
    await flushPromises()

    expect(wrapper.text()).toContain('审计追踪')
    expect(wrapper.text()).toContain('时间范围')
    expect(wrapper.text()).toContain('操作对象')
  })
})
```

- [ ] **Step 2: Run the unification tests to verify they fail**

Run: `npm --prefix frontend test -- src/tests/request-list-view.spec.ts src/tests/device-permission-view.spec.ts src/tests/audit-log-view.spec.ts`

Expected: FAIL because the current request/query/audit pages still use raw tables without the cockpit page header, filter panel, or denser management presentation.

- [ ] **Step 3: Apply the shared shell components to request, query, and audit pages**

```vue
<!-- frontend/src/views/requests/RequestListView.vue -->
<template>
  <section class="management-page">
    <PageHero title="申请管理" description="查看当前审批链中的请求、状态与最近流转结果。" :metrics="[]" />
    <FilterPanel title="状态筛选">
      <select>
        <option value="">全部状态</option>
        <option value="WAIT_DEPT_MANAGER">待部门负责人</option>
        <option value="WAIT_QA">待 QA</option>
        <option value="WAIT_QM">待 QM</option>
        <option value="WAIT_QI_EXECUTE">待 QI 执行</option>
      </select>
    </FilterPanel>
    <table class="management-table">
      <thead>
        <tr>
          <th>申请单号</th>
          <th>类型</th>
          <th>目标账号</th>
          <th>状态</th>
          <th>创建时间</th>
        </tr>
      </thead>
    </table>
  </section>
</template>
```

```vue
<!-- frontend/src/views/queries/DevicePermissionView.vue -->
<template>
  <section class="management-page">
    <PageHero title="查询管理" description="在统一表格里查看设备角色与账号关系。" :metrics="[]" />
    <FilterPanel title="设备筛选">
      <input placeholder="输入设备节点 ID" />
    </FilterPanel>
    <table class="management-table">
      <thead>
        <tr>
          <th>角色</th>
          <th>设备账号</th>
        </tr>
      </thead>
    </table>
  </section>
</template>
```

```vue
<!-- frontend/src/views/audit/AuditLogView.vue -->
<template>
  <section class="management-page">
    <PageHero title="审计追踪" description="按时间、人员和操作对象查看系统留痕。" :metrics="[]" />
    <FilterPanel title="时间范围">
      <input placeholder="开始时间" />
      <input placeholder="结束时间" />
      <input placeholder="操作对象" />
    </FilterPanel>
    <table class="management-table">
      <thead>
        <tr>
          <th>操作人</th>
          <th>动作</th>
          <th>操作对象</th>
          <th>时间</th>
        </tr>
      </thead>
    </table>
  </section>
</template>
```

- [ ] **Step 4: Run the unification tests again**

Run: `npm --prefix frontend test -- src/tests/request-list-view.spec.ts src/tests/device-permission-view.spec.ts src/tests/audit-log-view.spec.ts`

Expected: PASS with request/query/audit pages all using the shared cockpit visual structure.

- [ ] **Step 5: Checkpoint the page-unification slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/views/requests/RequestListView.vue frontend/src/views/requests/RequestFormView.vue frontend/src/views/queries/DevicePermissionView.vue frontend/src/views/audit/AuditLogView.vue frontend/src/tests/request-list-view.spec.ts frontend/src/tests/device-permission-view.spec.ts frontend/src/tests/audit-log-view.spec.ts
  git commit -m "feat: unify request query and audit pages"
} else {
  Write-Host "No git repo detected; checkpoint page-unification slice after verification."
  Get-Item frontend/src/views/requests/RequestListView.vue, frontend/src/views/audit/AuditLogView.vue
}
```

Expected: Request/query/audit pages are checkpointed in the same cockpit language as the master-data pages.

## Task 11: Run Final Verification, E2E Navigation Checks, and Docker Smoke

**Files:**
- Modify: `frontend/tests/e2e/login.spec.ts`
- Create: `frontend/tests/e2e/cockpit-navigation.spec.ts`
- Create: `docs/test/2026-04-24-ams2-ui-crud-smoke.md`

- [ ] **Step 1: Write the failing e2e navigation smoke test**

```ts
import { expect, test } from '@playwright/test'

test('authenticated shell shows cockpit navigation', async ({ page }) => {
  await page.addInitScript(() => {
    window.sessionStorage.setItem('ams_auth_token', 'dev-token')
  })

  await page.goto('/')

  await expect(page.getByText('用户管理')).toBeVisible()
  await expect(page.getByText('设备账号管理')).toBeVisible()
  await expect(page.getByTestId('global-search')).toBeVisible()
})
```

- [ ] **Step 2: Run the e2e smoke test to verify it fails**

Run: `npm --prefix frontend run test:e2e -- cockpit-navigation.spec.ts`

Expected: FAIL because the current browser shell does not render the final cockpit navigation or the test id on the command bar.

- [ ] **Step 3: Add final smoke coverage and the manual上线 checklist**

```ts
// frontend/tests/e2e/login.spec.ts
import { expect, test } from '@playwright/test'

test('login page loads redesigned hero and form', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByText('AMS2.0 Operations Cockpit')).toBeVisible()
  await expect(page.getByRole('button', { name: '进入系统' })).toBeVisible()
})
```

```md
<!-- docs/test/2026-04-24-ams2-ui-crud-smoke.md -->
# AMS2.0 UI/CRUD Smoke Checklist

1. 打开 `http://127.0.0.1:5173/login`，确认登录页出现品牌说明、登录卡片和正式入口按钮。
2. 使用 `zhangsan / zhangsan123` 登录，确认进入工作台后可看到指标卡、异常提醒和快捷入口。
3. 进入“用户管理”，新增 1 个用户并成功刷新列表；再将其停用并看到状态变化。
4. 进入“部门管理”，新增 1 个部门，编辑说明文字，并验证有成员的部门删除时收到明确错误提示。
5. 进入“设备账号管理”，新增 1 个未绑定人员的账号，随后在抽屉里绑定人员并保存。
6. 从设备账号行内点击“发起加权申请”，确认请求单页面自动预填目标用户、设备和账号。
7. 提交 1 张申请后，在“申请管理”看到列表刷新；再到“审计追踪”确认出现新留痕。
8. 执行 `docker compose up --build -d` 后，再次打开系统并完成以上核心链路的浏览器冒烟。
```

- [ ] **Step 4: Run the full verification stack**

Run: `mvn -f backend/pom.xml test`

Expected: PASS with controller, integration, workflow, and dashboard tests all green.

Run: `npm --prefix frontend test`

Expected: PASS with shell, dashboard, CRUD pages, request/query/audit, and runtime-safety specs all green.

Run: `npm --prefix frontend run build`

Expected: PASS with Vite production bundle emitted under `frontend/dist`.

Run: `npm --prefix frontend run test:e2e -- login.spec.ts cockpit-navigation.spec.ts`

Expected: PASS with both login and authenticated-shell smoke tests green.

Run: `docker compose up --build -d`

Expected: PASS with `backend` exposing `8080` and `frontend` exposing `5173`, no container crash loop.

Run: `docker compose ps`

Expected: PASS with both services in `Up` state.

- [ ] **Step 5: Checkpoint the release candidate**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/tests/e2e/login.spec.ts frontend/tests/e2e/cockpit-navigation.spec.ts docs/test/2026-04-24-ams2-ui-crud-smoke.md
  git commit -m "chore: verify cockpit ui crud release candidate"
} else {
  Write-Host "No git repo detected; record the final verification output and generated docs as the release checkpoint."
  Get-Item docs/test/2026-04-24-ams2-ui-crud-smoke.md, frontend/tests/e2e/cockpit-navigation.spec.ts
}
```

Expected: A final checkpoint exists with the exact verification artifacts needed before declaring the system ready to launch.

## Coverage Check

- Spec section `视觉方向 / Operations Cockpit` is implemented by Tasks 1, 2, and 3.
- Spec section `统一信息架构与导航` is implemented by Task 1 and completed route integration in Task 9.
- Spec section `登录页与工作台重构` is implemented by Tasks 2 and 3.
- Spec section `用户 CRUD` is implemented by Tasks 4 and 5.
- Spec section `部门 CRUD 与删除保护` is implemented by Tasks 6 and 7.
- Spec section `设备账号 CRUD、绑定/解绑人员、独立管理页` is implemented by Tasks 8 and 9.
- Spec section `敏感动作必须走申请流` is implemented by Task 9 and visually reinforced in Task 10.
- Spec section `申请/查询/审计页视觉统一` is implemented by Task 10.
- Spec section `Docker 下关键流程可验证` is implemented by Task 11.

No spec gaps remain in this plan.

## Placeholder Scan

- No `TODO`, `TBD`, `implement later`, or unresolved placeholders remain.
- Every task lists exact file paths.
- Every test step contains concrete test code.
- Every verification step contains an exact command and expected result.
- Every checkpoint step includes an explicit fallback for the current non-git workspace.

## Type Consistency Check

- Shared backend list shape is standardized as `ApiResponse<ListPayload<T>>` starting in Task 4 and reused by later CRUD endpoints.
- Frontend master-data row types are consistently named `UserItem`, `DepartmentItem`, and `DeviceAccountItem`.
- Backend command types are consistently named `UserUpsertCommand`, `DepartmentUpsertCommand`, and `DeviceAccountUpsertCommand`.
- Dashboard DTO root stays `DashboardSummary`; metric field names remain `userTotal`, `departmentTotal`, `deviceAccountTotal`, and `pendingRequestTotal`.
- Device-account request compatibility consistently uses `(deviceNodeId, accountName)` instead of the old `(userId, deviceNodeId)` uniqueness assumption.
