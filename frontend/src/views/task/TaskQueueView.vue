<template>
  <section class="task-page">
    <header class="page-header">
      <div>
        <h1>{{ pageCopy.title }}</h1>
        <p>{{ pageCopy.description }}</p>
      </div>
      <div class="header-summary">
        <div>{{ copy.summary.pending }}: {{ visibleRows.length }}</div>
      </div>
    </header>

    <section class="panel-card">
      <header class="section-header">
        <h2>{{ sectionTitle }}</h2>
        <div v-if="pagePath === '/task/accessOperation'" class="tab-strip">
          <button
            type="button"
            class="tab-button"
            :class="{ 'is-active': operationTab === 'pending' }"
            @click="operationTab = 'pending'"
          >
            {{ copy.tabs.pending }}
          </button>
          <button
            type="button"
            class="tab-button"
            :class="{ 'is-active': operationTab === 'done' }"
            @click="operationTab = 'done'"
          >
            {{ copy.tabs.done }}
          </button>
        </div>
      </header>

      <div v-if="feedbackMessage" class="feedback-card">{{ feedbackMessage }}</div>

      <div class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th>{{ copy.table.requestNo }}</th>
              <th>{{ copy.table.requestType }}</th>
              <th>{{ copy.table.account }}</th>
              <th>{{ copy.table.status }}</th>
              <th>{{ copy.table.createdAt }}</th>
              <th>{{ copy.table.actions }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in visibleRows" :key="row.id">
              <td>{{ row.requestNo }}</td>
              <td>{{ row.requestType }}</td>
              <td>{{ row.targetAccountName || '-' }}</td>
              <td>{{ row.status }}</td>
              <td>{{ row.createdAt || '-' }}</td>
              <td class="action-cell">
                <button
                  v-if="pagePath === '/task/accessApproval'"
                  :data-testid="`approve-request-${row.id}`"
                  class="action-button"
                  type="button"
                  @click="handleApprove(row.id)"
                >
                  {{ copy.actions.approve }}
                </button>
                <button
                  v-if="pagePath === '/task/accessOperation' && operationTab === 'pending'"
                  :data-testid="`complete-request-${row.id}`"
                  class="action-button"
                  type="button"
                  @click="handleComplete(row.id)"
                >
                  {{ copy.actions.complete }}
                </button>
                <span v-if="pagePath !== '/task/accessApproval' && !(pagePath === '/task/accessOperation' && operationTab === 'pending')">
                  -
                </span>
              </td>
            </tr>
            <tr v-if="visibleRows.length === 0">
              <td colspan="6">{{ copy.empty }}</td>
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
import { submitExecution } from '../../api/executions'
import { fetchRequests, type RequestItem } from '../../api/requests'
import { approveRequest } from '../../api/tasks'
import { useI18nText } from '../../i18n'

type TaskPath =
  | '/task/accessApproval'
  | '/task/accessOperation'
  | '/task/reviewApproval'
  | '/task/revocation'

const route = useRoute()
const { locale } = useI18nText()

const zhPages = {
  '/task/accessApproval': {
    title: '审批任务',
    description: '处理待审批的权限申请任务。',
    sectionTitle: '待处理队列'
  },
  '/task/accessOperation': {
    title: '操作任务',
    description: '处理已审批通过、待执行的权限操作。',
    sectionTitle: '待操作'
  },
  '/task/reviewApproval': {
    title: '审核任务',
    description: '查看审核相关任务队列。',
    sectionTitle: '审核任务队列'
  },
  '/task/revocation': {
    title: '申诉任务',
    description: '查看申诉与复审任务。',
    sectionTitle: '申诉任务队列'
  }
} as const

const enPages = {
  '/task/accessApproval': {
    title: 'Approval Tasks',
    description: 'Process pending access-approval requests.',
    sectionTitle: 'Pending Queue'
  },
  '/task/accessOperation': {
    title: 'Operation Tasks',
    description: 'Process approved requests waiting for execution.',
    sectionTitle: 'Pending Operations'
  },
  '/task/reviewApproval': {
    title: 'Review Tasks',
    description: 'Review audit-related task queues.',
    sectionTitle: 'Review Queue'
  },
  '/task/revocation': {
    title: 'Appeal Tasks',
    description: 'Review appeal and recheck tasks.',
    sectionTitle: 'Appeal Queue'
  }
} as const

const zhCopy = {
  summary: {
    pending: '当前行数'
  },
  tabs: {
    pending: '待操作',
    done: '已完成'
  },
  table: {
    requestNo: '申请单号',
    requestType: '申请类型',
    account: '目标账号',
    status: '状态',
    createdAt: '创建时间',
    actions: '操作'
  },
  actions: {
    approve: '同意',
    complete: '完成'
  },
  feedback: {
    approved: '审批已流转',
    completed: '执行已完成'
  },
  empty: '暂无数据'
} as const

const enCopy = {
  summary: {
    pending: 'Rows'
  },
  tabs: {
    pending: 'Pending',
    done: 'Completed'
  },
  table: {
    requestNo: 'Request No',
    requestType: 'Request Type',
    account: 'Target Account',
    status: 'Status',
    createdAt: 'Created At',
    actions: 'Actions'
  },
  actions: {
    approve: 'Approve',
    complete: 'Complete'
  },
  feedback: {
    approved: 'Approval advanced',
    completed: 'Execution completed'
  },
  empty: 'No data'
} as const

const requests = ref<RequestItem[]>([])
const operationTab = ref<'pending' | 'done'>('pending')
const feedbackMessage = ref('')

const pagePath = computed<TaskPath>(() => {
  const value = typeof route.meta.pagePath === 'string' ? route.meta.pagePath : route.path
  if (
    value === '/task/accessOperation' ||
    value === '/task/reviewApproval' ||
    value === '/task/revocation'
  ) {
    return value
  }
  return '/task/accessApproval'
})
const pageCopy = computed(() => (locale.value === 'zh-CN' ? zhPages : enPages)[pagePath.value])
const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))
const sectionTitle = computed(() => pageCopy.value.sectionTitle)
const visibleRows = computed(() => {
  if (pagePath.value === '/task/accessApproval') {
    return requests.value.filter((request) =>
      ['WAIT_DEPT_MANAGER', 'WAIT_QA', 'WAIT_QM'].includes(request.status)
    )
  }

  if (pagePath.value === '/task/accessOperation') {
    return requests.value.filter((request) =>
      operationTab.value === 'pending' ? request.status === 'WAIT_QI_EXECUTE' : request.status === 'COMPLETED'
    )
  }

  if (pagePath.value === '/task/reviewApproval') {
    return requests.value.filter((request) => ['WAIT_QA', 'WAIT_QM'].includes(request.status))
  }

  return requests.value.filter((request) => request.status === 'REJECTED')
})

onMounted(() => {
  void loadRequests()
})

async function loadRequests() {
  requests.value = await fetchRequests()
}

async function handleApprove(requestId: string) {
  await approveRequest(requestId)
  feedbackMessage.value = copy.value.feedback.approved
  await loadRequests()
}

async function handleComplete(requestId: string) {
  await submitExecution(requestId)
  feedbackMessage.value = copy.value.feedback.completed
  await loadRequests()
}
</script>

<style scoped>
.task-page {
  display: grid;
  gap: 16px;
}

.page-header,
.panel-card,
.feedback-card {
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
  color: #606266;
}

.panel-card {
  padding: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.tab-strip {
  display: flex;
  gap: 8px;
}

.tab-button,
.action-button {
  min-height: 32px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #303133;
  padding: 0 12px;
  cursor: pointer;
}

.tab-button.is-active {
  border-color: #409eff;
  color: #409eff;
}

.feedback-card {
  margin-bottom: 12px;
  padding: 10px 12px;
  color: #606266;
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

.action-cell {
  min-width: 120px;
}

@media (max-width: 960px) {
  .page-header,
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
