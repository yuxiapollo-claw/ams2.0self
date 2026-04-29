<template>
  <section class="workbench">
    <PageHero
      :kicker="t('login.eyebrow')"
      :title="copy.hero.title"
      :subtitle="copy.hero.subtitle"
    />

    <div class="metrics" v-if="!loading">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
      </article>
    </div>

    <FilterPanel :title="copy.filters.title">
      <form class="toolbar" @submit.prevent="handleSearch">
        <label class="toolbar__field" for="device-node-id">
          <span>{{ copy.filters.deviceNodeId }}</span>
          <input
            id="device-node-id"
            v-model="searchDeviceNodeId"
            class="toolbar__control"
            name="deviceNodeId"
            type="text"
            :placeholder="copy.filters.placeholder"
          />
        </label>

        <button class="inline-action" type="submit" :disabled="loading">
          {{ loading ? copy.actions.searching : copy.actions.search }}
        </button>
      </form>
    </FilterPanel>

    <div v-if="loading" class="loading-card" aria-live="polite">
      {{ copy.loading }}
    </div>

    <div v-else-if="errorMessage" class="error-card" role="alert">
      <div class="error-card__title">{{ copy.error.title }}</div>
      <div class="error-card__body">{{ errorMessage }}</div>
    </div>

    <section class="table-card">
      <header class="table-card__header">
        <div>
          <h2 class="table-card__title">{{ copy.table.title }}</h2>
          <p class="table-card__meta">{{ tableSummary }}</p>
        </div>
      </header>

      <div v-if="rows.length === 0" class="empty-state">
        {{ copy.empty }}
      </div>

      <div v-else class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th>{{ copy.table.role }}</th>
              <th>{{ copy.table.deviceAccounts }}</th>
              <th>{{ copy.table.accountCount }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.roleName + row.deviceAccounts">
              <td class="primary-cell">{{ row.roleName }}</td>
              <td>{{ row.deviceAccounts }}</td>
              <td>{{ row.accountCount }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchDevicePermissions } from '../../api/queries'
import { useI18nText } from '../../i18n'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
const { t, locale } = useI18nText()

const zhCopy = {
  hero: {
    title: '设备权限查询工作台',
    subtitle: '在单个驾驶舱表格中查看设备角色覆盖，并通过可复用筛选快速定位。'
  },
  metrics: {
    currentDevice: '当前设备',
    roleRows: '角色行',
    mappedAccounts: '映射账号',
    emptyRoles: '空角色'
  },
  filters: {
    title: '设备筛选',
    deviceNodeId: '设备节点 ID',
    placeholder: '输入设备节点 ID'
  },
  actions: {
    search: '执行查询',
    searching: '查询中...'
  },
  loading: '正在加载设备权限...',
  error: {
    title: '查询失败',
    fallback: '请尝试其他设备节点 ID。'
  },
  table: {
    title: '角色覆盖',
    role: '角色',
    deviceAccounts: '设备账号',
    accountCount: '账号数量'
  },
  empty: '当前设备暂无角色覆盖信息。'
} as const

const enCopy = {
  hero: {
    title: 'Device Permission Query Workbench',
    subtitle: 'Inspect device-role coverage from a single cockpit table with repeatable filters.'
  },
  metrics: {
    currentDevice: 'Current Device',
    roleRows: 'Role Rows',
    mappedAccounts: 'Mapped Accounts',
    emptyRoles: 'Empty Roles'
  },
  filters: {
    title: 'Device Filter',
    deviceNodeId: 'Device Node ID',
    placeholder: 'Enter a device node id'
  },
  actions: {
    search: 'Run Query',
    searching: 'Searching...'
  },
  loading: 'Loading device permissions...',
  error: {
    title: 'Query Failed',
    fallback: 'Try a different device node id.'
  },
  table: {
    title: 'Role Coverage',
    role: 'Role',
    deviceAccounts: 'Device Accounts',
    accountCount: 'Account Count'
  },
  empty: 'No role coverage is available for the selected device.'
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

const DEFAULT_DEVICE_NODE_ID = '100'

const searchDeviceNodeId = ref(DEFAULT_DEVICE_NODE_ID)
const activeDeviceNodeId = ref(DEFAULT_DEVICE_NODE_ID)
const loading = ref(false)
const errorMessage = ref('')
const roles = ref<{ roleName: string; deviceAccounts: string[] }[]>([])
let queryRequestSequence = 0

const rows = computed(() =>
  roles.value.map((role) => ({
    roleName: role.roleName,
    deviceAccounts: role.deviceAccounts.length > 0 ? role.deviceAccounts.join(', ') : '-',
    accountCount: role.deviceAccounts.length
  }))
)

const tableSummary = computed(() =>
  locale.value === 'zh-CN'
    ? `${rows.value.length} 条角色结果，设备 ${activeDeviceNodeId.value}`
    : `${rows.value.length} visible role rows for device ${activeDeviceNodeId.value}`
)

const metrics = computed(() => [
  { label: copy.value.metrics.currentDevice, value: activeDeviceNodeId.value },
  { label: copy.value.metrics.roleRows, value: roles.value.length },
  {
    label: copy.value.metrics.mappedAccounts,
    value: roles.value.reduce((total, role) => total + role.deviceAccounts.length, 0)
  },
  {
    label: copy.value.metrics.emptyRoles,
    value: roles.value.filter((role) => role.deviceAccounts.length === 0).length
  }
])

async function load(deviceNodeId: string) {
  const requestId = ++queryRequestSequence
  loading.value = true
  errorMessage.value = ''
  roles.value = []

  try {
    const result = await fetchDevicePermissions(deviceNodeId)
    if (requestId !== queryRequestSequence) {
      return
    }

    activeDeviceNodeId.value = result.deviceNodeId
    roles.value = result.roles
  } catch (error) {
    if (requestId !== queryRequestSequence) {
      return
    }

    roles.value = []
    activeDeviceNodeId.value = deviceNodeId
    errorMessage.value = error instanceof Error && error.message.trim().length > 0
      ? error.message.trim()
      : copy.value.error.fallback
  } finally {
    if (requestId === queryRequestSequence) {
      loading.value = false
    }
  }
}

async function handleSearch() {
  await load(searchDeviceNodeId.value.trim() || DEFAULT_DEVICE_NODE_ID)
}

onMounted(async () => {
  await load(DEFAULT_DEVICE_NODE_ID)
})
</script>

<style scoped>
.workbench {
  display: grid;
  gap: 14px;
}

.metrics {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric-card {
  padding: 15px 16px 13px;
  border: 1px solid var(--cockpit-border);
  border-radius: var(--cockpit-radius-md);
  background:
    radial-gradient(420px 160px at 15% 0%, rgba(94, 234, 212, 0.12), transparent 55%),
    rgba(255, 255, 255, 0.05);
  box-shadow: var(--cockpit-shadow);
}

.metric-card__label {
  font-size: 12px;
  letter-spacing: 0.04em;
  color: var(--cockpit-muted);
  text-transform: uppercase;
}

.metric-card__value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 900;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: end;
}

.toolbar__field {
  display: grid;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
}

.toolbar__control {
  min-height: 44px;
  width: 100%;
  padding: 11px 14px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.18);
  color: var(--cockpit-text);
  font: inherit;
}

.toolbar__control::placeholder {
  color: rgba(255, 255, 255, 0.44);
}

.inline-action {
  min-height: 44px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: linear-gradient(135deg, rgba(94, 234, 212, 0.95), rgba(96, 165, 250, 0.95));
  color: #041322;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.inline-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-card,
.error-card,
.table-card {
  border: 1px solid var(--cockpit-border);
  border-radius: var(--cockpit-radius-lg);
  box-shadow: var(--cockpit-shadow);
}

.loading-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--cockpit-muted);
}

.error-card {
  padding: 16px;
  background: rgba(251, 113, 133, 0.12);
  border-color: rgba(251, 113, 133, 0.32);
  display: grid;
  gap: 10px;
}

.error-card__title {
  font-size: 14px;
  font-weight: 900;
}

.error-card__body {
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.5;
}

.table-card {
  overflow: hidden;
  background: rgba(255, 255, 255, 0.04);
}

.table-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--cockpit-border);
}

.table-card__title {
  margin: 0;
  font-size: 15px;
  font-weight: 900;
}

.table-card__meta {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--cockpit-muted);
}

.empty-state {
  padding: 22px 18px 24px;
  color: var(--cockpit-muted);
}

.table-wrap {
  overflow-x: auto;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 720px;
}

.result-table th,
.result-table td {
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  text-align: left;
  font-size: 13px;
  vertical-align: top;
}

.result-table th {
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.primary-cell {
  font-weight: 800;
}

@media (max-width: 1080px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .toolbar,
  .metrics {
    grid-template-columns: 1fr;
  }

  .inline-action {
    width: 100%;
  }
}
</style>
