<template>
  <section class="directory-page">
    <header class="page-header">
      <div>
        <h1>{{ pageCopy.title }}</h1>
        <p>{{ pageCopy.description }}</p>
      </div>
      <div class="header-summary">
        <div>{{ copy.summary.accounts }}: {{ accountOptions.length }}</div>
        <div>{{ copy.summary.requests }}: {{ requests.length }}</div>
      </div>
    </header>

    <section class="panel-card filter-card">
      <label class="field">
        <span>{{ copy.filters.user }}</span>
        <select v-model="targetUserId" class="field__control" name="targetUserId">
          <option v-for="user in users" :key="user.id" :value="user.id">
            {{ user.userName }} ({{ user.loginName }})
          </option>
        </select>
      </label>
      <label class="field">
        <span>{{ copy.filters.device }}</span>
        <select v-model="targetDeviceNodeId" class="field__control" name="targetDeviceNodeId">
          <option v-for="device in devices" :key="device.id" :value="device.id">
            {{ device.name }}
          </option>
        </select>
      </label>
    </section>

    <template v-if="pagePath === '/access/myView'">
      <section class="panel-card">
        <header class="section-header">
          <h2>{{ copy.sections.granted }}</h2>
        </header>
        <div class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>{{ copy.table.account }}</th>
                <th>{{ copy.table.device }}</th>
                <th>{{ copy.table.roles }}</th>
                <th>{{ copy.table.status }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="account in accountOptions" :key="account.id">
                <td>{{ account.accountName }}</td>
                <td>{{ account.deviceName }}</td>
                <td>{{ account.roles.join(' / ') || '-' }}</td>
                <td>{{ account.accountStatus }}</td>
              </tr>
              <tr v-if="accountOptions.length === 0">
                <td colspan="4">{{ copy.empty }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel-card">
        <header class="section-header">
          <h2>{{ copy.sections.pending }}</h2>
        </header>
        <div class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>{{ copy.table.requestNo }}</th>
                <th>{{ copy.table.requestType }}</th>
                <th>{{ copy.table.account }}</th>
                <th>{{ copy.table.status }}</th>
                <th>{{ copy.table.createdAt }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in requests" :key="request.id">
                <td>{{ request.requestNo }}</td>
                <td>{{ request.requestType }}</td>
                <td>{{ request.targetAccountName || '-' }}</td>
                <td>{{ request.status }}</td>
                <td>{{ request.createdAt || '-' }}</td>
              </tr>
              <tr v-if="requests.length === 0">
                <td colspan="5">{{ copy.empty }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <section v-else class="panel-card">
      <header class="section-header">
        <h2>{{ copy.sections.directory }}</h2>
      </header>
      <div class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th>{{ copy.table.prefix }}</th>
              <th>{{ copy.table.account }}</th>
              <th>{{ copy.table.type }}</th>
              <th>{{ copy.table.description }}</th>
              <th>{{ copy.table.actions }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="account in accountOptions" :key="account.id">
              <td>{{ account.deviceName }}</td>
              <td>{{ account.accountName }}</td>
              <td>{{ account.sourceType }}</td>
              <td>{{ account.remark || '-' }}</td>
              <td class="action-cell">
                <RouterLink class="action-link" :to="buildLink('/access/myRequest', account.accountName)">
                  {{ copy.actions.request }}
                </RouterLink>
                <RouterLink class="action-link" :to="buildLink('/access/myView', account.accountName)">
                  {{ copy.actions.view }}
                </RouterLink>
                <RouterLink class="action-link" :to="buildLink('/access/myRemove', account.accountName)">
                  {{ copy.actions.remove }}
                </RouterLink>
                <RouterLink class="action-link" :to="buildLink('/access/myChangepd', account.accountName)">
                  {{ copy.actions.reset }}
                </RouterLink>
              </td>
            </tr>
            <tr v-if="accountOptions.length === 0">
              <td colspan="5">{{ copy.empty }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchAssetTree, type AssetNode } from '../../api/assets'
import { fetchDeviceAccountsByDevice, type DeviceAccountItem } from '../../api/device-accounts'
import { fetchRequests, type RequestItem } from '../../api/requests'
import { fetchUsers, type UserItem } from '../../api/users'
import { useI18nText } from '../../i18n'

type DirectoryPath = '/access/myView' | '/access/accountManagement'

const route = useRoute()
const { locale } = useI18nText()

const zhPages = {
  '/access/myView': {
    title: '我的权限视图',
    description: '查看当前账号已经拥有的权限，以及还在流转中的申请记录。'
  },
  '/access/accountManagement': {
    title: '账号列表',
    description: '展示当前上下文下的账号，并提供快捷操作入口。'
  }
} as const

const enPages = {
  '/access/myView': {
    title: 'My Access View',
    description: 'Review granted access and in-flight request records for the selected account.'
  },
  '/access/accountManagement': {
    title: 'Account Directory',
    description: 'Show accounts in the current context and provide quick operation shortcuts.'
  }
} as const

const zhCopy = {
  summary: {
    accounts: '账号数',
    requests: '申请数'
  },
  filters: {
    user: '目标用户',
    device: '设备节点'
  },
  sections: {
    granted: '已获得权限',
    pending: '申请记录',
    directory: '账号目录'
  },
  table: {
    prefix: '前缀',
    account: '账号名称',
    device: '设备',
    roles: '权限角色',
    status: '状态',
    requestNo: '申请单号',
    requestType: '申请类型',
    createdAt: '创建时间',
    type: '账号类型',
    description: '账号描述',
    actions: '快捷操作'
  },
  actions: {
    request: '申请权限',
    view: '查看权限',
    remove: '删除权限',
    reset: '重置密码'
  },
  empty: '暂无数据'
} as const

const enCopy = {
  summary: {
    accounts: 'Accounts',
    requests: 'Requests'
  },
  filters: {
    user: 'Target User',
    device: 'Device Node'
  },
  sections: {
    granted: 'Granted Access',
    pending: 'Request Records',
    directory: 'Account Directory'
  },
  table: {
    prefix: 'Prefix',
    account: 'Account',
    device: 'Device',
    roles: 'Roles',
    status: 'Status',
    requestNo: 'Request No',
    requestType: 'Request Type',
    createdAt: 'Created At',
    type: 'Account Type',
    description: 'Description',
    actions: 'Actions'
  },
  actions: {
    request: 'Request Access',
    view: 'View Access',
    remove: 'Remove Access',
    reset: 'Reset Password'
  },
  empty: 'No data'
} as const

const users = ref<UserItem[]>([])
const assetTree = ref<AssetNode[]>([])
const deviceAccounts = ref<DeviceAccountItem[]>([])
const requests = ref<RequestItem[]>([])
const targetUserId = ref('')
const targetDeviceNodeId = ref('')

const pagePath = computed<DirectoryPath>(() =>
  route.meta.pagePath === '/access/accountManagement' ? '/access/accountManagement' : '/access/myView'
)
const pageCopy = computed(() => (locale.value === 'zh-CN' ? zhPages : enPages)[pagePath.value])
const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))
const devices = computed(() => collectDevices(assetTree.value))
const accountOptions = computed(() => {
  if (!targetUserId.value) {
    return deviceAccounts.value
  }

  const matched = deviceAccounts.value.filter((account) => account.userId === targetUserId.value)
  return matched.length > 0 ? matched : deviceAccounts.value
})

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

watch(targetDeviceNodeId, async (deviceNodeId) => {
  deviceAccounts.value = []

  if (!deviceNodeId) {
    return
  }

  deviceAccounts.value = await fetchDeviceAccountsByDevice(deviceNodeId)
}, { immediate: true })

onMounted(async () => {
  const [loadedUsers, loadedTree, loadedRequests] = await Promise.all([
    fetchUsers(),
    fetchAssetTree(),
    fetchRequests()
  ])

  users.value = loadedUsers
  assetTree.value = loadedTree
  requests.value = loadedRequests
})

function buildLink(path: string, accountName: string) {
  return {
    path,
    query: {
      targetUserId: targetUserId.value,
      targetDeviceNodeId: targetDeviceNodeId.value,
      targetAccountName: accountName
    }
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
.directory-page {
  display: grid;
  gap: 16px;
}

.page-header,
.panel-card {
  border: 1px solid #dcdfe6;
  background: #fff;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 20px;
}

.page-header h1,
.section-header h2 {
  margin: 0;
  color: #303133;
}

.page-header p {
  margin: 8px 0 0;
  color: #606266;
  line-height: 1.6;
}

.header-summary {
  display: grid;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.panel-card {
  padding: 16px;
}

.filter-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field {
  display: grid;
  gap: 8px;
  color: #303133;
  font-size: 14px;
}

.field__control {
  min-height: 36px;
  width: 100%;
  border: 1px solid #dcdfe6;
  padding: 8px 10px;
  font: inherit;
}

.section-header {
  margin-bottom: 12px;
}

.table-wrap {
  overflow-x: auto;
}

.result-table {
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
}

.result-table th,
.result-table td {
  border: 1px solid #ebeef5;
  padding: 10px 12px;
  text-align: left;
  font-size: 14px;
  color: #303133;
}

.result-table th {
  background: #f5f7fa;
  color: #606266;
}

.action-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-link {
  color: #409eff;
  text-decoration: none;
}

@media (max-width: 960px) {
  .page-header,
  .filter-card {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
  }
}
</style>
