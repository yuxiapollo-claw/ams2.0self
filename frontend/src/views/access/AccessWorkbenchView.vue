<template>
  <section class="access-page">
    <header class="page-header">
      <div>
        <h1>{{ pageCopy.title }}</h1>
        <p>{{ pageCopy.description }}</p>
      </div>
      <dl class="page-meta">
        <div>
          <dt>{{ copy.meta.operator }}</dt>
          <dd>AMSAdmin</dd>
        </div>
        <div>
          <dt>{{ copy.meta.account }}</dt>
          <dd>{{ targetAccountName || '-' }}</dd>
        </div>
      </dl>
    </header>

    <div v-if="loadErrorMessage" class="alert-card" role="alert">{{ loadErrorMessage }}</div>

    <div class="page-grid">
      <form class="panel-card" @submit.prevent="handleSubmit">
        <div class="field-grid">
          <label class="field">
            <span>{{ copy.fields.targetUser }}</span>
            <select v-model="targetUserId" class="field__control" name="targetUserId">
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.userName }} ({{ user.loginName }})
              </option>
            </select>
          </label>

          <label class="field">
            <span>{{ copy.fields.device }}</span>
            <select v-model="targetDeviceNodeId" class="field__control" name="targetDeviceNodeId">
              <option v-for="device in devices" :key="device.id" :value="device.id">
                {{ device.name }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>{{ copy.fields.account }}</span>
            <select v-model="targetAccountName" class="field__control" name="targetAccountName">
              <option v-for="account in accountOptions" :key="account.id" :value="account.accountName">
                {{ account.accountName }}
              </option>
            </select>
            <span v-if="deviceAccountsErrorMessage" class="field__note field__note--error">
              {{ deviceAccountsErrorMessage }}
            </span>
          </label>

          <label v-if="requiresRole" class="field">
            <span>{{ copy.fields.role }}</span>
            <select v-model="roleNodeId" class="field__control" name="roleNodeId">
              <option v-for="role in roleOptions" :key="role.id" :value="role.id">
                {{ role.name }}
              </option>
            </select>
          </label>
        </div>

        <label class="field">
          <span>{{ copy.fields.reason }}</span>
          <textarea
            v-model="reason"
            class="field__control field__control--textarea"
            name="reason"
            rows="5"
            :placeholder="copy.fields.reasonPlaceholder"
          />
        </label>

        <div class="form-actions">
          <button class="primary-button" type="submit" :disabled="submitting || !canSubmit">
            {{ submitting ? copy.actions.submitting : copy.actions.submit }}
          </button>
          <span class="form-hint">{{ pageCopy.helper }}</span>
        </div>

        <div v-if="submitMessage" class="success-card" aria-live="polite">{{ submitMessage }}</div>
      </form>

      <aside class="panel-card panel-card--side">
        <h2>{{ copy.guidance.title }}</h2>
        <ol class="guidance-list">
          <li>{{ copy.guidance.step1 }}</li>
          <li>{{ copy.guidance.step2 }}</li>
          <li>{{ copy.guidance.step3 }}</li>
        </ol>
        <div class="summary-box">
          <div>{{ copy.summary.user }}: {{ selectedUserLabel }}</div>
          <div>{{ copy.summary.device }}: {{ selectedDeviceLabel }}</div>
          <div>{{ copy.summary.account }}: {{ targetAccountName || '-' }}</div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchAssetTree, type AssetNode } from '../../api/assets'
import { fetchDeviceAccountsByDevice, type DeviceAccountItem } from '../../api/device-accounts'
import { createRequest } from '../../api/requests'
import { fetchUsers, type UserItem } from '../../api/users'
import { useI18nText } from '../../i18n'

type PagePath = '/access/myRequest' | '/access/myRemove' | '/access/myChangepd'

interface ModeCopy {
  title: string
  description: string
  helper: string
  requestType: string
  requiresRole: boolean
}

const route = useRoute()
const { locale } = useI18nText()

const zhModes: Record<PagePath, ModeCopy> = {
  '/access/myRequest': {
    title: '权限申请',
    description: '为当前账号发起新的权限申请，并进入审批流转。',
    helper: '提交后会进入待审批队列。',
    requestType: 'ROLE_ADD',
    requiresRole: true
  },
  '/access/myRemove': {
    title: '删除权限',
    description: '回收账号上的既有权限，保留完整申请记录。',
    helper: '提交后会进入权限回收审批。',
    requestType: 'ROLE_REMOVE',
    requiresRole: true
  },
  '/access/myChangepd': {
    title: '重置密码',
    description: '为当前账号发起密码重置申请。',
    helper: '密码重置同样需要审批留痕。',
    requestType: 'PASSWORD_RESET',
    requiresRole: false
  }
}

const enModes: Record<PagePath, ModeCopy> = {
  '/access/myRequest': {
    title: 'Access Request',
    description: 'Launch a new access request for the current account and send it into approval.',
    helper: 'Submitted requests enter the approval queue immediately.',
    requestType: 'ROLE_ADD',
    requiresRole: true
  },
  '/access/myRemove': {
    title: 'Remove Access',
    description: 'Reclaim an existing entitlement while keeping the request trail.',
    helper: 'Submitted removals enter the approval queue immediately.',
    requestType: 'ROLE_REMOVE',
    requiresRole: true
  },
  '/access/myChangepd': {
    title: 'Reset Password',
    description: 'Launch a password-reset request for the selected account.',
    helper: 'Password resets also stay approval-driven.',
    requestType: 'PASSWORD_RESET',
    requiresRole: false
  }
}

const zhCopy = {
  meta: {
    operator: '当前操作人',
    account: '目标账号'
  },
  fields: {
    targetUser: '目标用户',
    device: '设备节点',
    account: '目标账号',
    role: '权限角色',
    reason: '申请原因',
    reasonPlaceholder: '填写申请原因、业务场景与使用边界。'
  },
  actions: {
    submit: '提交申请',
    submitting: '提交中...'
  },
  guidance: {
    title: '处理说明',
    step1: '先确认目标用户、设备和账号上下文。',
    step2: '按需要选择角色或直接发起密码重置。',
    step3: '提交后由审批与执行任务继续流转。'
  },
  summary: {
    user: '用户',
    device: '设备',
    account: '账号'
  },
  messages: {
    loadError: '无法加载权限申请所需的基础数据。',
    accountError: '无法加载当前设备下的账号。',
    success: '申请已提交'
  },
  labels: {
    noUser: '未选择用户',
    noDevice: '未选择设备'
  }
} as const

const enCopy = {
  meta: {
    operator: 'Operator',
    account: 'Target Account'
  },
  fields: {
    targetUser: 'Target User',
    device: 'Device Node',
    account: 'Target Account',
    role: 'Role',
    reason: 'Request Reason',
    reasonPlaceholder: 'Describe the reason, business scenario, and expected boundary.'
  },
  actions: {
    submit: 'Submit Request',
    submitting: 'Submitting...'
  },
  guidance: {
    title: 'Guidance',
    step1: 'Confirm the target user, device, and account context first.',
    step2: 'Select a role when needed or launch a password reset directly.',
    step3: 'After submission the workflow continues in approval and execution queues.'
  },
  summary: {
    user: 'User',
    device: 'Device',
    account: 'Account'
  },
  messages: {
    loadError: 'Unable to load the access workbench context.',
    accountError: 'Unable to load device accounts for the selected device.',
    success: 'Request submitted'
  },
  labels: {
    noUser: 'No user selected',
    noDevice: 'No device selected'
  }
} as const

const users = ref<UserItem[]>([])
const assetTree = ref<AssetNode[]>([])
const deviceAccounts = ref<DeviceAccountItem[]>([])
const targetUserId = ref('')
const targetDeviceNodeId = ref('')
const targetAccountName = ref('')
const roleNodeId = ref('')
const reason = ref('')
const submitting = ref(false)
const loadErrorMessage = ref('')
const deviceAccountsErrorMessage = ref('')
const submitMessage = ref('')

const pagePath = computed<PagePath>(() => {
  const value = typeof route.meta.pagePath === 'string' ? route.meta.pagePath : route.path
  if (value === '/access/myRemove' || value === '/access/myChangepd') {
    return value
  }
  return '/access/myRequest'
})

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))
const pageCopy = computed(() => (locale.value === 'zh-CN' ? zhModes : enModes)[pagePath.value])
const devices = computed(() => collectDevices(assetTree.value))
const selectedDevice = computed(() => devices.value.find((device) => device.id === targetDeviceNodeId.value))
const roleOptions = computed(() => selectedDevice.value?.children ?? [])
const accountOptions = computed(() => {
  if (!targetUserId.value) {
    return deviceAccounts.value
  }

  const matched = deviceAccounts.value.filter((account) => account.userId === targetUserId.value)
  return matched.length > 0 ? matched : deviceAccounts.value
})
const requiresRole = computed(() => pageCopy.value.requiresRole)
const canSubmit = computed(() =>
  Boolean(
    targetUserId.value &&
      targetDeviceNodeId.value &&
      targetAccountName.value &&
      reason.value.trim() &&
      (!requiresRole.value || roleNodeId.value)
  )
)
const selectedUserLabel = computed(() => {
  const current = users.value.find((user) => user.id === targetUserId.value)
  return current ? `${current.userName} (${current.loginName})` : copy.value.labels.noUser
})
const selectedDeviceLabel = computed(() => selectedDevice.value?.name ?? copy.value.labels.noDevice)

watch(users, (value) => {
  if (!targetUserId.value && value.length > 0) {
    targetUserId.value = value[0].id
  }
}, { immediate: true })

watch(devices, (value) => {
  if (!targetDeviceNodeId.value && value.length > 0) {
    targetDeviceNodeId.value = value[0].id
  }
}, { immediate: true })

watch(roleOptions, (value) => {
  if (!requiresRole.value) {
    roleNodeId.value = ''
    return
  }

  if (!value.some((role) => role.id === roleNodeId.value)) {
    roleNodeId.value = value[0]?.id ?? ''
  }
})

watch(accountOptions, (value) => {
  if (!value.some((account) => account.accountName === targetAccountName.value)) {
    targetAccountName.value = value[0]?.accountName ?? ''
  }
})

watch(targetDeviceNodeId, async (deviceNodeId) => {
  deviceAccountsErrorMessage.value = ''
  deviceAccounts.value = []

  if (!deviceNodeId) {
    return
  }

  try {
    deviceAccounts.value = await fetchDeviceAccountsByDevice(deviceNodeId)
  } catch (error) {
    deviceAccounts.value = []
    deviceAccountsErrorMessage.value =
      error instanceof Error && error.message.trim().length > 0
        ? error.message.trim()
        : copy.value.messages.accountError
  }
}, { immediate: true })

onMounted(async () => {
  loadErrorMessage.value = ''

  try {
    const [loadedUsers, loadedTree] = await Promise.all([fetchUsers(), fetchAssetTree()])
    users.value = loadedUsers
    assetTree.value = loadedTree
  } catch (error) {
    users.value = []
    assetTree.value = []
    loadErrorMessage.value =
      error instanceof Error && error.message.trim().length > 0
        ? error.message.trim()
        : copy.value.messages.loadError
  }
})

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) {
    return
  }

  submitting.value = true
  submitMessage.value = ''

  try {
    await createRequest({
      requestType: pageCopy.value.requestType,
      targetUserId: Number(targetUserId.value),
      targetDeviceNodeId: Number(targetDeviceNodeId.value),
      targetAccountName: targetAccountName.value,
      reason: reason.value.trim(),
      items: [
        {
          roleNodeId: Number(roleNodeId.value || '0')
        }
      ]
    })

    submitMessage.value = copy.value.messages.success
    reason.value = ''
  } finally {
    submitting.value = false
  }
}

function collectDevices(nodes: AssetNode[]): AssetNode[] {
  return nodes.flatMap((node) => {
    const current = node.type === 'DEVICE' ? [node] : []
    return current.concat(collectDevices(node.children))
  })
}
</script>

<style scoped>
.access-page {
  display: grid;
  gap: 16px;
}

.page-header,
.panel-card,
.alert-card,
.success-card {
  border: 1px solid #dcdfe6;
  background: #fff;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.page-header p {
  margin: 8px 0 0;
  color: #606266;
  line-height: 1.6;
}

.page-meta {
  display: grid;
  gap: 10px;
  margin: 0;
  min-width: 200px;
}

.page-meta div {
  display: grid;
  gap: 4px;
}

.page-meta dt {
  font-size: 12px;
  color: #909399;
}

.page-meta dd {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.alert-card,
.success-card {
  padding: 12px 14px;
  color: #606266;
}

.page-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
  gap: 16px;
}

.panel-card {
  padding: 18px;
}

.panel-card--side h2 {
  margin: 0 0 12px;
  font-size: 18px;
  color: #303133;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 14px;
  color: #303133;
}

.field__control {
  min-height: 36px;
  width: 100%;
  border: 1px solid #dcdfe6;
  padding: 8px 10px;
  font: inherit;
  color: #303133;
  background: #fff;
}

.field__control--textarea {
  min-height: 120px;
  resize: vertical;
}

.field__note {
  font-size: 12px;
  color: #909399;
}

.field__note--error {
  color: #f56c6c;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.primary-button {
  min-width: 112px;
  height: 36px;
  border: 1px solid #409eff;
  background: #409eff;
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.form-hint {
  font-size: 13px;
  color: #606266;
}

.guidance-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.7;
}

.summary-box {
  margin-top: 16px;
  display: grid;
  gap: 8px;
  padding: 12px;
  background: #f5f7fa;
  color: #303133;
}

@media (max-width: 960px) {
  .page-grid,
  .field-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
  }
}
</style>
