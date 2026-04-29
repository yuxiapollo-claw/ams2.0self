<template>
  <section class="domain-page">
    <header class="page-header">
      <div>
        <h1>{{ page.title }}</h1>
        <p>{{ page.description }}</p>
      </div>
      <div v-if="page.tabs.length" class="tab-strip">
        <button v-for="tab in page.tabs" :key="tab" type="button" class="tab-button">{{ tab }}</button>
      </div>
    </header>

    <div v-if="errorMessage" class="message-card message-card--error" role="alert">{{ errorMessage }}</div>

    <section class="data-card">
      <header class="data-card__header">
        <h2>{{ page.sectionTitle }}</h2>
        <p>{{ rowCountText }}</p>
      </header>

      <div class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th v-for="header in page.headers" :key="header">{{ header }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.key">
              <td v-for="cell in row.cells" :key="cell">{{ cell }}</td>
            </tr>
            <tr v-if="rows.length === 0">
              <td :colspan="page.headers.length">{{ page.empty }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchAssetTree, type AssetNode } from '../../api/assets'
import { fetchDevicePermissions, type DevicePermissionResult } from '../../api/queries'
import { fetchRequests, type RequestItem } from '../../api/requests'
import { useI18nText } from '../../i18n'

type ManagerPath = '/sysAccessManager/empAccessManagement' | '/sysAccessManager/reviewCheck'

interface PageConfig {
  title: string
  description: string
  sectionTitle: string
  headers: string[]
  tabs: string[]
  empty: string
}

interface RowShape {
  key: string
  cells: string[]
}

const route = useRoute()
const { locale } = useI18nText()

const zhPages: Record<ManagerPath, PageConfig> = {
  '/sysAccessManager/empAccessManagement': {
    title: '我管理的系统权限',
    description: '从系统/权限经理视角查看当前负责的权限与账号。',
    sectionTitle: '系统权限明细',
    headers: ['系统名称', '权限角色', '设备账号', '状态'],
    tabs: [],
    empty: '暂无系统权限数据'
  },
  '/sysAccessManager/reviewCheck': {
    title: '审核查看',
    description: '查看当前系统/权限经理负责的审核任务。',
    sectionTitle: '审核任务列表',
    headers: ['申请单号', '申请类型', '状态', '创建时间', '账号'],
    tabs: ['进行中', '未开始'],
    empty: '暂无审核任务数据'
  }
}

const enPages: Record<ManagerPath, PageConfig> = {
  '/sysAccessManager/empAccessManagement': {
    title: 'Systems I Manage',
    description: 'Review current permissions and accounts from the system/permission manager perspective.',
    sectionTitle: 'Managed Permission Detail',
    headers: ['System', 'Role', 'Device Account', 'Status'],
    tabs: [],
    empty: 'No managed permission data'
  },
  '/sysAccessManager/reviewCheck': {
    title: 'Review Check',
    description: 'Review audit tasks owned by the current system/permission manager.',
    sectionTitle: 'Review Queue',
    headers: ['Request No', 'Type', 'Status', 'Created At', 'Account'],
    tabs: ['In Progress', 'Not Started'],
    empty: 'No review task data'
  }
}

const assets = ref<AssetNode[]>([])
const permissions = ref<DevicePermissionResult | null>(null)
const requests = ref<RequestItem[]>([])
const errorMessage = ref('')

const pagePath = computed<ManagerPath>(() =>
  route.meta.pagePath === '/sysAccessManager/reviewCheck' ? '/sysAccessManager/reviewCheck' : '/sysAccessManager/empAccessManagement'
)
const page = computed(() => (locale.value === 'zh-CN' ? zhPages : enPages)[pagePath.value])
const rows = computed<RowShape[]>(() => {
  if (pagePath.value === '/sysAccessManager/empAccessManagement') {
    const systemName = firstDeviceName(assets.value)
    return (permissions.value?.roles ?? []).map((role) => ({
      key: role.roleName,
      cells: [systemName, role.roleName, role.deviceAccounts.join(' / ') || '-', '启用']
    }))
  }

  return requests.value.map((request) => ({
    key: request.id,
    cells: [request.requestNo, request.requestType, request.status, request.createdAt || '-', request.targetAccountName || '-']
  }))
})
const rowCountText = computed(() =>
  locale.value === 'zh-CN' ? `共 ${rows.value.length} 行数据` : `${rows.value.length} visible rows`
)

onMounted(async () => {
  errorMessage.value = ''
  try {
    assets.value = await fetchAssetTree()
    requests.value = await fetchRequests()
    const deviceId = firstDeviceId(assets.value)
    permissions.value = deviceId ? await fetchDevicePermissions(deviceId) : { deviceNodeId: '', roles: [] }
  } catch (error) {
    errorMessage.value = error instanceof Error && error.message.trim().length > 0 ? error.message.trim() : 'load failed'
  }
})

function firstDeviceId(nodes: AssetNode[]): string {
  for (const node of nodes) {
    if (node.type === 'DEVICE') {
      return node.id
    }
    const nested = firstDeviceId(node.children)
    if (nested) {
      return nested
    }
  }
  return ''
}

function firstDeviceName(nodes: AssetNode[]): string {
  for (const node of nodes) {
    if (node.type === 'DEVICE') {
      return node.name
    }
    const nested = firstDeviceName(node.children)
    if (nested) {
      return nested
    }
  }
  return 'SYSTEM'
}
</script>

<style scoped>
.domain-page {
  display: grid;
  gap: 16px;
}

.page-header,
.data-card,
.message-card {
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
.data-card__header h2 {
  margin: 0;
  color: #303133;
}

.page-header p,
.data-card__header p {
  margin: 8px 0 0;
  color: #606266;
  line-height: 1.6;
}

.tab-strip {
  display: flex;
  gap: 8px;
}

.tab-button {
  min-height: 32px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #303133;
  padding: 0 12px;
}

.message-card {
  padding: 12px 14px;
}

.message-card--error {
  color: #f56c6c;
}

.data-card {
  overflow: hidden;
}

.data-card__header {
  padding: 16px 18px;
  border-bottom: 1px solid #ebeef5;
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
  color: #303133;
}

.result-table th {
  background: #f5f7fa;
  color: #606266;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
