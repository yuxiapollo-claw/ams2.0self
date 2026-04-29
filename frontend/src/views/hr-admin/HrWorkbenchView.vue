<template>
  <section class="domain-page">
    <header class="page-header">
      <div>
        <h1>{{ page.title }}</h1>
        <p>{{ page.description }}</p>
      </div>
      <div class="header-actions">
        <button v-for="button in page.buttons" :key="button" type="button" class="ghost-button">{{ button }}</button>
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
import { fetchDepartments, type DepartmentItem } from '../../api/departments'
import { fetchUsers, type UserItem } from '../../api/users'
import { useI18nText } from '../../i18n'

type HrPath = '/hrAdmin/hrManagement' | '/hrAdmin/enterprisemaintain'

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

const zhPages: Record<HrPath, PageConfig> = {
  '/hrAdmin/hrManagement': {
    title: '人事信息管理',
    description: '查看员工编码、姓名、部门与账号状态。',
    sectionTitle: '员工信息列表',
    headers: ['员工编码', '员工姓名', '部门名称', '登录名', '状态'],
    buttons: ['添加'],
    empty: '暂无员工数据'
  },
  '/hrAdmin/enterprisemaintain': {
    title: '企业架构管理',
    description: '查看部门、负责人和成员覆盖情况。',
    sectionTitle: '部门结构列表',
    headers: ['部门名称', '部门负责人', '状态', '部门成员', '描述'],
    buttons: ['添加'],
    empty: '暂无部门架构数据'
  }
}

const enPages: Record<HrPath, PageConfig> = {
  '/hrAdmin/hrManagement': {
    title: 'HR Information Management',
    description: 'Review employee code, name, department, and account status.',
    sectionTitle: 'Employee List',
    headers: ['Employee Code', 'Employee Name', 'Department', 'Login Name', 'Status'],
    buttons: ['Add'],
    empty: 'No employee data'
  },
  '/hrAdmin/enterprisemaintain': {
    title: 'Enterprise Structure Management',
    description: 'Review departments, owners, and member coverage.',
    sectionTitle: 'Department Structure',
    headers: ['Department', 'Manager', 'Status', 'Members', 'Description'],
    buttons: ['Add'],
    empty: 'No department structure data'
  }
}

const users = ref<UserItem[]>([])
const departments = ref<DepartmentItem[]>([])

const pagePath = computed<HrPath>(() =>
  route.meta.pagePath === '/hrAdmin/enterprisemaintain' ? '/hrAdmin/enterprisemaintain' : '/hrAdmin/hrManagement'
)
const page = computed(() => (locale.value === 'zh-CN' ? zhPages : enPages)[pagePath.value])
const rows = computed<RowShape[]>(() => {
  if (pagePath.value === '/hrAdmin/hrManagement') {
    return users.value.map((user) => ({
      key: user.id,
      cells: [user.userCode, user.userName, user.departmentName, user.loginName, user.accountStatus]
    }))
  }

  return departments.value.map((department) => ({
    key: department.id,
    cells: [
      department.departmentName,
      department.managerUserName || '-',
      department.status,
      String(department.memberCount),
      department.description || '-'
    ]
  }))
})
const rowCountText = computed(() =>
  locale.value === 'zh-CN' ? `共 ${rows.value.length} 行数据` : `${rows.value.length} visible rows`
)

onMounted(async () => {
  const [loadedUsers, loadedDepartments] = await Promise.all([fetchUsers(), fetchDepartments()])
  users.value = loadedUsers
  departments.value = loadedDepartments
})
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
  gap: 8px;
}

.ghost-button {
  min-height: 32px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #303133;
  padding: 0 12px;
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
