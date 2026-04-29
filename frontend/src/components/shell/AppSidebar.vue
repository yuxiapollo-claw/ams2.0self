<template>
  <aside class="sidebar">
    <div class="sidebar__brand">
      <div class="sidebar__brand-mark">AMS</div>
      <div>
        <div class="sidebar__brand-title">{{ copy.product }}</div>
        <div class="sidebar__brand-subtitle">{{ copy.productSub }}</div>
      </div>
    </div>

    <nav class="sidebar__nav" :aria-label="copy.ariaLabel">
      <RouterLink
        v-for="item in primaryItems"
        :key="item.to"
        :to="item.to"
        class="sidebar__link"
        :class="{ 'is-active': isActive(item.to) }"
      >
        <span class="sidebar__link-title">{{ item.label }}</span>
        <span class="sidebar__link-meta">{{ item.description }}</span>
      </RouterLink>
    </nav>

    <section class="sidebar__group">
      <div class="sidebar__group-title">{{ copy.requestGroup }}</div>
      <nav class="sidebar__nav sidebar__nav--compact" :aria-label="copy.requestGroup">
        <RouterLink
          v-for="item in requestItems"
          :key="item.to"
          :to="item.to"
          class="sidebar__link sidebar__link--compact"
          :class="{ 'is-active': isActive(item.to) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18nText } from '../../i18n'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const { locale } = useI18nText()

const zhCopy = {
  ariaLabel: '主导航',
  product: '本地权限管理系统',
  productSub: '用户、部门、系统与权限的一体化工作台',
  requestGroup: '申请管理',
  items: {
    dashboard: {
      label: '概览',
      description: '查看工作台摘要、入口与近期申请'
    },
    users: {
      label: '用户管理',
      description: '维护用户信息、状态与密码'
    },
    departments: {
      label: '部门管理',
      description: '维护部门负责人并批量关联用户'
    },
    access: {
      label: '系统及权限管理',
      description: '管理系统清单与五级权限树'
    },
    queries: {
      label: '权限查询',
      description: '查看设备或系统的权限覆盖情况'
    },
    audit: {
      label: '审计日志',
      description: '追踪关键操作与历史记录'
    }
  },
  requests: {
    apply: '权限申请',
    remove: '删除权限',
    reset: '密码重置'
  }
} as const

const enCopy = {
  ariaLabel: 'Primary navigation',
  product: 'Local Access Management',
  productSub: 'Users, departments, systems, and permission flows',
  requestGroup: 'Request Management',
  items: {
    dashboard: {
      label: 'Overview',
      description: 'Review platform status and key entry points'
    },
    users: {
      label: 'Users',
      description: 'Manage profiles, status, and passwords'
    },
    departments: {
      label: 'Departments',
      description: 'Manage leaders and batch member assignments'
    },
    access: {
      label: 'Systems & Permissions',
      description: 'Manage systems and the five-level permission tree'
    },
    queries: {
      label: 'Permission Query',
      description: 'Inspect permission coverage by device or system'
    },
    audit: {
      label: 'Audit Logs',
      description: 'Trace key operations and history'
    }
  },
  requests: {
    apply: 'Apply',
    remove: 'Remove',
    reset: 'Reset Password'
  }
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

const primaryItems = computed(() => {
  const items = [
    {
      to: '/dashboard',
      label: copy.value.items.dashboard.label,
      description: copy.value.items.dashboard.description
    },
    {
      to: '/users',
      label: copy.value.items.users.label,
      description: copy.value.items.users.description
    }
  ]

  if (authStore.isSystemAdmin) {
    items.push(
      {
        to: '/departments',
        label: copy.value.items.departments.label,
        description: copy.value.items.departments.description
      },
      {
        to: '/systemAdmin/sysAccessAdmin',
        label: copy.value.items.access.label,
        description: copy.value.items.access.description
      }
    )
  }

  items.push(
    {
      to: '/queries/device-permissions',
      label: copy.value.items.queries.label,
      description: copy.value.items.queries.description
    },
    {
      to: '/audit/logs',
      label: copy.value.items.audit.label,
      description: copy.value.items.audit.description
    }
  )

  return items
})

const requestItems = computed(() => [
  { to: '/access/myRequest', label: copy.value.requests.apply },
  { to: '/access/myRemove', label: copy.value.requests.remove },
  { to: '/access/myChangepd', label: copy.value.requests.reset }
])

function isActive(path: string) {
  return route.path === path
}
</script>

<style scoped>
.sidebar {
  display: grid;
  align-content: start;
  gap: 22px;
  min-height: calc(100vh - var(--cockpit-topbar-h));
  padding: 22px 18px 24px;
  border-right: 1px solid var(--cockpit-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.04)),
    var(--cockpit-surface);
  backdrop-filter: blur(18px);
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.sidebar__brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 900;
  color: #041322;
  background: linear-gradient(135deg, var(--cockpit-accent), var(--cockpit-accent-2));
  box-shadow: var(--cockpit-shadow);
}

.sidebar__brand-title {
  font-size: 15px;
  font-weight: 900;
}

.sidebar__brand-subtitle {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--cockpit-muted);
}

.sidebar__nav {
  display: grid;
  gap: 10px;
}

.sidebar__nav--compact {
  gap: 8px;
}

.sidebar__link {
  display: grid;
  gap: 4px;
  padding: 14px 14px 13px;
  border-radius: 16px;
  text-decoration: none;
  color: inherit;
  border: 1px solid transparent;
  background: transparent;
  transition: transform 160ms ease, border-color 160ms ease, background 160ms ease;
}

.sidebar__link:hover {
  transform: translateY(-1px);
  border-color: var(--cockpit-border);
  background: rgba(255, 255, 255, 0.08);
}

.sidebar__link.is-active {
  border-color: rgba(96, 165, 250, 0.3);
  background:
    linear-gradient(135deg, rgba(94, 234, 212, 0.14), rgba(96, 165, 250, 0.18)),
    rgba(255, 255, 255, 0.08);
}

.sidebar__link--compact {
  padding: 11px 14px;
  font-size: 13px;
  font-weight: 700;
}

.sidebar__link-title {
  font-size: 13px;
  font-weight: 800;
}

.sidebar__link-meta {
  font-size: 12px;
  line-height: 1.45;
  color: var(--cockpit-muted);
}

.sidebar__group {
  display: grid;
  gap: 10px;
}

.sidebar__group-title {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

@media (max-width: 980px) {
  .sidebar {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid var(--cockpit-border);
  }
}
</style>
