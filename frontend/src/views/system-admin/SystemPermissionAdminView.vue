<template>
  <section class="workbench">
    <PageHero :kicker="copy.kicker" :title="copy.title" :subtitle="copy.subtitle" />

    <div class="metrics">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
      </article>
    </div>

    <section class="panel-card tabs-card">
      <div class="tab-switch">
        <button
          class="tab-switch__button"
          :class="{ 'is-active': activeTab === 'systems' }"
          type="button"
          @click="activeTab = 'systems'"
        >
          {{ copy.tabs.systems }}
        </button>
        <button
          class="tab-switch__button"
          :class="{ 'is-active': activeTab === 'permissions' }"
          type="button"
          @click="activeTab = 'permissions'"
        >
          {{ copy.tabs.permissions }}
        </button>
      </div>
    </section>

    <div v-if="loading" class="loading-card">{{ copy.loading }}</div>
    <div v-else-if="errorMessage" class="error-card">{{ errorMessage }}</div>

    <template v-else>
      <section v-if="activeTab === 'systems'" class="panel-card">
        <header class="panel-card__header">
          <div>
            <h2 class="panel-card__title">{{ copy.systems.title }}</h2>
            <p class="panel-card__meta">{{ copy.systems.subtitle }}</p>
          </div>
          <div class="header-actions">
            <button class="inline-action" type="button" @click="loadPage">{{ copy.actions.refresh }}</button>
            <button class="hero-action" type="button" @click="openSystemEditor('create')">{{ copy.actions.createSystem }}</button>
          </div>
        </header>

        <div v-if="systems.length === 0" class="empty-state">{{ copy.systems.empty }}</div>

        <div v-else class="table-wrap">
          <table class="result-table">
            <thead>
              <tr>
                <th>{{ copy.systems.columns.name }}</th>
                <th>{{ copy.systems.columns.description }}</th>
                <th class="result-table__actions">{{ copy.systems.columns.actions }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="system in systems" :key="system.id">
                <td class="primary-cell">{{ system.systemName }}</td>
                <td>{{ system.systemDescription || '-' }}</td>
                <td class="result-table__actions">
                  <button class="row-action" type="button" @click="openSystemEditor('edit', system)">{{ copy.actions.edit }}</button>
                  <button class="row-action row-action--danger" type="button" @click="removeSystem(system)">{{ copy.actions.delete }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else class="permission-grid">
        <section class="panel-card">
          <header class="panel-card__header">
            <div>
              <h2 class="panel-card__title">{{ copy.permissions.treeTitle }}</h2>
              <p class="panel-card__meta">{{ copy.permissions.treeSubtitle }}</p>
            </div>
            <button class="inline-action" type="button" @click="loadPermissions">{{ copy.actions.refresh }}</button>
          </header>

          <div v-if="permissionTree.length === 0" class="empty-state">{{ copy.permissions.empty }}</div>
          <el-tree
            v-else
            class="permission-tree"
            node-key="key"
            :data="permissionTree"
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
                  class="tree-node__status"
                  :class="{ 'tree-node__status--disabled': !data.enabled }"
                >
                  {{ data.enabled ? copy.status.enabled : copy.status.disabled }}
                </span>
              </div>
            </template>
          </el-tree>
        </section>

        <section class="panel-card">
          <header class="panel-card__header">
            <div>
              <h2 class="panel-card__title">{{ copy.permissions.detailTitle }}</h2>
              <p class="panel-card__meta">{{ copy.permissions.detailSubtitle }}</p>
            </div>
          </header>

          <div v-if="!selectedNode" class="empty-state">{{ copy.permissions.noSelection }}</div>
          <div v-else class="detail-stack">
            <article class="detail-card">
              <div class="detail-card__label">{{ copy.permissions.selectedType }}</div>
              <div class="detail-card__value">
                {{ selectedNode.nodeType === 'SYSTEM' ? copy.tabs.systems : copy.tabs.permissions }}
              </div>
            </article>
            <article class="detail-card">
              <div class="detail-card__label">{{ copy.permissions.selectedPath }}</div>
              <div class="detail-card__value detail-card__value--path">{{ selectedNode.fullPath }}</div>
            </article>
            <article class="detail-card">
              <div class="detail-card__label">{{ copy.permissions.level }}</div>
              <div class="detail-card__value">{{ selectedNode.level }}</div>
            </article>

            <div class="action-grid">
              <button class="hero-action" type="button" :disabled="!canCreateChild" @click="openPermissionEditor('create')">
                {{ copy.actions.createChild }}
              </button>
              <button class="row-action" type="button" :disabled="!canEditPermission" @click="openPermissionEditor('edit')">
                {{ copy.actions.editPermission }}
              </button>
              <button class="row-action row-action--danger" type="button" :disabled="!canEditPermission" @click="removePermission">
                {{ copy.actions.delete }}
              </button>
            </div>
          </div>
        </section>
      </section>
    </template>

    <div v-if="systemEditorOpen" class="modal-shell">
      <button class="modal-shell__backdrop" type="button" @click="closeSystemEditor" />
      <aside class="modal-shell__panel">
        <section class="modal-card">
          <header class="modal-card__header">
            <div>
              <div class="modal-card__eyebrow">{{ copy.systems.editor.eyebrow }}</div>
              <h2 class="modal-card__title">
                {{ systemEditorMode === 'create' ? copy.systems.editor.createTitle : copy.systems.editor.editTitle }}
              </h2>
            </div>
            <button class="inline-action" type="button" @click="closeSystemEditor">{{ copy.actions.close }}</button>
          </header>

          <form class="form-grid" @submit.prevent="submitSystemEditor">
            <label class="field">
              <span>{{ copy.systems.columns.name }}</span>
              <input v-model="systemForm.systemName" class="field__control" required />
            </label>

            <label class="field">
              <span>{{ copy.systems.columns.description }}</span>
              <textarea v-model="systemForm.systemDescription" class="field__control field__control--textarea" rows="4" />
            </label>

            <div v-if="systemEditorError" class="error-card error-card--inline">{{ systemEditorError }}</div>

            <footer class="modal-card__actions">
              <button class="button button--ghost" type="button" @click="closeSystemEditor">{{ copy.actions.cancel }}</button>
              <button class="button button--primary" type="submit" :disabled="submitting">
                {{ submitting ? copy.actions.saving : copy.actions.save }}
              </button>
            </footer>
          </form>
        </section>
      </aside>
    </div>

    <div v-if="permissionEditorOpen" class="modal-shell">
      <button class="modal-shell__backdrop" type="button" @click="closePermissionEditor" />
      <aside class="modal-shell__panel">
        <section class="modal-card">
          <header class="modal-card__header">
            <div>
              <div class="modal-card__eyebrow">{{ copy.permissions.editor.eyebrow }}</div>
              <h2 class="modal-card__title">
                {{ permissionEditorMode === 'create' ? copy.permissions.editor.createTitle : copy.permissions.editor.editTitle }}
              </h2>
              <p class="modal-card__subtitle">{{ permissionEditorHint }}</p>
            </div>
            <button class="inline-action" type="button" @click="closePermissionEditor">{{ copy.actions.close }}</button>
          </header>

          <form class="form-grid" @submit.prevent="submitPermissionEditor">
            <label class="field">
              <span>{{ copy.permissions.columns.name }}</span>
              <input v-model="permissionForm.permissionName" class="field__control" required />
            </label>

            <label class="field">
              <span>{{ copy.permissions.columns.status }}</span>
              <select v-model="permissionForm.enabled" class="field__control" required>
                <option :value="true">{{ copy.status.enabled }}</option>
                <option :value="false">{{ copy.status.disabled }}</option>
              </select>
            </label>

            <div v-if="permissionEditorError" class="error-card error-card--inline">{{ permissionEditorError }}</div>

            <footer class="modal-card__actions">
              <button class="button button--ghost" type="button" @click="closePermissionEditor">{{ copy.actions.cancel }}</button>
              <button class="button button--primary" type="submit" :disabled="submitting">
                {{ submitting ? copy.actions.saving : copy.actions.save }}
              </button>
            </footer>
          </form>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createAccessSystem,
  createPermissionNode,
  deleteAccessSystem,
  deletePermissionNode,
  fetchAccessSystems,
  fetchPermissionTree,
  updateAccessSystem,
  updatePermissionNode,
  type AccessPermissionTreeNode,
  type AccessSystemItem
} from '../../api/access'
import PageHero from '../../components/shell/PageHero.vue'
import { useI18nText } from '../../i18n'

const { locale } = useI18nText()

const systems = ref<AccessSystemItem[]>([])
const permissionTree = ref<AccessPermissionTreeNode[]>([])
const selectedNode = ref<AccessPermissionTreeNode | null>(null)
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const activeTab = ref<'systems' | 'permissions'>('systems')
const systemEditorOpen = ref(false)
const systemEditorMode = ref<'create' | 'edit'>('create')
const permissionEditorOpen = ref(false)
const permissionEditorMode = ref<'create' | 'edit'>('create')
const selectedSystem = ref<AccessSystemItem | null>(null)
const systemEditorError = ref('')
const permissionEditorError = ref('')

const systemForm = reactive({
  systemName: '',
  systemDescription: ''
})

const permissionForm = reactive({
  permissionName: '',
  enabled: true
})

const zhCopy = {
  kicker: '系统与权限',
  title: '系统及权限管理',
  subtitle: '系统为第一层级，权限最多可扩展到第五层级；叶子节点即最终权限路径。',
  loading: '正在加载系统和权限树...',
  actions: {
    refresh: '刷新',
    createSystem: '新建系统',
    createChild: '创建下级权限',
    edit: '编辑',
    editPermission: '编辑权限',
    delete: '删除',
    close: '关闭',
    cancel: '取消',
    save: '保存',
    saving: '处理中...'
  },
  tabs: {
    systems: '系统',
    permissions: '权限'
  },
  status: {
    enabled: '启用',
    disabled: '停用'
  },
  systems: {
    title: '系统列表',
    subtitle: '系统层级只保留系统名称和系统描述两个字段。',
    empty: '当前还没有系统数据。',
    columns: {
      name: '系统名称',
      description: '系统描述',
      actions: '操作'
    },
    editor: {
      eyebrow: '系统编辑',
      createTitle: '新建系统',
      editTitle: '编辑系统'
    }
  },
  permissions: {
    treeTitle: '权限树',
    treeSubtitle: '点击系统或权限节点后，可在右侧执行新增、编辑或删除。',
    detailTitle: '节点详情',
    detailSubtitle: '权限页最多支持五个层级，最终叶子节点就是完整权限路径。',
    empty: '请先创建系统，然后在系统节点下新增权限。',
    noSelection: '请先从左侧树中选择一个系统或权限节点。',
    selectedType: '当前选中类型',
    selectedPath: '完整路径',
    level: '层级',
    columns: {
      name: '权限名称',
      status: '是否启用'
    },
    editor: {
      eyebrow: '权限编辑',
      createTitle: '新建权限',
      editTitle: '编辑权限'
    }
  }
} as const

const enCopy = {
  kicker: 'Systems & Access',
  title: 'System and Permission Management',
  subtitle: 'The system name is level one and permissions can extend to level five. Leaf nodes represent the final permission path.',
  loading: 'Loading systems and permission tree...',
  actions: {
    refresh: 'Refresh',
    createSystem: 'Create System',
    createChild: 'Create Child Permission',
    edit: 'Edit',
    editPermission: 'Edit Permission',
    delete: 'Delete',
    close: 'Close',
    cancel: 'Cancel',
    save: 'Save',
    saving: 'Processing...'
  },
  tabs: {
    systems: 'Systems',
    permissions: 'Permissions'
  },
  status: {
    enabled: 'Enabled',
    disabled: 'Disabled'
  },
  systems: {
    title: 'System List',
    subtitle: 'The system level keeps only the system name and system description fields.',
    empty: 'No system data yet.',
    columns: {
      name: 'System Name',
      description: 'System Description',
      actions: 'Actions'
    },
    editor: {
      eyebrow: 'System Editor',
      createTitle: 'Create System',
      editTitle: 'Edit System'
    }
  },
  permissions: {
    treeTitle: 'Permission Tree',
    treeSubtitle: 'Select a system or permission node, then create, edit, or delete from the right panel.',
    detailTitle: 'Node Detail',
    detailSubtitle: 'The permission page supports up to five levels, and the final leaf node is the complete permission path.',
    empty: 'Create a system first, then add permissions beneath it.',
    noSelection: 'Select a system or permission node from the tree first.',
    selectedType: 'Selected Type',
    selectedPath: 'Full Path',
    level: 'Level',
    columns: {
      name: 'Permission Name',
      status: 'Enabled'
    },
    editor: {
      eyebrow: 'Permission Editor',
      createTitle: 'Create Permission',
      editTitle: 'Edit Permission'
    }
  }
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

const metrics = computed(() => {
  const stats = summarizeTree(permissionTree.value)
  return [
      { label: copy.value.tabs.systems, value: systems.value.length },
      { label: copy.value.tabs.permissions, value: stats.totalPermissions },
      { label: copy.value.status.enabled, value: stats.enabledPermissions },
      { label: locale.value === 'zh-CN' ? '最大层级' : 'Max Level', value: stats.maxLevel }
    ]
})

const canCreateChild = computed(() => {
  if (!selectedNode.value) {
    return false
  }
  return selectedNode.value.level < 5
})

const canEditPermission = computed(() => selectedNode.value?.nodeType === 'PERMISSION')

const permissionEditorHint = computed(() => {
  if (!selectedNode.value) {
    return ''
  }
  return locale.value === 'zh-CN'
    ? `当前父级路径：${selectedNode.value.fullPath}`
    : `Current parent path: ${selectedNode.value.fullPath}`
})

onMounted(() => {
  void loadPage()
})

async function loadPage() {
  loading.value = true
  errorMessage.value = ''
  try {
    await Promise.all([loadSystems(), loadPermissions()])
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function loadSystems() {
  systems.value = await fetchAccessSystems()
}

async function loadPermissions() {
  permissionTree.value = await fetchPermissionTree()
  if (selectedNode.value) {
    selectedNode.value = findNodeByKey(permissionTree.value, selectedNode.value.key)
  }
}

function openSystemEditor(mode: 'create' | 'edit', system?: AccessSystemItem) {
  selectedSystem.value = system ?? null
  systemEditorMode.value = mode
  systemEditorError.value = ''
  systemForm.systemName = system?.systemName ?? ''
  systemForm.systemDescription = system?.systemDescription ?? ''
  systemEditorOpen.value = true
}

function closeSystemEditor() {
  if (submitting.value) {
    return
  }
  systemEditorOpen.value = false
}

async function submitSystemEditor() {
  if (submitting.value) {
    return
  }
  submitting.value = true
  systemEditorError.value = ''

  try {
    if (systemEditorMode.value === 'create') {
      await createAccessSystem({
        systemName: systemForm.systemName.trim(),
        systemDescription: systemForm.systemDescription.trim()
      })
    } else if (selectedSystem.value) {
      await updateAccessSystem(selectedSystem.value.id, {
        systemName: systemForm.systemName.trim(),
        systemDescription: systemForm.systemDescription.trim()
      })
    }
    systemEditorOpen.value = false
    await loadPage()
    ElMessage.success(copy.value.actions.save)
  } catch (error) {
    systemEditorError.value = extractErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function removeSystem(system: AccessSystemItem) {
  if (!window.confirm(locale.value === 'zh-CN' ? '确认删除该系统吗？' : 'Delete this system?')) {
    return
  }
  try {
    await deleteAccessSystem(system.id)
    await loadPage()
    ElMessage.success(copy.value.actions.delete)
  } catch (error) {
    ElMessage.error(extractErrorMessage(error))
  }
}

function handleNodeSelect(node: AccessPermissionTreeNode) {
  selectedNode.value = node
}

function openPermissionEditor(mode: 'create' | 'edit') {
  if (!selectedNode.value) {
    return
  }

  permissionEditorMode.value = mode
  permissionEditorError.value = ''
  if (mode === 'edit') {
    permissionForm.permissionName = selectedNode.value.label
    permissionForm.enabled = selectedNode.value.enabled
  } else {
    permissionForm.permissionName = ''
    permissionForm.enabled = true
  }
  permissionEditorOpen.value = true
}

function closePermissionEditor() {
  if (submitting.value) {
    return
  }
  permissionEditorOpen.value = false
}

async function submitPermissionEditor() {
  if (!selectedNode.value || submitting.value) {
    return
  }
  submitting.value = true
  permissionEditorError.value = ''

  try {
    if (permissionEditorMode.value === 'create') {
      await createPermissionNode({
        systemId: Number(selectedNode.value.systemId),
        parentPermissionId: selectedNode.value.nodeType === 'PERMISSION' ? Number(selectedNode.value.entityId) : null,
        permissionName: permissionForm.permissionName.trim(),
        enabled: permissionForm.enabled
      })
    } else if (selectedNode.value.nodeType === 'PERMISSION') {
      await updatePermissionNode(selectedNode.value.entityId, {
        systemId: Number(selectedNode.value.systemId),
        parentPermissionId: selectedNode.value.parentPermissionId ? Number(selectedNode.value.parentPermissionId) : null,
        permissionName: permissionForm.permissionName.trim(),
        enabled: permissionForm.enabled
      })
    }
    permissionEditorOpen.value = false
    await loadPermissions()
    ElMessage.success(copy.value.actions.save)
  } catch (error) {
    permissionEditorError.value = extractErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function removePermission() {
  if (!selectedNode.value || selectedNode.value.nodeType !== 'PERMISSION') {
    return
  }
  if (!window.confirm(locale.value === 'zh-CN' ? '确认删除该权限节点吗？' : 'Delete this permission node?')) {
    return
  }
  try {
    await deletePermissionNode(selectedNode.value.entityId)
    selectedNode.value = null
    await loadPermissions()
    ElMessage.success(copy.value.actions.delete)
  } catch (error) {
    ElMessage.error(extractErrorMessage(error))
  }
}

function summarizeTree(nodes: AccessPermissionTreeNode[]) {
  let totalPermissions = 0
  let enabledPermissions = 0
  let maxLevel = 1

  const walk = (items: AccessPermissionTreeNode[]) => {
    for (const item of items) {
      maxLevel = Math.max(maxLevel, item.level)
      if (item.nodeType === 'PERMISSION') {
        totalPermissions += 1
        if (item.enabled) {
          enabledPermissions += 1
        }
      }
      walk(item.children)
    }
  }

  walk(nodes)
  return { totalPermissions, enabledPermissions, maxLevel }
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

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.panel-card,
.loading-card,
.error-card,
.modal-card {
  border: 1px solid var(--cockpit-border);
  border-radius: var(--cockpit-radius-lg);
  box-shadow: var(--cockpit-shadow);
}

.metric-card,
.panel-card,
.loading-card {
  background: rgba(255, 255, 255, 0.05);
}

.metric-card {
  padding: 16px;
}

.metric-card__label {
  font-size: 12px;
  color: var(--cockpit-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.metric-card__value {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 900;
}

.tabs-card {
  padding: 12px;
}

.tab-switch {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.tab-switch__button {
  min-height: 40px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--cockpit-text);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.tab-switch__button.is-active {
  background: linear-gradient(135deg, var(--cockpit-accent), var(--cockpit-accent-2));
  color: #041322;
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

.header-actions {
  display: flex;
  gap: 10px;
}

.permission-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 14px;
}

.permission-tree :deep(.el-tree-node__content) {
  min-height: 54px;
  padding: 4px 0;
}

.permission-tree :deep(.el-tree-node:focus > .el-tree-node__content) {
  background: rgba(96, 165, 250, 0.12);
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

.tree-node__status {
  min-width: 48px;
  text-align: center;
  font-size: 12px;
  font-weight: 800;
  color: var(--cockpit-text);
}

.tree-node__status--disabled {
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

.action-grid {
  display: grid;
  gap: 10px;
  margin-top: 4px;
}

.table-wrap {
  overflow-x: auto;
  margin-top: 16px;
}

.result-table {
  width: 100%;
  min-width: 760px;
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

.result-table__actions {
  width: 200px;
}

.result-table td.result-table__actions {
  display: flex;
  gap: 8px;
}

.primary-cell {
  font-weight: 800;
}

.hero-action,
.inline-action,
.row-action,
.button {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--cockpit-border);
  background: rgba(255, 255, 255, 0.08);
  color: var(--cockpit-text);
  font: inherit;
  cursor: pointer;
}

.hero-action,
.button--primary {
  border-color: transparent;
  background: linear-gradient(135deg, var(--cockpit-accent), var(--cockpit-accent-2));
  color: #041322;
  font-weight: 900;
}

.row-action--danger {
  border-color: rgba(251, 113, 133, 0.32);
  color: rgba(255, 196, 206, 0.98);
}

.empty-state,
.loading-card {
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

.modal-shell {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  justify-content: flex-end;
}

.modal-shell__backdrop {
  flex: 1;
  border: 0;
  background: var(--cockpit-overlay);
}

.modal-shell__panel {
  width: min(520px, 100%);
  height: 100%;
}

.modal-card {
  height: 100%;
  display: grid;
  grid-template-rows: auto 1fr;
  background:
    radial-gradient(420px 280px at 10% 12%, rgba(94, 234, 212, 0.12), transparent 58%),
    rgba(11, 16, 32, 0.96);
}

.modal-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 22px;
  border-bottom: 1px solid var(--cockpit-border);
}

.modal-card__eyebrow {
  font-size: 11px;
  color: var(--cockpit-muted);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.modal-card__title {
  margin: 8px 0 0;
  font-size: 22px;
}

.modal-card__subtitle {
  margin: 8px 0 0;
  color: var(--cockpit-muted);
  line-height: 1.6;
}

.form-grid {
  display: grid;
  align-content: start;
  gap: 16px;
  padding: 20px 22px 24px;
  overflow-y: auto;
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
  min-height: 124px;
  padding: 12px 14px;
}

.modal-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1080px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .permission-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .metrics {
    grid-template-columns: 1fr;
  }

  .panel-card__header,
  .header-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .modal-shell {
    align-items: flex-end;
  }

  .modal-shell__backdrop {
    display: none;
  }

  .modal-shell__panel {
    width: 100%;
    max-height: min(90dvh, 860px);
  }
}
</style>
