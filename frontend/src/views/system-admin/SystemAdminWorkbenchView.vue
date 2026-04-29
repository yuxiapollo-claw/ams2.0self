<template>
  <section class="domain-page">
    <header class="page-header">
      <div>
        <h1>{{ page.title }}</h1>
        <p>{{ page.description }}</p>
      </div>
      <div class="header-actions">
        <button v-for="button in page.buttons" :key="button" type="button" class="ghost-button">{{ button }}</button>
        <button v-for="tab in page.tabs" :key="tab" type="button" class="ghost-button ghost-button--tab">{{ tab }}</button>
      </div>
    </header>

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
import { fetchDepartments, type DepartmentItem } from '../../api/departments'
import { fetchDeviceAccounts, type DeviceAccountItem } from '../../api/device-accounts'
import { fetchRequests, type RequestItem } from '../../api/requests'
import { fetchUsers, type UserItem } from '../../api/users'
import { useI18nText } from '../../i18n'

type AdminPath =
  | '/systemAdmin/userAccessManagement'
  | '/systemAdmin/empAccessManagement'
  | '/systemAdmin/copyProfile'
  | '/systemAdmin/sysAccessAdmin'
  | '/systemAdmin/reviewManagement'
  | '/systemAdmin/admindelegation'
  | '/systemAdmin/adminworkflow'
  | '/systemAdmin/applicationConfig'
  | '/systemAdmin/mailTemplateCfg'

interface PageConfig {
  title: string
  description: string
  sectionTitle: string
  headers: string[]
  buttons: string[]
  tabs: string[]
  empty: string
}

interface RowShape {
  key: string
  cells: string[]
}

const route = useRoute()
const { locale } = useI18nText()

const zhPages: Record<AdminPath, PageConfig> = {
  '/systemAdmin/userAccessManagement': {
    title: '用户权限查看',
    description: '查看全局账号与用户绑定关系。',
    sectionTitle: '账号绑定明细',
    headers: ['前缀', '账号名称', '账号类型', '账号描述'],
    buttons: [],
    tabs: [],
    empty: '暂无账号数据'
  },
  '/systemAdmin/empAccessManagement': {
    title: '系统用户管理',
    description: '查看系统与权限角色的覆盖情况。',
    sectionTitle: '系统权限明细',
    headers: ['权限路径', '状态', '文件管理', '账号管理'],
    buttons: [],
    tabs: [],
    empty: '暂无系统用户数据'
  },
  '/systemAdmin/copyProfile': {
    title: '用户权限复制',
    description: '查看参考用户与目标用户的权限复制计划。',
    sectionTitle: '复制计划',
    headers: ['参考用户', '目标用户', '权限路径'],
    buttons: [],
    tabs: [],
    empty: '暂无复制计划'
  },
  '/systemAdmin/sysAccessAdmin': {
    title: '系统/权限管理',
    description: '查看系统、权限和节点主数据。',
    sectionTitle: '系统与权限清单',
    headers: ['系统/权限', '节点类型', '子节点数'],
    buttons: [],
    tabs: ['系统', '权限'],
    empty: '暂无系统/权限数据'
  },
  '/systemAdmin/reviewManagement': {
    title: '审核管理',
    description: '查看审核计划和相关申请单。',
    sectionTitle: '审核计划',
    headers: ['系统名称', '审核名称', '审核类型', '开始日期', '详情'],
    buttons: [],
    tabs: [],
    empty: '暂无审核计划'
  },
  '/systemAdmin/admindelegation': {
    title: '代理设置',
    description: '查看管理员代理配置。',
    sectionTitle: '代理配置列表',
    headers: ['用户名', '用户全称', '开始日期', '结束日期', '状态'],
    buttons: [],
    tabs: [],
    empty: '暂无代理配置'
  },
  '/systemAdmin/adminworkflow': {
    title: '工作流配置',
    description: '查看工作流和工作组配置。',
    sectionTitle: '工作流定义',
    headers: ['工作流名称', '描述', '步骤数', '系统名称'],
    buttons: [],
    tabs: ['工作流', '工作组'],
    empty: '暂无工作流配置'
  },
  '/systemAdmin/applicationConfig': {
    title: '应用配置管理',
    description: '查看应用名称与前缀。',
    sectionTitle: '应用配置列表',
    headers: ['应用名称', '前缀'],
    buttons: ['删除', '修改', '添加'],
    tabs: [],
    empty: '暂无应用配置'
  },
  '/systemAdmin/mailTemplateCfg': {
    title: '邮件模板配置',
    description: '查看邮件通知模板。',
    sectionTitle: '模板清单',
    headers: ['模板名称', '描述', '主题', '详情'],
    buttons: ['删除', '修改', '添加'],
    tabs: [],
    empty: '暂无邮件模板'
  }
}

const enPages: Record<AdminPath, PageConfig> = {
  '/systemAdmin/userAccessManagement': {
    title: 'User Access Overview',
    description: 'Review global account-to-user bindings.',
    sectionTitle: 'Account Binding Detail',
    headers: ['Prefix', 'Account', 'Type', 'Description'],
    buttons: [],
    tabs: [],
    empty: 'No account data'
  },
  '/systemAdmin/empAccessManagement': {
    title: 'System User Management',
    description: 'Review system and entitlement coverage.',
    sectionTitle: 'System Permission Detail',
    headers: ['Permission Path', 'Status', 'Document Control', 'Account Management'],
    buttons: [],
    tabs: [],
    empty: 'No system user data'
  },
  '/systemAdmin/copyProfile': {
    title: 'Copy User Profile',
    description: 'Review source and target user profile-copy plans.',
    sectionTitle: 'Copy Plans',
    headers: ['Source User', 'Target User', 'Permission Path'],
    buttons: [],
    tabs: [],
    empty: 'No copy plans'
  },
  '/systemAdmin/sysAccessAdmin': {
    title: 'System / Permission Admin',
    description: 'Review system, permission, and node master data.',
    sectionTitle: 'System and Permission Inventory',
    headers: ['Node', 'Type', 'Child Count'],
    buttons: [],
    tabs: ['Systems', 'Permissions'],
    empty: 'No system/permission data'
  },
  '/systemAdmin/reviewManagement': {
    title: 'Review Management',
    description: 'Review audit plans and related requests.',
    sectionTitle: 'Review Plans',
    headers: ['System', 'Review Name', 'Type', 'Start Date', 'Detail'],
    buttons: [],
    tabs: [],
    empty: 'No review plans'
  },
  '/systemAdmin/admindelegation': {
    title: 'Admin Delegation',
    description: 'Review administrator delegation settings.',
    sectionTitle: 'Delegation List',
    headers: ['Login Name', 'Full Name', 'Start Date', 'End Date', 'Status'],
    buttons: [],
    tabs: [],
    empty: 'No delegation settings'
  },
  '/systemAdmin/adminworkflow': {
    title: 'Workflow Configuration',
    description: 'Review workflows and workgroup configuration.',
    sectionTitle: 'Workflow Definitions',
    headers: ['Workflow', 'Description', 'Steps', 'System'],
    buttons: [],
    tabs: ['Workflows', 'Workgroups'],
    empty: 'No workflows'
  },
  '/systemAdmin/applicationConfig': {
    title: 'Application Configuration',
    description: 'Review application names and prefixes.',
    sectionTitle: 'Application Config List',
    headers: ['Application', 'Prefix'],
    buttons: ['Delete', 'Edit', 'Add'],
    tabs: [],
    empty: 'No application configuration'
  },
  '/systemAdmin/mailTemplateCfg': {
    title: 'Mail Template Configuration',
    description: 'Review notification mail templates.',
    sectionTitle: 'Template Inventory',
    headers: ['Template', 'Description', 'Subject', 'Detail'],
    buttons: ['Delete', 'Edit', 'Add'],
    tabs: [],
    empty: 'No mail templates'
  }
}

const users = ref<UserItem[]>([])
const deviceAccounts = ref<DeviceAccountItem[]>([])
const requests = ref<RequestItem[]>([])
const assets = ref<AssetNode[]>([])
const departments = ref<DepartmentItem[]>([])

const pagePath = computed<AdminPath>(() => {
  const value = typeof route.meta.pagePath === 'string' ? route.meta.pagePath : route.path
  if (
    value === '/systemAdmin/empAccessManagement' ||
    value === '/systemAdmin/copyProfile' ||
    value === '/systemAdmin/sysAccessAdmin' ||
    value === '/systemAdmin/reviewManagement' ||
    value === '/systemAdmin/admindelegation' ||
    value === '/systemAdmin/adminworkflow' ||
    value === '/systemAdmin/applicationConfig' ||
    value === '/systemAdmin/mailTemplateCfg'
  ) {
    return value
  }
  return '/systemAdmin/userAccessManagement'
})
const page = computed(() => (locale.value === 'zh-CN' ? zhPages : enPages)[pagePath.value])

const rows = computed<RowShape[]>(() => {
  switch (pagePath.value) {
    case '/systemAdmin/userAccessManagement':
      return deviceAccounts.value.map((account) => ({
        key: account.id,
        cells: [account.deviceName, account.accountName, account.sourceType, account.remark || '-']
      }))
    case '/systemAdmin/empAccessManagement':
      return deviceAccounts.value.map((account) => ({
        key: account.id,
        cells: [account.roles.join(' / ') || `${account.deviceName}/${account.accountName}`, '启用', account.deviceName, account.accountName]
      }))
    case '/systemAdmin/copyProfile': {
      const sourceUser = users.value[0]
      const targetUser = users.value[1] ?? users.value[0]
      return sourceUser && targetUser
        ? [{
            key: `${sourceUser.id}-${targetUser.id}`,
            cells: [sourceUser.userName, targetUser.userName, deviceAccounts.value[0]?.roles.join(' / ') || '默认权限组']
          }]
        : []
    }
    case '/systemAdmin/sysAccessAdmin':
      return flattenAssets(assets.value).map((node) => ({
        key: node.id,
        cells: [node.name, node.type || '-', String(node.childCount)]
      }))
    case '/systemAdmin/reviewManagement':
      return requests.value.map((request) => ({
        key: request.id,
        cells: [resolveSystemName(request.targetAccountName), request.requestNo, request.requestType, request.createdAt || '-', request.targetAccountName || '-']
      }))
    case '/systemAdmin/admindelegation':
      return users.value.map((user) => ({
        key: user.id,
        cells: [user.loginName, user.userName, '2026-04-01', '2026-12-31', user.accountStatus]
      }))
    case '/systemAdmin/adminworkflow':
      return [
        { key: 'wf-1', cells: ['权限申请流', '申请权限审批到执行', '4', 'DMS'] },
        { key: 'wf-2', cells: ['权限删除流', '权限删除审批流程', '3', 'DMS'] },
        { key: 'wf-3', cells: ['密码重置流', '密码重置审批流程', '2', 'DMS'] }
      ]
    case '/systemAdmin/applicationConfig':
      return topLevelSystems(assets.value).map((system) => ({
        key: system.id,
        cells: [system.name, `APP-${system.name.toUpperCase().replace(/\s+/g, '-')}`]
      }))
    case '/systemAdmin/mailTemplateCfg':
      return [
        { key: 'mail-1', cells: ['申请创建通知', '提交申请后通知审批人', '【AMS】新申请通知', '标准审批通知模板'] },
        { key: 'mail-2', cells: ['执行完成通知', '执行完成后通知申请人', '【AMS】权限执行完成', '执行完成结果模板'] }
      ]
  }
})

const rowCountText = computed(() =>
  locale.value === 'zh-CN' ? `共 ${rows.value.length} 行数据` : `${rows.value.length} visible rows`
)

onMounted(async () => {
  const [loadedUsers, loadedAccounts, loadedRequests, loadedAssets, loadedDepartments] = await Promise.all([
    fetchUsers(),
    fetchDeviceAccounts(),
    fetchRequests(),
    fetchAssetTree(),
    fetchDepartments()
  ])
  users.value = loadedUsers
  deviceAccounts.value = loadedAccounts
  requests.value = loadedRequests
  assets.value = loadedAssets
  departments.value = loadedDepartments
})

function topLevelSystems(nodes: AssetNode[]) {
  return nodes.filter((node) => node.type === 'DEVICE' || node.children.length > 0)
}

function flattenAssets(nodes: AssetNode[]) {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, type: node.type ?? '', childCount: node.children.length },
    ...flattenAssets(node.children)
  ])
}

function resolveSystemName(accountName: string) {
  const matched = deviceAccounts.value.find((account) => account.accountName === accountName)
  return matched?.deviceName ?? departments.value[0]?.departmentName ?? 'DMS'
}
</script>

<style scoped>
.domain-page {
  display: grid;
  gap: 16px;
}

.page-header,
.data-card {
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
}

.ghost-button--tab {
  background: #f5f7fa;
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
