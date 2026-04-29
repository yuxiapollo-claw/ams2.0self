<template>
  <header class="topbar">
    <div class="topbar__titles">
      <div class="topbar__eyebrow">{{ copy.eyebrow }}</div>
      <div class="topbar__title">{{ currentTitle }}</div>
    </div>

    <div class="topbar__actions">
      <button class="topbar__chip" type="button" @click="preferences.toggleLocale()">
        {{ localeLabel }}
      </button>
      <button class="topbar__chip" type="button" @click="preferences.toggleTheme()">
        {{ themeLabel }}
      </button>
      <div class="topbar__user">
        <div class="topbar__user-name">{{ authStore.user?.userName || copy.guest }}</div>
        <div class="topbar__user-meta">
          {{ authStore.user?.loginName || '-' }}
          <span v-if="authStore.isSystemAdmin">{{ copy.admin }}</span>
        </div>
      </div>
      <button class="topbar__logout" type="button" @click="handleLogout">
        {{ copy.logout }}
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18nText } from '../../i18n'
import { useAuthStore } from '../../stores/auth'
import { usePreferencesStore } from '../../stores/preferences'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const preferences = usePreferencesStore()
const { locale, theme } = useI18nText()

const zhCopy = {
  eyebrow: '访问控制工作台',
  logout: '退出登录',
  guest: '未登录',
  admin: '系统管理员',
  localeZh: '切换为 English',
  localeEn: '切换为中文',
  themeDark: '切换明亮',
  themeLight: '切换暗黑'
} as const

const enCopy = {
  eyebrow: 'Access Control Workspace',
  logout: 'Sign Out',
  guest: 'Guest',
  admin: 'System Admin',
  localeZh: 'Switch to English',
  localeEn: 'Switch to Chinese',
  themeDark: 'Light Mode',
  themeLight: 'Dark Mode'
} as const

const routeTitles = {
  login: { zh: '登录', en: 'Sign In' },
  dashboard: { zh: '概览', en: 'Overview' },
  users: { zh: '用户管理', en: 'User Management' },
  departments: { zh: '部门管理', en: 'Department Management' },
  'system-permissions': { zh: '系统及权限管理', en: 'System & Permission Management' },
  'device-permissions': { zh: '权限查询', en: 'Permission Query' },
  'audit-logs': { zh: '审计日志', en: 'Audit Logs' },
  'permission-apply': { zh: '权限申请', en: 'Permission Apply' },
  'permission-remove': { zh: '删除权限申请', en: 'Permission Remove Request' },
  'permission-reset': { zh: '密码重置申请', en: 'Password Reset Request' }
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

const currentTitle = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  const translated = routeTitles[routeName as keyof typeof routeTitles]
  if (translated) {
    return locale.value === 'zh-CN' ? translated.zh : translated.en
  }

  const title = route.meta.title
  return typeof title === 'string' && title.trim().length > 0 ? title : 'AMS'
})

const localeLabel = computed(() =>
  locale.value === 'zh-CN' ? copy.value.localeZh : copy.value.localeEn
)

const themeLabel = computed(() =>
  theme.value === 'dark' ? copy.value.themeDark : copy.value.themeLight
)

async function handleLogout() {
  authStore.logout()
  await router.push('/login')
}
</script>

<style scoped>
.topbar {
  height: var(--cockpit-topbar-h);
  padding: 0 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--cockpit-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.04)),
    var(--cockpit-surface);
  backdrop-filter: blur(20px);
}

.topbar__titles {
  display: grid;
  gap: 4px;
}

.topbar__eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.topbar__title {
  font-size: 18px;
  font-weight: 900;
}

.topbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar__chip,
.topbar__logout {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--cockpit-border);
  background: rgba(255, 255, 255, 0.08);
  color: var(--cockpit-text);
  font: inherit;
  cursor: pointer;
}

.topbar__logout {
  background: linear-gradient(135deg, rgba(94, 234, 212, 0.18), rgba(96, 165, 250, 0.2));
}

.topbar__user {
  display: grid;
  gap: 3px;
  min-width: 140px;
}

.topbar__user-name {
  font-size: 13px;
  font-weight: 800;
  text-align: right;
}

.topbar__user-meta {
  font-size: 12px;
  color: var(--cockpit-muted);
  text-align: right;
}

.topbar__user-meta span {
  margin-left: 8px;
}

@media (max-width: 980px) {
  .topbar {
    height: auto;
    align-items: flex-start;
    flex-direction: column;
    padding: 16px 18px;
  }

  .topbar__actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .topbar__user {
    min-width: 0;
  }

  .topbar__user-name,
  .topbar__user-meta {
    text-align: left;
  }
}
</style>
