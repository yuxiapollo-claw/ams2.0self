<template>
  <section class="workbench">
    <PageHero :kicker="copy.kicker" :title="copy.title" :subtitle="copy.subtitle">
      <template #actions>
        <button
          v-if="isAdmin"
          class="hero-action"
          type="button"
          :disabled="loading || departments.length === 0"
          @click="openCreateDialog"
        >
          {{ copy.actions.create }}
        </button>
        <button
          v-else
          class="hero-action"
          type="button"
          :disabled="loading || !currentUserRecord"
          @click="openChangePasswordDialog"
        >
          {{ copy.actions.changePassword }}
        </button>
      </template>
    </PageHero>

    <div class="metrics">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
      </article>
    </div>

    <section v-if="isAdmin" class="info-card">
      <div class="info-card__title">{{ copy.defaultPasswordTitle }}</div>
      <div class="info-card__body">{{ copy.defaultPasswordBody }}</div>
    </section>

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
          <p class="table-card__meta">{{ copy.tableMeta(filteredUsers.length, users.length) }}</p>
        </div>
        <button class="inline-action" type="button" @click="loadPage">{{ copy.actions.refresh }}</button>
      </header>

      <div v-if="filteredUsers.length === 0" class="empty-state">{{ copy.empty }}</div>

      <div v-else class="table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th>{{ copy.columns.userName }}</th>
              <th>{{ copy.columns.loginName }}</th>
              <th>{{ copy.columns.department }}</th>
              <th>{{ copy.columns.employmentStatus }}</th>
              <th>{{ copy.columns.accountStatus }}</th>
              <th>{{ copy.columns.identity }}</th>
              <th class="result-table__actions">{{ copy.columns.actions }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in filteredUsers" :key="user.id">
              <td class="primary-cell">{{ user.userName }}</td>
              <td>{{ user.loginName }}</td>
              <td>{{ user.departmentName || '-' }}</td>
              <td>{{ formatEmploymentStatus(user.employmentStatus) }}</td>
              <td>
                <span class="status-pill" :class="user.accountStatus === 'ENABLED' ? 'status-pill--good' : 'status-pill--danger'">
                  {{ formatStatus(user.accountStatus) }}
                </span>
              </td>
              <td>
                <span class="identity-pill" :class="user.systemAdmin ? 'identity-pill--admin' : ''">
                  {{ user.systemAdmin ? copy.identities.admin : copy.identities.user }}
                </span>
              </td>
              <td class="result-table__actions">
                <template v-if="isAdmin">
                  <button class="row-action" type="button" @click="openEditDialog(user)">
                    {{ copy.actions.edit }}
                  </button>
                  <button class="row-action" type="button" @click="toggleStatus(user)">
                    {{ user.accountStatus === 'ENABLED' ? copy.actions.disable : copy.actions.enable }}
                  </button>
                  <button class="row-action" type="button" @click="resetPassword(user)">
                    {{ copy.actions.resetPassword }}
                  </button>
                  <button
                    v-if="!isCurrentLogin(user)"
                    class="row-action row-action--danger"
                    type="button"
                    @click="removeUser(user)"
                  >
                    {{ copy.actions.delete }}
                  </button>
                </template>
                <button
                  v-else-if="isCurrentLogin(user)"
                  class="row-action"
                  type="button"
                  @click="openChangePasswordDialog"
                >
                  {{ copy.actions.changePassword }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="editorOpen" class="modal-shell">
      <button class="modal-shell__backdrop" type="button" @click="closeEditorDialog" />
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
            <button class="inline-action" type="button" @click="closeEditorDialog">{{ copy.actions.close }}</button>
          </header>

          <form class="form-grid" @submit.prevent="submitEditor">
            <label class="field">
              <span>{{ copy.columns.userName }}</span>
              <input v-model="editorForm.userName" class="field__control" required />
            </label>

            <label class="field">
              <span>{{ copy.columns.loginName }}</span>
              <input v-model="editorForm.loginName" class="field__control" required />
            </label>

            <label class="field">
              <span>{{ copy.columns.department }}</span>
              <select v-model="editorForm.departmentId" class="field__control" required>
                <option v-for="department in departments" :key="department.id" :value="department.id">
                  {{ department.departmentName }}
                </option>
              </select>
            </label>

            <label class="field">
              <span>{{ copy.columns.employmentStatus }}</span>
              <select v-model="editorForm.employmentStatus" class="field__control" required>
                <option value="ACTIVE">{{ copy.employment.active }}</option>
                <option value="LEAVE">{{ copy.employment.leave }}</option>
                <option value="RESIGNED">{{ copy.employment.resigned }}</option>
              </select>
            </label>

            <label class="field">
              <span>{{ copy.columns.accountStatus }}</span>
              <select v-model="editorForm.accountStatus" class="field__control" required>
                <option value="ENABLED">{{ copy.status.enabled }}</option>
                <option value="DISABLED">{{ copy.status.disabled }}</option>
              </select>
            </label>

            <div v-if="editorErrorMessage" class="error-card error-card--inline">{{ editorErrorMessage }}</div>

            <footer class="modal-card__actions">
              <button class="button button--ghost" type="button" @click="closeEditorDialog">{{ copy.actions.cancel }}</button>
              <button class="button button--primary" type="submit" :disabled="submitting">
                {{ submitting ? copy.actions.saving : copy.actions.save }}
              </button>
            </footer>
          </form>
        </section>
      </aside>
    </div>

    <div v-if="passwordOpen" class="modal-shell">
      <button class="modal-shell__backdrop" type="button" @click="closePasswordDialog" />
      <aside class="modal-shell__panel modal-shell__panel--narrow">
        <section class="modal-card">
          <header class="modal-card__header">
            <div>
              <div class="modal-card__eyebrow">{{ copy.password.eyebrow }}</div>
              <h2 class="modal-card__title">
                {{ passwordMode === 'self' ? copy.password.changeTitle : copy.password.resetTitle }}
              </h2>
              <p class="modal-card__subtitle">
                {{ passwordMode === 'self' ? copy.password.changeSubtitle : copy.password.resetSubtitle }}
              </p>
            </div>
            <button class="inline-action" type="button" @click="closePasswordDialog">{{ copy.actions.close }}</button>
          </header>

          <form class="form-grid" @submit.prevent="submitPassword">
            <template v-if="passwordMode === 'self'">
              <label class="field">
                <span>{{ copy.password.currentPassword }}</span>
                <input v-model="passwordForm.currentPassword" class="field__control" type="password" required />
              </label>

              <label class="field">
                <span>{{ copy.password.newPassword }}</span>
                <input v-model="passwordForm.newPassword" class="field__control" type="password" required />
              </label>

              <label class="field">
                <span>{{ copy.password.confirmPassword }}</span>
                <input v-model="passwordForm.confirmPassword" class="field__control" type="password" required />
              </label>
            </template>

            <div v-else class="info-card">
              <div class="info-card__title">{{ copy.password.resetNoticeTitle }}</div>
              <div class="info-card__body">
                {{ copy.password.resetNoticeBody(passwordTarget?.userName || '', defaultPassword) }}
              </div>
            </div>

            <div v-if="passwordErrorMessage" class="error-card error-card--inline">{{ passwordErrorMessage }}</div>

            <footer class="modal-card__actions">
              <button class="button button--ghost" type="button" @click="closePasswordDialog">{{ copy.actions.cancel }}</button>
              <button class="button button--primary" type="submit" :disabled="submitting">
                {{ submitting ? copy.actions.saving : copy.actions.confirm }}
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
import { fetchDepartments, type DepartmentItem } from '../../api/departments'
import {
  changeMyPassword,
  createUser,
  deleteUser,
  fetchUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus,
  type UserItem,
  type UserMutationPayload
} from '../../api/users'
import FilterPanel from '../../components/shell/FilterPanel.vue'
import PageHero from '../../components/shell/PageHero.vue'
import { useI18nText } from '../../i18n'
import { useAuthStore } from '../../stores/auth'

const { locale } = useI18nText()
const authStore = useAuthStore()

const users = ref<UserItem[]>([])
const departments = ref<DepartmentItem[]>([])
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const editorErrorMessage = ref('')
const passwordErrorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const editorOpen = ref(false)
const passwordOpen = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const passwordMode = ref<'self' | 'reset'>('self')
const selectedUser = ref<UserItem | null>(null)
const passwordTarget = ref<UserItem | null>(null)
const defaultPassword = ref('ChangeMe123!')

const editorForm = reactive({
  userName: '',
  loginName: '',
  departmentId: '',
  employmentStatus: 'ACTIVE',
  accountStatus: 'ENABLED'
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const zhCopy = {
  kicker: '用户账号',
  title: '用户管理',
  subtitle: '页面不展示用户编号，聚焦登录名、部门、任职状态、账户状态与密码操作。',
  filterTitle: '筛选条件',
  loading: '正在加载用户数据...',
  empty: '当前筛选条件下没有匹配用户。',
  defaultPasswordTitle: '默认密码策略',
  defaultPasswordBody: '新建用户和管理员重置密码后，默认密码都会设置为 ChangeMe123!。普通用户登录后可在本页修改自己的密码。',
  tableTitle: '用户列表',
  tableMeta: (count: number, total: number) => `当前显示 ${count} / ${total} 条记录`,
  actions: {
    create: '新建用户',
    refresh: '刷新',
    edit: '编辑',
    enable: '启用',
    disable: '停用',
    delete: '删除',
    resetPassword: '重置密码',
    changePassword: '修改密码',
    close: '关闭',
    cancel: '取消',
    save: '保存',
    saving: '处理中...',
    confirm: '确认'
  },
  columns: {
    userName: '姓名',
    loginName: '登录名',
    department: '部门',
    employmentStatus: '任职状态',
    accountStatus: '账户状态',
    identity: '身份',
    actions: '操作'
  },
  filters: {
    keyword: '关键字',
    keywordPlaceholder: '搜索姓名、登录名或部门',
    status: '账户状态',
    allStatuses: '全部状态'
  },
  status: {
    enabled: '启用',
    disabled: '停用'
  },
  employment: {
    active: '在职',
    leave: '请假',
    resigned: '离职'
  },
  identities: {
    admin: '系统管理员',
    user: '普通用户'
  },
  editor: {
    eyebrow: '用户资料编辑',
    createTitle: '新建用户',
    editTitle: '编辑用户',
    createSubtitle: '新建后默认密码为 ChangeMe123!，管理员可随时重置。',
    editSubtitle: '系统管理员可以修改所有用户的基础信息。'
  },
  password: {
    eyebrow: '密码操作',
    changeTitle: '修改我的密码',
    resetTitle: '管理员重置密码',
    changeSubtitle: '除系统管理员外，每个登录用户都可以在自己的用户页修改密码。',
    resetSubtitle: '系统管理员可以将任意用户密码重置为默认值。',
    currentPassword: '当前密码',
    newPassword: '新密码',
    confirmPassword: '确认新密码',
    resetNoticeTitle: '重置说明',
    resetNoticeBody: (userName: string, value: string) => `${userName} 的密码将被重置为默认密码 ${value}。`
  }
} as const

const enCopy = {
  kicker: 'User Accounts',
  title: 'User Management',
  subtitle: 'The user code is removed from the page. The current surface focuses on login name, department, status, and password actions.',
  filterTitle: 'Filters',
  loading: 'Loading user data...',
  empty: 'No users match the current filters.',
  defaultPasswordTitle: 'Default Password Policy',
  defaultPasswordBody: 'New users and admin password resets both use ChangeMe123! as the default password. Users can change their own password from this page after signing in.',
  tableTitle: 'User List',
  tableMeta: (count: number, total: number) => `Showing ${count} / ${total} records`,
  actions: {
    create: 'Create User',
    refresh: 'Refresh',
    edit: 'Edit',
    enable: 'Enable',
    disable: 'Disable',
    delete: 'Delete',
    resetPassword: 'Reset Password',
    changePassword: 'Change Password',
    close: 'Close',
    cancel: 'Cancel',
    save: 'Save',
    saving: 'Processing...',
    confirm: 'Confirm'
  },
  columns: {
    userName: 'User Name',
    loginName: 'Login Name',
    department: 'Department',
    employmentStatus: 'Employment',
    accountStatus: 'Account',
    identity: 'Identity',
    actions: 'Actions'
  },
  filters: {
    keyword: 'Keyword',
    keywordPlaceholder: 'Search by user, login, or department',
    status: 'Account Status',
    allStatuses: 'All statuses'
  },
  status: {
    enabled: 'Enabled',
    disabled: 'Disabled'
  },
  employment: {
    active: 'Active',
    leave: 'Leave',
    resigned: 'Resigned'
  },
  identities: {
    admin: 'System Admin',
    user: 'User'
  },
  editor: {
    eyebrow: 'User Profile Editor',
    createTitle: 'Create User',
    editTitle: 'Edit User',
    createSubtitle: 'A new user starts with the default password and can be reset by the administrator.',
    editSubtitle: 'System administrators can update all profile fields.'
  },
  password: {
    eyebrow: 'Password Action',
    changeTitle: 'Change My Password',
    resetTitle: 'Admin Password Reset',
    changeSubtitle: 'Only non-admin login users can change their own password from this page.',
    resetSubtitle: 'System administrators can reset any user back to the default password.',
    currentPassword: 'Current Password',
    newPassword: 'New Password',
    confirmPassword: 'Confirm New Password',
    resetNoticeTitle: 'Reset Notice',
    resetNoticeBody: (userName: string, value: string) => `${userName}'s password will be reset to ${value}.`
  }
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))
const isAdmin = computed(() => authStore.isSystemAdmin)
const currentUserRecord = computed(() => users.value.find((user) => user.id === authStore.user?.id) ?? null)

const filteredUsers = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  return users.value.filter((user) => {
    const matchesKeyword =
      normalized.length === 0 ||
      [user.userName, user.loginName, user.departmentName].some((value) =>
        value.toLowerCase().includes(normalized)
      )
    const matchesStatus = !statusFilter.value || user.accountStatus === statusFilter.value
    return matchesKeyword && matchesStatus
  })
})

const metrics = computed(() => [
  { label: copy.value.columns.userName, value: users.value.length },
  { label: copy.value.status.enabled, value: users.value.filter((user) => user.accountStatus === 'ENABLED').length },
  { label: copy.value.columns.department, value: new Set(users.value.map((user) => user.departmentId)).size },
  {
    label: copy.value.columns.identity,
    value: users.value.filter((user) => user.systemAdmin).length
  }
])

onMounted(() => {
  void loadPage()
})

async function loadPage() {
  loading.value = true
  errorMessage.value = ''

  try {
    const tasks: Array<Promise<unknown>> = [fetchUsers()]
    if (isAdmin.value) {
      tasks.push(fetchDepartments())
    }

    const [loadedUsers, loadedDepartments] = await Promise.all(tasks)
    users.value = loadedUsers as UserItem[]
    departments.value = isAdmin.value ? (loadedDepartments as DepartmentItem[]) : []
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
    users.value = []
    departments.value = []
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editorMode.value = 'create'
  selectedUser.value = null
  editorErrorMessage.value = ''
  editorForm.userName = ''
  editorForm.loginName = ''
  editorForm.departmentId = departments.value[0]?.id ?? ''
  editorForm.employmentStatus = 'ACTIVE'
  editorForm.accountStatus = 'ENABLED'
  editorOpen.value = true
}

function openEditDialog(user: UserItem) {
  selectedUser.value = user
  editorMode.value = 'edit'
  editorErrorMessage.value = ''
  editorForm.userName = user.userName
  editorForm.loginName = user.loginName
  editorForm.departmentId = user.departmentId
  editorForm.employmentStatus = user.employmentStatus
  editorForm.accountStatus = user.accountStatus
  editorOpen.value = true
}

function closeEditorDialog() {
  if (submitting.value) {
    return
  }
  editorOpen.value = false
}

async function submitEditor() {
  if (submitting.value) {
    return
  }
  editorErrorMessage.value = ''
  submitting.value = true

  try {
    const payload: UserMutationPayload = {
      userName: editorForm.userName.trim(),
      loginName: editorForm.loginName.trim(),
      departmentId: Number(editorForm.departmentId),
      employmentStatus: editorForm.employmentStatus,
      accountStatus: editorForm.accountStatus
    }

    if (editorMode.value === 'create') {
      const created = await createUser(payload)
      users.value = [...users.value, created]
      ElMessage.success(copy.value.defaultPasswordBody)
    } else if (selectedUser.value) {
      const updated = await updateUser(selectedUser.value.id, payload)
      users.value = users.value.map((user) => (user.id === updated.id ? updated : user))
      ElMessage.success(copy.value.actions.save)
    }
    editorOpen.value = false
  } catch (error) {
    editorErrorMessage.value = extractErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(user: UserItem) {
  try {
    const updated = await updateUserStatus(user.id, {
      accountStatus: user.accountStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    })
    users.value = users.value.map((item) => (item.id === updated.id ? updated : item))
    ElMessage.success(copy.value.actions.save)
  } catch (error) {
    ElMessage.error(extractErrorMessage(error))
  }
}

async function resetPassword(user: UserItem) {
  passwordMode.value = 'reset'
  passwordTarget.value = user
  passwordErrorMessage.value = ''
  defaultPassword.value = 'ChangeMe123!'
  passwordOpen.value = true
}

function openChangePasswordDialog() {
  passwordMode.value = 'self'
  passwordTarget.value = currentUserRecord.value
  passwordErrorMessage.value = ''
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordOpen.value = true
}

function closePasswordDialog() {
  if (submitting.value) {
    return
  }
  passwordOpen.value = false
}

async function submitPassword() {
  if (submitting.value) {
    return
  }

  passwordErrorMessage.value = ''
  submitting.value = true

  try {
    if (passwordMode.value === 'self') {
      if (passwordForm.newPassword.trim() !== passwordForm.confirmPassword.trim()) {
        throw new Error(locale.value === 'zh-CN' ? '两次输入的新密码不一致' : 'The new passwords do not match')
      }
      await changeMyPassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success(copy.value.actions.confirm)
    } else if (passwordTarget.value) {
      const response = await resetUserPassword(passwordTarget.value.id)
      defaultPassword.value = response.defaultPassword ?? defaultPassword.value
      ElMessage.success(response.message || copy.value.actions.confirm)
    }
    passwordOpen.value = false
  } catch (error) {
    passwordErrorMessage.value = extractErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function removeUser(user: UserItem) {
  if (!window.confirm(locale.value === 'zh-CN' ? '确认删除该用户吗？' : 'Delete this user?')) {
    return
  }

  try {
    await deleteUser(user.id)
    users.value = users.value.filter((item) => item.id !== user.id)
    ElMessage.success(copy.value.actions.delete)
  } catch (error) {
    ElMessage.error(extractErrorMessage(error))
  }
}

function isCurrentLogin(user: UserItem) {
  return user.id === authStore.user?.id
}

function formatEmploymentStatus(value: string) {
  switch (value) {
    case 'ACTIVE':
      return copy.value.employment.active
    case 'LEAVE':
      return copy.value.employment.leave
    case 'RESIGNED':
      return copy.value.employment.resigned
    default:
      return value
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
.info-card,
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

.info-card,
.loading-card {
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.05);
}

.info-card__title {
  font-size: 14px;
  font-weight: 900;
}

.info-card__body {
  margin-top: 8px;
  line-height: 1.6;
  color: var(--cockpit-muted);
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

.field__control::placeholder {
  color: var(--cockpit-muted);
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

.primary-cell {
  font-weight: 800;
}

.result-table__actions {
  width: 250px;
}

.result-table td.result-table__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.button--ghost {
  background: rgba(255, 255, 255, 0.08);
}

.status-pill,
.identity-pill {
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

.identity-pill--admin {
  background: rgba(96, 165, 250, 0.16);
  border-color: rgba(96, 165, 250, 0.28);
}

.empty-state {
  padding: 22px 18px;
  color: var(--cockpit-muted);
}

.error-card {
  padding: 14px 16px;
  background: rgba(251, 113, 133, 0.14);
  color: rgba(255, 196, 206, 0.98);
  border-color: rgba(251, 113, 133, 0.28);
}

.error-card--inline {
  margin-top: 6px;
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

.modal-shell__panel--narrow {
  width: min(460px, 100%);
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

  .modal-shell__panel,
  .modal-shell__panel--narrow {
    width: 100%;
    max-height: min(90dvh, 860px);
  }
}
</style>
