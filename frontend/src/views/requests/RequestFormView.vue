<template>
  <section class="launchpad">
    <PageHero
      :kicker="t('login.eyebrow')"
      :title="copy.hero.title"
      :subtitle="copy.hero.subtitle"
    />

    <div class="launchpad__grid">
      <article class="form-card">
        <header class="card-header">
          <div>
            <h2 class="card-title">{{ copy.card.title }}</h2>
            <p class="card-meta">{{ copy.card.meta }}</p>
          </div>
        </header>

        <div v-if="initialLoadErrorMessage" class="load-alert" role="alert">
          {{ initialLoadErrorMessage }}
        </div>

        <div class="step-strip" :aria-label="copy.aria.steps">
          <span v-for="step in approvalSteps" :key="step" class="step-pill">{{ step }}</span>
        </div>

        <div class="summary-grid">
          <article class="summary-tile">
            <div class="summary-tile__label">{{ copy.summary.user }}</div>
            <div class="summary-tile__value">{{ selectedUserLabel }}</div>
          </article>
          <article class="summary-tile">
            <div class="summary-tile__label">{{ copy.summary.device }}</div>
            <div class="summary-tile__value">{{ selectedDeviceLabel }}</div>
          </article>
          <article class="summary-tile">
            <div class="summary-tile__label">{{ copy.summary.role }}</div>
            <div class="summary-tile__value">{{ selectedRoleLabel }}</div>
          </article>
          <article class="summary-tile">
            <div class="summary-tile__label">{{ copy.summary.account }}</div>
            <div class="summary-tile__value">{{ selectedAccountLabel }}</div>
          </article>
        </div>

        <form class="form" @submit.prevent="handleSubmit">
          <label class="field">
            <span>{{ copy.fields.requestType }}</span>
            <select v-model="requestType" class="field__control" name="requestType">
              <option :value="DEFAULT_REQUEST_TYPE">{{ formatRequestType(DEFAULT_REQUEST_TYPE) }}</option>
            </select>
          </label>

          <label class="field">
            <span>{{ copy.fields.targetUser }}</span>
            <select v-model="targetUserId" class="field__control" name="targetUserId">
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.userName }} ({{ user.loginName }})
              </option>
            </select>
          </label>

          <label class="field">
            <span>{{ copy.fields.deviceNode }}</span>
            <select v-model="targetDeviceNodeId" class="field__control" name="targetDeviceNodeId">
              <option v-for="device in devices" :key="device.id" :value="device.id">
                {{ device.name }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>{{ copy.fields.targetAccount }}</span>
            <select v-model="targetAccountName" class="field__control" name="targetAccountName">
              <option v-for="account in accountOptions" :key="account.accountName" :value="account.accountName">
                {{ account.accountName }}
              </option>
            </select>
            <span v-if="deviceAccountsLoading" class="field-note">{{ copy.notes.loadingAccounts }}</span>
            <span v-else-if="deviceAccountsErrorMessage" class="field-note field-note--error">
              {{ deviceAccountsErrorMessage }}
            </span>
          </label>

          <label class="field">
            <span>{{ copy.fields.roleNode }}</span>
            <select v-model="roleNodeId" class="field__control" name="roleNodeId">
              <option v-for="role in roleOptions" :key="role.id" :value="role.id">
                {{ role.name }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>{{ copy.fields.reason }}</span>
            <textarea
              v-model="reason"
              class="field__control field__control--textarea"
              name="reason"
              rows="4"
              :placeholder="copy.fields.reasonPlaceholder"
            />
          </label>

          <div class="form-actions">
            <button class="hero-action" type="submit" :disabled="submitting || !canSubmit">
              {{ submitting ? copy.actions.submitting : copy.actions.submit }}
            </button>
            <p class="form-hint">
              {{ canSubmit ? copy.hints.ready : copy.hints.locked }}
            </p>
          </div>

          <div v-if="submitErrorMessage" class="load-alert" role="alert">
            {{ submitErrorMessage }}
          </div>
        </form>
      </article>

      <aside class="approval-card">
        <h2 class="approval-card__title">{{ copy.approval.title }}</h2>
        <p class="approval-card__body">{{ copy.approval.body }}</p>

        <ol class="approval-list">
          <li class="approval-list__item">
            <strong>{{ copy.approval.departmentManagerTitle }}</strong>
            <span>{{ copy.approval.departmentManagerDescription }}</span>
          </li>
          <li class="approval-list__item">
            <strong>{{ copy.approval.qaTitle }}</strong>
            <span>{{ copy.approval.qaDescription }}</span>
          </li>
          <li class="approval-list__item">
            <strong>{{ copy.approval.qmTitle }}</strong>
            <span>{{ copy.approval.qmDescription }}</span>
          </li>
          <li class="approval-list__item">
            <strong>{{ copy.approval.qiTitle }}</strong>
            <span>{{ copy.approval.qiDescription }}</span>
          </li>
        </ol>

        <div class="approval-note">{{ copy.approval.generatedFrom }}</div>

        <dl class="context-list">
          <div class="context-list__row">
            <dt>{{ copy.context.requestType }}</dt>
            <dd>{{ formatRequestType(requestType || DEFAULT_REQUEST_TYPE) }}</dd>
          </div>
          <div class="context-list__row">
            <dt>{{ copy.context.user }}</dt>
            <dd>{{ selectedUserLabel }}</dd>
          </div>
          <div class="context-list__row">
            <dt>{{ copy.context.device }}</dt>
            <dd>{{ selectedDeviceLabel }}</dd>
          </div>
          <div class="context-list__row">
            <dt>{{ copy.context.account }}</dt>
            <dd>{{ selectedAccountLabel }}</dd>
          </div>
        </dl>
      </aside>
    </div>

    <section v-if="submitMessage" class="success-card" aria-live="polite">
      <h2 class="success-card__title">{{ copy.success.title }}</h2>
      <p class="success-card__body">{{ submitMessage }}</p>
      <div class="success-card__actions">
        <a class="inline-link" href="/requests">{{ copy.success.link }}</a>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { fetchAssetTree, type AssetNode } from '../../api/assets'
import { fetchDeviceAccountsByDevice, type DeviceAccountItem } from '../../api/device-accounts'
import { useI18nText } from '../../i18n'
import { createRequest } from '../../api/requests'
import { fetchUsers, type UserItem } from '../../api/users'
import PageHero from '../../components/shell/PageHero.vue'

const { t, locale } = useI18nText()

const DEFAULT_REQUEST_TYPE = 'ROLE_ADD'

const zhCopy = {
  hero: {
    title: '申请发起台',
    subtitle: '基于预填上下文、实时校验与清晰下一步，安全发起角色申请。'
  },
  card: {
    title: '申请草稿',
    meta: '从管理工作台带入的字段仍可继续调整，直到正式提交。'
  },
  aria: {
    steps: '申请步骤'
  },
  steps: ['1 上下文', '2 审批路径', '3 提交'],
  summary: {
    user: '已选用户',
    device: '已选设备',
    role: '已选角色',
    account: '账号匹配'
  },
  fields: {
    requestType: '申请类型',
    targetUser: '目标用户',
    deviceNode: '设备节点',
    targetAccount: '目标账号',
    roleNode: '角色节点',
    reason: '申请原因',
    reasonPlaceholder: '说明需要该角色的业务原因以及依赖它的业务结果。'
  },
  notes: {
    loadingAccounts: '正在加载所选设备的账号...'
  },
  actions: {
    submit: '提交申请',
    submitting: '提交中...'
  },
  hints: {
    ready: '提交后将立即进入审批路径。',
    locked: '补全申请上下文后即可提交。'
  },
  approval: {
    title: '审批路径',
    body: '敏感角色变更必须走审批流。本页只负责发起申请，不会直接从驾驶舱下发权限。',
    departmentManagerTitle: '部门经理复核',
    departmentManagerDescription: '确认业务必要性与权限归属是否一致。',
    qaTitle: 'QA 校验',
    qaDescription: '核对申请角色范围是否符合运行标准。',
    qmTitle: 'QM 确认',
    qmDescription: '在执行前确认控制影响与风险边界。',
    qiTitle: 'QI 执行',
    qiDescription: '落实已批准的角色变更并留下可审计痕迹。',
    generatedFrom: '根据当前上下文生成：'
  },
  context: {
    requestType: '申请类型',
    user: '用户',
    device: '设备',
    account: '账号'
  },
  success: {
    title: '申请已创建',
    body: '申请已成功提交，可前往列表继续跟踪。',
    link: '打开申请列表'
  },
  requestTypes: {
    ROLE_ADD: '角色新增'
  },
  labels: {
    noUserSelected: '未选择用户',
    noDeviceSelected: '未选择设备',
    rolePending: '待选择角色',
    loadingAccounts: '正在加载账号选项...',
    accountLoadFailed: '账号加载失败',
    noMatchingAccount: '无匹配账号'
  },
  errors: {
    deviceAccounts: '无法加载所选设备的账号。',
    initialLoad: '无法加载申请上下文。',
    submit: '申请提交失败。'
  }
} as const

const enCopy = {
  hero: {
    title: 'Request Launchpad',
    subtitle: 'Build approval-safe role requests with live validation, prefilled context, and clear next steps.'
  },
  card: {
    title: 'Request Draft',
    meta: 'Values pushed from the management workbenches stay editable until the request is submitted.'
  },
  aria: {
    steps: 'Request steps'
  },
  steps: ['1 Context', '2 Approval Route', '3 Submit'],
  summary: {
    user: 'Selected User',
    device: 'Selected Device',
    role: 'Selected Role',
    account: 'Account Match'
  },
  fields: {
    requestType: 'Request Type',
    targetUser: 'Target User',
    deviceNode: 'Device Node',
    targetAccount: 'Target Account',
    roleNode: 'Role Node',
    reason: 'Request Reason',
    reasonPlaceholder: 'Describe why this role is needed and what business outcome depends on it.'
  },
  notes: {
    loadingAccounts: 'Loading accounts for the selected device...'
  },
  actions: {
    submit: 'Submit Request',
    submitting: 'Submitting...'
  },
  hints: {
    ready: 'The request will enter the approval route immediately after submission.',
    locked: 'Complete the request context to unlock submission.'
  },
  approval: {
    title: 'Approval Route',
    body: 'Sensitive role changes stay approval-driven. This page launches the request, but it never applies the permission directly from the cockpit.',
    departmentManagerTitle: 'Department manager review',
    departmentManagerDescription: 'Confirm the business need and ownership alignment.',
    qaTitle: 'QA checkpoint',
    qaDescription: 'Validate the requested role scope against operating standards.',
    qmTitle: 'QM confirmation',
    qmDescription: 'Approve the control impact before execution.',
    qiTitle: 'QI execution',
    qiDescription: 'Apply the approved role change and leave an auditable trace.',
    generatedFrom: 'Generated from the current context:'
  },
  context: {
    requestType: 'Request Type',
    user: 'User',
    device: 'Device',
    account: 'Account'
  },
  success: {
    title: 'Request Created',
    body: 'Request created successfully.',
    link: 'Open Request List'
  },
  requestTypes: {
    ROLE_ADD: 'Role Add'
  },
  labels: {
    noUserSelected: 'No user selected',
    noDeviceSelected: 'No device selected',
    rolePending: 'Role pending',
    loadingAccounts: 'Loading account options...',
    accountLoadFailed: 'Account load failed',
    noMatchingAccount: 'No matching account'
  },
  errors: {
    deviceAccounts: 'Unable to load device accounts for the selected device.',
    initialLoad: 'Unable to load request context.',
    submit: 'Request submission failed.'
  }
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

const route = useRoute()
const requestType = ref(readQueryString(route.query.requestType) || DEFAULT_REQUEST_TYPE)
const targetUserId = ref(readQueryString(route.query.targetUserId))
const targetDeviceNodeId = ref(readQueryString(route.query.targetDeviceNodeId))
const targetAccountName = ref(readQueryString(route.query.targetAccountName))
const roleNodeId = ref('')
const reason = ref('')
const submitMessage = ref('')
const submitErrorMessage = ref('')
const submitting = ref(false)
const initialLoadErrorMessage = ref('')
const users = ref<UserItem[]>([])
const assetTree = ref<AssetNode[]>([])
const deviceAccounts = ref<DeviceAccountItem[]>([])
const deviceAccountsLoading = ref(false)
const deviceAccountsErrorMessage = ref('')
const pendingPrefillAccountName = ref(readQueryString(route.query.targetAccountName))

const approvalSteps = computed(() => copy.value.steps)
const devices = computed(() => collectDevices(assetTree.value))
const selectedUser = computed(() => users.value.find((user) => user.id === targetUserId.value))
const selectedDevice = computed(() => devices.value.find((device) => device.id === targetDeviceNodeId.value))
const roleOptions = computed(() => selectedDevice.value?.children ?? [])

const accountOptions = computed(() => {
  if (!targetUserId.value) {
    return deviceAccounts.value
  }

  return deviceAccounts.value.filter((account) => account.userId === targetUserId.value)
})

const selectedAccount = computed(() =>
  accountOptions.value.find((account) => account.accountName === targetAccountName.value)
)

const selectedRole = computed(() =>
  roleOptions.value.find((role) => role.id === roleNodeId.value)
)

const selectedUserLabel = computed(() =>
  selectedUser.value ? `${selectedUser.value.userName} (${selectedUser.value.loginName})` : copy.value.labels.noUserSelected
)

const selectedDeviceLabel = computed(() => selectedDevice.value?.name ?? copy.value.labels.noDeviceSelected)

const selectedRoleLabel = computed(() => selectedRole.value?.name ?? copy.value.labels.rolePending)

const selectedAccountLabel = computed(() => {
  if (selectedAccount.value) {
    return selectedAccount.value.accountName
  }

  if (deviceAccountsLoading.value) {
    return copy.value.labels.loadingAccounts
  }

  if (deviceAccountsErrorMessage.value) {
    return copy.value.labels.accountLoadFailed
  }

  return copy.value.labels.noMatchingAccount
})

const canSubmit = computed(() =>
  Boolean(
    requestType.value &&
    targetUserId.value &&
    targetDeviceNodeId.value &&
    !initialLoadErrorMessage.value &&
    !deviceAccountsLoading.value &&
    !deviceAccountsErrorMessage.value &&
    selectedAccount.value &&
    selectedRole.value &&
    reason.value.trim()
  )
)

let deviceAccountRequestSequence = 0

watchEffect(() => {
  const nextRequestType = readQueryString(route.query.requestType)
  const nextTargetUserId = readQueryString(route.query.targetUserId)
  const nextTargetDeviceNodeId = readQueryString(route.query.targetDeviceNodeId)
  const nextTargetAccountName = readQueryString(route.query.targetAccountName)

  requestType.value = nextRequestType || DEFAULT_REQUEST_TYPE
  targetUserId.value = nextTargetUserId
  targetDeviceNodeId.value = nextTargetDeviceNodeId
  targetAccountName.value = nextTargetAccountName
  pendingPrefillAccountName.value = nextTargetAccountName
})

watch(
  [users, () => route.query.targetUserId],
  ([value, routeTargetUserId]) => {
    if (!readQueryString(routeTargetUserId) && !targetUserId.value && value.length > 0) {
      targetUserId.value = value[0].id
    }
  },
  { immediate: true }
)

watch(
  [devices, () => route.query.targetDeviceNodeId],
  ([value, routeTargetDeviceNodeId]) => {
    if (!readQueryString(routeTargetDeviceNodeId) && !targetDeviceNodeId.value && value.length > 0) {
      targetDeviceNodeId.value = value[0].id
    }
  },
  { immediate: true }
)

watch(roleOptions, (value) => {
  if (!value.some((role) => role.id === roleNodeId.value)) {
    roleNodeId.value = value[0]?.id ?? ''
  }
})

watch(accountOptions, (value) => {
  if (pendingPrefillAccountName.value) {
    if (value.length === 0) {
      return
    }

    if (value.some((account) => account.accountName === pendingPrefillAccountName.value)) {
      targetAccountName.value = pendingPrefillAccountName.value
      pendingPrefillAccountName.value = ''
      return
    }

    pendingPrefillAccountName.value = ''
  }

  if (!value.some((account) => account.accountName === targetAccountName.value)) {
    targetAccountName.value = value[0]?.accountName ?? ''
  }
})

watch(
  targetDeviceNodeId,
  async (deviceNodeId) => {
    const requestId = ++deviceAccountRequestSequence
    deviceAccountsErrorMessage.value = ''
    deviceAccounts.value = []

    if (!deviceNodeId) {
      deviceAccountsLoading.value = false
      return
    }

    deviceAccountsLoading.value = true

    try {
      const nextDeviceAccounts = await fetchDeviceAccountsByDevice(deviceNodeId)
      if (requestId !== deviceAccountRequestSequence) {
        return
      }

      deviceAccounts.value = nextDeviceAccounts
    } catch (error) {
      if (requestId !== deviceAccountRequestSequence) {
        return
      }

      deviceAccounts.value = []
      deviceAccountsErrorMessage.value = getDeviceAccountLoadMessage(error)
    } finally {
      if (requestId === deviceAccountRequestSequence) {
        deviceAccountsLoading.value = false
      }
    }
  },
  { immediate: true }
)

onMounted(async () => {
  initialLoadErrorMessage.value = ''

  try {
    const [loadedUsers, loadedAssetTree] = await Promise.all([fetchUsers(), fetchAssetTree()])
    users.value = loadedUsers
    assetTree.value = loadedAssetTree
  } catch (error) {
    users.value = []
    assetTree.value = []
    initialLoadErrorMessage.value = getInitialLoadMessage(error)
  }
})

async function handleSubmit() {
  if (submitting.value || !canSubmit.value) {
    return
  }

  submitting.value = true
  submitMessage.value = ''
  submitErrorMessage.value = ''

  try {
    await createRequest({
      requestType: requestType.value,
      targetUserId: Number(targetUserId.value),
      targetDeviceNodeId: Number(targetDeviceNodeId.value),
      targetAccountName: targetAccountName.value,
      reason: reason.value,
      items: [{ roleNodeId: Number(roleNodeId.value) }]
    })
    submitMessage.value = copy.value.success.body
    reason.value = ''
  } catch (error) {
    submitErrorMessage.value = getSubmitErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

function collectDevices(nodes: AssetNode[]): AssetNode[] {
  return nodes.flatMap((node) => {
    const matches = node.type === 'DEVICE' ? [node] : []
    return matches.concat(collectDevices(node.children))
  })
}

function readQueryString(value: unknown) {
  return typeof value === 'string' ? value : ''
}

function formatRequestType(value: string) {
  return (copy.value.requestTypes[value as keyof typeof copy.value.requestTypes] ?? value) || '-'
}

function getDeviceAccountLoadMessage(error: unknown) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message.trim()
  }

  return copy.value.errors.deviceAccounts
}

function getInitialLoadMessage(error: unknown) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message.trim()
  }

  return copy.value.errors.initialLoad
}

function getSubmitErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message.trim()
  }

  return copy.value.errors.submit
}
</script>

<style scoped>
.launchpad {
  display: grid;
  gap: 14px;
}

.launchpad__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.9fr);
  gap: 14px;
  align-items: start;
}

.form-card,
.approval-card,
.success-card {
  border: 1px solid var(--cockpit-border);
  border-radius: var(--cockpit-radius-lg);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: var(--cockpit-shadow);
}

.form-card {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 12px;
}

.card-title,
.approval-card__title,
.success-card__title {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
}

.card-meta,
.approval-card__body,
.success-card__body {
  margin: 8px 0 0;
  color: var(--cockpit-muted);
  line-height: 1.6;
}

.load-alert {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(251, 113, 133, 0.12);
  border: 1px solid rgba(251, 113, 133, 0.28);
  color: rgba(255, 196, 206, 0.98);
  line-height: 1.5;
}

.step-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.step-pill {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(94, 234, 212, 0.12);
  border: 1px solid rgba(94, 234, 212, 0.2);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-tile {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(0, 0, 0, 0.14);
}

.summary-tile__label {
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.summary-tile__value {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.5;
}

.form {
  display: grid;
  gap: 12px;
}

.field {
  display: grid;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
}

.field__control {
  min-height: 46px;
  width: 100%;
  padding: 11px 14px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.18);
  color: var(--cockpit-text);
  font: inherit;
}

.field__control--textarea {
  min-height: 124px;
  resize: vertical;
}

.field__control::placeholder {
  color: rgba(255, 255, 255, 0.44);
}

.field-note {
  font-size: 12px;
  color: var(--cockpit-muted);
}

.field-note--error {
  color: rgba(254, 202, 202, 0.96);
}

.form-actions {
  display: grid;
  gap: 10px;
  margin-top: 4px;
}

.hero-action {
  min-height: 46px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: linear-gradient(135deg, rgba(94, 234, 212, 0.95), rgba(96, 165, 250, 0.95));
  color: #041322;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.hero-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-hint {
  margin: 0;
  font-size: 12px;
  color: var(--cockpit-muted);
}

.approval-card {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.approval-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding-left: 18px;
}

.approval-list__item {
  display: grid;
  gap: 4px;
}

.approval-list__item span {
  color: var(--cockpit-muted);
  line-height: 1.5;
}

.approval-note {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(96, 165, 250, 0.12);
  border: 1px solid rgba(96, 165, 250, 0.16);
  font-size: 13px;
  font-weight: 700;
}

.context-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.context-list__row {
  display: grid;
  gap: 4px;
}

.context-list__row dt {
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.context-list__row dd {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}

.success-card {
  padding: 18px;
  display: grid;
  gap: 12px;
}

.success-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.inline-link {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(94, 234, 212, 0.24);
  color: rgba(187, 247, 208, 0.96);
  text-decoration: none;
  font-weight: 800;
}

@media (max-width: 1080px) {
  .launchpad__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
