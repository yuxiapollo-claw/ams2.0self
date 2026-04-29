<template>
  <section class="workbench">
    <PageHero :kicker="copy.kicker" :title="copy.title" :subtitle="copy.subtitle">
      <template #actions>
        <button class="hero-action" type="button" :disabled="loading" @click="openEditor('create')">
          {{ copy.actions.create }}
        </button>
      </template>
    </PageHero>

    <div class="metrics">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
      </article>
    </div>

    <FilterPanel :title="copy.filterTitle">
      <div class="toolbar">
        <label class="field">
          <span>{{ copy.filters.keyword }}</span>
          <input v-model="keyword" class="field__control" :placeholder="copy.filters.keywordPlaceholder" />
        </label>

        <label class="field">
          <span>{{ copy.filters.status }}</span>
          <select v-model="statusFilter" class="field__control">
            <option value="">{{ copy.filters.allStatuses }}</option>
            <option value="ENABLED">{{ copy.status.enabled }}</option>
            <option value="DISABLED">{{ copy.status.disabled }}</option>
          </select>
        </label>
      </div>
    </FilterPanel>

    <div v-if="loading" class="loading-card">{{ copy.loading }}</div>
    <div v-else-if="errorMessage" class="error-card">{{ errorMessage }}</div>

    <section v-else class="table-card">
      <header class="table-card__header">
        <div>
          <h2 class="table-card__title">{{ copy.tableTitle }}</h2>
          <p class="table-card__meta">{{ copy.tableMeta(filteredDepartments.length, departments.length) }}</p>
        </div>
        <button class="inline-action" type="button" @click="loadPage">{{ copy.actions.refresh }}</button>
      </header>

      <div v-if="filteredDepartments.length === 0" class="empty-state">{{ copy.empty }}</div>

      <div v-else class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th>{{ copy.columns.departmentName }}</th>
              <th>{{ copy.columns.leader }}</th>
              <th>{{ copy.columns.memberCount }}</th>
              <th>{{ copy.columns.status }}</th>
              <th>{{ copy.columns.description }}</th>
              <th class="result-table__actions">{{ copy.columns.actions }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="department in filteredDepartments" :key="department.id">
              <td class="primary-cell">{{ department.departmentName }}</td>
              <td>{{ department.managerUserName || copy.unassigned }}</td>
              <td>{{ department.memberCount }}</td>
              <td>
                <span class="status-pill" :class="department.status === 'ENABLED' ? 'status-pill--good' : 'status-pill--danger'">
                  {{ formatStatus(department.status) }}
                </span>
              </td>
              <td>{{ department.description || '-' }}</td>
              <td class="result-table__actions">
                <button class="row-action" type="button" @click="openEditor('edit', department)">
                  {{ copy.actions.edit }}
                </button>
                <button class="row-action" type="button" @click="openMembersDialog(department)">
                  {{ copy.actions.bindMembers }}
                </button>
                <button class="row-action row-action--danger" type="button" @click="removeDepartment(department)">
                  {{ copy.actions.delete }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="editorOpen" class="modal-shell">
      <button class="modal-shell__backdrop" type="button" @click="closeEditor" />
      <aside class="modal-shell__panel">
        <section class="modal-card">
          <header class="modal-card__header">
            <div>
              <div class="modal-card__eyebrow">{{ copy.editor.eyebrow }}</div>
              <h2 class="modal-card__title">
                {{ editorMode === 'create' ? copy.editor.createTitle : copy.editor.editTitle }}
              </h2>
              <p class="modal-card__subtitle">
                {{ editorMode === 'create' ? copy.editor.createSubtitle : copy.editor.editSubtitle }}
              </p>
            </div>
            <button class="inline-action" type="button" @click="closeEditor">{{ copy.actions.close }}</button>
          </header>

          <form class="form-grid" @submit.prevent="submitEditor">
            <label class="field">
              <span>{{ copy.columns.departmentName }}</span>
              <input v-model="editorForm.departmentName" class="field__control" required />
            </label>

            <label class="field">
              <span>{{ copy.columns.leader }}</span>
              <select v-model="editorForm.managerUserId" class="field__control">
                <option value="">{{ copy.unassigned }}</option>
                <option v-for="user in users" :key="user.id" :value="user.id">
                  {{ user.userName }} / {{ user.loginName }}
                </option>
              </select>
            </label>

            <label class="field">
              <span>{{ copy.columns.status }}</span>
              <select v-model="editorForm.status" class="field__control" required>
                <option value="ENABLED">{{ copy.status.enabled }}</option>
                <option value="DISABLED">{{ copy.status.disabled }}</option>
              </select>
            </label>

            <label class="field">
              <span>{{ copy.columns.description }}</span>
              <textarea v-model="editorForm.description" class="field__control field__control--textarea" rows="4" />
            </label>

            <div v-if="editorErrorMessage" class="error-card error-card--inline">{{ editorErrorMessage }}</div>

            <footer class="modal-card__actions">
              <button class="button button--ghost" type="button" @click="closeEditor">{{ copy.actions.cancel }}</button>
              <button class="button button--primary" type="submit" :disabled="submitting">
                {{ submitting ? copy.actions.saving : copy.actions.save }}
              </button>
            </footer>
          </form>
        </section>
      </aside>
    </div>

    <div v-if="membersOpen" class="modal-shell">
      <button class="modal-shell__backdrop" type="button" @click="closeMembersDialog" />
      <aside class="modal-shell__panel">
        <section class="modal-card">
          <header class="modal-card__header">
            <div>
              <div class="modal-card__eyebrow">{{ copy.members.eyebrow }}</div>
              <h2 class="modal-card__title">{{ membersDepartment?.departmentName }}</h2>
              <p class="modal-card__subtitle">{{ copy.members.subtitle }}</p>
            </div>
            <button class="inline-action" type="button" @click="closeMembersDialog">{{ copy.actions.close }}</button>
          </header>

          <div class="form-grid">
            <section class="member-block">
              <div class="member-block__title">{{ copy.members.currentMembers }}</div>
              <div v-if="memberLoading" class="loading-card">{{ copy.members.loading }}</div>
              <div v-else-if="departmentMembers.length === 0" class="empty-state">{{ copy.members.empty }}</div>
              <div v-else class="member-list">
                <span v-for="member in departmentMembers" :key="member.id" class="member-tag">
                  {{ member.userName }} / {{ member.loginName }}
                </span>
              </div>
            </section>

            <label class="field">
              <span>{{ copy.members.addUsers }}</span>
              <select v-model="selectedMemberIds" class="field__control field__control--textarea" multiple>
                <option v-for="user in availableUsers" :key="user.id" :value="user.id">
                  {{ user.userName }} / {{ user.loginName }} / {{ user.departmentName || copy.unassigned }}
                </option>
              </select>
            </label>

            <div v-if="membersErrorMessage" class="error-card error-card--inline">{{ membersErrorMessage }}</div>

            <footer class="modal-card__actions">
              <button class="button button--ghost" type="button" @click="closeMembersDialog">{{ copy.actions.cancel }}</button>
              <button class="button button--primary" type="button" :disabled="submitting" @click="submitMembers">
                {{ submitting ? copy.actions.saving : copy.actions.confirm }}
              </button>
            </footer>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  bindDepartmentMembers,
  createDepartment,
  deleteDepartment,
  fetchDepartmentMembers,
  fetchDepartments,
  updateDepartment,
  type DepartmentItem,
  type DepartmentMemberItem,
  type DepartmentMutationPayload
} from '../../api/departments'
import { fetchUsers, type UserItem } from '../../api/users'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
import { useI18nText } from '../../i18n'

const { locale } = useI18nText()

const departments = ref<DepartmentItem[]>([])
const users = ref<UserItem[]>([])
const departmentMembers = ref<DepartmentMemberItem[]>([])
const loading = ref(true)
const memberLoading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const editorErrorMessage = ref('')
const membersErrorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const editorOpen = ref(false)
const membersOpen = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const selectedDepartment = ref<DepartmentItem | null>(null)
const membersDepartment = ref<DepartmentItem | null>(null)
const selectedMemberIds = ref<string[]>([])

const editorForm = reactive({
  departmentName: '',
  managerUserId: '',
  status: 'ENABLED',
  description: ''
})

const zhCopy = {
  kicker: '组织架构',
  title: '部门管理',
  subtitle: '此页面仅对系统管理员开放，可维护部门负责人，并将用户批量加入到单一部门。',
  filterTitle: '筛选条件',
  loading: '正在加载部门和用户数据...',
  empty: '当前筛选条件下没有匹配部门。',
  unassigned: '未指定',
  tableTitle: '部门列表',
  tableMeta: (count: number, total: number) => `当前显示 ${count} / ${total} 个部门`,
  actions: {
    create: '新建部门',
    refresh: '刷新',
    edit: '编辑',
    bindMembers: '批量添加成员',
    delete: '删除',
    close: '关闭',
    cancel: '取消',
    save: '保存',
    saving: '处理中...',
    confirm: '确认'
  },
  filters: {
    keyword: '关键字',
    keywordPlaceholder: '搜索部门、负责人或描述',
    status: '状态',
    allStatuses: '全部状态'
  },
  status: {
    enabled: '启用',
    disabled: '停用'
  },
  columns: {
    departmentName: '部门名称',
    leader: '部门负责人',
    memberCount: '成员数量',
    status: '状态',
    description: '部门描述',
    actions: '操作'
  },
  editor: {
    eyebrow: '部门编辑',
    createTitle: '新建部门',
    editTitle: '编辑部门',
    createSubtitle: '创建部门后，可在成员窗口中批量加入用户。',
    editSubtitle: '部门负责人和部门成员是两项独立配置，可分别维护。'
  },
  members: {
    eyebrow: '成员关联',
    subtitle: '一个用户只能同时属于一个部门。保存后，选中的用户会自动迁移到当前部门。',
    currentMembers: '当前成员',
    addUsers: '选择要加入的用户',
    loading: '正在加载部门成员...',
    empty: '该部门当前还没有关联用户。'
  }
} as const

const enCopy = {
  kicker: 'Organization',
  title: 'Department Management',
  subtitle: 'This page is restricted to system administrators. Manage department leaders and batch-assign users into a single department.',
  filterTitle: 'Filters',
  loading: 'Loading department and user data...',
  empty: 'No departments match the current filters.',
  unassigned: 'Unassigned',
  tableTitle: 'Department List',
  tableMeta: (count: number, total: number) => `Showing ${count} / ${total} departments`,
  actions: {
    create: 'Create Department',
    refresh: 'Refresh',
    edit: 'Edit',
    bindMembers: 'Batch Add Members',
    delete: 'Delete',
    close: 'Close',
    cancel: 'Cancel',
    save: 'Save',
    saving: 'Processing...',
    confirm: 'Confirm'
  },
  filters: {
    keyword: 'Keyword',
    keywordPlaceholder: 'Search by department, leader, or description',
    status: 'Status',
    allStatuses: 'All statuses'
  },
  status: {
    enabled: 'Enabled',
    disabled: 'Disabled'
  },
  columns: {
    departmentName: 'Department Name',
    leader: 'Department Leader',
    memberCount: 'Members',
    status: 'Status',
    description: 'Description',
    actions: 'Actions'
  },
  editor: {
    eyebrow: 'Department Editor',
    createTitle: 'Create Department',
    editTitle: 'Edit Department',
    createSubtitle: 'After creating the department you can batch-add users from the member dialog.',
    editSubtitle: 'If the leader changes, that leader is also moved into this department.'
  },
  members: {
    eyebrow: 'Member Binding',
    subtitle: 'A user can belong to only one department. Saving will move the selected users into this department.',
    currentMembers: 'Current Members',
    addUsers: 'Choose users to add',
    loading: 'Loading members...',
    empty: 'This department currently has no assigned users.'
  }
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))

const filteredDepartments = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  return departments.value.filter((department) => {
    const matchesKeyword =
      normalized.length === 0 ||
      [department.departmentName, department.managerUserName ?? '', department.description ?? ''].some((value) =>
        value.toLowerCase().includes(normalized)
      )
    const matchesStatus = !statusFilter.value || department.status === statusFilter.value
    return matchesKeyword && matchesStatus
  })
})

const metrics = computed(() => [
  { label: copy.value.columns.departmentName, value: departments.value.length },
  { label: copy.value.columns.memberCount, value: departments.value.reduce((sum, item) => sum + item.memberCount, 0) },
  { label: copy.value.status.enabled, value: departments.value.filter((item) => item.status === 'ENABLED').length },
  { label: copy.value.columns.leader, value: departments.value.filter((item) => item.managerUserId).length }
])

const availableUsers = computed(() => users.value.filter((user) => !user.systemAdmin))

onMounted(() => {
  void loadPage()
})

async function loadPage() {
  loading.value = true
  errorMessage.value = ''

  try {
    const [loadedDepartments, loadedUsers] = await Promise.all([fetchDepartments(), fetchUsers()])
    departments.value = loadedDepartments
    users.value = loadedUsers
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
    departments.value = []
    users.value = []
  } finally {
    loading.value = false
  }
}

function openEditor(mode: 'create' | 'edit', department?: DepartmentItem) {
  editorMode.value = mode
  selectedDepartment.value = department ?? null
  editorErrorMessage.value = ''
  editorForm.departmentName = department?.departmentName ?? ''
  editorForm.managerUserId = department?.managerUserId ?? ''
  editorForm.status = department?.status ?? 'ENABLED'
  editorForm.description = department?.description ?? ''
  editorOpen.value = true
}

function closeEditor() {
  if (submitting.value) {
    return
  }
  editorOpen.value = false
}

async function submitEditor() {
  if (submitting.value) {
    return
  }
  submitting.value = true
  editorErrorMessage.value = ''

  try {
    const payload: DepartmentMutationPayload = {
      departmentName: editorForm.departmentName.trim(),
      managerUserId: editorForm.managerUserId ? Number(editorForm.managerUserId) : null,
      description: editorForm.description.trim(),
      status: editorForm.status
    }

    if (editorMode.value === 'create') {
      const created = await createDepartment(payload)
      departments.value = [...departments.value, created]
      ElMessage.success(copy.value.actions.save)
    } else if (selectedDepartment.value) {
      const updated = await updateDepartment(selectedDepartment.value.id, payload)
      departments.value = departments.value.map((item) => (item.id === updated.id ? updated : item))
      ElMessage.success(copy.value.actions.save)
    }

    editorOpen.value = false
    await loadPage()
  } catch (error) {
    editorErrorMessage.value = extractErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function openMembersDialog(department: DepartmentItem) {
  membersDepartment.value = department
  membersOpen.value = true
  memberLoading.value = true
  membersErrorMessage.value = ''
  selectedMemberIds.value = []

  try {
    departmentMembers.value = await fetchDepartmentMembers(department.id)
  } catch (error) {
    membersErrorMessage.value = extractErrorMessage(error)
    departmentMembers.value = []
  } finally {
    memberLoading.value = false
  }
}

function closeMembersDialog() {
  if (submitting.value) {
    return
  }
  membersOpen.value = false
}

async function submitMembers() {
  if (!membersDepartment.value || submitting.value) {
    return
  }

  if (selectedMemberIds.value.length === 0) {
    membersErrorMessage.value = locale.value === 'zh-CN' ? '请至少选择一个用户' : 'Select at least one user'
    return
  }

  submitting.value = true
  membersErrorMessage.value = ''

  try {
    departmentMembers.value = await bindDepartmentMembers(
      membersDepartment.value.id,
      selectedMemberIds.value.map((item) => Number(item))
    )
    ElMessage.success(copy.value.actions.confirm)
    membersOpen.value = false
    await loadPage()
  } catch (error) {
    membersErrorMessage.value = extractErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function removeDepartment(department: DepartmentItem) {
  if (!window.confirm(locale.value === 'zh-CN' ? '确认删除该部门吗？' : 'Delete this department?')) {
    return
  }

  try {
    await deleteDepartment(department.id)
    departments.value = departments.value.filter((item) => item.id !== department.id)
    ElMessage.success(copy.value.actions.delete)
  } catch (error) {
    ElMessage.error(extractErrorMessage(error))
  }
}

function formatStatus(value: string) {
  return value === 'ENABLED' ? copy.value.status.enabled : value === 'DISABLED' ? copy.value.status.disabled : value
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
.table-card,
.loading-card,
.error-card,
.modal-card {
  border: 1px solid var(--cockpit-border);
  border-radius: var(--cockpit-radius-lg);
  box-shadow: var(--cockpit-shadow);
}

.metric-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
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

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(220px, 0.7fr);
  gap: 14px;
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

.table-card {
  overflow: hidden;
  background: rgba(255, 255, 255, 0.04);
}

.table-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--cockpit-border);
}

.table-card__title {
  margin: 0;
  font-size: 16px;
}

.table-card__meta {
  margin: 8px 0 0;
  color: var(--cockpit-muted);
  font-size: 12px;
}

.table-wrap {
  overflow-x: auto;
}

.result-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.result-table th,
.result-table td {
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
}

.result-table th {
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.result-table__actions {
  width: 230px;
}

.result-table td.result-table__actions {
  display: flex;
  flex-wrap: wrap;
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

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
  font-weight: 800;
}

.status-pill--good {
  background: rgba(94, 234, 212, 0.14);
  border-color: rgba(94, 234, 212, 0.26);
}

.status-pill--danger {
  background: rgba(251, 113, 133, 0.14);
  border-color: rgba(251, 113, 133, 0.26);
}

.loading-card,
.empty-state {
  padding: 18px;
  color: var(--cockpit-muted);
  background: rgba(255, 255, 255, 0.05);
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
  width: min(540px, 100%);
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

.modal-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.member-block {
  display: grid;
  gap: 10px;
}

.member-block__title {
  font-size: 13px;
  font-weight: 800;
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.member-tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(96, 165, 250, 0.24);
  background: rgba(96, 165, 250, 0.14);
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .metrics,
  .toolbar {
    grid-template-columns: 1fr;
  }

  .table-card__header {
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
