<template>
  <section class="workbench">
    <PageHero
      :kicker="t('login.eyebrow')"
      :title="t('users.hero.title')"
      :subtitle="t('users.hero.subtitle')"
    >
      <template #actions>
        <button
          class="hero-action"
          data-testid="create-user"
          type="button"
          :disabled="initialLoading || interactionLocked"
          @click="openCreateDrawer"
        >
          {{ t('users.actions.create') }}
        </button>
      </template>
    </PageHero>

    <div class="metrics" v-if="!initialLoading && !initialErrorMessage">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
      </article>
    </div>

    <div v-if="initialLoading" class="loading-card" aria-live="polite">
      {{ pageLoadingMessage }}
    </div>

    <div v-else-if="initialErrorMessage" class="error-card" role="alert">
      <div class="error-card__title">{{ t('users.error.loadTitle') }}</div>
      <div class="error-card__body">{{ initialErrorMessage }}</div>
      <button
        class="inline-action"
        data-testid="retry-load"
        type="button"
        @click="retryLoadPage"
      >
        {{ t('users.actions.retry') }}
      </button>
    </div>

    <template v-else>
      <FilterPanel :title="t('users.filters.title')">
        <div class="toolbar">
          <label class="toolbar__field" for="user-keyword">
            <span>{{ t('users.filters.search') }}</span>
            <input
              id="user-keyword"
              v-model="keyword"
              class="toolbar__control"
              name="keyword"
              type="search"
              :placeholder="t('users.filters.searchPlaceholder')"
            />
          </label>

          <label class="toolbar__field" for="account-status-filter">
            <span>{{ t('users.filters.accountStatus') }}</span>
            <select
              id="account-status-filter"
              v-model="accountStatusFilter"
              class="toolbar__control"
              name="accountStatusFilter"
            >
              <option value="">{{ t('users.filters.allStatuses') }}</option>
              <option value="ENABLED">{{ t('users.status.enabled') }}</option>
              <option value="DISABLED">{{ t('users.status.disabled') }}</option>
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
            <h2 class="table-card__title">{{ t('users.table.title') }}</h2>
            <p class="table-card__meta">{{ filteredUsers.length }} / {{ users.length }} {{ t('users.table.meta') }}</p>
          </div>
          <button
            class="inline-action"
            data-testid="refresh-users"
            type="button"
            :disabled="interactionLocked || initialLoading"
            @click="loadUsers"
          >
            {{ refreshLoading ? t('common.refreshing') : t('users.actions.refresh') }}
          </button>
        </header>

        <div v-if="filteredUsers.length === 0" class="empty-state">
          {{ t('users.empty') }}
        </div>

        <div v-else class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>{{ t('users.table.userCode') }}</th>
                <th>{{ t('users.table.userName') }}</th>
                <th>{{ t('users.table.loginName') }}</th>
                <th>{{ t('users.table.department') }}</th>
                <th>{{ t('users.table.employment') }}</th>
                <th>{{ t('users.table.account') }}</th>
                <th class="result-table__actions">{{ t('users.table.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in filteredUsers" :key="user.id">
                <td>{{ user.userCode }}</td>
                <td>{{ user.userName }}</td>
                <td>{{ user.loginName }}</td>
                <td>{{ user.departmentName }}</td>
                <td>
                  <span class="status-pill status-pill--neutral">{{ formatEmploymentStatus(user.employmentStatus) }}</span>
                </td>
                <td>
                  <span
                    class="status-pill"
                    :class="user.accountStatus === 'ENABLED' ? 'status-pill--good' : 'status-pill--danger'"
                  >
                    {{ formatAccountStatus(user.accountStatus) }}
                  </span>
                </td>
                <td class="result-table__actions">
                  <button
                    class="row-action"
                    :data-testid="`edit-${user.id}`"
                    type="button"
                    :disabled="isBusy(user.id)"
                    @click="openEditDrawer(user)"
                  >
                    {{ t('users.actions.edit') }}
                  </button>
                  <button
                    class="row-action"
                    :data-testid="`status-${user.id}`"
                    type="button"
                    :disabled="isBusy(user.id)"
                    @click="toggleAccountStatus(user)"
                  >
                    {{ user.accountStatus === 'ENABLED' ? t('users.actions.disable') : t('users.actions.enable') }}
                  </button>
                  <button
                    class="row-action row-action--danger"
                    :data-testid="`delete-${user.id}`"
                    type="button"
                    :disabled="isBusy(user.id)"
                    @click="removeUser(user)"
                  >
                    {{ t('users.actions.delete') }}
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
      <aside class="drawer-shell__panel" aria-label="User editor">
        <UserDrawerForm
          :mode="drawerMode"
          :initial-user="selectedUser"
          :department-options="departmentOptions"
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
import { fetchDepartments, type DepartmentItem } from '../../api/departments'
import { useI18nText } from '../../i18n'
import {
  createUser,
  deleteUser,
  fetchUsers,
  updateUser,
  updateUserStatus,
  type UserItem,
  type UserMutationPayload
} from '../../api/users'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
import UserDrawerForm from '../../components/users/UserDrawerForm.vue'
const { t } = useI18nText()

const users = ref<UserItem[]>([])
const departments = ref<DepartmentItem[]>([])
const initialLoading = ref(true)
const initialErrorMessage = ref('')
const pageErrorMessage = ref('')
const keyword = ref('')
const accountStatusFilter = ref('')
const drawerOpen = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const selectedUser = ref<UserItem | null>(null)
const drawerSubmitting = ref(false)
const drawerErrorMessage = ref('')
const activeRowId = ref('')
const refreshLoading = ref(false)
const pageLoadingMessage = ref(t('users.loading.initial'))

const isMutating = computed(() => drawerSubmitting.value || activeRowId.value.length > 0)
const interactionLocked = computed(() => isMutating.value || refreshLoading.value)

const departmentOptions = computed(() =>
  departments.value
    .filter((department) => ['ACTIVE', 'ENABLED'].includes(department.status))
    .map((department) => ({
      id: department.id,
      departmentName: department.departmentName
    }))
)

const filteredUsers = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return users.value.filter((user) => {
    const matchesKeyword =
      normalizedKeyword.length === 0 ||
      [user.userCode, user.userName, user.loginName, user.departmentName]
        .some((value) => value.toLowerCase().includes(normalizedKeyword))

    const matchesStatus =
      accountStatusFilter.value.length === 0 ||
      user.accountStatus === accountStatusFilter.value

    return matchesKeyword && matchesStatus
  })
})

const metrics = computed(() => [
  { label: t('users.metrics.totalUsers'), value: users.value.length },
  { label: t('users.metrics.enabledAccounts'), value: users.value.filter((user) => user.accountStatus === 'ENABLED').length },
  { label: t('users.metrics.activeEmployees'), value: users.value.filter((user) => user.employmentStatus === 'ACTIVE').length },
  { label: t('users.metrics.departmentsCovered'), value: new Set(users.value.map((user) => user.departmentId)).size }
])

onMounted(() => {
  void loadPage(true)
})

async function loadPage(force = false) {
  if (!force && (initialLoading.value || refreshLoading.value)) {
    return
  }

  initialLoading.value = true
  pageLoadingMessage.value = initialErrorMessage.value ? t('users.loading.retry') : t('users.loading.initial')
  initialErrorMessage.value = ''
  pageErrorMessage.value = ''

  try {
    const [loadedUsers, loadedDepartments] = await Promise.all([fetchUsers(), fetchDepartments()])
    users.value = loadedUsers
    departments.value = loadedDepartments
  } catch (error) {
    users.value = []
    departments.value = []
    initialErrorMessage.value = getErrorMessage(error, t('users.error.initialFallback'))
  } finally {
    initialLoading.value = false
  }
}

async function loadUsers() {
  if (initialLoading.value || refreshLoading.value) {
    return
  }

  refreshLoading.value = true
  pageErrorMessage.value = ''

  try {
    users.value = await fetchUsers()
  } catch (error) {
    pageErrorMessage.value = getErrorMessage(error, t('users.error.refreshFallback'))
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
  selectedUser.value = null
  drawerErrorMessage.value = ''
  drawerOpen.value = true
}

function openEditDrawer(user: UserItem) {
  if (interactionLocked.value) {
    return
  }

  drawerMode.value = 'edit'
  selectedUser.value = user
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

async function handleDrawerSubmit(payload: UserMutationPayload) {
  if (interactionLocked.value) {
    return
  }

  drawerSubmitting.value = true
  drawerErrorMessage.value = ''
  pageErrorMessage.value = ''

  try {
    let savedUser: UserItem | null = null

    if (drawerMode.value === 'create') {
      savedUser = await createUser(payload)
    } else if (selectedUser.value) {
      savedUser = await updateUser(selectedUser.value.id, payload)
    }

    if (savedUser) {
      upsertUser(savedUser)
    }
    drawerOpen.value = false
  } catch (error) {
    drawerErrorMessage.value = getErrorMessage(error, t('users.error.saveFallback'))
  } finally {
    drawerSubmitting.value = false
  }
}

async function toggleAccountStatus(user: UserItem) {
  if (interactionLocked.value) {
    return
  }

  activeRowId.value = user.id
  pageErrorMessage.value = ''

  try {
    const updatedUser = await updateUserStatus(user.id, {
      accountStatus: user.accountStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    })
    upsertUser(updatedUser)
  } catch (error) {
    pageErrorMessage.value = getErrorMessage(error, t('users.error.statusFallback'))
  } finally {
    activeRowId.value = ''
  }
}

async function removeUser(user: UserItem) {
  if (interactionLocked.value) {
    return
  }

  if (!window.confirm(t('users.confirm.delete'))) {
    return
  }

  activeRowId.value = user.id
  pageErrorMessage.value = ''

  try {
    await deleteUser(user.id)
    users.value = users.value.filter((item) => item.id !== user.id)
  } catch (error) {
    pageErrorMessage.value = getErrorMessage(error, t('users.error.deleteFallback'))
  } finally {
    activeRowId.value = ''
  }
}

function isBusy(userId: string) {
  return interactionLocked.value
}

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

function upsertUser(nextUser: UserItem) {
  const index = users.value.findIndex((user) => user.id === nextUser.id)

  if (index === -1) {
    users.value = [...users.value, nextUser]
    return
  }

  users.value = users.value.map((user) => (user.id === nextUser.id ? nextUser : user))
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
  min-width: 820px;
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

.result-table__actions {
  width: 240px;
}

.result-table td.result-table__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.status-pill--neutral {
  background: rgba(96, 165, 250, 0.14);
  border-color: rgba(96, 165, 250, 0.24);
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
