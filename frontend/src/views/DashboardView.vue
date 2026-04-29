<template>
  <section class="workbench">
    <PageHero :kicker="copy.kicker" :title="copy.title" :subtitle="copy.subtitle" />

    <div class="metrics">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
      </article>
    </div>

    <div class="grid">
      <section class="panel-card">
        <header class="panel-card__header">
          <div>
            <h2 class="panel-card__title">{{ copy.quickTitle }}</h2>
            <p class="panel-card__meta">{{ copy.quickSubtitle }}</p>
          </div>
        </header>

        <div class="quick-grid">
          <RouterLink v-for="item in quickLinks" :key="item.to" :to="item.to" class="quick-link">
            <div class="quick-link__title">{{ item.title }}</div>
            <div class="quick-link__body">{{ item.description }}</div>
          </RouterLink>
        </div>
      </section>

      <section class="panel-card">
        <header class="panel-card__header">
          <div>
            <h2 class="panel-card__title">{{ copy.activityTitle }}</h2>
            <p class="panel-card__meta">{{ copy.activitySubtitle }}</p>
          </div>
        </header>

        <div v-if="loading" class="empty-state">{{ copy.loading }}</div>
        <div v-else-if="errorMessage" class="error-card">{{ errorMessage }}</div>
        <div v-else-if="requests.length === 0" class="empty-state">{{ copy.empty }}</div>
        <div v-else class="activity-list">
          <article v-for="item in requests.slice(0, 6)" :key="item.id" class="activity-row">
            <div>
              <div class="activity-row__title">{{ formatRequestType(item.requestType) }}</div>
              <div class="activity-row__path">{{ item.permissionPath }}</div>
            </div>
            <div class="activity-row__meta">
              <span class="status-pill">{{ formatStatus(item.currentStatus) }}</span>
              <span>{{ formatDate(item.createdAt) }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchAccessSystems, fetchPermissionRequests, type PermissionRequestItem } from '../api/access'
import { fetchDepartments } from '../api/departments'
import { fetchUsers } from '../api/users'
import PageHero from '../components/shell/PageHero.vue'
import { useI18nText } from '../i18n'
import { useAuthStore } from '../stores/auth'

const { locale } = useI18nText()
const authStore = useAuthStore()

const usersCount = ref(0)
const departmentsCount = ref(0)
const systemsCount = ref(0)
const requests = ref<PermissionRequestItem[]>([])
const loading = ref(true)
const errorMessage = ref('')

const zhCopy = {
  kicker: '驾驶舱',
  title: '权限治理概览',
  subtitle: '从用户、部门、系统与权限申请四条主线进入当前版本的核心功能。',
  quickTitle: '快捷入口',
  quickSubtitle: '围绕这次改造后的主流程布局。',
  activityTitle: '最新申请',
  activitySubtitle: '展示当前登录用户可见的权限申请记录。',
  loading: '正在加载概览数据...',
  empty: '当前还没有权限申请记录。',
  metrics: {
    users: '用户数量',
    departments: '部门数量',
    systems: '系统数量',
    requests: '可见申请'
  },
  quickLinks: {
    users: {
      title: '进入用户管理',
      description: '维护登录名、状态、默认密码重置和个人改密。'
    },
    departments: {
      title: '进入部门管理',
      description: '由系统管理员维护部门负责人并批量关联成员。'
    },
    systems: {
      title: '进入系统及权限管理',
      description: '维护系统清单和最多五级的权限路径。'
    },
    apply: {
      title: '发起权限申请',
      description: '从树形权限路径里选择最终权限并提交流程。'
    },
    query: {
      title: '查看权限查询',
      description: '按设备节点查看角色和账号的权限覆盖情况。'
    },
    audit: {
      title: '查看审计日志',
      description: '追踪关键操作记录，辅助排查与回溯。'
    }
  }
} as const

const enCopy = {
  kicker: 'Cockpit',
  title: 'Access Governance Overview',
  subtitle: 'Enter the current release through the four main lines: users, departments, systems, and request flows.',
  quickTitle: 'Quick Entry',
  quickSubtitle: 'Focused on the rebuilt workflow.',
  activityTitle: 'Latest Requests',
  activitySubtitle: 'Shows the permission requests visible to the current login user.',
  loading: 'Loading overview data...',
  empty: 'No permission requests yet.',
  metrics: {
    users: 'Users',
    departments: 'Departments',
    systems: 'Systems',
    requests: 'Visible Requests'
  },
  quickLinks: {
    users: {
      title: 'Open User Management',
      description: 'Manage login names, account status, default resets, and personal password changes.'
    },
    departments: {
      title: 'Open Department Management',
      description: 'System administrators maintain leaders and batch member assignments.'
    },
    systems: {
      title: 'Open Systems & Permissions',
      description: 'Maintain the system list and the five-level permission hierarchy.'
    },
    apply: {
      title: 'Launch Permission Request',
      description: 'Select a terminal permission path from the tree and submit the workflow.'
    },
    query: {
      title: 'Open Permission Query',
      description: 'Inspect permission coverage by device node, role, and account.'
    },
    audit: {
      title: 'Open Audit Logs',
      description: 'Trace key operation records for troubleshooting and review.'
    }
  }
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

const metrics = computed(() => [
  { label: copy.value.metrics.users, value: usersCount.value },
  { label: copy.value.metrics.departments, value: departmentsCount.value },
  { label: copy.value.metrics.systems, value: systemsCount.value },
  { label: copy.value.metrics.requests, value: requests.value.length }
])

const quickLinks = computed(() => {
  const items = [
    { to: '/users', title: copy.value.quickLinks.users.title, description: copy.value.quickLinks.users.description },
    { to: '/access/myRequest', title: copy.value.quickLinks.apply.title, description: copy.value.quickLinks.apply.description },
    { to: '/queries/device-permissions', title: copy.value.quickLinks.query.title, description: copy.value.quickLinks.query.description },
    { to: '/audit/logs', title: copy.value.quickLinks.audit.title, description: copy.value.quickLinks.audit.description }
  ]

  if (authStore.isSystemAdmin) {
    items.splice(
      1,
      0,
      { to: '/departments', title: copy.value.quickLinks.departments.title, description: copy.value.quickLinks.departments.description },
      {
        to: '/systemAdmin/sysAccessAdmin',
        title: copy.value.quickLinks.systems.title,
        description: copy.value.quickLinks.systems.description
      }
    )
  }

  return items
})

onMounted(() => {
  void loadDashboard()
})

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''

  try {
    const tasks: Array<Promise<unknown>> = [fetchUsers(), fetchAccessSystems(), fetchPermissionRequests()]
    if (authStore.isSystemAdmin) {
      tasks.splice(1, 0, fetchDepartments())
    }

    const [users, maybeDepartments, systems, permissionRequests] = await Promise.all(tasks)
    usersCount.value = Array.isArray(users) ? users.length : 0
    if (authStore.isSystemAdmin) {
      departmentsCount.value = Array.isArray(maybeDepartments) ? maybeDepartments.length : 0
      systemsCount.value = Array.isArray(systems) ? systems.length : 0
      requests.value = Array.isArray(permissionRequests) ? permissionRequests : []
    } else {
      departmentsCount.value = 0
      systemsCount.value = Array.isArray(maybeDepartments) ? maybeDepartments.length : 0
      requests.value = Array.isArray(systems) ? systems : []
    }
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
    usersCount.value = 0
    departmentsCount.value = 0
    systemsCount.value = 0
    requests.value = []
  } finally {
    loading.value = false
  }
}

function extractErrorMessage(error: unknown) {
  if (error && typeof error === 'object') {
    const response = (error as { response?: { data?: { message?: string } } }).response
    const backendMessage = response?.data?.message
    if (typeof backendMessage === 'string' && backendMessage.trim().length > 0) {
      return backendMessage
    }
  }
  return error instanceof Error && error.message.trim().length > 0 ? error.message : 'Load failed'
}

function formatRequestType(value: string) {
  const map =
    locale.value === 'zh-CN'
      ? {
          PERMISSION_APPLY: '权限申请',
          PERMISSION_REMOVE: '删除权限申请',
          PASSWORD_RESET: '密码重置申请'
        }
      : {
          PERMISSION_APPLY: 'Permission Apply',
          PERMISSION_REMOVE: 'Permission Remove',
          PASSWORD_RESET: 'Password Reset'
        }
  return map[value as keyof typeof map] ?? value
}

function formatStatus(value: string) {
  return locale.value === 'zh-CN'
    ? value === 'PENDING'
      ? '处理中'
      : value === 'COMPLETED'
        ? '已完成'
        : value
    : value === 'PENDING'
      ? 'Pending'
      : value === 'COMPLETED'
        ? 'Completed'
        : value
}

function formatDate(value: string) {
  return value ? value.replace('T', ' ').replace('Z', '') : '-'
}
</script>

<style scoped>
.workbench {
  display: grid;
  gap: 16px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.panel-card {
  border: 1px solid var(--cockpit-border);
  border-radius: var(--cockpit-radius-lg);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: var(--cockpit-shadow);
}

.metric-card {
  padding: 16px 18px;
}

.metric-card__label {
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.metric-card__value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 900;
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.88fr);
  gap: 16px;
}

.panel-card {
  padding: 18px;
}

.panel-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-card__title {
  margin: 0;
  font-size: 18px;
}

.panel-card__meta {
  margin: 8px 0 0;
  color: var(--cockpit-muted);
  line-height: 1.6;
}

.quick-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-link {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(96, 165, 250, 0.22);
  background:
    linear-gradient(135deg, rgba(94, 234, 212, 0.12), rgba(96, 165, 250, 0.14)),
    rgba(255, 255, 255, 0.05);
  text-decoration: none;
  color: inherit;
  transition: transform 160ms ease;
}

.quick-link:hover {
  transform: translateY(-2px);
}

.quick-link__title {
  font-size: 14px;
  font-weight: 900;
}

.quick-link__body {
  margin-top: 10px;
  color: var(--cockpit-muted);
  line-height: 1.55;
  font-size: 13px;
}

.activity-list {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.activity-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--cockpit-border);
  background: rgba(255, 255, 255, 0.05);
}

.activity-row__title {
  font-size: 13px;
  font-weight: 900;
}

.activity-row__path {
  margin-top: 6px;
  font-size: 13px;
  color: var(--cockpit-muted);
  line-height: 1.55;
}

.activity-row__meta {
  display: grid;
  justify-items: end;
  gap: 8px;
  font-size: 12px;
  color: var(--cockpit-muted);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.16);
  color: var(--cockpit-text);
  font-size: 12px;
  font-weight: 800;
}

.empty-state,
.error-card {
  margin-top: 16px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--cockpit-border);
  background: rgba(255, 255, 255, 0.05);
  color: var(--cockpit-muted);
}

.error-card {
  color: rgba(255, 196, 206, 0.98);
  background: rgba(251, 113, 133, 0.14);
  border-color: rgba(251, 113, 133, 0.28);
}

@media (max-width: 1080px) {
  .metrics,
  .grid,
  .quick-grid {
    grid-template-columns: 1fr 1fr;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .metrics,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .activity-row {
    flex-direction: column;
  }

  .activity-row__meta {
    justify-items: start;
  }
}
</style>
