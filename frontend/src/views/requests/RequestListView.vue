<template>
  <section class="workbench">
    <PageHero
      :kicker="t('login.eyebrow')"
      :title="copy.hero.title"
      :subtitle="copy.hero.subtitle"
    />

    <div v-if="!loading && !errorMessage" class="metrics">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
      </article>
    </div>

    <div v-if="loading" class="loading-card" aria-live="polite">
      {{ copy.loading }}
    </div>

    <div v-else-if="errorMessage" class="error-card" role="alert">
      <div class="error-card__title">{{ copy.error.title }}</div>
      <div class="error-card__body">{{ errorMessage }}</div>
      <button class="inline-action" type="button" @click="loadRequests">{{ copy.actions.retry }}</button>
    </div>

    <template v-else>
      <FilterPanel :title="copy.filters.title">
        <div class="toolbar">
          <label class="toolbar__field" for="request-status-filter">
            <span>{{ copy.filters.status }}</span>
            <select
              id="request-status-filter"
              v-model="statusFilter"
              class="toolbar__control"
              name="statusFilter"
            >
              <option value="">{{ copy.filters.allStatuses }}</option>
              <option v-for="status in statusOptions" :key="status" :value="status">
                {{ formatStatus(status) }}
              </option>
            </select>
          </label>

          <label class="toolbar__field" for="request-type-filter">
            <span>{{ copy.filters.type }}</span>
            <select
              id="request-type-filter"
              v-model="requestTypeFilter"
              class="toolbar__control"
              name="requestTypeFilter"
            >
              <option value="">{{ copy.filters.allTypes }}</option>
              <option v-for="requestType in requestTypeOptions" :key="requestType" :value="requestType">
                {{ formatRequestType(requestType) }}
              </option>
            </select>
          </label>

          <label class="toolbar__field" for="request-created-date">
            <span>{{ copy.filters.createdDate }}</span>
            <input
              id="request-created-date"
              v-model="createdDateFilter"
              class="toolbar__control"
              name="createdDateFilter"
              type="text"
              :placeholder="copy.filters.datePlaceholder"
            />
          </label>
        </div>
      </FilterPanel>

      <section class="table-card">
        <header class="table-card__header">
          <div>
            <h2 class="table-card__title">{{ copy.table.title }}</h2>
            <p class="table-card__meta">{{ requestSummaryText }}</p>
          </div>
        </header>

        <div v-if="filteredRequests.length === 0" class="empty-state">
          {{ copy.empty }}
        </div>

        <div v-else class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>{{ copy.table.requestNo }}</th>
                <th>{{ copy.table.type }}</th>
                <th>{{ copy.table.targetAccount }}</th>
                <th>{{ copy.table.status }}</th>
                <th>{{ copy.table.createdAt }}</th>
                <th>{{ copy.table.actions }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in filteredRequests" :key="request.id">
                <td class="primary-cell">{{ request.requestNo }}</td>
                <td>{{ formatRequestType(request.requestType) }}</td>
                <td>{{ request.targetAccountName || '-' }}</td>
                <td>
                  <span class="status-pill" :class="statusTone(request.status)">
                    {{ formatStatus(request.status) }}
                  </span>
                </td>
                <td>{{ request.createdAt || '-' }}</td>
                <td>
                  <button
                    class="row-action"
                    :data-testid="`view-request-${request.id}`"
                    type="button"
                    @click="openDetail(request)"
                  >
                    {{ copy.actions.viewDetail }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <div v-if="detailRequest" class="detail-shell">
      <button class="detail-shell__backdrop" type="button" :aria-label="copy.actions.close" @click="closeDetail" />
      <aside class="detail-shell__panel" :aria-label="copy.detail.title">
        <section class="detail-card">
          <header class="detail-card__header">
            <div>
              <h2 class="detail-card__title">{{ copy.detail.title }}</h2>
              <p class="detail-card__meta">{{ detailRequest.requestNo }}</p>
            </div>
            <button class="inline-action" type="button" @click="closeDetail">{{ copy.actions.close }}</button>
          </header>

          <dl class="detail-grid">
            <div class="detail-grid__item">
              <dt>{{ copy.detail.type }}</dt>
              <dd>{{ formatRequestType(detailRequest.requestType) }}</dd>
            </div>
            <div class="detail-grid__item">
              <dt>{{ copy.detail.status }}</dt>
              <dd>{{ formatStatus(detailRequest.status) }}</dd>
            </div>
            <div class="detail-grid__item">
              <dt>{{ copy.detail.targetAccount }}</dt>
              <dd>{{ detailRequest.targetAccountName || '-' }}</dd>
            </div>
            <div class="detail-grid__item">
              <dt>{{ copy.detail.createdAt }}</dt>
              <dd>{{ detailRequest.createdAt || '-' }}</dd>
            </div>
          </dl>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchRequests, type RequestItem } from '../../api/requests'
import { useI18nText } from '../../i18n'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
const { t, locale } = useI18nText()

const zhCopy = {
  hero: {
    title: '申请管理工作台',
    subtitle: '跟踪审批积压、查看最新申请动态，并快速分诊敏感权限变更。'
  },
  metrics: {
    pending: '待处理申请',
    roleAdd: '角色新增申请',
    closed: '已关闭申请',
    latest: '最新单号'
  },
  loading: '正在加载申请队列...',
  error: {
    title: '加载申请失败',
    fallback: '刷新页面后重试。'
  },
  filters: {
    title: '筛选条件',
    status: '审批状态',
    allStatuses: '全部状态',
    type: '申请类型',
    allTypes: '全部申请类型',
    createdDate: '创建日期',
    datePlaceholder: 'YYYY-MM-DD'
  },
  table: {
    title: '审批队列',
    meta: '条申请',
    requestNo: '申请单号',
    type: '申请类型',
    targetAccount: '目标账号',
    status: '状态',
    createdAt: '创建时间',
    actions: '操作'
  },
  empty: '当前筛选条件下暂无匹配申请。',
  actions: {
    retry: '重试',
    viewDetail: '查看详情',
    close: '关闭'
  },
  detail: {
    title: '申请详情',
    type: '申请类型',
    status: '状态',
    targetAccount: '目标账号',
    createdAt: '创建时间'
  },
  requestTypes: {
    ROLE_ADD: '角色新增',
    PASSWORD_RESET: '密码重置'
  },
  statuses: {
    WAIT_DEPT_MANAGER: '待部门经理审批',
    WAIT_QA: '待 QA 审批',
    WAIT_QM: '待 QM 审批',
    WAIT_QI_EXECUTE: '待 QI 执行',
    DONE: '已完成',
    REJECTED: '已驳回'
  }
} as const

const enCopy = {
  hero: {
    title: 'Request Management Workbench',
    subtitle: 'Monitor approval backlog, scan recent request traffic, and triage sensitive access changes.'
  },
  metrics: {
    pending: 'Pending Requests',
    roleAdd: 'Role Add Requests',
    closed: 'Closed Requests',
    latest: 'Latest Ticket'
  },
  loading: 'Loading request queue...',
  error: {
    title: 'Failed to load requests',
    fallback: 'Refresh the page and try again.'
  },
  filters: {
    title: 'Filters',
    status: 'Approval Status',
    allStatuses: 'All statuses',
    type: 'Request Type',
    allTypes: 'All request types',
    createdDate: 'Created Date',
    datePlaceholder: 'YYYY-MM-DD'
  },
  table: {
    title: 'Approval Queue',
    meta: 'requests',
    requestNo: 'Request No',
    type: 'Type',
    targetAccount: 'Target Account',
    status: 'Status',
    createdAt: 'Created At',
    actions: 'Actions'
  },
  empty: 'No requests match the current filter.',
  actions: {
    retry: 'Retry',
    viewDetail: 'View Details',
    close: 'Close'
  },
  detail: {
    title: 'Request Detail',
    type: 'Type',
    status: 'Status',
    targetAccount: 'Target Account',
    createdAt: 'Created At'
  },
  requestTypes: {
    ROLE_ADD: 'Role Add',
    PASSWORD_RESET: 'Password Reset'
  },
  statuses: {
    WAIT_DEPT_MANAGER: 'Waiting For Department Manager',
    WAIT_QA: 'Waiting For QA',
    WAIT_QM: 'Waiting For QM',
    WAIT_QI_EXECUTE: 'Waiting For QI Execution',
    DONE: 'Completed',
    REJECTED: 'Rejected'
  }
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

const loading = ref(false)
const errorMessage = ref('')
const requests = ref<RequestItem[]>([])
const statusFilter = ref('')
const requestTypeFilter = ref('')
const createdDateFilter = ref('')
const detailRequest = ref<RequestItem | null>(null)

const statusOptions = computed(() =>
  Array.from(new Set(requests.value.map((request) => request.status))).filter((status) => status.length > 0)
)

const requestTypeOptions = computed(() =>
  Array.from(new Set(requests.value.map((request) => request.requestType))).filter((requestType) => requestType.length > 0)
)

const filteredRequests = computed(() =>
  requests.value.filter((request) => {
    const matchesStatus = statusFilter.value.length === 0 || request.status === statusFilter.value
    const matchesType = requestTypeFilter.value.length === 0 || request.requestType === requestTypeFilter.value
    const matchesCreatedDate =
      createdDateFilter.value.trim().length === 0 || request.createdAt.includes(createdDateFilter.value.trim())

    return matchesStatus && matchesType && matchesCreatedDate
  })
)

const metrics = computed(() => [
  { label: copy.value.metrics.pending, value: requests.value.filter((request) => request.status.startsWith('WAIT_')).length },
  { label: copy.value.metrics.roleAdd, value: requests.value.filter((request) => request.requestType === 'ROLE_ADD').length },
  { label: copy.value.metrics.closed, value: requests.value.filter((request) => !request.status.startsWith('WAIT_')).length },
  { label: copy.value.metrics.latest, value: requests.value[0]?.requestNo ?? '-' }
])

const requestSummaryText = computed(() =>
  `${filteredRequests.value.length} / ${requests.value.length} ${copy.value.table.meta}`
)

onMounted(() => {
  void loadRequests()
})

async function loadRequests() {
  loading.value = true
  errorMessage.value = ''

  try {
    requests.value = await fetchRequests()
  } catch (error) {
    requests.value = []
    errorMessage.value = error instanceof Error && error.message.trim().length > 0
      ? error.message.trim()
      : copy.value.error.fallback
  } finally {
    loading.value = false
  }
}

function formatRequestType(value: string) {
  return (copy.value.requestTypes[value as keyof typeof copy.value.requestTypes] ?? value) || '-'
}

function formatStatus(value: string) {
  return (copy.value.statuses[value as keyof typeof copy.value.statuses] ?? value) || '-'
}

function statusTone(value: string) {
  if (value.startsWith('WAIT_')) {
    return 'status-pill--warn'
  }
  if (value === 'DONE') {
    return 'status-pill--good'
  }
  if (value === 'REJECTED') {
    return 'status-pill--danger'
  }
  return 'status-pill--neutral'
}

function openDetail(request: RequestItem) {
  detailRequest.value = request
}

function closeDetail() {
  detailRequest.value = null
}
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
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

.inline-action {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.16);
  color: var(--cockpit-text);
  font: inherit;
  cursor: pointer;
}

.row-action {
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.16);
  color: var(--cockpit-text);
  font: inherit;
  cursor: pointer;
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
  min-width: 760px;
}

.result-table th,
.result-table td {
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  text-align: left;
  font-size: 13px;
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

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
  font-weight: 700;
}

.status-pill--good {
  border-color: rgba(94, 234, 212, 0.28);
  background: rgba(94, 234, 212, 0.14);
}

.status-pill--warn {
  border-color: rgba(251, 191, 36, 0.28);
  background: rgba(251, 191, 36, 0.14);
}

.status-pill--danger {
  border-color: rgba(251, 113, 133, 0.28);
  background: rgba(251, 113, 133, 0.14);
}

.status-pill--neutral {
  background: rgba(96, 165, 250, 0.14);
  border-color: rgba(96, 165, 250, 0.24);
}

.detail-shell {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  justify-content: flex-end;
}

.detail-shell__backdrop {
  flex: 1;
  border: 0;
  background: rgba(7, 10, 20, 0.58);
  cursor: pointer;
}

.detail-shell__panel {
  width: min(100%, 420px);
  border-left: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: -18px 0 48px rgba(0, 0, 0, 0.4);
}

.detail-card {
  height: 100%;
  padding: 18px;
  background: rgba(15, 23, 42, 0.96);
  display: grid;
  align-content: start;
  gap: 18px;
}

.detail-card__header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 12px;
}

.detail-card__title {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
}

.detail-card__meta {
  margin: 6px 0 0;
  color: var(--cockpit-muted);
}

.detail-grid {
  display: grid;
  gap: 14px;
  margin: 0;
}

.detail-grid__item {
  display: grid;
  gap: 4px;
}

.detail-grid__item dt {
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.detail-grid__item dd {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
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

  .table-card__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-shell {
    align-items: flex-end;
  }

  .detail-shell__backdrop {
    display: none;
  }

  .detail-shell__panel {
    width: 100%;
    max-height: min(88dvh, 720px);
    border-left: 0;
    border-top: 1px solid rgba(255, 255, 255, 0.14);
  }
}
</style>
