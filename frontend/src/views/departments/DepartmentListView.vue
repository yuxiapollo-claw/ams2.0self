<template>
  <section class="workbench">
    <PageHero
      :kicker="t('login.eyebrow')"
      :title="t('departments.hero.title')"
      :subtitle="t('departments.hero.subtitle')"
    >
      <template #actions>
        <button
          class="hero-action"
          data-testid="create-department"
          type="button"
          :disabled="initialLoading || interactionLocked"
          @click="openCreateDrawer"
        >
          {{ t('departments.actions.create') }}
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
      <div class="error-card__title">{{ t('departments.error.loadTitle') }}</div>
      <div class="error-card__body">{{ initialErrorMessage }}</div>
      <button
        class="inline-action"
        data-testid="retry-load"
        type="button"
        @click="retryLoadPage"
      >
        {{ t('departments.actions.retry') }}
      </button>
    </div>

    <template v-else>
      <FilterPanel :title="t('departments.filters.title')">
        <div class="toolbar">
          <label class="toolbar__field" for="department-keyword">
            <span>{{ t('departments.filters.search') }}</span>
            <input
              id="department-keyword"
              v-model="keyword"
              class="toolbar__control"
              name="keyword"
              type="search"
              :placeholder="t('departments.filters.searchPlaceholder')"
            />
          </label>

          <label class="toolbar__field" for="department-status-filter">
            <span>{{ t('departments.filters.status') }}</span>
            <select
              id="department-status-filter"
              v-model="statusFilter"
              class="toolbar__control"
              name="statusFilter"
            >
              <option value="">{{ t('departments.filters.allStatuses') }}</option>
              <option value="ENABLED">{{ t('departments.status.enabled') }}</option>
              <option value="DISABLED">{{ t('departments.status.disabled') }}</option>
            </select>
          </label>
        </div>
      </FilterPanel>

      <div v-if="pageErrorMessage" class="error-card error-card--inline" role="alert">
        <div class="error-card__title">{{ t('common.actionFailed') }}</div>
        <div class="error-card__body">{{ pageErrorMessage }}</div>
      </div>

      <section class="table-card">
        <header class="table-card__header">
          <div>
            <h2 class="table-card__title">{{ t('departments.table.title') }}</h2>
            <p class="table-card__meta">{{ filteredDepartments.length }} / {{ departments.length }} {{ t('departments.table.meta') }}</p>
          </div>
          <button
            class="inline-action"
            data-testid="refresh-departments"
            type="button"
            :disabled="refreshLocked || initialLoading"
            @click="refreshPage"
          >
            {{ refreshLoading ? t('common.refreshing') : t('departments.actions.refresh') }}
          </button>
        </header>

        <div v-if="filteredDepartments.length === 0" class="empty-state">
          {{ t('departments.empty') }}
        </div>

        <div v-else class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>{{ t('departments.table.department') }}</th>
                <th>{{ t('departments.table.manager') }}</th>
                <th>{{ t('departments.table.members') }}</th>
                <th>{{ t('departments.table.status') }}</th>
                <th>{{ t('departments.table.updated') }}</th>
                <th>{{ t('departments.table.description') }}</th>
                <th class="result-table__actions">{{ t('departments.table.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="department in filteredDepartments" :key="department.id">
                <td>
                  <div class="primary-cell">{{ department.departmentName }}</div>
                </td>
                <td>{{ formatManagerLabel(department) }}</td>
                <td>{{ department.memberCount ?? 0 }}</td>
                <td>
                  <span
                    class="status-pill"
                    :class="department.status === 'ENABLED' ? 'status-pill--good' : 'status-pill--danger'"
                  >
                    {{ formatStatus(department.status) }}
                  </span>
                </td>
                <td>{{ formatUpdatedAt(department.updatedAt) }}</td>
                <td>
                  <div class="secondary-cell">{{ department.description || t('departments.description.empty') }}</div>
                </td>
                <td class="result-table__actions">
                  <button
                    class="row-action"
                    :data-testid="`edit-${department.id}`"
                    type="button"
                    :disabled="isBusy(department.id)"
                    @click="openEditDrawer(department)"
                  >
                    {{ t('departments.actions.edit') }}
                  </button>
                  <button
                    class="row-action row-action--danger"
                    :data-testid="`delete-${department.id}`"
                    type="button"
                    :disabled="isBusy(department.id)"
                    @click="removeDepartment(department)"
                  >
                    {{ t('departments.actions.delete') }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <div v-if="drawerOpen" class="drawer-shell">
      <button class="drawer-shell__backdrop" type="button" aria-label="Close drawer" @click="closeDrawer" />
      <aside class="drawer-shell__panel" aria-label="Department editor">
        <DepartmentDrawerForm
          :mode="drawerMode"
          :initial-department="selectedDepartment"
          :manager-options="managerOptions"
          :submitting="drawerSubmitting || refreshLoading"
          :error-message="drawerErrorMessage"
          @close="closeDrawer"
          @submit="handleDrawerSubmit"
        />
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18nText } from '../../i18n'
import {
  createDepartment,
  deleteDepartment,
  fetchDepartments,
  updateDepartment,
  type DepartmentItem,
  type DepartmentMutationPayload
} from '../../api/departments'
import { fetchUsers, type UserItem } from '../../api/users'
import DepartmentDrawerForm from '../../components/departments/DepartmentDrawerForm.vue'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
const { t } = useI18nText()

const departments = ref<DepartmentItem[]>([])
const users = ref<UserItem[]>([])
const initialLoading = ref(true)
const initialErrorMessage = ref('')
const pageErrorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const drawerOpen = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const selectedDepartment = ref<DepartmentItem | null>(null)
const drawerSubmitting = ref(false)
const drawerErrorMessage = ref('')
const activeDepartmentId = ref('')
const refreshLoading = ref(false)
const pageLoadingMessage = ref(t('departments.loading.initial'))

const isMutating = computed(() => drawerSubmitting.value || activeDepartmentId.value.length > 0)
const interactionLocked = computed(() => isMutating.value || refreshLoading.value)
const refreshLocked = computed(() => interactionLocked.value || drawerOpen.value)

const managerOptions = computed(() =>
  users.value.map((user) => ({
    id: user.id,
    label: `${user.userName} · ${user.userCode}`
  }))
)

const filteredDepartments = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return departments.value.filter((department) => {
    const matchesKeyword =
      normalizedKeyword.length === 0 ||
      [department.departmentName, department.managerUserName ?? '', department.description ?? '']
        .some((value) => value.toLowerCase().includes(normalizedKeyword))

    const matchesStatus =
      statusFilter.value.length === 0 ||
      department.status === statusFilter.value

    return matchesKeyword && matchesStatus
  })
})

const metrics = computed(() => [
  { label: t('departments.metrics.total'), value: departments.value.length },
  { label: t('departments.metrics.enabled'), value: departments.value.filter((department) => department.status === 'ENABLED').length },
  {
    label: t('departments.metrics.assignedManagers'),
    value: departments.value.filter((department) => !!department.managerUserId).length
  },
  {
    label: t('departments.metrics.membersCovered'),
    value: departments.value.reduce((total, department) => total + (department.memberCount ?? 0), 0)
  }
])

onMounted(() => {
  void loadPage(true)
})

async function loadPage(force = false) {
  if (!force && (initialLoading.value || refreshLoading.value)) {
    return
  }

  initialLoading.value = true
  pageLoadingMessage.value = initialErrorMessage.value
    ? t('departments.loading.retry')
    : t('departments.loading.initial')
  initialErrorMessage.value = ''
  pageErrorMessage.value = ''

  try {
    await reloadPageData()
  } catch (error) {
    departments.value = []
    users.value = []
    initialErrorMessage.value = getErrorMessage(error, t('departments.error.initialFallback'))
  } finally {
    initialLoading.value = false
  }
}

async function refreshPage() {
  if (initialLoading.value || refreshLocked.value) {
    return
  }

  refreshLoading.value = true
  pageErrorMessage.value = ''

  try {
    await reloadPageData()
  } catch (error) {
    pageErrorMessage.value = getErrorMessage(error, t('departments.error.refreshFallback'))
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
  selectedDepartment.value = null
  drawerErrorMessage.value = ''
  drawerOpen.value = true
}

function openEditDrawer(department: DepartmentItem) {
  if (interactionLocked.value) {
    return
  }

  drawerMode.value = 'edit'
  selectedDepartment.value = department
  drawerErrorMessage.value = ''
  drawerOpen.value = true
}

function closeDrawer() {
  if (drawerSubmitting.value) {
    return
  }

  drawerOpen.value = false
  drawerErrorMessage.value = ''
}

async function handleDrawerSubmit(payload: DepartmentMutationPayload) {
  if (interactionLocked.value) {
    return
  }

  drawerSubmitting.value = true
  drawerErrorMessage.value = ''
  pageErrorMessage.value = ''

  try {
    let savedDepartment: DepartmentItem | null = null

    if (drawerMode.value === 'create') {
      savedDepartment = await createDepartment(payload)
    } else if (selectedDepartment.value) {
      savedDepartment = await updateDepartment(selectedDepartment.value.id, payload)
    }

    if (savedDepartment) {
      upsertDepartment(savedDepartment)
    }
    drawerOpen.value = false
    await refreshAfterMutation(t('departments.error.savedRefreshFollowUp'))
  } catch (error) {
    drawerErrorMessage.value = getErrorMessage(error, t('departments.error.saveFallback'))
  } finally {
    drawerSubmitting.value = false
  }
}

async function removeDepartment(department: DepartmentItem) {
  if (interactionLocked.value) {
    return
  }

  if (!window.confirm(t('departments.confirm.delete'))) {
    return
  }

  activeDepartmentId.value = department.id
  pageErrorMessage.value = ''

  try {
    await deleteDepartment(department.id)
    departments.value = departments.value.filter((item) => item.id !== department.id)
    await refreshAfterMutation(t('departments.error.deletedRefreshFollowUp'))
  } catch (error) {
    pageErrorMessage.value = getErrorMessage(error, t('departments.error.deleteFallback'))
  } finally {
    activeDepartmentId.value = ''
  }
}

function isBusy(departmentId: string) {
  return interactionLocked.value || activeDepartmentId.value === departmentId
}

function formatStatus(value: string) {
  return value === 'ENABLED' ? t('departments.status.enabled') : value === 'DISABLED' ? t('departments.status.disabled') : value
}

function formatManagerLabel(department: DepartmentItem) {
  if (department.managerUserName && department.managerUserName.trim().length > 0) {
    return department.managerUserName
  }

  if (department.managerUserId && department.managerUserId.trim().length > 0) {
    return t('departments.manager.previousUnavailable')
  }

  return t('departments.manager.unassigned')
}

function formatUpdatedAt(value?: string) {
  return value && value.trim().length > 0 ? value : '-'
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

async function reloadPageData() {
  const [loadedDepartments, loadedUsers] = await Promise.all([fetchDepartments(), fetchUsers()])
  departments.value = loadedDepartments
  users.value = loadedUsers
}

async function refreshAfterMutation(fallback: string) {
  refreshLoading.value = true

  try {
    await reloadPageData()
  } catch (error) {
    pageErrorMessage.value = getRefreshFollowUpMessage(error, fallback)
  } finally {
    refreshLoading.value = false
  }
}

function getRefreshFollowUpMessage(error: unknown, fallback: string) {
  const backendMessage = readBackendMessage(error)
  if (backendMessage) {
    return `${fallback} ${backendMessage}`
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return `${fallback} ${error.message.trim()}`
  }

  return fallback
}

function upsertDepartment(nextDepartment: DepartmentItem) {
  const index = departments.value.findIndex((department) => department.id === nextDepartment.id)

  if (index === -1) {
    departments.value = [...departments.value, nextDepartment]
    return
  }

  departments.value = departments.value.map((department) =>
    department.id === nextDepartment.id ? nextDepartment : department
  )
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
.row-action {
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
.row-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(220px, 0.8fr);
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
  width: 190px;
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
  max-width: 280px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.5;
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
  width: min(100%, 460px);
  border-left: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: -18px 0 48px rgba(0, 0, 0, 0.4);
}

@media (max-width: 1080px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .toolbar {
    grid-template-columns: 1fr;
  }

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
}
</style>
