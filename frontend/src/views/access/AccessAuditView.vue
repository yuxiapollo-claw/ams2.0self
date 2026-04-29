<template>
  <section class="audit-page">
    <header class="page-header">
      <h1>{{ pageCopy.title }}</h1>
      <p>{{ pageCopy.description }}</p>
    </header>

    <section class="panel-card">
      <div class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th v-for="header in pageCopy.headers" :key="header">{{ header }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.key">
              <td v-for="cell in row.cells" :key="cell">{{ cell }}</td>
            </tr>
            <tr v-if="rows.length === 0">
              <td :colspan="pageCopy.headers.length">{{ copy.empty }}</td>
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
import { fetchRequests, type RequestItem } from '../../api/requests'
import { fetchUsers, type UserItem } from '../../api/users'
import { useI18nText } from '../../i18n'

type AuditPath = '/access/reviewCheck' | '/access/bedelegation'

interface TableRow {
  key: string
  cells: string[]
}

const route = useRoute()
const { locale } = useI18nText()

const zhPages = {
  '/access/reviewCheck': {
    title: '审核查看',
    description: '查看与当前用户相关的审核型记录。',
    headers: ['申请单号', '申请类型', '状态', '创建时间', '详情']
  },
  '/access/bedelegation': {
    title: '被授权查看',
    description: '查看当前生效的被授权人员信息。',
    headers: ['用户名', '用户全名', '开始日期', '结束日期', '状态']
  }
} as const

const enPages = {
  '/access/reviewCheck': {
    title: 'Review Check',
    description: 'Review audit-style records related to the current user.',
    headers: ['Request No', 'Type', 'Status', 'Created At', 'Detail']
  },
  '/access/bedelegation': {
    title: 'Delegated To Me',
    description: 'Review active delegated-user entries.',
    headers: ['Login Name', 'Full Name', 'Start Date', 'End Date', 'Status']
  }
} as const

const copy = computed(() => ({
  empty: locale.value === 'zh-CN' ? '暂无数据' : 'No data'
}))

const requests = ref<RequestItem[]>([])
const users = ref<UserItem[]>([])

const pagePath = computed<AuditPath>(() =>
  route.meta.pagePath === '/access/bedelegation' ? '/access/bedelegation' : '/access/reviewCheck'
)
const pageCopy = computed(() => (locale.value === 'zh-CN' ? zhPages : enPages)[pagePath.value])
const rows = computed<TableRow[]>(() => {
  if (pagePath.value === '/access/reviewCheck') {
    return requests.value.map((request) => ({
      key: request.id,
      cells: [
        request.requestNo,
        request.requestType,
        request.status,
        request.createdAt || '-',
        request.targetAccountName || '-'
      ]
    }))
  }

  return users.value.map((user) => ({
    key: user.id,
    cells: [user.loginName, user.userName, '2026-04-01', '2026-12-31', user.accountStatus]
  }))
})

onMounted(async () => {
  const [loadedRequests, loadedUsers] = await Promise.all([fetchRequests(), fetchUsers()])
  requests.value = loadedRequests
  users.value = loadedUsers
})
</script>

<style scoped>
.audit-page {
  display: grid;
  gap: 16px;
}

.page-header,
.panel-card {
  border: 1px solid #dcdfe6;
  background: #fff;
}

.page-header {
  padding: 18px 20px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.page-header p {
  margin: 8px 0 0;
  color: #606266;
  line-height: 1.6;
}

.panel-card {
  padding: 16px;
}

.table-wrap {
  overflow-x: auto;
}

.result-table {
  width: 100%;
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
</style>
