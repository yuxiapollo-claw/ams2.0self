<template>
  <section class="domain-page">
    <header class="page-header">
      <div>
        <h1>{{ page.title }}</h1>
        <p>{{ page.description }}</p>
      </div>
      <div v-if="page.buttons.length" class="header-actions">
        <button v-for="button in page.buttons" :key="button" type="button" class="ghost-button">
          {{ button }}
        </button>
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
import { fetchAuditLogs, type AuditLogItem } from '../../api/audit'
import { fetchDepartments, type DepartmentItem } from '../../api/departments'
import { fetchDeviceAccounts, type DeviceAccountItem } from '../../api/device-accounts'
import { fetchRequests, type RequestItem } from '../../api/requests'
import { fetchUsers, type UserItem } from '../../api/users'
import { useI18nText } from '../../i18n'

type ReportPath =
  | '/report/accountHistory'
  | '/report/reviewReport'
  | '/report/empAccess'
  | '/report/systemAccess'
  | '/report/sysconfig'
  | '/report/variationReport'

interface PageConfig {
  title: string
  description: string
  sectionTitle: string
  headers: string[]
  buttons: string[]
  empty: string
}

interface RowShape {
  key: string
  cells: string[]
}

const route = useRoute()
const { locale } = useI18nText()

const zhPages: Record<ReportPath, PageConfig> = {
  '/report/accountHistory': {
    title: '账号历史报告',
    description: '查看账号申请、变更与流转历史。',
    sectionTitle: '账号历史明细',
    headers: ['申请单号', '目标账号', '申请类型', '状态', '创建时间'],
    buttons: [],
    empty: '暂无账号历史数据'
  },
  '/report/reviewReport': {
    title: '审核报告',
    description: '查看审核类任务和审批进度。',
    sectionTitle: '审核统计',
    headers: ['系统名称', '审核单号', '审核类型', '审批状态', '时间'],
    buttons: ['PDF', 'Excel'],
    empty: '暂无审核报告数据'
  },
  '/report/empAccess': {
    title: '用户权限报告',
    description: '从人员维度查看账号和权限分配。',
    sectionTitle: '用户权限明细',
    headers: ['用户', '部门', '账号', '角色'],
    buttons: ['搜索', '导出'],
    empty: '暂无用户权限数据'
  },
  '/report/systemAccess': {
    title: '系统用户报告',
    description: '从系统维度统计账号和用户覆盖情况。',
    sectionTitle: '系统覆盖统计',
    headers: ['系统名称', '账号数量', '绑定用户数', '角色数量'],
    buttons: ['搜索', '导出报告'],
    empty: '暂无系统用户报告数据'
  },
  '/report/sysconfig': {
    title: '系统配置报告',
    description: '查看系统权限与工作流配置概览。',
    sectionTitle: '系统配置清单',
    headers: ['状态', '权限描述', '权限编码', '权限经理', '申请工作流', '删除工作流', '重置密码工作流', '转部门工作流'],
    buttons: [],
    empty: '暂无系统配置数据'
  },
  '/report/variationReport': {
    title: '权限差异报告',
    description: '识别已配置和缺失权限的差异项。',
    sectionTitle: '差异明细',
    headers: ['权限路径', '状态', '详情'],
    buttons: ['生成报告', '导入权限列表'],
    empty: '暂无权限差异数据'
  }
}

const enPages: Record<ReportPath, PageConfig> = {
  '/report/accountHistory': {
    title: 'Account History Report',
    description: 'Review request, change, and workflow history for accounts.',
    sectionTitle: 'Account History Detail',
    headers: ['Request No', 'Target Account', 'Request Type', 'Status', 'Created At'],
    buttons: [],
    empty: 'No account history data'
  },
  '/report/reviewReport': {
    title: 'Review Report',
    description: 'Review audit-style tasks and approval progress.',
    sectionTitle: 'Review Statistics',
    headers: ['System', 'Review Ticket', 'Review Type', 'Status', 'Time'],
    buttons: ['PDF', 'Excel'],
    empty: 'No review report data'
  },
  '/report/empAccess': {
    title: 'Employee Access Report',
    description: 'Review account and entitlement allocation by employee.',
    sectionTitle: 'Employee Access Detail',
    headers: ['User', 'Department', 'Account', 'Roles'],
    buttons: ['Search', 'Export'],
    empty: 'No employee access data'
  },
  '/report/systemAccess': {
    title: 'System Access Report',
    description: 'Summarize account and user coverage by system.',
    sectionTitle: 'System Coverage',
    headers: ['System', 'Accounts', 'Bound Users', 'Roles'],
    buttons: ['Search', 'Export Report'],
    empty: 'No system access data'
  },
  '/report/sysconfig': {
    title: 'System Configuration Report',
    description: 'Review system permissions and workflow configuration.',
    sectionTitle: 'System Configuration',
    headers: ['Status', 'Permission', 'Code', 'Owner', 'Apply Flow', 'Remove Flow', 'Reset Flow', 'Transfer Flow'],
    buttons: [],
    empty: 'No system configuration data'
  },
  '/report/variationReport': {
    title: 'Permission Variation Report',
    description: 'Identify configured and missing permission differences.',
    sectionTitle: 'Variation Detail',
    headers: ['Permission Path', 'Status', 'Detail'],
    buttons: ['Generate Report', 'Import Permission List'],
    empty: 'No variation data'
  }
}

const requests = ref<RequestItem[]>([])
const users = ref<UserItem[]>([])
const departments = ref<DepartmentItem[]>([])
const deviceAccounts = ref<DeviceAccountItem[]>([])
const assets = ref<AssetNode[]>([])
const auditLogs = ref<AuditLogItem[]>([])
const errorMessage = ref('')

const pagePath = computed<ReportPath>(() => {
  const value = typeof route.meta.pagePath === 'string' ? route.meta.pagePath : route.path
  if (
    value === '/report/reviewReport' ||
    value === '/report/empAccess' ||
    value === '/report/systemAccess' ||
    value === '/report/sysconfig' ||
    value === '/report/variationReport'
  ) {
    return value
  }
  return '/report/accountHistory'
})
const page = computed(() => (locale.value === 'zh-CN' ? zhPages : enPages)[pagePath.value])

const rows = computed<RowShape[]>(() => {
  switch (pagePath.value) {
    case '/report/accountHistory':
      return requests.value.map((request) => ({
        key: request.id,
        cells: [request.requestNo, request.targetAccountName || '-', request.requestType, request.status, request.createdAt || '-']
      }))
    case '/report/reviewReport':
      return requests.value.map((request) => ({
        key: request.id,
        cells: [resolveSystemName(request.targetAccountName), request.requestNo, request.requestType, request.status, request.createdAt || '-']
      }))
    case '/report/empAccess':
      return users.value.map((user) => {
        const accounts = deviceAccounts.value.filter((account) => account.userId === user.id)
        return {
          key: user.id,
          cells: [
            user.userName,
            user.departmentName,
            accounts.map((account) => account.accountName).join(' / ') || '-',
            accounts.flatMap((account) => account.roles).join(' / ') || '-'
          ]
        }
      })
    case '/report/systemAccess': {
      const rowsBySystem = new Map<string, { accountCount: number; users: Set<string>; roles: Set<string> }>()
      for (const account of deviceAccounts.value) {
        if (!rowsBySystem.has(account.deviceName)) {
          rowsBySystem.set(account.deviceName, { accountCount: 0, users: new Set<string>(), roles: new Set<string>() })
        }
        const current = rowsBySystem.get(account.deviceName)!
        current.accountCount += 1
        if (account.userName) {
          current.users.add(account.userName)
        }
        account.roles.forEach((role) => current.roles.add(role))
      }
      return Array.from(rowsBySystem.entries()).map(([systemName, current]) => ({
        key: systemName,
        cells: [systemName, String(current.accountCount), String(current.users.size), String(current.roles.size)]
      }))
    }
    case '/report/sysconfig':
      return flattenRoles(assets.value).map((role) => ({
        key: role.id,
        cells: ['启用', role.name, role.id, 'AMSAdmin', '权限申请流', '权限删除流', '密码重置流', '转部门流']
      }))
    case '/report/variationReport':
      return deviceAccounts.value.map((account) => ({
        key: account.id,
        cells: [
          `${account.deviceName}/${account.accountName}`,
          account.roles.length > 0 ? '已配置' : '待完善',
          account.roles.join(' / ') || '当前账号未绑定角色'
        ]
      }))
  }
})

const rowCountText = computed(() =>
  locale.value === 'zh-CN' ? `共 ${rows.value.length} 行数据` : `${rows.value.length} visible rows`
)

onMounted(async () => {
  errorMessage.value = ''
  try {
    const [loadedRequests, loadedUsers, loadedDepartments, loadedDeviceAccounts, loadedAssets, loadedAuditLogs] = await Promise.all([
      fetchRequests(),
      fetchUsers(),
      fetchDepartments(),
      fetchDeviceAccounts(),
      fetchAssetTree(),
      fetchAuditLogs()
    ])
    requests.value = loadedRequests
    users.value = loadedUsers
    departments.value = loadedDepartments
    deviceAccounts.value = loadedDeviceAccounts
    assets.value = loadedAssets
    auditLogs.value = loadedAuditLogs.list
  } catch (error) {
    errorMessage.value = error instanceof Error && error.message.trim().length > 0 ? error.message.trim() : 'load failed'
  }
})

function resolveSystemName(accountName: string) {
  const matched = deviceAccounts.value.find((account) => account.accountName === accountName)
  return matched?.deviceName ?? auditLogs.value[0]?.action.split('(')[1]?.replace(')', '').trim() ?? 'SYSTEM'
}

function flattenRoles(nodes: AssetNode[]): Array<{ id: string; name: string }> {
  return nodes.flatMap((node) => {
    const current = node.type === 'ROLE' ? [{ id: node.id, name: node.name }] : []
    return current.concat(flattenRoles(node.children))
  })
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

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-start;
}

.ghost-button {
  min-height: 32px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #303133;
  padding: 0 12px;
  cursor: pointer;
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
