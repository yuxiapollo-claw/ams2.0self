import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createDepartment,
  deleteDepartment,
  fetchDepartments,
  updateDepartment,
  type DepartmentItem
} from '../api/departments'
import { fetchUsers, type UserItem } from '../api/users'
import { usePreferencesStore, type Locale } from '../stores/preferences'
import DepartmentListView from '../views/departments/DepartmentListView.vue'

vi.mock('../api/departments', () => ({
  fetchDepartments: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn()
}))

vi.mock('../api/users', () => ({
  fetchUsers: vi.fn()
}))

const departments: DepartmentItem[] = [
  {
    id: '10',
    departmentName: 'Assembly Dept',
    managerUserId: '101',
    managerUserName: 'Zhang Lead',
    description: 'Handles assembly line coordination.',
    memberCount: 12,
    status: 'ENABLED',
    updatedAt: '2026-04-24T09:00:00Z'
  },
  {
    id: '20',
    departmentName: 'Quality Dept',
    managerUserId: '102',
    managerUserName: 'Li Lead',
    description: 'Owns inspection and release checks.',
    memberCount: 5,
    status: 'DISABLED',
    updatedAt: '2026-04-24T10:00:00Z'
  }
]

const users: UserItem[] = [
  {
    id: '101',
    userCode: 'EMP101',
    userName: 'Zhang Lead',
    departmentId: '10',
    departmentName: 'Assembly Dept',
    employmentStatus: 'ACTIVE',
    loginName: 'zhanglead',
    accountStatus: 'ENABLED'
  },
  {
    id: '102',
    userCode: 'EMP102',
    userName: 'Li Lead',
    departmentId: '20',
    departmentName: 'Quality Dept',
    employmentStatus: 'ACTIVE',
    loginName: 'lilead',
    accountStatus: 'ENABLED'
  }
]

function mountWithLocale(locale: Locale = 'zh-CN') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)

  return mount(DepartmentListView, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('DepartmentListView', () => {
  beforeEach(() => {
    vi.mocked(fetchDepartments).mockReset()
    vi.mocked(createDepartment).mockReset()
    vi.mocked(updateDepartment).mockReset()
    vi.mocked(deleteDepartment).mockReset()
    vi.mocked(fetchUsers).mockReset()
    vi.restoreAllMocks()

    vi.mocked(fetchDepartments).mockResolvedValue(departments)
    vi.mocked(fetchUsers).mockResolvedValue(users)
    vi.mocked(createDepartment).mockResolvedValue(departments[0])
    vi.mocked(updateDepartment).mockResolvedValue(departments[0])
    vi.mocked(deleteDepartment).mockResolvedValue()
  })

  it('renders loaded department rows and management affordances', async () => {
    const wrapper = mountWithLocale('en-US')
    await flushPromises()

    expect(fetchDepartments).toHaveBeenCalledTimes(1)
    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Department Management Workbench')
    expect(wrapper.text()).toContain('Create Department')
    expect(wrapper.text()).toContain('Assembly Dept')
    expect(wrapper.text()).toContain('Zhang Lead')
    expect(wrapper.text()).toContain('Handles assembly line coordination.')
    expect(wrapper.find('input[name="keyword"]').exists()).toBe(true)
    expect(wrapper.find('select[name="statusFilter"]').exists()).toBe(true)
  })

  it('opens create drawer and submits normalized create payload', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="create-department"]').trigger('click')

    await wrapper.get('input[name="departmentName"]').setValue(' Packaging Dept ')
    await wrapper.get('select[name="managerUserId"]').setValue('102')
    await wrapper.get('textarea[name="description"]').setValue(' Secondary packing line ')
    await wrapper.get('select[name="status"]').setValue('ENABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createDepartment).toHaveBeenCalledWith({
      departmentName: 'Packaging Dept',
      managerUserId: 102,
      description: 'Secondary packing line',
      status: 'ENABLED'
    })
  })

  it('prevents overlapping drawer submits while a save is pending', async () => {
    let resolveCreate: (value: DepartmentItem) => void = () => undefined
    const pendingCreate = new Promise<DepartmentItem>((resolve) => {
      resolveCreate = resolve
    })
    vi.mocked(createDepartment).mockReturnValueOnce(pendingCreate)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="create-department"]').trigger('click')
    await wrapper.get('input[name="departmentName"]').setValue('Packaging Dept')
    await wrapper.get('select[name="managerUserId"]').setValue('102')
    await wrapper.get('textarea[name="description"]').setValue('Secondary packing line')
    await wrapper.get('select[name="status"]').setValue('ENABLED')

    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(createDepartment).toHaveBeenCalledTimes(1)

    resolveCreate({
      id: '30',
      departmentName: 'Packaging Dept',
      managerUserId: '102',
      managerUserName: 'Li Lead',
      description: 'Secondary packing line',
      memberCount: 0,
      status: 'ENABLED',
      updatedAt: '2026-04-24T11:30:00Z'
    })
    await flushPromises()
  })

  it('opens edit drawer and submits update payload', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="edit-10"]').trigger('click')
    await wrapper.get('input[name="departmentName"]').setValue(' Assembly Dept North ')
    await wrapper.get('select[name="managerUserId"]').setValue('')
    await wrapper.get('textarea[name="description"]').setValue(' North campus assembly ')
    await wrapper.get('select[name="status"]').setValue('DISABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateDepartment).toHaveBeenCalledWith('10', {
      departmentName: 'Assembly Dept North',
      managerUserId: null,
      description: 'North campus assembly',
      status: 'DISABLED'
    })
  })

  it('does not resubmit a stale manager id when the current manager option is unavailable', async () => {
    const staleDepartments: DepartmentItem[] = [
      {
        id: '30',
        departmentName: 'Packaging Dept',
        managerUserId: '999',
        managerUserName: '',
        description: 'Secondary packing line',
        memberCount: 3,
        status: 'ENABLED',
        updatedAt: '2026-04-24T11:35:00Z'
      }
    ]

    vi.mocked(fetchDepartments).mockReset()
    vi.mocked(fetchUsers).mockReset()
    vi.mocked(fetchDepartments)
      .mockResolvedValueOnce(staleDepartments)
      .mockResolvedValueOnce([
        {
          ...staleDepartments[0],
          departmentName: 'Packaging Dept North'
        }
      ])
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockResolvedValueOnce(users)
    vi.mocked(updateDepartment).mockResolvedValueOnce({
      ...staleDepartments[0],
      departmentName: 'Packaging Dept North',
      managerUserId: null,
      description: 'Secondary packing line',
      status: 'ENABLED'
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    expect(wrapper.text()).toContain('前任经理不可用')

    await wrapper.get('button[data-testid="edit-30"]').trigger('click')

    expect((wrapper.get('select[name="managerUserId"]').element as HTMLSelectElement).value).toBe('')

    await wrapper.get('input[name="departmentName"]').setValue('Packaging Dept North')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateDepartment).toHaveBeenCalledWith('30', {
      departmentName: 'Packaging Dept North',
      managerUserId: null,
      description: 'Secondary packing line',
      status: 'ENABLED'
    })
  })

  it('deletes a department after confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="delete-20"]').trigger('click')
    await flushPromises()

    expect(deleteDepartment).toHaveBeenCalledWith('20')
  })

  it('shows backend mutation message from an axios-style error', async () => {
    vi.mocked(createDepartment).mockRejectedValueOnce({
      response: {
        data: {
          message: 'Department name already exists'
        }
      }
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="create-department"]').trigger('click')
    await wrapper.get('input[name="departmentName"]').setValue('Packaging Dept')
    await wrapper.get('select[name="managerUserId"]').setValue('102')
    await wrapper.get('textarea[name="description"]').setValue('Secondary packing line')
    await wrapper.get('select[name="status"]').setValue('ENABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Department name already exists')
  })

  it('reconciles a created department with follow-up refreshed page data', async () => {
    vi.mocked(fetchDepartments).mockReset()
    vi.mocked(fetchUsers).mockReset()
    vi.mocked(fetchDepartments)
      .mockResolvedValueOnce(departments)
      .mockResolvedValueOnce([
        ...departments,
        {
          id: '30',
          departmentName: 'Packaging Dept',
          managerUserId: '102',
          managerUserName: 'Li Lead',
          description: 'Refreshed from server',
          memberCount: 3,
          status: 'ENABLED',
          updatedAt: '2026-04-24T11:35:00Z'
        }
      ])
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockResolvedValueOnce(users)
    vi.mocked(createDepartment).mockResolvedValueOnce({
      id: '30',
      departmentName: 'Packaging Dept',
      managerUserId: '102',
      managerUserName: 'Li Lead',
      description: 'Secondary packing line',
      memberCount: 0,
      status: 'ENABLED',
      updatedAt: '2026-04-24T11:30:00Z'
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="create-department"]').trigger('click')
    await wrapper.get('input[name="departmentName"]').setValue('Packaging Dept')
    await wrapper.get('select[name="managerUserId"]').setValue('102')
    await wrapper.get('textarea[name="description"]').setValue('Secondary packing line')
    await wrapper.get('select[name="status"]').setValue('ENABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(fetchDepartments).toHaveBeenCalledTimes(2)
    expect(fetchUsers).toHaveBeenCalledTimes(2)
    expect(wrapper.find('input[name="departmentName"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Packaging Dept')
    expect(wrapper.text()).toContain('Refreshed from server')
  })

  it('keeps the saved edit visible and shows a non-destructive error when follow-up refresh fails', async () => {
    vi.mocked(fetchDepartments).mockReset()
    vi.mocked(fetchUsers).mockReset()
    vi.mocked(fetchDepartments)
      .mockResolvedValueOnce(departments)
      .mockRejectedValueOnce(new Error('refresh failed'))
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockResolvedValueOnce(users)
    vi.mocked(updateDepartment).mockResolvedValueOnce({
      ...departments[0],
      departmentName: 'Assembly Dept North',
      description: 'North campus assembly',
      status: 'DISABLED'
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="edit-10"]').trigger('click')
    await wrapper.get('input[name="departmentName"]').setValue('Assembly Dept North')
    await wrapper.get('textarea[name="description"]').setValue('North campus assembly')
    await wrapper.get('select[name="status"]').setValue('DISABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(fetchDepartments).toHaveBeenCalledTimes(2)
    expect(wrapper.find('input[name="departmentName"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Assembly Dept North')
    expect(wrapper.text()).toContain('停用')
    expect(wrapper.text()).toContain('refresh failed')
  })

  it('refreshes after delete while preserving current filters', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(fetchDepartments).mockReset()
    vi.mocked(fetchUsers).mockReset()
    vi.mocked(fetchDepartments)
      .mockResolvedValueOnce(departments)
      .mockResolvedValueOnce([departments[0]])
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockResolvedValueOnce(users)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('input[name="keyword"]').setValue('Quality')
    await wrapper.get('select[name="statusFilter"]').setValue('DISABLED')
    await wrapper.get('button[data-testid="delete-20"]').trigger('click')
    await flushPromises()

    expect(fetchDepartments).toHaveBeenCalledTimes(2)
    expect(fetchUsers).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).not.toContain('Quality Dept')
    expect((wrapper.get('input[name="keyword"]').element as HTMLInputElement).value).toBe('Quality')
    expect((wrapper.get('select[name="statusFilter"]').element as HTMLSelectElement).value).toBe('DISABLED')
    expect(wrapper.text()).toContain('当前筛选条件下没有匹配部门。')
  })

  it('guards overlapping refresh clicks and exposes refresh pending state', async () => {
    let resolveDepartmentsRefresh: (value: DepartmentItem[]) => void = () => undefined
    let resolveUsersRefresh: (value: UserItem[]) => void = () => undefined
    const pendingDepartmentsRefresh = new Promise<DepartmentItem[]>((resolve) => {
      resolveDepartmentsRefresh = resolve
    })
    const pendingUsersRefresh = new Promise<UserItem[]>((resolve) => {
      resolveUsersRefresh = resolve
    })

    vi.mocked(fetchDepartments)
      .mockResolvedValueOnce(departments)
      .mockReturnValueOnce(pendingDepartmentsRefresh)
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockReturnValueOnce(pendingUsersRefresh)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="refresh-departments"]').trigger('click')
    await nextTick()

    expect(wrapper.get('button[data-testid="refresh-departments"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('button[data-testid="refresh-departments"]').text()).toBe('正在刷新...')

    await wrapper.get('button[data-testid="refresh-departments"]').trigger('click')

    expect(fetchDepartments).toHaveBeenCalledTimes(2)
    expect(fetchUsers).toHaveBeenCalledTimes(2)

    resolveDepartmentsRefresh([
      ...departments,
      {
        id: '30',
        departmentName: 'Packaging Dept',
        managerUserId: '102',
        managerUserName: 'Li Lead',
        description: 'Secondary packing line',
        memberCount: 0,
        status: 'ENABLED',
        updatedAt: '2026-04-24T11:30:00Z'
      }
    ])
    resolveUsersRefresh(users)
    await flushPromises()

    expect(wrapper.get('button[data-testid="refresh-departments"]').text()).toBe('刷新')
    expect(wrapper.text()).toContain('Packaging Dept')
  })

  it('blocks refresh while the drawer is open and preserves unsaved form state', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="edit-10"]').trigger('click')
    await wrapper.get('input[name="departmentName"]').setValue('Assembly Dept Draft')
    await wrapper.get('textarea[name="description"]').setValue('Draft description')
    await nextTick()

    expect(wrapper.get('button[data-testid="refresh-departments"]').attributes('disabled')).toBeDefined()

    await wrapper.get('button[data-testid="refresh-departments"]').trigger('click')
    await flushPromises()

    expect(fetchDepartments).toHaveBeenCalledTimes(1)
    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect((wrapper.get('input[name="departmentName"]').element as HTMLInputElement).value).toBe('Assembly Dept Draft')
    expect((wrapper.get('textarea[name="description"]').element as HTMLTextAreaElement).value).toBe('Draft description')
  })

  it('blocks row mutations while refresh is pending', async () => {
    let resolveDepartmentsRefresh: (value: DepartmentItem[]) => void = () => undefined
    let resolveUsersRefresh: (value: UserItem[]) => void = () => undefined
    const pendingDepartmentsRefresh = new Promise<DepartmentItem[]>((resolve) => {
      resolveDepartmentsRefresh = resolve
    })
    const pendingUsersRefresh = new Promise<UserItem[]>((resolve) => {
      resolveUsersRefresh = resolve
    })

    vi.mocked(fetchDepartments)
      .mockResolvedValueOnce(departments)
      .mockReturnValueOnce(pendingDepartmentsRefresh)
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockReturnValueOnce(pendingUsersRefresh)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="refresh-departments"]').trigger('click')
    await nextTick()

    expect(wrapper.get('button[data-testid="edit-10"]').attributes('disabled')).toBeDefined()

    await wrapper.get('button[data-testid="edit-10"]').trigger('click')

    expect(wrapper.find('input[name="departmentName"]').exists()).toBe(false)

    resolveDepartmentsRefresh(departments)
    resolveUsersRefresh(users)
    await flushPromises()
  })

  it('shows initial-load error and retries successfully', async () => {
    vi.mocked(fetchDepartments)
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce(departments)
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockResolvedValueOnce(users)

    const wrapper = mountWithLocale()
    await flushPromises()

    expect(wrapper.text()).toContain('加载部门失败')
    expect(wrapper.text()).toContain('重试')

    await wrapper.get('button[data-testid="retry-load"]').trigger('click')
    await flushPromises()

    expect(fetchDepartments).toHaveBeenCalledTimes(2)
    expect(fetchUsers).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Assembly Dept')
    expect(wrapper.text()).not.toContain('加载部门失败')
  })
})
