<template>
  <section class="workbench">
    <PageHero :kicker="copy.kicker" :title="copy.title" :subtitle="copy.subtitle" />

    <div class="grid">
      <section class="panel-card">
        <header class="panel-card__header">
          <div>
            <h2 class="panel-card__title">{{ copy.treeTitle }}</h2>
            <p class="panel-card__meta">{{ copy.treeSubtitle }}</p>
          </div>
          <button class="inline-action" type="button" @click="loadPage">{{ copy.actions.refresh }}</button>
        </header>

        <div v-if="loading" class="loading-card">{{ copy.loading }}</div>
        <div v-else-if="errorMessage" class="error-card">{{ errorMessage }}</div>
        <div v-else-if="treeData.length === 0" class="empty-state">{{ copy.empty }}</div>

        <el-tree
          v-else
          class="permission-tree"
          node-key="key"
          :data="treeData"
          :current-node-key="selectedNode?.key"
          :expand-on-click-node="false"
          default-expand-all
          @node-click="handleNodeSelect"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <div>
                <div class="tree-node__label">{{ data.label }}</div>
                <div class="tree-node__path">{{ data.fullPath }}</div>
              </div>
              <span
                v-if="data.nodeType === 'PERMISSION'"
                class="tree-node__badge"
                :class="{ 'tree-node__badge--disabled': !data.enabled }"
              >
                {{ data.enabled ? copy.enabled : copy.disabled }}
              </span>
            </div>
          </template>
        </el-tree>
      </section>

      <section class="panel-card">
        <header class="panel-card__header">
          <div>
            <h2 class="panel-card__title">{{ copy.formTitle }}</h2>
            <p class="panel-card__meta">{{ copy.formSubtitle }}</p>
          </div>
        </header>

        <div class="detail-stack">
          <article class="detail-card">
            <div class="detail-card__label">{{ copy.selectedPath }}</div>
            <div class="detail-card__value detail-card__value--path">
              {{ selectedNode?.fullPath || copy.noSelection }}
            </div>
          </article>

          <article class="detail-card">
            <div class="detail-card__label">{{ copy.selectedRule }}</div>
            <div class="detail-card__value">{{ copy.selectionHint }}</div>
          </article>

          <label class="field">
            <span>{{ copy.reason }}</span>
            <textarea v-model="reason" class="field__control field__control--textarea" rows="5" />
          </label>

          <div v-if="submitErrorMessage" class="error-card error-card--inline">{{ submitErrorMessage }}</div>

          <button class="hero-action" type="button" :disabled="submitting || !canSubmit" @click="submitRequest">
            {{ submitting ? copy.actions.submitting : copy.actions.submit }}
          </button>
        </div>
      </section>
    </div>

    <section class="panel-card">
      <header class="panel-card__header">
        <div>
          <h2 class="panel-card__title">{{ copy.historyTitle }}</h2>
          <p class="panel-card__meta">{{ copy.historySubtitle }}</p>
        </div>
      </header>

      <div v-if="requests.length === 0" class="empty-state">{{ copy.historyEmpty }}</div>
      <div v-else class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th>{{ copy.historyColumns.requestNo }}</th>
              <th>{{ copy.historyColumns.type }}</th>
              <th>{{ copy.historyColumns.path }}</th>
              <th>{{ copy.historyColumns.status }}</th>
              <th>{{ copy.historyColumns.createdAt }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in requests" :key="item.id">
              <td class="primary-cell">{{ item.requestNo }}</td>
              <td>{{ formatRequestType(item.requestType) }}</td>
              <td>{{ item.permissionPath }}</td>
              <td>{{ formatStatus(item.currentStatus) }}</td>
              <td>{{ formatDate(item.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createPermissionRequest,
  fetchMyPermissionTree,
  fetchPermissionRequests,
  fetchPermissionTree,
  type AccessPermissionTreeNode,
  type PermissionRequestItem
} from '../../api/access'
import PageHero from '../../components/shell/PageHero.vue'
import { useI18nText } from '../../i18n'

const props = defineProps<{
  mode: 'apply' | 'remove' | 'reset'
}>()

const { locale } = useI18nText()

const treeData = ref<AccessPermissionTreeNode[]>([])
const requests = ref<PermissionRequestItem[]>([])
const selectedNode = ref<AccessPermissionTreeNode | null>(null)
const reason = ref('')
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const submitErrorMessage = ref('')

const modeCopy = computed(() => {
  const zh = {
    apply: {
      kicker: '申请管理',
      title: '权限申请',
      subtitle: '以树形结构展示所有权限路径，选中最终权限路径后可提交权限申请流程。',
      treeSubtitle: '只有最终权限路径可以提交申请，系统节点和父级权限仅用于定位。',
      formSubtitle: '提交后会生成一条新的权限申请记录。',
      submitLabel: '提交权限申请'
    },
    remove: {
      kicker: '申请管理',
      title: '删除权限申请',
      subtitle: '以树形结构展示当前用户已经获得的权限，选中最终权限路径后提交删除申请。',
      treeSubtitle: '这里展示的是当前登录用户已经获得的权限路径。',
      formSubtitle: '提交后会生成一条权限删除申请记录。',
      submitLabel: '提交删除申请'
    },
    reset: {
      kicker: '申请管理',
      title: '密码重置申请',
      subtitle: '以树形结构展示当前用户已经获得的权限，选中最终权限路径后提交密码重置申请。',
      treeSubtitle: '这里展示的是当前登录用户已经获得的权限路径。',
      formSubtitle: '提交后会生成一条密码重置申请记录。',
      submitLabel: '提交密码重置申请'
    }
  } as const

  const en = {
    apply: {
      kicker: 'Request Management',
      title: 'Permission Apply',
      subtitle: 'Browse all permission paths as a tree and submit the workflow from a terminal permission path.',
      treeSubtitle: 'Only terminal permission paths can be submitted. System nodes and parent permissions are navigation anchors.',
      formSubtitle: 'Submitting creates a new permission request record.',
      submitLabel: 'Submit Apply Request'
    },
    remove: {
      kicker: 'Request Management',
      title: 'Permission Remove',
      subtitle: 'Browse the current user permissions as a tree and submit a removal request from a terminal permission path.',
      treeSubtitle: 'This tree only contains permissions currently granted to the login user.',
      formSubtitle: 'Submitting creates a new permission removal request.',
      submitLabel: 'Submit Remove Request'
    },
    reset: {
      kicker: 'Request Management',
      title: 'Password Reset',
      subtitle: 'Browse the current user permissions as a tree and submit a password reset request from a terminal permission path.',
      treeSubtitle: 'This tree only contains permissions currently granted to the login user.',
      formSubtitle: 'Submitting creates a new password reset request.',
      submitLabel: 'Submit Reset Request'
    }
  } as const

  const source = locale.value === 'zh-CN' ? zh : en
  return source[props.mode]
})

const copy = computed(() => ({
  kicker: modeCopy.value.kicker,
  title: modeCopy.value.title,
  subtitle: modeCopy.value.subtitle,
  treeTitle: locale.value === 'zh-CN' ? '权限树' : 'Permission Tree',
  treeSubtitle: modeCopy.value.treeSubtitle,
  formTitle: locale.value === 'zh-CN' ? '申请提交' : 'Request Submission',
  formSubtitle: modeCopy.value.formSubtitle,
  selectedPath: locale.value === 'zh-CN' ? '已选权限路径' : 'Selected Permission Path',
  selectedRule: locale.value === 'zh-CN' ? '选择规则' : 'Selection Rule',
  selectionHint:
    locale.value === 'zh-CN'
      ? '必须选中没有下级节点的最终权限路径后，才允许提交申请。'
      : 'You can only submit after selecting a terminal permission path with no child nodes.',
  noSelection: locale.value === 'zh-CN' ? '尚未选择权限路径' : 'No permission path selected',
  reason: locale.value === 'zh-CN' ? '申请原因' : 'Request Reason',
  loading: locale.value === 'zh-CN' ? '正在加载权限树...' : 'Loading permission tree...',
  empty:
    locale.value === 'zh-CN'
      ? props.mode === 'apply'
        ? '当前还没有可申请的权限路径。'
        : '当前用户没有可用于本操作的权限路径。'
      : props.mode === 'apply'
        ? 'There are no permission paths available for apply requests yet.'
        : 'The current user has no permission paths available for this action.',
  historyTitle: locale.value === 'zh-CN' ? '我的申请记录' : 'My Request Records',
  historySubtitle:
    locale.value === 'zh-CN'
      ? '这里展示当前登录用户可见的权限申请历史。'
      : 'These are the permission request records visible to the current login user.',
  historyEmpty: locale.value === 'zh-CN' ? '暂无申请记录。' : 'No request records yet.',
  historyColumns: {
    requestNo: locale.value === 'zh-CN' ? '申请单号' : 'Request No.',
    type: locale.value === 'zh-CN' ? '申请类型' : 'Type',
    path: locale.value === 'zh-CN' ? '权限路径' : 'Permission Path',
    status: locale.value === 'zh-CN' ? '状态' : 'Status',
    createdAt: locale.value === 'zh-CN' ? '提交时间' : 'Created At'
  },
  actions: {
    refresh: locale.value === 'zh-CN' ? '刷新' : 'Refresh',
    submit: modeCopy.value.submitLabel,
    submitting: locale.value === 'zh-CN' ? '提交中...' : 'Submitting...'
  },
  enabled: locale.value === 'zh-CN' ? '启用' : 'Enabled',
  disabled: locale.value === 'zh-CN' ? '停用' : 'Disabled'
}))

const canSubmit = computed(
  () =>
    selectedNode.value?.nodeType === 'PERMISSION' &&
    selectedNode.value.leaf &&
    selectedNode.value.enabled &&
    reason.value.trim().length > 0
)

onMounted(() => {
  void loadPage()
})

async function loadPage() {
  loading.value = true
  errorMessage.value = ''
  submitErrorMessage.value = ''

  try {
    const [tree, history] = await Promise.all([
      props.mode === 'apply' ? fetchPermissionTree() : fetchMyPermissionTree(),
      fetchPermissionRequests()
    ])
    treeData.value = tree
    requests.value = history
    if (selectedNode.value) {
      selectedNode.value = findNodeByKey(treeData.value, selectedNode.value.key)
    }
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
    treeData.value = []
    requests.value = []
  } finally {
    loading.value = false
  }
}

function handleNodeSelect(node: AccessPermissionTreeNode) {
  selectedNode.value = node
}

async function submitRequest() {
  if (!selectedNode.value || !canSubmit.value || submitting.value) {
    return
  }

  submitting.value = true
  submitErrorMessage.value = ''

  try {
    await createPermissionRequest({
      requestType: mapRequestType(props.mode),
      permissionId: Number(selectedNode.value.entityId),
      reason: reason.value.trim()
    })
    reason.value = ''
    ElMessage.success(copy.value.actions.submit)
    await loadPage()
  } catch (error) {
    submitErrorMessage.value = extractErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

function mapRequestType(mode: 'apply' | 'remove' | 'reset') {
  switch (mode) {
    case 'apply':
      return 'PERMISSION_APPLY'
    case 'remove':
      return 'PERMISSION_REMOVE'
    default:
      return 'PASSWORD_RESET'
  }
}

function formatRequestType(value: string) {
  const map =
    locale.value === 'zh-CN'
      ? {
          PERMISSION_APPLY: '权限申请',
          PERMISSION_REMOVE: '删除权限申请',
          PASSWORD_RESET: '密码重置申请'
        }
      : {
          PERMISSION_APPLY: 'Permission Apply',
          PERMISSION_REMOVE: 'Permission Remove',
          PASSWORD_RESET: 'Password Reset'
        }
  return map[value as keyof typeof map] ?? value
}

function formatStatus(value: string) {
  return locale.value === 'zh-CN'
    ? value === 'PENDING'
      ? '处理中'
      : value === 'COMPLETED'
        ? '已完成'
        : value
    : value === 'PENDING'
      ? 'Pending'
      : value === 'COMPLETED'
        ? 'Completed'
        : value
}

function formatDate(value: string) {
  return value ? value.replace('T', ' ').replace('Z', '') : '-'
}

function findNodeByKey(nodes: AccessPermissionTreeNode[], key: string): AccessPermissionTreeNode | null {
  for (const node of nodes) {
    if (node.key === key) {
      return node
    }
    const child = findNodeByKey(node.children, key)
    if (child) {
      return child
    }
  }
  return null
}

function extractErrorMessage(error: unknown) {
  if (error && typeof error === 'object') {
    const response = (error as { response?: { data?: { message?: string } } }).response
    const backendMessage = response?.data?.message
    if (typeof backendMessage === 'string' && backendMessage.trim().length > 0) {
      return backendMessage
    }
  }
  return error instanceof Error && error.message.trim().length > 0 ? error.message : 'Operation failed'
}
</script>

<style scoped>
.workbench {
  display: grid;
  gap: 14px;
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.9fr);
  gap: 14px;
}

.panel-card,
.loading-card,
.error-card {
  border: 1px solid var(--cockpit-border);
  border-radius: var(--cockpit-radius-lg);
  box-shadow: var(--cockpit-shadow);
  background: rgba(255, 255, 255, 0.05);
}

.panel-card {
  padding: 18px;
}

.panel-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-card__title {
  margin: 0;
  font-size: 18px;
}

.panel-card__meta {
  margin: 8px 0 0;
  color: var(--cockpit-muted);
  line-height: 1.6;
}

.permission-tree {
  margin-top: 16px;
}

.permission-tree :deep(.el-tree-node__content) {
  min-height: 54px;
  padding: 4px 0;
}

.permission-tree :deep(.el-tree-node__content:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.tree-node {
  width: 100%;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.tree-node__label {
  font-size: 13px;
  font-weight: 800;
}

.tree-node__path {
  margin-top: 4px;
  color: var(--cockpit-muted);
  font-size: 12px;
}

.tree-node__badge {
  font-size: 12px;
  font-weight: 800;
}

.tree-node__badge--disabled {
  color: rgba(255, 196, 206, 0.98);
}

.detail-stack {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.detail-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--cockpit-border);
  background: rgba(255, 255, 255, 0.05);
}

.detail-card__label {
  font-size: 12px;
  color: var(--cockpit-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.detail-card__value {
  margin-top: 10px;
  font-size: 14px;
  font-weight: 800;
}

.detail-card__value--path {
  line-height: 1.6;
  word-break: break-word;
}

.field {
  display: grid;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
}

.field__control {
  min-height: 44px;
  width: 100%;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid var(--cockpit-border);
  background: var(--cockpit-input-bg);
  color: var(--cockpit-text);
  font: inherit;
}

.field__control--textarea {
  min-height: 144px;
  padding: 12px 14px;
}

.hero-action,
.inline-action {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--cockpit-border);
  background: rgba(255, 255, 255, 0.08);
  color: var(--cockpit-text);
  font: inherit;
  cursor: pointer;
}

.hero-action {
  border-color: transparent;
  background: linear-gradient(135deg, var(--cockpit-accent), var(--cockpit-accent-2));
  color: #041322;
  font-weight: 900;
}

.loading-card,
.empty-state {
  padding: 18px;
  color: var(--cockpit-muted);
}

.error-card {
  padding: 14px 16px;
  background: rgba(251, 113, 133, 0.14);
  color: rgba(255, 196, 206, 0.98);
  border-color: rgba(251, 113, 133, 0.28);
}

.error-card--inline {
  margin-top: 4px;
}

.table-wrap {
  overflow-x: auto;
  margin-top: 16px;
}

.result-table {
  width: 100%;
  min-width: 920px;
  border-collapse: collapse;
}

.result-table th,
.result-table td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
}

.result-table th {
  font-size: 12px;
  color: var(--cockpit-muted);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.primary-cell {
  font-weight: 800;
}

@media (max-width: 1080px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
