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
      <div class="toolbar">
        <label class="toolbar__field" for="audit-from">
          <span>{{ copy.filters.from }}</span>
          <input
            id="audit-from"
            v-model="fromTime"
            class="toolbar__control"
            type="text"
            :placeholder="copy.filters.datePlaceholder"
          />
        </label>

        <label class="toolbar__field" for="audit-to">
          <span>{{ copy.filters.to }}</span>
          <input
            id="audit-to"
            v-model="toTime"
            class="toolbar__control"
            type="text"
            :placeholder="copy.filters.datePlaceholder"
          />
        </label>

        <label class="toolbar__field" for="audit-operator">
          <span>{{ copy.filters.operator }}</span>
          <input
            id="audit-operator"
            v-model="operatorFilter"
            class="toolbar__control"
            type="search"
            :placeholder="copy.filters.operatorPlaceholder"
          />
        </label>

        <label class="toolbar__field" for="audit-device">
          <span>{{ copy.filters.device }}</span>
          <input
            id="audit-device"
            v-model="deviceFilter"
            class="toolbar__control"
            type="search"
            :placeholder="copy.filters.devicePlaceholder"
          />
        </label>

        <label class="toolbar__field" for="audit-action">
          <span>{{ copy.filters.action }}</span>
          <input
            id="audit-action"
            v-model="actionFilter"
            class="toolbar__control"
            type="search"
            :placeholder="copy.filters.actionPlaceholder"
          />
        </label>

        <label class="toolbar__field" for="audit-target">
          <span>{{ copy.filters.targetObject }}</span>
          <input
            id="audit-target"
            v-model="targetObjectFilter"
            class="toolbar__control"
            type="search"
            :placeholder="copy.filters.targetPlaceholder"
          />
        </label>
      </div>
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

      <div v-if="filteredRows.length === 0" class="empty-state">
        {{ copy.empty }}
      </div>

      <div v-else class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th>{{ copy.table.time }}</th>
              <th>{{ copy.table.operator }}</th>
              <th>{{ copy.table.action }}</th>
              <th>{{ copy.table.targetObject }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.id">
              <td>{{ row.createdAt }}</td>
              <td class="primary-cell">{{ row.operatorName }}</td>
              <td>{{ row.actionLabel }}</td>
              <td>{{ row.targetObject }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchAuditLogs, type AuditLogItem } from '../../api/audit'
import { useI18nText } from '../../i18n'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
const { t, locale } = useI18nText()

const zhCopy = {
  hero: {
    title: '审计日志工作台',
    subtitle: '按时间、操作人和目标对象筛选系统轨迹，无需离开驾驶舱。'
  },
  metrics: {
    totalRecords: '记录总数',
    uniqueOperators: '操作人数',
    targetObjects: '目标对象',
    latestEvent: '最新事件'
  },
  filters: {
    title: '时间范围',
    from: '开始日期',
    to: '结束日期',
    operator: '操作人',
    device: '设备筛选',
    action: '操作筛选',
    targetObject: '目标对象',
    datePlaceholder: 'YYYY-MM-DD',
    operatorPlaceholder: '按操作人搜索',
    devicePlaceholder: '按设备或目标对象筛选',
    actionPlaceholder: '按操作筛选',
    targetPlaceholder: '按目标对象筛选'
  },
  loading: '正在加载审计记录...',
  error: {
    title: '审计日志暂不可用',
    fallback: '请重新加载审计日志。'
  },
  table: {
    title: '追踪记录',
    time: '时间',
    operator: '操作人',
    action: '操作',
    targetObject: '目标对象'
  },
  empty: '当前筛选条件下暂无审计记录。',
  systemTarget: '系统'
} as const

const enCopy = {
  hero: {
    title: 'Audit Log Workbench',
    subtitle: 'Filter the system trace by time, operator, and target object without leaving the cockpit shell.'
  },
  metrics: {
    totalRecords: 'Total Records',
    uniqueOperators: 'Unique Operators',
    targetObjects: 'Target Objects',
    latestEvent: 'Latest Event'
  },
  filters: {
    title: 'Time Range',
    from: 'From',
    to: 'To',
    operator: 'Operator',
    device: 'Device Filter',
    action: 'Action Filter',
    targetObject: 'Target Object',
    datePlaceholder: 'YYYY-MM-DD',
    operatorPlaceholder: 'Search by operator',
    devicePlaceholder: 'Filter by device or target',
    actionPlaceholder: 'Filter by action',
    targetPlaceholder: 'Filter by target object'
  },
  loading: 'Loading audit records...',
  error: {
    title: 'Audit Logs Unavailable',
    fallback: 'Try reloading the audit trail.'
  },
  table: {
    title: 'Trace Records',
    time: 'Time',
    operator: 'Operator',
    action: 'Action',
    targetObject: 'Target Object'
  },
  empty: 'No audit records match the current filters.',
  systemTarget: 'SYSTEM'
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

type AuditRow = AuditLogItem & {
  actionLabel: string
  targetObject: string
}

const loading = ref(false)
const logs = ref<AuditLogItem[]>([])
const errorMessage = ref('')
const fromTime = ref('')
const toTime = ref('')
const operatorFilter = ref('')
const deviceFilter = ref('')
const actionFilter = ref('')
const targetObjectFilter = ref('')

const rows = computed<AuditRow[]>(() =>
  logs.value.map((item) => {
    const { actionLabel, targetObject } = splitAction(item.action)
    return {
      ...item,
      actionLabel,
      targetObject
    }
  })
)

const filteredRows = computed(() => {
  const normalizedOperator = operatorFilter.value.trim().toLowerCase()
  const fromDate = normalizeCalendarDate(fromTime.value)
  const toDate = normalizeCalendarDate(toTime.value)
  const normalizedDevice = deviceFilter.value.trim().toLowerCase()
  const normalizedAction = actionFilter.value.trim().toLowerCase()
  const normalizedTargetObject = targetObjectFilter.value.trim().toLowerCase()

  return rows.value.filter((row) => {
    const rowDate = extractCalendarDate(row.createdAt)
    const matchesFrom = fromDate == null || (rowDate != null && rowDate >= fromDate)
    const matchesTo = toDate == null || (rowDate != null && rowDate <= toDate)
    const matchesOperator =
      normalizedOperator.length === 0 || row.operatorName.toLowerCase().includes(normalizedOperator)
    const matchesDevice =
      normalizedDevice.length === 0 ||
      `${row.targetObject} ${row.actionLabel}`.toLowerCase().includes(normalizedDevice)
    const matchesAction =
      normalizedAction.length === 0 || row.actionLabel.toLowerCase().includes(normalizedAction)
    const matchesTargetObject =
      normalizedTargetObject.length === 0 || row.targetObject.toLowerCase().includes(normalizedTargetObject)

    return matchesFrom && matchesTo && matchesOperator && matchesDevice && matchesAction && matchesTargetObject
  })
})

const metrics = computed(() => [
  { label: copy.value.metrics.totalRecords, value: rows.value.length },
  { label: copy.value.metrics.uniqueOperators, value: new Set(rows.value.map((row) => row.operatorName)).size },
  { label: copy.value.metrics.targetObjects, value: new Set(rows.value.map((row) => row.targetObject)).size },
  { label: copy.value.metrics.latestEvent, value: rows.value[0]?.createdAt ?? '-' }
])

const tableSummary = computed(() =>
  locale.value === 'zh-CN'
    ? `${filteredRows.value.length} / ${rows.value.length} 条记录`
    : `${filteredRows.value.length} visible rows of ${rows.value.length}`
)

onMounted(() => {
  void loadAuditLogs()
})

async function loadAuditLogs() {
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await fetchAuditLogs()
    logs.value = result.list
  } catch (error) {
    logs.value = []
    errorMessage.value = error instanceof Error && error.message.trim().length > 0
      ? error.message.trim()
      : copy.value.error.fallback
  } finally {
    loading.value = false
  }
}

function splitAction(action: string) {
  const trimmedAction = action.trim()
  const match = trimmedAction.match(/^(.*?)(?:\s+\((.+)\))?$/)

  return {
    actionLabel: match?.[1]?.trim() || trimmedAction || '-',
    targetObject: match?.[2]?.trim() || copy.value.systemTarget
  }
}

function normalizeCalendarDate(value: string) {
  const trimmedValue = value.trim()
  if (trimmedValue.length === 0) {
    return null
  }

  return /^\d{4}-\d{2}-\d{2}$/.test(trimmedValue) ? trimmedValue : null
}

function extractCalendarDate(value: string) {
  const match = value.trim().match(/^(\d{4}-\d{2}-\d{2})/)
  return match?.[1] ?? null
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
  grid-template-columns: repeat(6, minmax(0, 1fr));
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

.toolbar__control::placeholder {
  color: rgba(255, 255, 255, 0.44);
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

@media (max-width: 1080px) {
  .metrics,
  .toolbar {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .metrics,
  .toolbar {
    grid-template-columns: 1fr;
  }

  .table-card__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
