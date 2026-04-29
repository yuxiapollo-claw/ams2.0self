<template>
  <section class="workbench">
    <PageHero :kicker="copy.kicker" :title="copy.title" :subtitle="copy.subtitle">
      <template #actions>
        <button
          class="hero-action"
          data-testid="create-application-config"
          type="button"
          :disabled="initialLoading || interactionLocked"
          @click="openCreateDrawer"
        >
          {{ copy.actions.create }}
        </button>
      </template>
    </PageHero>

    <div v-if="!initialLoading && !initialErrorMessage" class="metrics">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
      </article>
    </div>

    <div v-if="initialLoading" class="loading-card" aria-live="polite">
      {{ pageLoadingMessage }}
    </div>

    <div v-else-if="initialErrorMessage" class="error-card" role="alert">
      <div class="error-card__title">{{ copy.error.loadTitle }}</div>
      <div class="error-card__body">{{ initialErrorMessage }}</div>
      <button class="inline-action" data-testid="retry-load" type="button" @click="retryLoadPage">
        {{ copy.actions.retry }}
      </button>
    </div>

    <template v-else>
      <FilterPanel :title="copy.filters.title">
        <div class="toolbar">
          <label class="toolbar__field" for="application-config-keyword">
            <span>{{ copy.filters.search }}</span>
            <input
              id="application-config-keyword"
              v-model="keyword"
              class="toolbar__control"
              name="keyword"
              type="search"
              :placeholder="copy.filters.searchPlaceholder"
            />
          </label>

          <label class="toolbar__field" for="application-config-status">
            <span>{{ copy.filters.status }}</span>
            <select
              id="application-config-status"
              v-model="statusFilter"
              class="toolbar__control"
              name="statusFilter"
            >
              <option value="">{{ copy.filters.allStatuses }}</option>
              <option value="ENABLED">{{ copy.status.enabled }}</option>
              <option value="DISABLED">{{ copy.status.disabled }}</option>
            </select>
          </label>
        </div>
      </FilterPanel>

      <div v-if="pageErrorMessage" class="error-card error-card--inline" role="alert">
        <div class="error-card__title">{{ copy.error.actionTitle }}</div>
        <div class="error-card__body">{{ pageErrorMessage }}</div>
      </div>

      <section class="table-card">
        <header class="table-card__header">
          <div>
            <h2 class="table-card__title">{{ copy.table.title }}</h2>
            <p class="table-card__meta">{{ filteredItems.length }} / {{ items.length }} {{ copy.table.meta }}</p>
          </div>
          <button
            class="inline-action"
            data-testid="refresh-application-configs"
            type="button"
            :disabled="interactionLocked || initialLoading"
            @click="refreshPage"
          >
            {{ refreshLoading ? copy.actions.refreshing : copy.actions.refresh }}
          </button>
        </header>

        <div v-if="filteredItems.length === 0" class="empty-state">
          {{ copy.empty }}
        </div>

        <div v-else class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>{{ copy.table.applicationName }}</th>
                <th>{{ copy.table.applicationCode }}</th>
                <th>{{ copy.table.description }}</th>
                <th>{{ copy.table.status }}</th>
                <th>{{ copy.table.updatedAt }}</th>
                <th class="result-table__actions">{{ copy.table.actions }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredItems" :key="item.id">
                <td>
                  <div class="primary-cell">{{ item.applicationName }}</div>
                </td>
                <td>{{ item.applicationCode }}</td>
                <td>
                  <div class="secondary-cell">{{ item.description || copy.descriptionEmpty }}</div>
                </td>
                <td>
                  <span
                    class="status-pill"
                    :class="item.status === 'ENABLED' ? 'status-pill--good' : 'status-pill--danger'"
                  >
                    {{ formatStatus(item.status) }}
                  </span>
                </td>
                <td>{{ formatUpdatedAt(item.updatedAt) }}</td>
                <td class="result-table__actions">
                  <button
                    class="row-action"
                    :data-testid="`edit-${item.id}`"
                    type="button"
                    :disabled="isBusy(item.id)"
                    @click="openEditDrawer(item)"
                  >
                    {{ copy.actions.edit }}
                  </button>
                  <button
                    class="row-action row-action--danger"
                    :data-testid="`delete-${item.id}`"
                    type="button"
                    :disabled="isBusy(item.id)"
                    @click="removeItem(item)"
                  >
                    {{ copy.actions.delete }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <div v-if="drawerOpen" class="drawer-shell">
      <button class="drawer-shell__backdrop" type="button" :aria-label="copy.actions.close" @click="closeDrawer" />
      <aside class="drawer-shell__panel" :aria-label="drawerMode === 'create' ? copy.drawer.createTitle : copy.drawer.editTitle">
        <form class="drawer-card" @submit.prevent="handleDrawerSubmit">
          <header class="drawer-card__header">
            <div>
              <div class="drawer-card__eyebrow">{{ copy.kicker }}</div>
              <h2 class="drawer-card__title">{{ drawerMode === 'create' ? copy.drawer.createTitle : copy.drawer.editTitle }}</h2>
            </div>
            <button class="drawer-card__close" type="button" :disabled="drawerSubmitting" @click="closeDrawer">
              {{ copy.actions.close }}
            </button>
          </header>

          <div v-if="drawerErrorMessage" class="error-card error-card--inline" role="alert">
            <div class="error-card__title">{{ copy.error.actionTitle }}</div>
            <div class="error-card__body">{{ drawerErrorMessage }}</div>
          </div>

          <div class="drawer-card__body">
            <label class="form-field" for="application-name">
              <span>{{ copy.form.applicationName }}</span>
              <input id="application-name" v-model="form.applicationName" class="toolbar__control" name="applicationName" type="text" />
            </label>

            <label class="form-field" for="application-code">
              <span>{{ copy.form.applicationCode }}</span>
              <input id="application-code" v-model="form.applicationCode" class="toolbar__control" name="applicationCode" type="text" />
            </label>

            <label class="form-field" for="application-description">
              <span>{{ copy.form.description }}</span>
              <textarea
                id="application-description"
                v-model="form.description"
                class="toolbar__control toolbar__control--textarea"
                name="description"
              />
            </label>

            <label class="form-field" for="application-status">
              <span>{{ copy.form.status }}</span>
              <select id="application-status" v-model="form.status" class="toolbar__control" name="status">
                <option value="ENABLED">{{ copy.status.enabled }}</option>
                <option value="DISABLED">{{ copy.status.disabled }}</option>
              </select>
            </label>
          </div>

          <footer class="drawer-card__footer">
            <button class="row-action" type="button" :disabled="drawerSubmitting" @click="closeDrawer">
              {{ copy.actions.cancel }}
            </button>
            <button class="hero-action" type="submit" :disabled="drawerSubmitting || refreshLoading">
              {{ drawerSubmitting ? copy.actions.saving : copy.actions.save }}
            </button>
          </footer>
        </form>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  createApplicationConfig,
  deleteApplicationConfig,
  fetchApplicationConfigs,
  updateApplicationConfig,
  type ApplicationConfigItem,
  type ApplicationConfigMutationPayload
} from '../../api/admin-config'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
import { useI18nText } from '../../i18n'

interface Copy {
  kicker: string
  title: string
  subtitle: string
  empty: string
  descriptionEmpty: string
  filters: {
    title: string
    search: string
    searchPlaceholder: string
    status: string
    allStatuses: string
  }
  table: {
    title: string
    meta: string
    applicationName: string
    applicationCode: string
    description: string
    status: string
    updatedAt: string
    actions: string
  }
  form: {
    applicationName: string
    applicationCode: string
    description: string
    status: string
  }
  drawer: {
    createTitle: string
    editTitle: string
  }
  actions: {
    create: string
    edit: string
    delete: string
    save: string
    saving: string
    cancel: string
    close: string
    refresh: string
    refreshing: string
    retry: string
  }
  error: {
    loadTitle: string
    actionTitle: string
    initialFallback: string
    refreshFallback: string
    saveFallback: string
    deleteFallback: string
  }
  confirmDelete: string
  status: {
    enabled: string
    disabled: string
  }
  metrics: {
    total: string
    enabled: string
    disabled: string
    described: string
  }
  loading: {
    initial: string
    retry: string
  }
}

const zhCopy: Copy = {
  kicker: '系统管理',
  title: '应用配置',
  subtitle: '维护业务系统前缀、名称和启停状态。',
  empty: '当前筛选条件下没有匹配的应用配置。',
  descriptionEmpty: '暂无描述',
  filters: {
    title: '筛选条件',
    search: '关键字',
    searchPlaceholder: '搜索应用名称、前缀或描述',
    status: '状态',
    allStatuses: '全部状态'
  },
  table: {
    title: '应用配置列表',
    meta: '条可见记录',
    applicationName: '应用名称',
    applicationCode: '应用前缀',
    description: '说明',
    status: '状态',
    updatedAt: '更新时间',
    actions: '操作'
  },
  form: {
    applicationName: '应用名称',
    applicationCode: '应用前缀',
    description: '说明',
    status: '状态'
  },
  drawer: {
    createTitle: '新增应用配置',
    editTitle: '编辑应用配置'
  },
  actions: {
    create: '新增应用',
    edit: '编辑',
    delete: '删除',
    save: '保存',
    saving: '保存中...',
    cancel: '取消',
    close: '关闭',
    refresh: '刷新',
    refreshing: '刷新中...',
    retry: '重试'
  },
  error: {
    loadTitle: '加载应用配置失败',
    actionTitle: '操作失败',
    initialFallback: '应用配置加载失败，请稍后重试。',
    refreshFallback: '刷新应用配置失败，请稍后重试。',
    saveFallback: '保存应用配置失败，请检查输入后重试。',
    deleteFallback: '删除应用配置失败，请稍后重试。'
  },
  confirmDelete: '确认删除该应用配置吗？',
  status: {
    enabled: '启用',
    disabled: '停用'
  },
  metrics: {
    total: '应用总数',
    enabled: '启用中',
    disabled: '停用中',
    described: '已补充说明'
  },
  loading: {
    initial: '正在加载应用配置...',
    retry: '正在重新加载应用配置...'
  }
}

const enCopy: Copy = {
  kicker: 'System Admin',
  title: 'Application Configuration',
  subtitle: 'Maintain business system names, prefixes, and lifecycle status.',
  empty: 'No application configuration matches the current filters.',
  descriptionEmpty: 'No description',
  filters: {
    title: 'Filters',
    search: 'Keyword',
    searchPlaceholder: 'Search application name, prefix, or description',
    status: 'Status',
    allStatuses: 'All statuses'
  },
  table: {
    title: 'Application Config List',
    meta: 'visible records',
    applicationName: 'Application',
    applicationCode: 'Prefix',
    description: 'Description',
    status: 'Status',
    updatedAt: 'Updated At',
    actions: 'Actions'
  },
  form: {
    applicationName: 'Application Name',
    applicationCode: 'Application Prefix',
    description: 'Description',
    status: 'Status'
  },
  drawer: {
    createTitle: 'Create Application',
    editTitle: 'Edit Application'
  },
  actions: {
    create: 'Create Application',
    edit: 'Edit',
    delete: 'Delete',
    save: 'Save',
    saving: 'Saving...',
    cancel: 'Cancel',
    close: 'Close',
    refresh: 'Refresh',
    refreshing: 'Refreshing...',
    retry: 'Retry'
  },
  error: {
    loadTitle: 'Failed to load application configuration',
    actionTitle: 'Action failed',
    initialFallback: 'Failed to load application configuration. Try again later.',
    refreshFallback: 'Failed to refresh application configuration. Try again later.',
    saveFallback: 'Failed to save application configuration. Check the form and retry.',
    deleteFallback: 'Failed to delete application configuration. Try again later.'
  },
  confirmDelete: 'Delete this application configuration?',
  status: {
    enabled: 'Enabled',
    disabled: 'Disabled'
  },
  metrics: {
    total: 'Applications',
    enabled: 'Enabled',
    disabled: 'Disabled',
    described: 'With Description'
  },
  loading: {
    initial: 'Loading application configuration...',
    retry: 'Reloading application configuration...'
  }
}

const { locale } = useI18nText()

const items = ref<ApplicationConfigItem[]>([])
const initialLoading = ref(true)
const initialErrorMessage = ref('')
const pageErrorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const drawerOpen = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const selectedItem = ref<ApplicationConfigItem | null>(null)
const drawerSubmitting = ref(false)
const drawerErrorMessage = ref('')
const activeRowId = ref('')
const refreshLoading = ref(false)
const pageLoadingMessage = ref('')
const form = ref<ApplicationConfigMutationPayload>(emptyForm())

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))
const interactionLocked = computed(() => drawerSubmitting.value || refreshLoading.value || activeRowId.value.length > 0)
const filteredItems = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return items.value.filter((item) => {
    const matchesKeyword =
      normalizedKeyword.length === 0 ||
      [item.applicationName, item.applicationCode, item.description]
        .some((value) => value.toLowerCase().includes(normalizedKeyword))

    const matchesStatus = statusFilter.value.length === 0 || item.status === statusFilter.value
    return matchesKeyword && matchesStatus
  })
})

const metrics = computed(() => [
  { label: copy.value.metrics.total, value: items.value.length },
  { label: copy.value.metrics.enabled, value: items.value.filter((item) => item.status === 'ENABLED').length },
  { label: copy.value.metrics.disabled, value: items.value.filter((item) => item.status === 'DISABLED').length },
  { label: copy.value.metrics.described, value: items.value.filter((item) => item.description.trim().length > 0).length }
])

onMounted(() => {
  pageLoadingMessage.value = copy.value.loading.initial
  void loadPage(true)
})

async function loadPage(force = false) {
  if (!force && (initialLoading.value || refreshLoading.value)) {
    return
  }

  initialLoading.value = true
  pageLoadingMessage.value = initialErrorMessage.value ? copy.value.loading.retry : copy.value.loading.initial
  initialErrorMessage.value = ''
  pageErrorMessage.value = ''

  try {
    items.value = await fetchApplicationConfigs()
  } catch (error) {
    items.value = []
    initialErrorMessage.value = getErrorMessage(error, copy.value.error.initialFallback)
  } finally {
    initialLoading.value = false
  }
}

async function refreshPage() {
  if (initialLoading.value || interactionLocked.value) {
    return
  }

  refreshLoading.value = true
  pageErrorMessage.value = ''

  try {
    items.value = await fetchApplicationConfigs()
  } catch (error) {
    pageErrorMessage.value = getErrorMessage(error, copy.value.error.refreshFallback)
  } finally {
    refreshLoading.value = false
  }
}

function retryLoadPage() {
  void loadPage()
}

function openCreateDrawer() {
  if (interactionLocked.value) {
    return
  }

  drawerMode.value = 'create'
  selectedItem.value = null
  drawerErrorMessage.value = ''
  form.value = emptyForm()
  drawerOpen.value = true
}

function openEditDrawer(item: ApplicationConfigItem) {
  if (interactionLocked.value) {
    return
  }

  drawerMode.value = 'edit'
  selectedItem.value = item
  drawerErrorMessage.value = ''
  form.value = {
    applicationName: item.applicationName,
    applicationCode: item.applicationCode,
    description: item.description,
    status: item.status
  }
  drawerOpen.value = true
}

function closeDrawer() {
  if (drawerSubmitting.value) {
    return
  }

  drawerOpen.value = false
  drawerErrorMessage.value = ''
}

async function handleDrawerSubmit() {
  if (interactionLocked.value) {
    return
  }

  drawerSubmitting.value = true
  drawerErrorMessage.value = ''
  pageErrorMessage.value = ''

  const payload = normalizePayload(form.value)

  try {
    const savedItem =
      drawerMode.value === 'create'
        ? await createApplicationConfig(payload)
        : selectedItem.value
          ? await updateApplicationConfig(selectedItem.value.id, payload)
          : null

    if (savedItem) {
      upsertItem(savedItem)
    }
    drawerOpen.value = false
  } catch (error) {
    drawerErrorMessage.value = getErrorMessage(error, copy.value.error.saveFallback)
  } finally {
    drawerSubmitting.value = false
  }
}

async function removeItem(item: ApplicationConfigItem) {
  if (interactionLocked.value) {
    return
  }

  if (!window.confirm(copy.value.confirmDelete)) {
    return
  }

  activeRowId.value = item.id
  pageErrorMessage.value = ''

  try {
    await deleteApplicationConfig(item.id)
    items.value = items.value.filter((current) => current.id !== item.id)
  } catch (error) {
    pageErrorMessage.value = getErrorMessage(error, copy.value.error.deleteFallback)
  } finally {
    activeRowId.value = ''
  }
}

function isBusy(itemId: string) {
  return interactionLocked.value || activeRowId.value === itemId
}

function formatStatus(value: string) {
  return value === 'ENABLED' ? copy.value.status.enabled : value === 'DISABLED' ? copy.value.status.disabled : value
}

function formatUpdatedAt(value: string) {
  return value.trim().length > 0 ? value : '-'
}

function normalizePayload(payload: ApplicationConfigMutationPayload): ApplicationConfigMutationPayload {
  return {
    applicationName: payload.applicationName.trim(),
    applicationCode: payload.applicationCode.trim(),
    description: payload.description.trim(),
    status: payload.status
  }
}

function upsertItem(nextItem: ApplicationConfigItem) {
  const index = items.value.findIndex((item) => item.id === nextItem.id)
  if (index === -1) {
    items.value = [...items.value, nextItem]
    return
  }
  items.value = items.value.map((item) => (item.id === nextItem.id ? nextItem : item))
}

function emptyForm(): ApplicationConfigMutationPayload {
  return {
    applicationName: '',
    applicationCode: '',
    description: '',
    status: 'ENABLED'
  }
}

function getErrorMessage(error: unknown, fallback: string) {
  const backendMessage = readBackendMessage(error)
  if (backendMessage) {
    return backendMessage
  }
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }
  return fallback
}

function readBackendMessage(error: unknown) {
  if (!error || typeof error !== 'object' || Array.isArray(error)) {
    return ''
  }

  const response = 'response' in error ? error.response : undefined
  if (!response || typeof response !== 'object' || Array.isArray(response)) {
    return ''
  }

  const data = 'data' in response ? response.data : undefined
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return ''
  }

  const message = 'message' in data ? data.message : undefined
  return typeof message === 'string' ? message.trim() : ''
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

.hero-action,
.inline-action,
.row-action,
.drawer-card__close {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.16);
  color: var(--cockpit-text);
  font: inherit;
  cursor: pointer;
}

.hero-action {
  min-height: 44px;
  background: linear-gradient(135deg, rgba(94, 234, 212, 0.95), rgba(96, 165, 250, 0.95));
  color: #041322;
  border-color: transparent;
  font-weight: 800;
}

.hero-action:disabled,
.inline-action:disabled,
.row-action:disabled,
.drawer-card__close:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(220px, 0.8fr);
  gap: 14px;
}

.toolbar__field,
.form-field {
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

.toolbar__control--textarea {
  min-height: 120px;
  resize: vertical;
}

.loading-card,
.error-card,
.table-card,
.drawer-card {
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

.error-card--inline {
  background: rgba(251, 113, 133, 0.08);
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
  min-width: 980px;
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

.result-table__actions {
  width: 180px;
}

.result-table td.result-table__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.primary-cell {
  font-weight: 800;
}

.secondary-cell {
  max-width: 320px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.5;
  white-space: normal;
}

.row-action--danger {
  border-color: rgba(251, 113, 133, 0.28);
  color: rgba(255, 196, 206, 0.98);
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

.status-pill--danger {
  border-color: rgba(251, 113, 133, 0.28);
  background: rgba(251, 113, 133, 0.14);
}

.drawer-shell {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  justify-content: flex-end;
}

.drawer-shell__backdrop {
  flex: 1;
  border: 0;
  background: rgba(7, 10, 20, 0.58);
  cursor: pointer;
}

.drawer-shell__panel {
  width: min(100%, 480px);
  border-left: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: -18px 0 48px rgba(0, 0, 0, 0.4);
}

.drawer-card {
  height: 100%;
  border-radius: 0;
  background: rgba(10, 14, 28, 0.96);
  display: grid;
  grid-template-rows: auto 1fr auto;
}

.drawer-card__header,
.drawer-card__footer {
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.drawer-card__header {
  border-bottom: 1px solid var(--cockpit-border);
}

.drawer-card__footer {
  border-top: 1px solid var(--cockpit-border);
}

.drawer-card__eyebrow {
  font-size: 12px;
  color: var(--cockpit-muted);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.drawer-card__title {
  margin: 6px 0 0;
  font-size: 18px;
  font-weight: 900;
}

.drawer-card__body {
  padding: 18px;
  display: grid;
  gap: 14px;
  align-content: start;
  overflow-y: auto;
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

  .drawer-shell {
    align-items: flex-end;
  }

  .drawer-shell__backdrop {
    display: none;
  }

  .drawer-shell__panel {
    width: 100%;
    max-height: min(88dvh, 780px);
    border-left: 0;
    border-top: 1px solid rgba(255, 255, 255, 0.14);
    border-top-left-radius: 22px;
    border-top-right-radius: 22px;
    overflow: hidden;
  }

  .drawer-card {
    border-radius: 22px 22px 0 0;
  }
}
</style>
