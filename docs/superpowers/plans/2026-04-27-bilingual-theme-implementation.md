# AMS2.0 Bilingual Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add full Chinese/English UI switching and dark/light theme switching to the current AMS2.0 frontend, with local preference persistence and updated automated coverage.

**Architecture:** Build a lightweight preference layer with Pinia for `locale` and `theme`, a flat dictionary-based translation module for UI text, and a CSS variable theme system driven by `document.documentElement.dataset.theme`. Route metadata, shell navigation, and page-level literals are migrated from hard-coded strings to translatable keys so the UI updates immediately when the user toggles language or theme.

**Tech Stack:** Vue 3, Pinia, Vue Router, Vitest, Playwright, CSS custom properties

---

## File Structure

### New Files

- `frontend/src/stores/preferences.ts`
  - Holds `locale` and `theme`, initializes defaults, restores persisted values, applies `data-theme`, and exposes toggle/set APIs.
- `frontend/src/i18n/messages.ts`
  - Flat `zh-CN` / `en-US` dictionary with shared keys for shell, login, dashboard, CRUD pages, request/query/audit pages, and common enums.
- `frontend/src/i18n/index.ts`
  - Provides `translate()` and `useI18nText()` so components can resolve text reactively from the current preference state.
- `frontend/src/tests/preferences.spec.ts`
  - Covers default values, persisted value restoration, and DOM `data-theme` application.

### Existing Files To Modify

- `frontend/src/main.ts`
  - Bootstraps Pinia, initializes preferences before mount, and keeps the existing smoke marker behavior.
- `frontend/src/styles/theme.css`
  - Expands the current single dark token set into `dark` and `light` theme variants while preserving the existing `--cockpit-*` token naming.
- `frontend/src/config/navigation.ts`
  - Replaces `label` / `description` literals with dictionary keys.
- `frontend/src/router/index.ts`
  - Replaces route `meta.title` / `subtitle` / `section` literals with dictionary keys.
- `frontend/src/components/shell/AppTopbar.vue`
  - Uses translated shell text, renders locale/theme toggle buttons, and derives translated route context from route meta keys.
- `frontend/src/components/shell/AppSidebar.vue`
  - Uses translated navigation labels and footer quick actions.
- `frontend/src/components/shell/FilterPanel.vue`
  - Translates collapse/expand button labels.
- `frontend/src/views/LoginView.vue`
  - Uses translated login copy and localized error states.
- `frontend/src/views/DashboardView.vue`
  - Uses translated hero, metric labels, empty states, and error actions.
- `frontend/src/views/users/UserListView.vue`
  - Uses translated hero, filters, table headers, status labels, empty states, loading text, errors, and confirmation text.
- `frontend/src/components/users/UserDrawerForm.vue`
  - Uses translated drawer labels, field labels, CTA labels, and status options.
- `frontend/src/views/departments/DepartmentListView.vue`
  - Same localization and theme token cleanup pattern as the user page.
- `frontend/src/components/departments/DepartmentDrawerForm.vue`
  - Same localization pattern as the user drawer.
- `frontend/src/views/device-accounts/DeviceAccountListView.vue`
  - Uses translated hero, filters, table headers, statuses, empty states, and action labels.
- `frontend/src/components/device-accounts/DeviceAccountDrawerForm.vue`
  - Uses translated drawer labels and status/source options.
- `frontend/src/views/requests/RequestListView.vue`
  - Uses translated hero, filters, status labels, empty states, and errors.
- `frontend/src/views/requests/RequestFormView.vue`
  - Uses translated launchpad copy, field labels, submission states, and validation errors.
- `frontend/src/views/queries/DevicePermissionView.vue`
  - Uses translated filters, headings, empty states, and failure messages.
- `frontend/src/views/audit/AuditLogView.vue`
  - Uses translated filters, table headings, empty states, and failure messages.
- `frontend/src/tests/app-layout.spec.ts`
  - Adds shell toggle assertions and locale/theme interaction coverage.
- `frontend/src/tests/login-view.spec.ts`
  - Moves login assertions to dictionary-driven Chinese default and English switch coverage.
- `frontend/src/tests/dashboard-view.spec.ts`
  - Moves dashboard assertions to translated output.
- `frontend/src/tests/user-list-view.spec.ts`
  - Moves user page assertions to translated output.
- `frontend/src/tests/department-list-view.spec.ts`
  - Moves department page assertions to translated output.
- `frontend/src/tests/device-account-list-view.spec.ts`
  - Moves device-account page assertions to translated output.
- `frontend/src/tests/request-list-view.spec.ts`
  - Moves request list assertions to translated output.
- `frontend/src/tests/request-form.spec.ts`
  - Moves request form assertions to translated output.
- `frontend/src/tests/device-permission-view.spec.ts`
  - Moves device permission assertions to translated output.
- `frontend/src/tests/audit-log-view.spec.ts`
  - Moves audit log assertions to translated output.
- `frontend/tests/e2e/login.spec.ts`
  - Updates smoke assertions from hard-coded English to the new default Chinese login copy.
- `frontend/tests/e2e/cockpit-navigation.spec.ts`
  - Updates shell smoke to assert translated Chinese navigation and presence of the toggle controls.
- `docs/test/2026-04-24-ams2-ui-crud-smoke.md`
  - Adds manual smoke items for locale and theme toggling.

## Task 1: Add Preferences Store and Translation Core

**Files:**
- Create: `frontend/src/stores/preferences.ts`
- Create: `frontend/src/i18n/messages.ts`
- Create: `frontend/src/i18n/index.ts`
- Create: `frontend/src/tests/preferences.spec.ts`
- Modify: `frontend/src/main.ts`

- [ ] **Step 1: Write the failing preferences/i18n test**

```ts
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { translate } from '../i18n'
import { usePreferencesStore } from '../stores/preferences'

describe('preferences store', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    setActivePinia(createPinia())
  })

  it('initializes zh-CN + dark by default and persists toggles', () => {
    const store = usePreferencesStore()

    store.initializePreferences()

    expect(store.locale).toBe('zh-CN')
    expect(store.theme).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(translate(store.locale, 'shell.topbar.logout')).toBe('退出登录')

    store.toggleLocale()
    store.toggleTheme()

    expect(store.locale).toBe('en-US')
    expect(store.theme).toBe('light')
    expect(localStorage.getItem('ams_locale')).toBe('en-US')
    expect(localStorage.getItem('ams_theme')).toBe('light')
    expect(document.documentElement.dataset.theme).toBe('light')
    expect(translate(store.locale, 'shell.topbar.logout')).toBe('Sign Out')
  })

  it('restores saved values before first render', () => {
    localStorage.setItem('ams_locale', 'en-US')
    localStorage.setItem('ams_theme', 'light')

    const store = usePreferencesStore()
    store.initializePreferences()

    expect(store.locale).toBe('en-US')
    expect(store.theme).toBe('light')
    expect(translate(store.locale, 'shell.topbar.theme.light')).toBe('Light')
  })
})
```

- [ ] **Step 2: Run the focused preferences test and verify it fails**

Run:

```powershell
npm --prefix frontend test -- src/tests/preferences.spec.ts
```

Expected: FAIL because `frontend/src/stores/preferences.ts`, `frontend/src/i18n/messages.ts`, and `frontend/src/i18n/index.ts` do not exist yet.

- [ ] **Step 3: Write the minimal preferences and i18n implementation**

```ts
// frontend/src/stores/preferences.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'

export type Locale = 'zh-CN' | 'en-US'
export type ThemeMode = 'dark' | 'light'

const LOCALE_KEY = 'ams_locale'
const THEME_KEY = 'ams_theme'
const DEFAULT_LOCALE: Locale = 'zh-CN'
const DEFAULT_THEME: ThemeMode = 'dark'

function isLocale(value: string | null): value is Locale {
  return value === 'zh-CN' || value === 'en-US'
}

function isTheme(value: string | null): value is ThemeMode {
  return value === 'dark' || value === 'light'
}

export const usePreferencesStore = defineStore('preferences', () => {
  const locale = ref<Locale>(DEFAULT_LOCALE)
  const theme = ref<ThemeMode>(DEFAULT_THEME)

  function applyTheme() {
    if (typeof document !== 'undefined') {
      document.documentElement.dataset.theme = theme.value
    }
  }

  function persist() {
    if (typeof window === 'undefined') return
    window.localStorage.setItem(LOCALE_KEY, locale.value)
    window.localStorage.setItem(THEME_KEY, theme.value)
  }

  function initializePreferences() {
    if (typeof window !== 'undefined') {
      const savedLocale = window.localStorage.getItem(LOCALE_KEY)
      const savedTheme = window.localStorage.getItem(THEME_KEY)

      locale.value = isLocale(savedLocale) ? savedLocale : DEFAULT_LOCALE
      theme.value = isTheme(savedTheme) ? savedTheme : DEFAULT_THEME
    }

    applyTheme()
    persist()
  }

  function setLocale(nextLocale: Locale) {
    locale.value = nextLocale
    persist()
  }

  function toggleLocale() {
    setLocale(locale.value === 'zh-CN' ? 'en-US' : 'zh-CN')
  }

  function setTheme(nextTheme: ThemeMode) {
    theme.value = nextTheme
    applyTheme()
    persist()
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  return {
    locale,
    theme,
    initializePreferences,
    setLocale,
    toggleLocale,
    setTheme,
    toggleTheme
  }
})
```

```ts
// frontend/src/i18n/messages.ts
import type { Locale } from '../stores/preferences'

export const messages: Record<Locale, Record<string, string>> = {
  'zh-CN': {
    'shell.topbar.logout': '退出登录',
    'shell.topbar.theme.dark': '暗色',
    'shell.topbar.theme.light': '明亮',
    'shell.topbar.locale.currentZh': '中文',
    'shell.topbar.locale.currentEn': 'EN'
  },
  'en-US': {
    'shell.topbar.logout': 'Sign Out',
    'shell.topbar.theme.dark': 'Dark',
    'shell.topbar.theme.light': 'Light',
    'shell.topbar.locale.currentZh': 'ZH',
    'shell.topbar.locale.currentEn': 'EN'
  }
}

export type MessageKey = keyof (typeof messages)['zh-CN']
```

```ts
// frontend/src/i18n/index.ts
import { computed } from 'vue'
import { messages, type MessageKey } from './messages'
import { usePreferencesStore, type Locale } from '../stores/preferences'

export function translate(locale: Locale, key: MessageKey): string {
  return messages[locale][key] ?? messages['zh-CN'][key] ?? key
}

export function useI18nText() {
  const preferences = usePreferencesStore()

  function t(key: MessageKey) {
    return translate(preferences.locale, key)
  }

  return {
    t,
    locale: computed(() => preferences.locale),
    theme: computed(() => preferences.theme)
  }
}
```

```ts
// frontend/src/main.ts
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import { RouterView } from 'vue-router'
import router from './router'
import { usePreferencesStore } from './stores/preferences'
import './styles/theme.css'

if (typeof document !== 'undefined' && !document.getElementById('ams-root')) {
  const marker = document.createElement('div')
  marker.id = 'ams-root'
  marker.style.display = 'none'
  marker.textContent = 'AMS2.0'
  document.body.appendChild(marker)
}

const pinia = createPinia()
const preferences = usePreferencesStore(pinia)
preferences.initializePreferences()

createApp(RouterView)
  .use(pinia)
  .use(router)
  .use(ElementPlus)
  .mount('#app')
```

- [ ] **Step 4: Run the focused preferences test and verify it passes**

Run:

```powershell
npm --prefix frontend test -- src/tests/preferences.spec.ts
```

Expected: PASS with both preference store tests green.

- [ ] **Step 5: Checkpoint the preferences/i18n foundation**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/stores/preferences.ts frontend/src/i18n/messages.ts frontend/src/i18n/index.ts frontend/src/tests/preferences.spec.ts frontend/src/main.ts
  git commit -m "feat: add frontend preferences and translation core"
} else {
  Write-Host "No git repo detected; checkpoint preferences/i18n slice after verification."
  Get-Item frontend/src/stores/preferences.ts, frontend/src/i18n/messages.ts, frontend/src/i18n/index.ts, frontend/src/tests/preferences.spec.ts
}
```

Expected: Either a normal git commit, or a verified file checkpoint confirming the preference and i18n foundation exists.

## Task 2: Localize the Shell and Add Toggle Controls

**Files:**
- Modify: `frontend/src/config/navigation.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/shell/AppTopbar.vue`
- Modify: `frontend/src/components/shell/AppSidebar.vue`
- Modify: `frontend/src/components/shell/FilterPanel.vue`
- Modify: `frontend/src/i18n/messages.ts`
- Modify: `frontend/src/tests/app-layout.spec.ts`

- [ ] **Step 1: Write the failing shell interaction test**

```ts
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { usePreferencesStore } from '../stores/preferences'
import AppLayout from '../layouts/AppLayout.vue'

describe('AppLayout shell', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.dataset.theme = 'dark'
  })

  function createShellRouter() {
    const DummyView = { template: '<div>Ok</div>' }
    return createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'dashboard', component: DummyView, meta: { titleKey: 'dashboard.hero.title' } },
        { path: '/users', name: 'users', component: DummyView, meta: { titleKey: 'users.hero.title' } },
        { path: '/departments', name: 'departments', component: DummyView, meta: { titleKey: 'departments.hero.title' } },
        { path: '/device-accounts', name: 'device-accounts', component: DummyView, meta: { titleKey: 'deviceAccounts.hero.title' } },
        { path: '/requests', name: 'requests', component: DummyView, meta: { titleKey: 'requests.list.hero.title' } },
        { path: '/requests/new', name: 'request-form', component: DummyView, meta: { titleKey: 'requests.form.hero.title' } },
        { path: '/queries/device-permissions', name: 'device-permissions', component: DummyView, meta: { titleKey: 'devicePermissions.hero.title' } },
        { path: '/audit/logs', name: 'audit-logs', component: DummyView, meta: { titleKey: 'audit.hero.title' } }
      ]
    })
  }

  it('renders Chinese shell text by default and toggles locale/theme from the topbar', async () => {
    const pinia = createPinia()
    const router = createShellRouter()
    const preferences = usePreferencesStore(pinia)
    preferences.initializePreferences()

    router.push('/')
    await router.isReady()

    const wrapper = mount(AppLayout, {
      global: {
        plugins: [pinia, router]
      }
    })

    expect(wrapper.text()).toContain('工作台')
    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.get('[data-testid="toggle-locale"]').text()).toBe('中文')
    expect(wrapper.get('[data-testid="toggle-theme"]').text()).toBe('暗色')

    await wrapper.get('[data-testid="toggle-locale"]').trigger('click')
    await wrapper.get('[data-testid="toggle-theme"]').trigger('click')

    expect(wrapper.text()).toContain('User Management')
    expect(document.documentElement.dataset.theme).toBe('light')
    expect(wrapper.get('[data-testid="toggle-theme"]').text()).toBe('Light')
  })
})
```

- [ ] **Step 2: Run the shell interaction test and verify it fails**

Run:

```powershell
npm --prefix frontend test -- src/tests/app-layout.spec.ts
```

Expected: FAIL because shell components do not yet expose translation-aware route meta, locale buttons, or theme buttons.

- [ ] **Step 3: Write the minimal shell localization and toggle implementation**

```ts
// frontend/src/config/navigation.ts
import type { MessageKey } from '../i18n/messages'

export interface NavigationItem {
  labelKey: MessageKey
  to: string
  descriptionKey?: MessageKey
}

export const primaryNavigation: NavigationItem[] = [
  { labelKey: 'nav.dashboard', to: '/' },
  { labelKey: 'nav.users', to: '/users' },
  { labelKey: 'nav.departments', to: '/departments' },
  { labelKey: 'nav.deviceAccounts', to: '/device-accounts' },
  { labelKey: 'nav.requests', to: '/requests' },
  { labelKey: 'nav.devicePermissions', to: '/queries/device-permissions' },
  { labelKey: 'nav.audit', to: '/audit/logs' }
]

export const quickActions: NavigationItem[] = [
  {
    labelKey: 'nav.quick.newRequest',
    to: '/requests/new',
    descriptionKey: 'nav.quick.newRequestDescription'
  }
]
```

```ts
// frontend/src/router/index.ts
meta: {
  titleKey: 'dashboard.hero.title',
  subtitleKey: 'dashboard.hero.subtitle'
}
```

```ts
// frontend/src/components/shell/AppTopbar.vue
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18nText } from '../../i18n'
import { usePreferencesStore } from '../../stores/preferences'
import { primaryNavigation, quickActions } from '../../config/navigation'

const preferences = usePreferencesStore()
const { t } = useI18nText()

function bestItemForPath(path: string) {
  const current = normalizePath(path)
  const candidates = [...primaryNavigation, ...quickActions]
  return candidates.find((item) => {
    const target = normalizePath(item.to)
    return target === '/'
      ? current === '/'
      : current === target || current.startsWith(`${target}/`)
  })
}

const contextTitle = computed(() => {
  const titleKey = route.meta?.titleKey
  if (typeof titleKey === 'string' && titleKey.length > 0) return t(titleKey as never)
  const best = bestItemForPath(route.path)
  return best ? t(best.labelKey) : t('nav.dashboard')
})

const localeButtonLabel = computed(() =>
  preferences.locale === 'zh-CN'
    ? t('shell.topbar.locale.currentZh')
    : t('shell.topbar.locale.currentEn')
)

const themeButtonLabel = computed(() =>
  preferences.theme === 'dark'
    ? t('shell.topbar.theme.dark')
    : t('shell.topbar.theme.light')
)
```

```vue
<!-- frontend/src/components/shell/AppTopbar.vue -->
<button data-testid="toggle-locale" class="topbar__toggle" type="button" @click="preferences.toggleLocale()">
  {{ localeButtonLabel }}
</button>
<button data-testid="toggle-theme" class="topbar__toggle" type="button" @click="preferences.toggleTheme()">
  {{ themeButtonLabel }}
</button>
<RouterLink class="topbar__primary" to="/requests/new">{{ t('shell.topbar.newRequest') }}</RouterLink>
<button class="topbar__logout" type="button" @click="handleLogout">{{ t('shell.topbar.logout') }}</button>
```

```vue
<!-- frontend/src/components/shell/AppSidebar.vue -->
<div class="sidebar__sub">{{ t('shell.sidebar.productSub') }}</div>
<span class="sidebar__linkText">{{ t(item.labelKey) }}</span>
<div class="sidebar__hint">{{ t('shell.sidebar.quickActions') }}</div>
<span class="sidebar__actionText">{{ t(action.labelKey) }}</span>
<span v-if="action.descriptionKey" class="sidebar__actionDesc">{{ t(action.descriptionKey) }}</span>
```

```vue
<!-- frontend/src/components/shell/FilterPanel.vue -->
<button v-if="collapsible" class="panel__toggle" type="button" @click="isOpen = !isOpen">
  {{ isOpen ? t('common.collapse') : t('common.expand') }}
</button>
```

```ts
// frontend/src/i18n/messages.ts
'zh-CN': {
  'common.expand': '展开',
  'common.collapse': '收起',
  'nav.dashboard': '工作台',
  'nav.users': '用户管理',
  'nav.departments': '部门管理',
  'nav.deviceAccounts': '设备账号管理',
  'nav.requests': '申请管理',
  'nav.devicePermissions': '设备权限查询',
  'nav.audit': '审计日志',
  'nav.quick.newRequest': '新建申请',
  'nav.quick.newRequestDescription': '发起一条新的资产权限申请。',
  'shell.sidebar.productSub': '运营驾驶舱',
  'shell.sidebar.quickActions': '快捷入口',
  'shell.topbar.menu': '菜单',
  'shell.topbar.searchAria': '全局搜索',
  'shell.topbar.searchPlaceholder': '全局搜索（即将上线）',
  'shell.topbar.searchSoon': '即将上线',
  'shell.topbar.newRequest': '新建申请'
},
'en-US': {
  'common.expand': 'Expand',
  'common.collapse': 'Collapse',
  'nav.dashboard': 'Workbench',
  'nav.users': 'User Management',
  'nav.departments': 'Department Management',
  'nav.deviceAccounts': 'Device Account Management',
  'nav.requests': 'Request Management',
  'nav.devicePermissions': 'Device Permission Query',
  'nav.audit': 'Audit Logs',
  'nav.quick.newRequest': 'New Request',
  'nav.quick.newRequestDescription': 'Start a new asset permission request.',
  'shell.sidebar.productSub': 'Operations Cockpit',
  'shell.sidebar.quickActions': 'Quick Actions',
  'shell.topbar.menu': 'Menu',
  'shell.topbar.searchAria': 'Global Search',
  'shell.topbar.searchPlaceholder': 'Global Search (Coming Soon)',
  'shell.topbar.searchSoon': 'Coming Soon',
  'shell.topbar.newRequest': 'New Request'
}
```

- [ ] **Step 4: Run the preferences and shell tests and verify they pass**

Run:

```powershell
npm --prefix frontend test -- src/tests/preferences.spec.ts src/tests/app-layout.spec.ts
```

Expected: PASS with preference and shell tests green and no missing-topbar-toggle failures.

- [ ] **Step 5: Checkpoint the shell localization slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/config/navigation.ts frontend/src/router/index.ts frontend/src/components/shell/AppTopbar.vue frontend/src/components/shell/AppSidebar.vue frontend/src/components/shell/FilterPanel.vue frontend/src/i18n/messages.ts frontend/src/tests/app-layout.spec.ts
  git commit -m "feat: add localized shell navigation and toggle controls"
} else {
  Write-Host "No git repo detected; checkpoint shell localization slice after verification."
  Get-Item frontend/src/components/shell/AppTopbar.vue, frontend/src/components/shell/AppSidebar.vue, frontend/src/router/index.ts
}
```

Expected: Either a normal git commit, or a verified checkpoint showing the shell now owns locale/theme toggles and translation-aware route/navigation metadata.

## Task 3: Add Dark/Light Tokens and Localize Login and Dashboard

**Files:**
- Modify: `frontend/src/styles/theme.css`
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/views/DashboardView.vue`
- Modify: `frontend/src/i18n/messages.ts`
- Modify: `frontend/src/tests/login-view.spec.ts`
- Modify: `frontend/src/tests/dashboard-view.spec.ts`

- [ ] **Step 1: Write the failing login/dashboard localization tests**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import { usePreferencesStore } from '../stores/preferences'
import { fetchDashboardSummary } from '../api/dashboard'

vi.mock('../api/dashboard', () => ({
  fetchDashboardSummary: vi.fn()
}))

function mountWithLocale(component: unknown, locale: 'zh-CN' | 'en-US') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)
  return mount(component as never, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('localized entry pages', () => {
  beforeEach(() => {
    vi.mocked(fetchDashboardSummary).mockResolvedValue({
      metrics: { userTotal: 1, departmentTotal: 2, deviceAccountTotal: 3, pendingRequestTotal: 4 },
      alerts: [],
      recentRequests: [],
      quickActions: []
    })
  })

  it('renders the login page in Chinese by default and English after locale switch', async () => {
    const zhWrapper = mountWithLocale(LoginView, 'zh-CN')
    expect(zhWrapper.text()).toContain('AMS2.0 审批与权限驾驶舱')
    expect(zhWrapper.text()).toContain('进入系统')

    const enWrapper = mountWithLocale(LoginView, 'en-US')
    expect(enWrapper.text()).toContain('AMS2.0 Operations Cockpit')
    expect(enWrapper.text()).toContain('Enter Workspace')
  })

  it('renders dashboard hero and metrics in the active locale', async () => {
    const wrapper = mountWithLocale(DashboardView, 'en-US')
    await flushPromises()

    expect(wrapper.text()).toContain('Operations Workbench')
    expect(wrapper.text()).toContain('Total Users')
    expect(wrapper.text()).toContain('Quick Actions')
  })
})
```

- [ ] **Step 2: Run the login/dashboard tests and verify they fail**

Run:

```powershell
npm --prefix frontend test -- src/tests/login-view.spec.ts src/tests/dashboard-view.spec.ts
```

Expected: FAIL because the current pages still render hard-coded copy and the login/dashboard dictionaries do not exist yet.

- [ ] **Step 3: Write the minimal theme token and entry-page localization implementation**

```css
/* frontend/src/styles/theme.css */
:root {
  --cockpit-radius-lg: 16px;
  --cockpit-radius-md: 12px;
  --cockpit-radius-sm: 10px;
  --cockpit-sidebar-w: 272px;
  --cockpit-topbar-h: 64px;
}

:root[data-theme='dark'] {
  --cockpit-bg: #0b1020;
  --cockpit-surface: rgba(255, 255, 255, 0.08);
  --cockpit-surface-2: rgba(255, 255, 255, 0.12);
  --cockpit-panel-bg: rgba(255, 255, 255, 0.05);
  --cockpit-input-bg: rgba(0, 0, 0, 0.18);
  --cockpit-border: rgba(255, 255, 255, 0.14);
  --cockpit-text: rgba(255, 255, 255, 0.92);
  --cockpit-muted: rgba(255, 255, 255, 0.68);
  --cockpit-accent: #5eead4;
  --cockpit-accent-2: #60a5fa;
  --cockpit-danger: #fb7185;
  --cockpit-overlay: rgba(2, 6, 23, 0.56);
  --cockpit-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
  --cockpit-body-top: #070a14;
  --cockpit-body-bottom: #070a14;
}

:root[data-theme='light'] {
  --cockpit-bg: #edf4fb;
  --cockpit-surface: rgba(255, 255, 255, 0.86);
  --cockpit-surface-2: rgba(255, 255, 255, 0.96);
  --cockpit-panel-bg: rgba(255, 255, 255, 0.78);
  --cockpit-input-bg: rgba(255, 255, 255, 0.92);
  --cockpit-border: rgba(148, 163, 184, 0.26);
  --cockpit-text: #0f172a;
  --cockpit-muted: #475569;
  --cockpit-accent: #0f766e;
  --cockpit-accent-2: #2563eb;
  --cockpit-danger: #e11d48;
  --cockpit-overlay: rgba(15, 23, 42, 0.22);
  --cockpit-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
  --cockpit-body-top: #f8fbff;
  --cockpit-body-bottom: #e7eef8;
}

body {
  margin: 0;
  color: var(--cockpit-text);
  background:
    radial-gradient(1100px 700px at 15% 8%, color-mix(in srgb, var(--cockpit-accent) 18%, transparent), transparent 55%),
    radial-gradient(900px 600px at 85% 22%, color-mix(in srgb, var(--cockpit-accent-2) 16%, transparent), transparent 52%),
    radial-gradient(900px 650px at 35% 92%, color-mix(in srgb, var(--cockpit-danger) 12%, transparent), transparent 52%),
    linear-gradient(180deg, var(--cockpit-body-top) 0%, var(--cockpit-bg) 65%, var(--cockpit-body-bottom) 100%);
}
```

```ts
// frontend/src/i18n/messages.ts
'zh-CN': {
  'login.eyebrow': 'AMS2.0 审批与权限驾驶舱',
  'login.headline': '审批驱动的账号与权限管理中台',
  'login.description': '统一管理用户、部门、设备账号与申请单的全流程，支持标准化审批、可追溯审计与权限查询。',
  'login.loginName': '登录名',
  'login.password': '密码',
  'login.submit': '进入系统',
  'login.error.invalidCredentials': '登录名或密码不正确，请重新输入。',
  'login.error.retryLater': '登录失败，请稍后重试。',
  'dashboard.hero.title': '运营工作台',
  'dashboard.hero.subtitle': '统一入口，集中处理当前系统运行、审批动态与权限巡检。',
  'dashboard.metrics.userTotal': '用户总量',
  'dashboard.metrics.departmentTotal': '部门总量',
  'dashboard.metrics.deviceAccountTotal': '设备账号',
  'dashboard.metrics.pendingRequestTotal': '待处理申请',
  'dashboard.section.alerts': '预警',
  'dashboard.section.quickActions': '快捷操作',
  'dashboard.empty.alerts': '暂无预警',
  'dashboard.empty.quickActions': '暂无快捷操作',
  'dashboard.error.title': '加载失败',
  'dashboard.error.retry': '重试'
},
'en-US': {
  'login.eyebrow': 'AMS2.0 Operations Cockpit',
  'login.headline': 'Approval-driven account and permission operations hub',
  'login.description': 'Manage users, departments, device accounts, and approval requests from one traceable control surface.',
  'login.loginName': 'Login Name',
  'login.password': 'Password',
  'login.submit': 'Enter Workspace',
  'login.error.invalidCredentials': 'Login name or password is incorrect. Please try again.',
  'login.error.retryLater': 'Login failed. Please try again later.',
  'dashboard.hero.title': 'Operations Workbench',
  'dashboard.hero.subtitle': 'One cockpit for current workload, approval signals, and permission checks.',
  'dashboard.metrics.userTotal': 'Total Users',
  'dashboard.metrics.departmentTotal': 'Total Departments',
  'dashboard.metrics.deviceAccountTotal': 'Device Accounts',
  'dashboard.metrics.pendingRequestTotal': 'Pending Requests',
  'dashboard.section.alerts': 'Alerts',
  'dashboard.section.quickActions': 'Quick Actions',
  'dashboard.empty.alerts': 'No alerts right now.',
  'dashboard.empty.quickActions': 'No quick actions are available.',
  'dashboard.error.title': 'Load failed',
  'dashboard.error.retry': 'Retry'
}
```

```vue
<!-- frontend/src/views/LoginView.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useI18nText } from '../i18n'

const { t } = useI18nText()
// keep existing auth logic
</script>

<template>
  <section class="entry">
    <div class="entry__frame">
      <div class="entry__brand">
        <div class="entry__eyebrow">{{ t('login.eyebrow') }}</div>
        <h1 class="entry__headline">{{ t('login.headline') }}</h1>
        <p class="entry__desc">{{ t('login.description') }}</p>
      </div>
      <form class="entry__form" @submit.prevent="handleSubmit">
        <label class="field">
          <span class="field__label">{{ t('login.loginName') }}</span>
          <input v-model="loginName" name="loginName" type="text" :disabled="pending" />
        </label>
        <label class="field">
          <span class="field__label">{{ t('login.password') }}</span>
          <input v-model="password" name="password" type="password" :disabled="pending" />
        </label>
        <button class="submit" type="submit" :disabled="pending">{{ t('login.submit') }}</button>
      </form>
    </div>
  </section>
</template>
```

```ts
// frontend/src/views/DashboardView.vue
const { t } = useI18nText()

const metrics = computed(() => [
  { label: t('dashboard.metrics.userTotal'), value: summary.value?.metrics.userTotal ?? 0 },
  { label: t('dashboard.metrics.departmentTotal'), value: summary.value?.metrics.departmentTotal ?? 0 },
  { label: t('dashboard.metrics.deviceAccountTotal'), value: summary.value?.metrics.deviceAccountTotal ?? 0 },
  { label: t('dashboard.metrics.pendingRequestTotal'), value: summary.value?.metrics.pendingRequestTotal ?? 0 }
])
```

```vue
<!-- frontend/src/views/DashboardView.vue -->
<PageHero
  :kicker="t('login.eyebrow')"
  :title="t('dashboard.hero.title')"
  :subtitle="t('dashboard.hero.subtitle')"
/>
<button class="retry" type="button" @click="loadSummary">{{ t('dashboard.error.retry') }}</button>
```

- [ ] **Step 4: Run the login/dashboard tests and verify they pass**

Run:

```powershell
npm --prefix frontend test -- src/tests/login-view.spec.ts src/tests/dashboard-view.spec.ts
```

Expected: PASS with translated login and dashboard assertions green in both locales.

- [ ] **Step 5: Checkpoint the dual-theme entry-page slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/styles/theme.css frontend/src/views/LoginView.vue frontend/src/views/DashboardView.vue frontend/src/i18n/messages.ts frontend/src/tests/login-view.spec.ts frontend/src/tests/dashboard-view.spec.ts
  git commit -m "feat: localize login dashboard and add light theme tokens"
} else {
  Write-Host "No git repo detected; checkpoint theme/login/dashboard slice after verification."
  Get-Item frontend/src/styles/theme.css, frontend/src/views/LoginView.vue, frontend/src/views/DashboardView.vue
}
```

Expected: Either a normal git commit, or a verified checkpoint showing the first localized pages running on the dual-theme token system.

## Task 4: Localize User, Department, and Device Account CRUD Pages

**Files:**
- Modify: `frontend/src/views/users/UserListView.vue`
- Modify: `frontend/src/components/users/UserDrawerForm.vue`
- Modify: `frontend/src/views/departments/DepartmentListView.vue`
- Modify: `frontend/src/components/departments/DepartmentDrawerForm.vue`
- Modify: `frontend/src/views/device-accounts/DeviceAccountListView.vue`
- Modify: `frontend/src/components/device-accounts/DeviceAccountDrawerForm.vue`
- Modify: `frontend/src/i18n/messages.ts`
- Modify: `frontend/src/tests/user-list-view.spec.ts`
- Modify: `frontend/src/tests/department-list-view.spec.ts`
- Modify: `frontend/src/tests/device-account-list-view.spec.ts`

- [ ] **Step 1: Write the failing CRUD-page localization tests**

```ts
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { usePreferencesStore } from '../stores/preferences'
import UserListView from '../views/users/UserListView.vue'
import DepartmentListView from '../views/departments/DepartmentListView.vue'
import DeviceAccountListView from '../views/device-accounts/DeviceAccountListView.vue'

function mountWithLocale(component: unknown, locale: 'zh-CN' | 'en-US') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)
  return mount(component as never, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('localized CRUD workbenches', () => {
  it('renders user management in Chinese by default', async () => {
    const wrapper = mountWithLocale(UserListView, 'zh-CN')
    await flushPromises()

    expect(wrapper.text()).toContain('用户管理工作台')
    expect(wrapper.text()).toContain('新建用户')
    expect(wrapper.text()).toContain('刷新')
  })

  it('renders department management in English when locale switches', async () => {
    const wrapper = mountWithLocale(DepartmentListView, 'en-US')
    await flushPromises()

    expect(wrapper.text()).toContain('Department Management Workbench')
    expect(wrapper.text()).toContain('Create Department')
  })

  it('renders device-account actions in Chinese and keeps request CTA localized', async () => {
    const wrapper = mountWithLocale(DeviceAccountListView, 'zh-CN')
    await flushPromises()

    expect(wrapper.text()).toContain('设备账号管理工作台')
    expect(wrapper.text()).toContain('发起加权申请')
  })
})
```

- [ ] **Step 2: Run the focused CRUD-page tests and verify they fail**

Run:

```powershell
npm --prefix frontend test -- src/tests/user-list-view.spec.ts src/tests/department-list-view.spec.ts src/tests/device-account-list-view.spec.ts
```

Expected: FAIL because the CRUD pages and drawer forms still render hard-coded English copy and status strings.

- [ ] **Step 3: Write the minimal CRUD-page localization implementation**

```ts
// frontend/src/i18n/messages.ts
'zh-CN': {
  'users.hero.title': '用户管理工作台',
  'users.hero.subtitle': '集中处理账号、所属部门、任职状态与启停用。',
  'users.actions.create': '新建用户',
  'users.actions.refresh': '刷新',
  'users.actions.retry': '重试',
  'users.actions.edit': '编辑',
  'users.actions.enable': '启用',
  'users.actions.disable': '停用',
  'users.actions.delete': '删除',
  'users.table.userCode': '用户编号',
  'users.table.userName': '姓名',
  'users.table.loginName': '登录名',
  'users.table.department': '部门',
  'users.table.employment': '任职状态',
  'users.table.account': '账号状态',
  'users.table.actions': '操作',
  'users.filters.title': '筛选条件',
  'users.filters.search': '检索',
  'users.filters.searchPlaceholder': '按编号、姓名、登录名或部门搜索',
  'users.filters.accountStatus': '账号状态',
  'users.empty': '当前筛选条件下没有匹配用户。',
  'users.loading.initial': '正在加载用户与部门数据...',
  'users.loading.retry': '正在重新加载用户与部门数据...',
  'users.error.loadTitle': '加载用户失败',
  'users.drawer.titleCreate': '新建用户',
  'users.drawer.titleEdit': '编辑用户',
  'users.drawer.submitCreate': '新建用户',
  'users.drawer.submitSave': '保存修改',
  'users.drawer.cancel': '取消',
  'users.status.enabled': '启用',
  'users.status.disabled': '停用',
  'users.employment.active': '在职',
  'users.employment.leave': '请假',
  'users.employment.resigned': '离职',
  'departments.hero.title': '部门管理工作台',
  'departments.actions.create': '新建部门',
  'deviceAccounts.hero.title': '设备账号管理工作台',
  'deviceAccounts.actions.requestRoleAdd': '发起加权申请'
},
'en-US': {
  'users.hero.title': 'User Management Workbench',
  'users.hero.subtitle': 'Create, revise, suspend, and retire user accounts from one cockpit surface.',
  'users.actions.create': 'Create User',
  'users.actions.refresh': 'Refresh',
  'users.actions.retry': 'Retry',
  'users.actions.edit': 'Edit',
  'users.actions.enable': 'Enable',
  'users.actions.disable': 'Disable',
  'users.actions.delete': 'Delete',
  'users.table.userCode': 'User Code',
  'users.table.userName': 'User Name',
  'users.table.loginName': 'Login Name',
  'users.table.department': 'Department',
  'users.table.employment': 'Employment',
  'users.table.account': 'Account',
  'users.table.actions': 'Actions',
  'users.filters.title': 'Filters',
  'users.filters.search': 'Search',
  'users.filters.searchPlaceholder': 'Search by code, name, login, or department',
  'users.filters.accountStatus': 'Account Status',
  'users.empty': 'No users match the current filters.',
  'users.loading.initial': 'Loading users and departments...',
  'users.loading.retry': 'Retrying users and departments...',
  'users.error.loadTitle': 'Failed to load users',
  'users.drawer.titleCreate': 'Create User',
  'users.drawer.titleEdit': 'Edit User',
  'users.drawer.submitCreate': 'Create User',
  'users.drawer.submitSave': 'Save Changes',
  'users.drawer.cancel': 'Cancel',
  'users.status.enabled': 'Enabled',
  'users.status.disabled': 'Disabled',
  'users.employment.active': 'Active',
  'users.employment.leave': 'Leave',
  'users.employment.resigned': 'Resigned',
  'departments.hero.title': 'Department Management Workbench',
  'departments.actions.create': 'Create Department',
  'deviceAccounts.hero.title': 'Device Account Management Workbench',
  'deviceAccounts.actions.requestRoleAdd': 'Request Role Add'
}
```

```ts
// frontend/src/views/users/UserListView.vue
const { t } = useI18nText()

const metrics = computed(() => [
  { label: t('users.metrics.totalUsers'), value: users.value.length },
  { label: t('users.metrics.enabledAccounts'), value: users.value.filter((user) => user.accountStatus === 'ENABLED').length },
  { label: t('users.metrics.activeEmployees'), value: users.value.filter((user) => user.employmentStatus === 'ACTIVE').length },
  { label: t('users.metrics.departmentsCovered'), value: new Set(users.value.map((user) => user.departmentId)).size }
])

function formatAccountStatus(value: string) {
  return value === 'ENABLED' ? t('users.status.enabled') : value === 'DISABLED' ? t('users.status.disabled') : value
}

function formatEmploymentStatus(value: string) {
  switch (value) {
    case 'ACTIVE':
      return t('users.employment.active')
    case 'LEAVE':
      return t('users.employment.leave')
    case 'RESIGNED':
      return t('users.employment.resigned')
    default:
      return value
  }
}
```

```vue
<!-- frontend/src/views/users/UserListView.vue -->
<PageHero
  :kicker="t('login.eyebrow')"
  :title="t('users.hero.title')"
  :subtitle="t('users.hero.subtitle')"
>
  <template #actions>
    <button class="hero-action" data-testid="create-user" type="button" @click="openCreateDrawer">
      {{ t('users.actions.create') }}
    </button>
  </template>
</PageHero>
<FilterPanel :title="t('users.filters.title')">
  <span>{{ t('users.filters.search') }}</span>
</FilterPanel>
```

```vue
<!-- frontend/src/components/users/UserDrawerForm.vue -->
<p class="drawer-form__eyebrow">{{ t('users.drawer.eyebrow') }}</p>
<h2 class="drawer-form__title">{{ mode === 'create' ? t('users.drawer.titleCreate') : t('users.drawer.titleEdit') }}</h2>
<button class="button button--ghost" type="button" @click="$emit('close')">{{ t('users.drawer.cancel') }}</button>
<button class="button button--primary" type="submit" :disabled="submitting || !canSubmit">
  {{ submitting ? t('common.saving') : mode === 'create' ? t('users.drawer.submitCreate') : t('users.drawer.submitSave') }}
</button>
```

```vue
<!-- frontend/src/views/device-accounts/DeviceAccountListView.vue -->
<button
  class="row-action"
  :data-testid="`action-request-role-add-${deviceAccount.id}`"
  type="button"
  :disabled="isBusy(deviceAccount.id) || !deviceAccount.userId"
  @click="goToRequest(deviceAccount)"
>
  {{ t('deviceAccounts.actions.requestRoleAdd') }}
</button>
```

Complete the remaining CRUD-page localization work with explicit file ownership:

1. `frontend/src/views/departments/DepartmentListView.vue`
   - Replace the PageHero kicker/title/subtitle literals with `t('login.eyebrow')`, `t('departments.hero.title')`, and `t('departments.hero.subtitle')`.
   - Translate the create/retry/refresh/edit/delete button labels, filter title, filter labels, placeholders, table title/meta, table headers, empty state, loading cards, inline error cards, confirm prompt text, and fallback description copy.
   - Localize the metrics labels, `formatStatus()` output, `formatManagerLabel()` fallback labels, and all mutation/load follow-up messages returned from `getErrorMessage()` / `getRefreshFollowUpMessage()`.
2. `frontend/src/components/departments/DepartmentDrawerForm.vue`
   - Replace the drawer eyebrow/title/subtitle literals with dictionary keys for create/edit modes.
   - Translate field labels, select placeholder options, close/cancel/save CTA labels, and the in-flight submit label.
   - Keep form behavior unchanged while ensuring every user-visible string resolves through `useI18nText()`.
3. `frontend/src/views/device-accounts/DeviceAccountListView.vue`
   - Translate hero copy, metrics labels, filter title/labels/placeholders, table title/meta, table headers, empty states, loading/error messages, confirm prompts, and row action labels.
   - Localize account status, source type, assignment state, and any role-request helper labels through formatter functions.
4. `frontend/src/components/device-accounts/DeviceAccountDrawerForm.vue`
   - Replace drawer eyebrow/title/subtitle literals with keyed translations for create/edit modes.
   - Translate all field labels, select options (`Leave unbound`, account status, source type), error/submit buttons, and saving state labels.
   - Preserve the current submit payload shape while making every visible string locale-aware.

- [ ] **Step 4: Run the focused CRUD-page tests and verify they pass**

Run:

```powershell
npm --prefix frontend test -- src/tests/user-list-view.spec.ts src/tests/department-list-view.spec.ts src/tests/device-account-list-view.spec.ts
```

Expected: PASS with user, department, and device-account page tests green in the translated default locale.

- [ ] **Step 5: Checkpoint the localized CRUD-page slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/views/users/UserListView.vue frontend/src/components/users/UserDrawerForm.vue frontend/src/views/departments/DepartmentListView.vue frontend/src/components/departments/DepartmentDrawerForm.vue frontend/src/views/device-accounts/DeviceAccountListView.vue frontend/src/components/device-accounts/DeviceAccountDrawerForm.vue frontend/src/i18n/messages.ts frontend/src/tests/user-list-view.spec.ts frontend/src/tests/department-list-view.spec.ts frontend/src/tests/device-account-list-view.spec.ts
  git commit -m "feat: localize master-data pages and drawers"
} else {
  Write-Host "No git repo detected; checkpoint CRUD localization slice after verification."
  Get-Item frontend/src/views/users/UserListView.vue, frontend/src/views/departments/DepartmentListView.vue, frontend/src/views/device-accounts/DeviceAccountListView.vue
}
```

Expected: Either a normal git commit, or a verified checkpoint showing the three main CRUD workbenches localized and theme-ready.

## Task 5: Localize Request, Query, and Audit Pages and Update Smoke Coverage

**Files:**
- Modify: `frontend/src/views/requests/RequestListView.vue`
- Modify: `frontend/src/views/requests/RequestFormView.vue`
- Modify: `frontend/src/views/queries/DevicePermissionView.vue`
- Modify: `frontend/src/views/audit/AuditLogView.vue`
- Modify: `frontend/src/i18n/messages.ts`
- Modify: `frontend/src/tests/request-list-view.spec.ts`
- Modify: `frontend/src/tests/request-form.spec.ts`
- Modify: `frontend/src/tests/device-permission-view.spec.ts`
- Modify: `frontend/src/tests/audit-log-view.spec.ts`
- Modify: `frontend/tests/e2e/login.spec.ts`
- Modify: `frontend/tests/e2e/cockpit-navigation.spec.ts`
- Modify: `docs/test/2026-04-24-ams2-ui-crud-smoke.md`

- [ ] **Step 1: Write the failing request/query/audit and smoke assertions**

```ts
// frontend/src/tests/request-list-view.spec.ts
it('renders the request workbench in Chinese by default', async () => {
  const wrapper = mountWithLocale(RequestListView, 'zh-CN')
  await flushPromises()

  expect(wrapper.text()).toContain('申请管理工作台')
  expect(wrapper.text()).toContain('筛选条件')
})

// frontend/src/tests/request-form.spec.ts
it('renders the request form launchpad in English when locale switches', async () => {
  const wrapper = mountWithLocale(RequestFormView, 'en-US')
  await flushPromises()

  expect(wrapper.text()).toContain('New Request')
  expect(wrapper.text()).toContain('Approval path')
})

// frontend/src/tests/device-permission-view.spec.ts
it('renders the device permission query page in Chinese', async () => {
  const wrapper = mountWithLocale(DevicePermissionView, 'zh-CN')
  await flushPromises()

  expect(wrapper.text()).toContain('设备权限查询工作台')
})

// frontend/src/tests/audit-log-view.spec.ts
it('renders the audit log page in English when locale switches', async () => {
  const wrapper = mountWithLocale(AuditLogView, 'en-US')
  await flushPromises()

  expect(wrapper.text()).toContain('Audit Log Workbench')
  expect(wrapper.text()).toContain('No audit records match the current filters.')
})
```

```ts
// frontend/tests/e2e/cockpit-navigation.spec.ts
await expect(sidebarNav.locator('a[href="/users"]')).toContainText('用户管理')
await expect(page.getByTestId('toggle-locale')).toBeVisible()
await expect(page.getByTestId('toggle-theme')).toBeVisible()
```

- [ ] **Step 2: Run the focused page tests and e2e smoke and verify they fail**

Run:

```powershell
npm --prefix frontend test -- src/tests/request-list-view.spec.ts src/tests/request-form.spec.ts src/tests/device-permission-view.spec.ts src/tests/audit-log-view.spec.ts
npm --prefix frontend run test:e2e -- login.spec.ts cockpit-navigation.spec.ts
```

Expected: FAIL because these pages and e2e checks still assume old hard-coded text.

- [ ] **Step 3: Write the minimal request/query/audit localization and smoke updates**

```ts
// frontend/src/i18n/messages.ts
'zh-CN': {
  'requests.list.hero.title': '申请管理工作台',
  'requests.list.hero.subtitle': '集中跟踪审批状态、发起时间和目标账号。',
  'requests.form.hero.title': '新建申请',
  'requests.form.hero.subtitle': '发起新的审批申请并确认目标账号、设备与理由。',
  'requests.form.section.approvalPath': '审批路径',
  'devicePermissions.hero.title': '设备权限查询工作台',
  'audit.hero.title': '审计日志工作台'
},
'en-US': {
  'requests.list.hero.title': 'Request Management Workbench',
  'requests.list.hero.subtitle': 'Track approval status, submission time, and target accounts in one place.',
  'requests.form.hero.title': 'New Request',
  'requests.form.hero.subtitle': 'Start a new approval request and confirm target user, device, and reason.',
  'requests.form.section.approvalPath': 'Approval Path',
  'devicePermissions.hero.title': 'Device Permission Query Workbench',
  'audit.hero.title': 'Audit Log Workbench'
}
```

```vue
<!-- frontend/src/views/requests/RequestListView.vue -->
<PageHero
  :kicker="t('login.eyebrow')"
  :title="t('requests.list.hero.title')"
  :subtitle="t('requests.list.hero.subtitle')"
/>
```

```vue
<!-- frontend/src/views/requests/RequestFormView.vue -->
<PageHero
  :kicker="t('nav.requests')"
  :title="t('requests.form.hero.title')"
  :subtitle="t('requests.form.hero.subtitle')"
/>
<section class="approval-panel">
  <h2>{{ t('requests.form.section.approvalPath') }}</h2>
</section>
```

```ts
// frontend/tests/e2e/login.spec.ts
import { expect, test } from '@playwright/test'

test('login page loads localized Chinese hero and form', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByText('AMS2.0 审批与权限驾驶舱')).toBeVisible()
  await expect(page.locator('input[name="loginName"]')).toBeVisible()
  await expect(page.locator('button[type="submit"]')).toHaveText('进入系统')
})
```

```ts
// frontend/tests/e2e/cockpit-navigation.spec.ts
await expect(page.getByTestId('toggle-locale')).toBeVisible()
await expect(page.getByTestId('toggle-theme')).toBeVisible()
await expect(sidebarNav.locator('a[href="/users"]')).toContainText('用户管理')
await expect(sidebarNav.locator('a[href="/device-accounts"]')).toContainText('设备账号管理')
await expect(sidebarNav.locator('a[href="/requests"]')).toContainText('申请管理')
```

```md
<!-- docs/test/2026-04-24-ams2-ui-crud-smoke.md -->
1. 登录后点击语言切换按钮，确认工作台、导航、按钮、表格列头会从中文切换为英文，再切回中文。
2. 点击主题切换按钮，确认工作台在暗色与明亮主题之间即时切换，列表、抽屉、输入框与顶部栏都保持可读性。
```

Complete the remaining request, query, and audit localization work with explicit file ownership:

1. `frontend/src/views/requests/RequestListView.vue`
   - Translate hero/kicker copy, metric labels, filter section labels, placeholders, table headings/meta, empty states, loading states, retry buttons, row action labels, confirm prompts, and request status formatter output.
   - Move all local fallback/error text into dictionary keys so request load failures and post-mutation refresh failures are localized.
2. `frontend/src/views/requests/RequestFormView.vue`
   - Translate hero copy, all section headings, field labels, helper text, select options, submit/cancel buttons, success/error feedback, and all local validation messages.
   - Keep the existing request creation behavior unchanged while routing every visible string through `useI18nText()`.
3. `frontend/src/views/queries/DevicePermissionView.vue`
   - Translate the hero copy, metrics labels, filter title/field labels/placeholders, query button labels, loading/error cards, table title/meta, empty state, and table headers.
   - Localize the query fallback error text and any zero-state device/account labels produced in the computed rows.
4. `frontend/src/views/audit/AuditLogView.vue`
   - Translate the hero copy, metrics labels, filter title/field labels/placeholders, loading/error cards, table title/meta, empty state, and table headers.
   - Localize the audit fallback error message and `splitAction()` target fallback label (`SYSTEM`) through dictionary keys.

- [ ] **Step 4: Run the focused page tests and smoke and verify they pass**

Run:

```powershell
npm --prefix frontend test -- src/tests/request-list-view.spec.ts src/tests/request-form.spec.ts src/tests/device-permission-view.spec.ts src/tests/audit-log-view.spec.ts
npm --prefix frontend run test:e2e -- login.spec.ts cockpit-navigation.spec.ts
```

Expected: PASS with all request/query/audit tests green and both Playwright smoke checks green against the new default Chinese UI.

- [ ] **Step 5: Checkpoint the localized request/query/audit slice**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src/views/requests/RequestListView.vue frontend/src/views/requests/RequestFormView.vue frontend/src/views/queries/DevicePermissionView.vue frontend/src/views/audit/AuditLogView.vue frontend/src/i18n/messages.ts frontend/src/tests/request-list-view.spec.ts frontend/src/tests/request-form.spec.ts frontend/src/tests/device-permission-view.spec.ts frontend/src/tests/audit-log-view.spec.ts frontend/tests/e2e/login.spec.ts frontend/tests/e2e/cockpit-navigation.spec.ts docs/test/2026-04-24-ams2-ui-crud-smoke.md
  git commit -m "feat: localize request query audit pages and smoke coverage"
} else {
  Write-Host "No git repo detected; checkpoint request/query/audit localization slice after verification."
  Get-Item frontend/src/views/requests/RequestListView.vue, frontend/src/views/queries/DevicePermissionView.vue, docs/test/2026-04-24-ams2-ui-crud-smoke.md
}
```

Expected: Either a normal git commit, or a verified checkpoint showing all remaining routed pages localized and smoke coverage updated.

## Task 6: Run Full Frontend Verification for the Bilingual Theme Release Candidate

**Files:**
- Verify: `frontend/src/tests/preferences.spec.ts`
- Verify: `frontend/src/tests/app-layout.spec.ts`
- Verify: `frontend/src/tests/login-view.spec.ts`
- Verify: `frontend/src/tests/dashboard-view.spec.ts`
- Verify: `frontend/src/tests/user-list-view.spec.ts`
- Verify: `frontend/src/tests/department-list-view.spec.ts`
- Verify: `frontend/src/tests/device-account-list-view.spec.ts`
- Verify: `frontend/src/tests/request-list-view.spec.ts`
- Verify: `frontend/src/tests/request-form.spec.ts`
- Verify: `frontend/src/tests/device-permission-view.spec.ts`
- Verify: `frontend/src/tests/audit-log-view.spec.ts`
- Verify: `frontend/tests/e2e/login.spec.ts`
- Verify: `frontend/tests/e2e/cockpit-navigation.spec.ts`

- [ ] **Step 1: Run the full frontend unit test suite**

Run:

```powershell
npm --prefix frontend test
```

Expected: PASS with all view, shell, preference, and runtime-safety tests green under the new default Chinese locale.

- [ ] **Step 2: Run the production build**

Run:

```powershell
npm --prefix frontend run build
```

Expected: PASS with a production bundle emitted under `frontend/dist`. The existing large-chunk warning is acceptable if no new build error appears.

- [ ] **Step 3: Run the updated Playwright smoke suite**

Run:

```powershell
npm --prefix frontend run test:e2e -- login.spec.ts cockpit-navigation.spec.ts
```

Expected: PASS with both smoke specs green against the localized shell and login entry page.

- [ ] **Step 4: Verify the preference smoke manually in the browser**

Run:

```powershell
Write-Host "Manual smoke: log in, toggle locale to English and back to Chinese, toggle theme to light and back to dark, refresh the page, confirm both choices persist."
```

Expected: Manual confirmation that locale and theme both persist across refresh and page navigation.

- [ ] **Step 5: Checkpoint the bilingual theme release candidate**

Run:

```powershell
if (Test-Path .git) {
  git add frontend/src docs/test frontend/tests/e2e
  git commit -m "feat: add bilingual ui and theme switching"
} else {
  Write-Host "No git repo detected; record the bilingual-theme verification output as the release checkpoint."
  Get-Item frontend/src/stores/preferences.ts, frontend/src/i18n/messages.ts, frontend/src/styles/theme.css, docs/test/2026-04-24-ams2-ui-crud-smoke.md
}
```

Expected: Either a normal git commit, or a verified checkpoint proving the bilingual and theme-switching release candidate passed unit, build, e2e, and manual preference smoke.

## Coverage Check

- Spec section `壳层入口` is implemented by Task 2.
- Spec section `默认与持久化规则` is implemented by Task 1 and verified again in Task 6.
- Spec section `信息架构与代码边界` is implemented by Tasks 1 and 2.
- Spec section `主题系统设计` is implemented by Task 3 and consumed across Tasks 4 and 5.
- Spec section `实施范围` is implemented by Tasks 3, 4, and 5 across all currently routed user-visible frontend pages.
- Spec section `错误处理与回退` is implemented by Tasks 1, 3, 4, and 5 via translation fallback, persisted preference fallback, and localized UI-level errors.
- Spec section `测试设计` is implemented by Tasks 1 through 6.

No spec gaps remain in this plan.

## Placeholder Scan

- No `TODO`, `TBD`, `implement later`, or unresolved placeholders remain.
- Every task lists exact file paths.
- Every test step contains concrete test code or concrete verification commands.
- Every checkpoint step includes an explicit fallback for the current non-git workspace.

## Type Consistency Check

- Preference types stay `Locale = 'zh-CN' | 'en-US'` and `ThemeMode = 'dark' | 'light'` throughout the plan.
- The shell and router consistently use `labelKey`, `descriptionKey`, `titleKey`, `subtitleKey`, and `sectionKey` instead of mixed literal fields.
- The translation API consistently uses `translate(locale, key)` and `useI18nText().t(key)`.
- Theme application consistently targets `document.documentElement.dataset.theme`.
