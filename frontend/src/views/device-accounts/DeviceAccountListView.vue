<template>
  <section class="workbench">
    <PageHero
      :kicker="t('login.eyebrow')"
      :title="t('deviceAccounts.hero.title')"
      :subtitle="t('deviceAccounts.hero.subtitle')"
    >
      <template #actions>
        <button
          class="hero-action"
          data-testid="create-device-account"
          type="button"
          :disabled="initialLoading || interactionLocked"
          @click="openCreateDrawer"
        >
          {{ t('deviceAccounts.actions.create') }}
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
      <div class="error-card__title">{{ t('deviceAccounts.error.loadTitle') }}</div>
      <div class="error-card__body">{{ initialErrorMessage }}</div>
      <button
        class="inline-action"
        data-testid="retry-load"
        type="button"
        @click="retryLoadPage"
      >
        {{ t('deviceAccounts.actions.retry') }}
      </button>
    </div>

    <template v-else>
      <FilterPanel :title="t('deviceAccounts.filters.title')">
        <div class="toolbar">
          <label class="toolbar__field" for="device-account-keyword">
            <span>{{ t('deviceAccounts.filters.search') }}</span>
            <input
              id="device-account-keyword"
              v-model="keyword"
              class="toolbar__control"
              name="keyword"
              type="search"
              :placeholder="t('deviceAccounts.filters.searchPlaceholder')"
            />
          </label>

          <label class="toolbar__field" for="device-account-binding-filter">
            <span>{{ t('deviceAccounts.filters.bindingStatus') }}</span>
            <select
              id="device-account-binding-filter"
              v-model="bindingFilter"
              class="toolbar__control"
              name="bindingFilter"
            >
              <option value="">{{ t('deviceAccounts.filters.allBindings') }}</option>
              <option value="BOUND">{{ t('deviceAccounts.filters.bound') }}</option>
              <option value="UNBOUND">{{ t('deviceAccounts.filters.unbound') }}</option>
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
            <h2 class="table-card__title">{{ t('deviceAccounts.table.title') }}</h2>
            <p class="table-card__meta">
              {{ filteredDeviceAccounts.length }} / {{ deviceAccounts.length }} {{ t('deviceAccounts.table.meta') }}
            </p>
          </div>
          <button
            class="inline-action"
            data-testid="refresh-device-accounts"
            type="button"
            :disabled="refreshLocked || initialLoading"
            @click="refreshPage"
          >
            {{ refreshLoading ? t('common.refreshing') : t('deviceAccounts.actions.refresh') }}
          </button>
        </header>

        <div v-if="filteredDeviceAccounts.length === 0" class="empty-state">
          {{ t('deviceAccounts.empty') }}
        </div>

        <div v-else class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>{{ t('deviceAccounts.table.device') }}</th>
                <th>{{ t('deviceAccounts.table.account') }}</th>
                <th>{{ t('deviceAccounts.table.boundUser') }}</th>
                <th>{{ t('deviceAccounts.table.roles') }}</th>
                <th>{{ t('deviceAccounts.table.status') }}</th>
                <th>{{ t('deviceAccounts.table.source') }}</th>
                <th>{{ t('deviceAccounts.table.remark') }}</th>
                <th class="result-table__actions">{{ t('deviceAccounts.table.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="deviceAccount in filteredDeviceAccounts" :key="deviceAccount.id">
                <td>{{ deviceAccount.deviceName }}</td>
                <td>
                  <div class="primary-cell">{{ deviceAccount.accountName }}</div>
                </td>
                <td>{{ formatBindingLabel(deviceAccount) }}</td>
                <td>
                  <div class="secondary-cell">{{ formatRoles(deviceAccount.roles) }}</div>
                </td>
                <td>
                  <span
                    class="status-pill"
                    :class="deviceAccount.accountStatus === 'ENABLED' ? 'status-pill--good' : 'status-pill--danger'"
                  >
                    {{ formatStatus(deviceAccount.accountStatus) }}
                  </span>
                </td>
                <td>{{ formatSourceType(deviceAccount.sourceType) }}</td>
                <td>
                  <div class="secondary-cell">{{ deviceAccount.remark || t('deviceAccounts.remark.empty') }}</div>
                </td>
                <td class="result-table__actions">
                  <button
                    class="row-action"
                    :data-testid="`edit-${deviceAccount.id}`"
                    type="button"
                    :disabled="isBusy(deviceAccount.id)"
                    @click="openEditDrawer(deviceAccount)"
                  >
                    {{ t('deviceAccounts.actions.edit') }}
                  </button>
                  <button
                    class="row-action"
                    :data-testid="`action-request-role-add-${deviceAccount.id}`"
                    type="button"
                    :disabled="isBusy(deviceAccount.id) || !deviceAccount.userId"
                    @click="goToRequest(deviceAccount)"
                  >
                    {{ t('deviceAccounts.actions.requestRoleAdd') }}
                  </button>
                  <button
                    class="row-action row-action--danger"
                    :data-testid="`delete-${deviceAccount.id}`"
                    type="button"
                    :disabled="isBusy(deviceAccount.id)"
                    @click="removeDeviceAccount(deviceAccount)"
                  >
                    {{ t('deviceAccounts.actions.delete') }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <div v-if="drawerOpen" class="drawer-shell">
      <button class="drawer-shell__backdrop" type="button" :aria-label="t('common.close')" @click="closeDrawer" />
      <aside
        class="drawer-shell__panel"
        :aria-label="drawerMode === 'create' ? t('deviceAccounts.drawer.titleCreate') : t('deviceAccounts.drawer.titleEdit')"
      >
        <DeviceAccountDrawerForm
          :mode="drawerMode"
          :initial-device-account="selectedDeviceAccount"
          :device-options="deviceOptions"
          :user-options="userOptions"
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
import { useRouter } from 'vue-router'
import { fetchAssetTree, type AssetNode } from '../../api/assets'
import {
  createDeviceAccount,
  deleteDeviceAccount,
  fetchDeviceAccounts,
  updateDeviceAccount,
  type DeviceAccountItem,
  type DeviceAccountMutationPayload
} from '../../api/device-accounts'
import { useI18nText } from '../../i18n'
import { fetchUsers, type UserItem } from '../../api/users'
import DeviceAccountDrawerForm from '../../components/device-accounts/DeviceAccountDrawerForm.vue'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
const { t } = useI18nText()

const router = useRouter()
const deviceAccounts = ref<DeviceAccountItem[]>([])
const users = ref<UserItem[]>([])
const assetTree = ref<AssetNode[]>([])
const initialLoading = ref(true)
const initialErrorMessage = ref('')
const pageErrorMessage = ref('')
const keyword = ref('')
const bindingFilter = ref('')
const drawerOpen = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const selectedDeviceAccount = ref<DeviceAccountItem | null>(null)
const drawerSubmitting = ref(false)
const drawerErrorMessage = ref('')
const activeDeviceAccountId = ref('')
const refreshLoading = ref(false)
const pageLoadingMessage = ref(t('deviceAccounts.loading.initial'))

const isMutating = computed(() => drawerSubmitting.value || activeDeviceAccountId.value.length > 0)
const interactionLocked = computed(() => isMutating.value || refreshLoading.value)
const refreshLocked = computed(() => interactionLocked.value || drawerOpen.value)

const deviceOptions = computed(() =>
  collectDevices(assetTree.value).map((device) => ({
    id: device.id,
    label: device.name
  }))
)

const userOptions = computed(() =>
  users.value.map((user) => ({
    id: user.id,
    label: `${user.userName} · ${user.userCode}`
  }))
)

const filteredDeviceAccounts = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return deviceAccounts.value.filter((deviceAccount) => {
    const matchesKeyword =
      normalizedKeyword.length === 0 ||
      [
        deviceAccount.deviceName,
        deviceAccount.accountName,
        deviceAccount.userName ?? '',
        deviceAccount.remark,
        deviceAccount.roles.join(' ')
      ]
        .some((value) => value.toLowerCase().includes(normalizedKeyword))

    const matchesBinding =
      bindingFilter.value.length === 0 ||
      (bindingFilter.value === 'BOUND' && !!deviceAccount.userId) ||
      (bindingFilter.value === 'UNBOUND' && !deviceAccount.userId)

    return matchesKeyword && matchesBinding
  })
})

const metrics = computed(() => [
  { label: t('deviceAccounts.metrics.totalAccounts'), value: deviceAccounts.value.length },
  { label: t('deviceAccounts.metrics.boundUsers'), value: deviceAccounts.value.filter((account) => !!account.userId).length },
  { label: t('deviceAccounts.metrics.unboundAccounts'), value: deviceAccounts.value.filter((account) => !account.userId).length },
  { label: t('deviceAccounts.metrics.withoutRoles'), value: deviceAccounts.value.filter((account) => account.roles.length === 0).length }
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
    ? t('deviceAccounts.loading.retry')
    : t('deviceAccounts.loading.initial')
  initialErrorMessage.value = ''
  pageErrorMessage.value = ''

  try {
    await reloadPageData()
  } catch (error) {
    deviceAccounts.value = []
    users.value = []
    assetTree.value = []
    initialErrorMessage.value = getErrorMessage(error, t('deviceAccounts.error.initialFallback'))
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
    pageErrorMessage.value = getErrorMessage(error, t('deviceAccounts.error.refreshFallback'))
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
  selectedDeviceAccount.value = null
  drawerErrorMessage.value = ''
  drawerOpen.value = true
}

function openEditDrawer(deviceAccount: DeviceAccountItem) {
  if (interactionLocked.value) {
    return
  }

  drawerMode.value = 'edit'
  selectedDeviceAccount.value = deviceAccount
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

async function handleDrawerSubmit(payload: DeviceAccountMutationPayload) {
  if (interactionLocked.value) {
    return
  }

  drawerSubmitting.value = true
  drawerErrorMessage.value = ''
  pageErrorMessage.value = ''

  try {
    let savedDeviceAccount: DeviceAccountItem | null = null

    if (drawerMode.value === 'create') {
      savedDeviceAccount = await createDeviceAccount(payload)
    } else if (selectedDeviceAccount.value) {
      savedDeviceAccount = await updateDeviceAccount(selectedDeviceAccount.value.id, payload)
    }

    if (savedDeviceAccount) {
      upsertDeviceAccount(savedDeviceAccount)
    }
    drawerOpen.value = false
    await refreshAfterMutation(t('deviceAccounts.error.savedRefreshFollowUp'))
  } catch (error) {
    drawerErrorMessage.value = getErrorMessage(error, t('deviceAccounts.error.saveFallback'))
  } finally {
    drawerSubmitting.value = false
  }
}

async function removeDeviceAccount(deviceAccount: DeviceAccountItem) {
  if (interactionLocked.value) {
    return
  }

  if (!window.confirm(t('deviceAccounts.confirm.delete'))) {
    return
  }

  activeDeviceAccountId.value = deviceAccount.id
  pageErrorMessage.value = ''

  try {
    await deleteDeviceAccount(deviceAccount.id)
    deviceAccounts.value = deviceAccounts.value.filter((item) => item.id !== deviceAccount.id)
    await refreshAfterMutation(t('deviceAccounts.error.deletedRefreshFollowUp'))
  } catch (error) {
    pageErrorMessage.value = getErrorMessage(error, t('deviceAccounts.error.deleteFallback'))
  } finally {
    activeDeviceAccountId.value = ''
  }
}

function goToRequest(deviceAccount: DeviceAccountItem) {
  if (interactionLocked.value || !deviceAccount.userId) {
    return
  }

  void router.push({
    name: 'request-form',
    query: {
      requestType: 'ROLE_ADD',
      targetUserId: deviceAccount.userId,
      targetDeviceNodeId: String(deviceAccount.deviceNodeId),
      targetAccountName: deviceAccount.accountName
    }
  })
}

function isBusy(deviceAccountId: string) {
  return interactionLocked.value || activeDeviceAccountId.value === deviceAccountId
}

function formatStatus(value: string) {
  return value === 'ENABLED'
    ? t('deviceAccounts.status.enabled')
    : value === 'DISABLED'
      ? t('deviceAccounts.status.disabled')
      : value
}

function formatSourceType(value: string) {
  return value === 'MANUAL'
    ? t('deviceAccounts.source.manual')
    : value === 'IMPORTED'
      ? t('deviceAccounts.source.imported')
      : value
}

function formatBindingLabel(deviceAccount: DeviceAccountItem) {
  return deviceAccount.userName && deviceAccount.userName.trim().length > 0
    ? deviceAccount.userName
    : t('deviceAccounts.binding.unbound')
}

function formatRoles(roles: string[]) {
  return roles.length > 0 ? roles.join(' / ') : t('deviceAccounts.roles.none')
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
  const [loadedDeviceAccounts, loadedUsers, loadedAssetTree] = await Promise.all([
    fetchDeviceAccounts(),
    fetchUsers(),
    fetchAssetTree()
  ])
  deviceAccounts.value = loadedDeviceAccounts
  users.value = loadedUsers
  assetTree.value = loadedAssetTree
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

function upsertDeviceAccount(nextDeviceAccount: DeviceAccountItem) {
  const index = deviceAccounts.value.findIndex((deviceAccount) => deviceAccount.id === nextDeviceAccount.id)

  if (index === -1) {
    deviceAccounts.value = [...deviceAccounts.value, nextDeviceAccount]
    return
  }

  deviceAccounts.value = deviceAccounts.value.map((deviceAccount) =>
    deviceAccount.id === nextDeviceAccount.id ? nextDeviceAccount : deviceAccount
  )
}

function collectDevices(nodes: AssetNode[]): AssetNode[] {
  return nodes.flatMap((node) => {
    const matches = node.type === 'DEVICE' ? [node] : []
    return matches.concat(collectDevices(node.children))
  })
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

.row-action--danger {
  border-color: rgba(248, 113, 113, 0.32);
  color: rgba(254, 202, 202, 0.94);
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
  background: rgba(255, 255, 255, 0.05);
  box-shadow: var(--cockpit-shadow);
}

.loading-card,
.error-card {
  padding: 18px;
}

.error-card__title {
  font-weight: 800;
}

.error-card__body {
  margin-top: 8px;
  color: var(--cockpit-muted);
}

.error-card--inline {
  border-color: rgba(248, 113, 113, 0.28);
  background: rgba(127, 29, 29, 0.22);
}

.table-card {
  overflow: hidden;
}

.table-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--cockpit-border);
}

.table-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 900;
}

.table-card__meta {
  margin: 6px 0 0;
  color: var(--cockpit-muted);
  font-size: 13px;
}

.empty-state {
  padding: 26px 20px;
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
  padding: 14px 20px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  vertical-align: top;
}

.result-table th {
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.result-table__actions {
  width: 280px;
}

.primary-cell {
  font-weight: 700;
}

.secondary-cell {
  color: var(--cockpit-muted);
  line-height: 1.5;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.08);
}

.status-pill--good {
  background: rgba(34, 197, 94, 0.18);
  color: rgba(187, 247, 208, 0.96);
}

.status-pill--danger {
  background: rgba(248, 113, 113, 0.18);
  color: rgba(254, 202, 202, 0.96);
}

.drawer-shell {
  position: fixed;
  inset: 0;
  display: flex;
  justify-content: flex-end;
  z-index: 60;
}

.drawer-shell__backdrop {
  flex: 1;
  border: 0;
  background: rgba(2, 6, 23, 0.48);
}

.drawer-shell__panel {
  position: relative;
  width: min(520px, 100%);
  height: 100%;
  box-shadow: -24px 0 60px rgba(2, 6, 23, 0.28);
}

@media (max-width: 900px) {
  .metrics,
  .toolbar {
    grid-template-columns: 1fr;
  }

  .table-card__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .drawer-shell__panel {
    width: min(100%, 100vw);
  }
}
</style>
