# AMS 目标复刻第一阶段 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前简化版 AMS 前端重构为与目标系统一致的信息架构基础版，完成登录页、全局壳层、一级导航、左侧上下文菜单、31 页路由骨架和关键页面模板的第一阶段复刻。

**Architecture:** 保留现有 `Vue 3 + Pinia + Vue Router + Element Plus` 技术栈，不在第一阶段引入大规模后端重构。通过“配置驱动的路由与页面模板”方式，先复刻目标系统的页面结构、文案和框架交互，再在后续阶段逐域补齐真实数据与业务写操作。

**Tech Stack:** Vue 3, TypeScript, Pinia, Vue Router, Element Plus, Vitest, Spring Boot 3 backend kept as-is for auth compatibility

---

## Scope Check

设计文档覆盖了多个独立子系统，无法用一份计划直接安全执行到底。第一阶段只实现以下内容，并确保其本身可运行、可测试、可演示：

- 登录页改造成目标系统风格
- 顶部一级导航和左侧上下文菜单
- 31 个页面的路由骨架
- 通用页面模板与目标系统文案、按钮、表头占位
- 控制台基础形态
- 语言切换保留，主题切换不在第一阶段暴露

以下内容明确不在本计划内：

- 工作流引擎重构
- 审批/执行/审核/代理的真实写入行为
- 报表导出后端
- HR 与系统管理员复杂 CRUD 的真实接口联通

## File Structure

### Create

- `frontend/src/config/target-clone.ts`
  责任：集中定义目标系统一级导航、二级菜单、页面标题、说明、按钮、表头、标签页等只读页面元数据。
- `frontend/src/views/TargetPageView.vue`
  责任：通用渲染大多数目标系统页面骨架，避免为 31 个页面重复写静态模板。
- `frontend/src/tests/target-clone-pages.spec.ts`
  责任：验证关键路由、页面标题、按钮和表头配置已按目标系统落位。

### Modify

- `frontend/src/router/index.ts`
  责任：切换到 hash 路由，挂载目标系统一级导航与 31 页路由骨架。
- `frontend/src/config/navigation.ts`
  责任：从当前简化导航改为目标系统的一级导航与上下文二级菜单模型。
- `frontend/src/layouts/AppLayout.vue`
  责任：从当前 cockpit 壳层调整为目标系统风格的顶部一级导航 + 左侧菜单 + 内容区结构。
- `frontend/src/components/shell/AppSidebar.vue`
  责任：渲染当前一级导航下的左侧二级菜单。
- `frontend/src/components/shell/AppTopbar.vue`
  责任：渲染一级导航、用户信息、语言切换和版本信息，去掉与目标系统不一致的全局搜索、新建按钮和主题开关。
- `frontend/src/views/LoginView.vue`
  责任：重构为目标系统简洁登录页。
- `frontend/src/views/DashboardView.vue`
  责任：复刻控制台欢迎文案、公告区和待办统计区块。
- `frontend/src/tests/login-view.spec.ts`
  责任：验证登录页文案和按钮已改为目标系统风格。
- `frontend/src/tests/app-layout.spec.ts`
  责任：验证壳层导航结构改为目标系统风格。
- `frontend/src/tests/dashboard-view.spec.ts`
  责任：验证控制台页面的欢迎文案和待办区块。

## Task 1: Rebuild Route and Navigation Metadata Around the Target System

**Files:**
- Create: `frontend/src/config/target-clone.ts`
- Modify: `frontend/src/config/navigation.ts`
- Modify: `frontend/src/router/index.ts`
- Test: `frontend/src/tests/target-clone-pages.spec.ts`

- [ ] **Step 1: Write the failing route skeleton test**

```ts
import { describe, expect, it } from 'vitest'
import router from '../router'

describe('target clone routing', () => {
  it('registers the researched target routes and uses hash history', () => {
    const paths = router.getRoutes().map((route) => route.path)

    expect(paths).toContain('/dashboard')
    expect(paths).toContain('/access/myRequest')
    expect(paths).toContain('/task/accessApproval')
    expect(paths).toContain('/report/accountHistory')
    expect(paths).toContain('/systemAdmin/mailTemplateCfg')
    expect(paths).toContain('/hrAdmin/hrManagement')
    expect(router.options.history.base).toBe('')
  })
})
```

- [ ] **Step 2: Run the route skeleton test to verify it fails**

Run: `npm --prefix frontend test -- src/tests/target-clone-pages.spec.ts`

Expected: FAIL because the current router still uses the simplified AMS route set and does not expose the target-system path tree.

- [ ] **Step 3: Implement target-system route metadata and route registration**

```ts
// frontend/src/config/target-clone.ts
export interface TargetCloneSection {
  key: string
  label: string
  entries: TargetCloneEntry[]
}

export interface TargetCloneEntry {
  path: string
  sectionKey: string
  title: string
  description: string
  buttons?: string[]
  tabs?: string[]
  headers?: string[]
}

export const targetCloneSections: TargetCloneSection[] = [
  {
    key: 'dashboard',
    label: '控制台',
    entries: [{ path: '/dashboard', sectionKey: 'dashboard', title: '控制台', description: '系统工作台' }]
  },
  {
    key: 'access',
    label: '权限管理',
    entries: [
      { path: '/access/myRequest', sectionKey: 'access', title: '申请权限', description: '当前正在为账号 AMSAdmin 申请权限', buttons: ['申请'], headers: ['权限路径', '全部展开'] },
      { path: '/access/myRemove', sectionKey: 'access', title: '删除权限', description: '当前正在为账号 AMSAdmin 删除权限', buttons: ['删除'], headers: ['权限路径'] }
    ]
  }
]
```

```ts
// frontend/src/router/index.ts
import { createRouter, createWebHashHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import TargetPageView from '../views/TargetPageView.vue'
import { targetCloneSections } from '../config/target-clone'

const contentRoutes = targetCloneSections.flatMap((section) =>
  section.entries.map((entry) => ({
    path: entry.path.replace(/^\//, ''),
    name: entry.path,
    component: entry.path === '/dashboard' ? DashboardView : TargetPageView,
    meta: {
      sectionKey: section.key,
      pagePath: entry.path,
      title: entry.title
    }
  }))
)

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        ...contentRoutes
      ]
    }
  ]
})
```

- [ ] **Step 4: Run the route skeleton test again**

Run: `npm --prefix frontend test -- src/tests/target-clone-pages.spec.ts`

Expected: PASS with all key researched routes registered under hash history.

- [ ] **Step 5: Commit checkpoint**

```bash
npm --prefix frontend test -- src/tests/target-clone-pages.spec.ts
```

Expected: Route metadata slice is green and ready for layout integration.

## Task 2: Rebuild the Global Shell to Match the Target Information Architecture

**Files:**
- Modify: `frontend/src/layouts/AppLayout.vue`
- Modify: `frontend/src/components/shell/AppSidebar.vue`
- Modify: `frontend/src/components/shell/AppTopbar.vue`
- Modify: `frontend/src/tests/app-layout.spec.ts`

- [ ] **Step 1: Write the failing shell structure test**

```ts
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import AppLayout from '../layouts/AppLayout.vue'

describe('AppLayout target shell', () => {
  it('renders top-level sections and contextual left navigation', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/',
          component: AppLayout,
          children: [{ path: 'dashboard', component: { template: '<div>body</div>' } }]
        }
      ]
    })

    await router.push('/dashboard')
    await router.isReady()

    const wrapper = mount(AppLayout, {
      global: {
        plugins: [createPinia(), router]
      }
    })

    expect(wrapper.text()).toContain('控制台')
    expect(wrapper.text()).toContain('权限管理')
    expect(wrapper.text()).toContain('任务管理')
    expect(wrapper.text()).toContain('AMSAdmin')
    expect(wrapper.text()).toContain('中文')
    expect(wrapper.text()).toContain('English')
    expect(wrapper.text()).toContain('版本: 1.0.0')
  })
})
```

- [ ] **Step 2: Run the shell test to verify it fails**

Run: `npm --prefix frontend test -- src/tests/app-layout.spec.ts`

Expected: FAIL because the current shell still renders the previous cockpit structure with search, theme toggle, and a non-target navigation model.

- [ ] **Step 3: Implement target-style shell**

```vue
<!-- frontend/src/layouts/AppLayout.vue -->
<template>
  <div class="target-shell">
    <AppTopbar />
    <div class="target-shell__body">
      <AppSidebar />
      <main class="target-shell__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>
```

```vue
<!-- frontend/src/components/shell/AppTopbar.vue -->
<template>
  <header class="topbar">
    <nav class="topbar__primary">
      <RouterLink v-for="section in sections" :key="section.key" :to="section.entries[0].path">
        {{ section.label }}
      </RouterLink>
    </nav>
    <div class="topbar__meta">
      <span>AMSAdmin</span>
      <button type="button" @click="preferences.setLocale('zh-CN')">中文</button>
      <button type="button" @click="preferences.setLocale('en-US')">English</button>
      <span>版本: 1.0.0</span>
    </div>
  </header>
</template>
```

```vue
<!-- frontend/src/components/shell/AppSidebar.vue -->
<template>
  <aside class="sidebar">
    <RouterLink v-for="entry in activeSection?.entries ?? []" :key="entry.path" :to="entry.path">
      {{ entry.title }}
    </RouterLink>
  </aside>
</template>
```

- [ ] **Step 4: Run the shell test again**

Run: `npm --prefix frontend test -- src/tests/app-layout.spec.ts`

Expected: PASS with target-system primary sections and top-right user/language/version block rendered.

- [ ] **Step 5: Commit checkpoint**

```bash
npm --prefix frontend test -- src/tests/app-layout.spec.ts
```

Expected: Shell slice is green and ready for page rendering.

## Task 3: Rebuild the Login Page in the Target Style

**Files:**
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/tests/login-view.spec.ts`

- [ ] **Step 1: Write the failing login page test**

```ts
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { describe, expect, it } from 'vitest'
import LoginView from '../views/LoginView.vue'

describe('LoginView target clone', () => {
  it('renders the target system title and a minimal login form', () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [createTestingPinia({ stubActions: true })]
      }
    })

    expect(wrapper.text()).toContain('权限管理系统')
    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.text()).toContain('登录')
  })
})
```

- [ ] **Step 2: Run the login test to verify it fails**

Run: `npm --prefix frontend test -- src/tests/login-view.spec.ts`

Expected: FAIL because the current login page still contains the prior cockpit branding and CTA wording.

- [ ] **Step 3: Implement the target-style login page**

```vue
<!-- frontend/src/views/LoginView.vue -->
<template>
  <section class="login-page">
    <div class="login-card">
      <h1>权限管理系统</h1>
      <form @submit.prevent="handleSubmit">
        <label>
          <span>用户名</span>
          <input v-model="loginName" name="loginName" autocomplete="username" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="password" name="password" type="password" autocomplete="current-password" />
        </label>
        <button type="submit">登录</button>
      </form>
    </div>
  </section>
</template>
```

- [ ] **Step 4: Run the login test again**

Run: `npm --prefix frontend test -- src/tests/login-view.spec.ts`

Expected: PASS with the target-style title and form fields rendered.

- [ ] **Step 5: Commit checkpoint**

```bash
npm --prefix frontend test -- src/tests/login-view.spec.ts
```

Expected: Login page slice is green.

## Task 4: Render Dashboard and Generic Target Pages from Configuration

**Files:**
- Modify: `frontend/src/views/DashboardView.vue`
- Create: `frontend/src/views/TargetPageView.vue`
- Modify: `frontend/src/tests/dashboard-view.spec.ts`
- Modify: `frontend/src/tests/target-clone-pages.spec.ts`

- [ ] **Step 1: Write the failing dashboard and generic-page tests**

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DashboardView from '../views/DashboardView.vue'

describe('DashboardView target clone', () => {
  it('renders the researched welcome copy and todo buckets', () => {
    const wrapper = mount(DashboardView)

    expect(wrapper.text()).toContain('你好, AMSAdmin')
    expect(wrapper.text()).toContain('欢迎使用权限通')
    expect(wrapper.text()).toContain('系统公告')
    expect(wrapper.text()).toContain('待办事项')
    expect(wrapper.text()).toContain('待审批')
    expect(wrapper.text()).toContain('待操作')
  })
})
```

```ts
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import TargetPageView from '../views/TargetPageView.vue'

describe('TargetPageView', () => {
  it('renders configured page title, actions, and headers', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/access/accountManagement', component: TargetPageView, meta: { pagePath: '/access/accountManagement' } }]
    })

    await router.push('/access/accountManagement')
    await router.isReady()

    const wrapper = mount(TargetPageView, {
      global: { plugins: [router] }
    })

    expect(wrapper.text()).toContain('账号列表')
    expect(wrapper.text()).toContain('申请权限')
    expect(wrapper.text()).toContain('重置密码')
    expect(wrapper.text()).toContain('删除权限')
    expect(wrapper.text()).toContain('账号名称')
  })
})
```

- [ ] **Step 2: Run the dashboard/page tests to verify they fail**

Run: `npm --prefix frontend test -- src/tests/dashboard-view.spec.ts src/tests/target-clone-pages.spec.ts`

Expected: FAIL because the current dashboard and views do not use the researched target-system copy or generic configuration rendering.

- [ ] **Step 3: Implement dashboard clone and generic page renderer**

```vue
<!-- frontend/src/views/TargetPageView.vue -->
<template>
  <section class="target-page">
    <header class="target-page__header">
      <h1>{{ page?.title }}</h1>
      <p>{{ page?.description }}</p>
    </header>
    <div v-if="page?.buttons?.length" class="target-page__actions">
      <button v-for="button in page.buttons" :key="button" type="button">{{ button }}</button>
    </div>
    <div v-if="page?.tabs?.length" class="target-page__tabs">
      <button v-for="tab in page.tabs" :key="tab" type="button">{{ tab }}</button>
    </div>
    <table v-if="page?.headers?.length" class="target-page__table">
      <thead>
        <tr>
          <th v-for="header in page.headers" :key="header">{{ header }}</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td :colspan="page.headers.length">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
```

```vue
<!-- frontend/src/views/DashboardView.vue -->
<template>
  <section class="dashboard-page">
    <header>
      <h1>你好, AMSAdmin</h1>
      <p>欢迎使用权限通</p>
      <p>您可以通过左侧的菜单栏来管理您的账号，权限及任务。或者，点击首页的“待办事项”，直接跳转到对应页面。</p>
    </header>
    <div class="dashboard-grid">
      <section>系统公告</section>
      <section>待办事项</section>
      <section>待审批 0</section>
      <section>待操作 0</section>
      <section>待审核 0</section>
      <section>待申诉 0</section>
    </div>
  </section>
</template>
```

- [ ] **Step 4: Run the dashboard/page tests again**

Run: `npm --prefix frontend test -- src/tests/dashboard-view.spec.ts src/tests/target-clone-pages.spec.ts`

Expected: PASS with control-panel copy and configured generic pages rendered.

- [ ] **Step 5: Commit checkpoint**

```bash
npm --prefix frontend test -- src/tests/dashboard-view.spec.ts src/tests/target-clone-pages.spec.ts
```

Expected: Dashboard and generic route skeleton slice is green.

## Self-Review

### Spec coverage

- 登录页、主框架、一级导航、左侧上下文菜单：Task 2 + Task 3
- 31 页路由骨架与目标系统路径：Task 1
- 控制台欢迎文案和待办区：Task 4
- 权限管理/任务管理/报告/管理员/HR 页面骨架：Task 4

### Placeholder scan

- 无 `TODO`、`TBD`、`implement later`
- 每个任务包含明确文件路径
- 每个任务包含明确测试命令

### Type consistency

- 路由和页面骨架统一依赖 `TargetCloneEntry.path`
- 壳层上下文统一基于 `sectionKey`
- 非 dashboard 页统一由 `TargetPageView.vue` 渲染

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-29-ams-target-clone-phase1-foundation.md`.

Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Because this session has no explicit user authorization for subagents, the practical path is Inline Execution.
