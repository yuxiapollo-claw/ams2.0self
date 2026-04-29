import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { DepartmentItem } from '../api/departments'
import {
  createUser,
  deleteUser,
  fetchUsers,
  updateUser,
  updateUserStatus,
  type UserItem
} from '../api/users'
import { fetchDepartments } from '../api/departments'
import { usePreferencesStore, type Locale } from '../stores/preferences'
import UserListView from '../views/users/UserListView.vue'

vi.mock('../api/users', () => ({
  fetchUsers: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  updateUserStatus: vi.fn(),
  deleteUser: vi.fn()
}))

vi.mock('../api/departments', () => ({
  fetchDepartments: vi.fn()
}))

const users: UserItem[] = [
  {
    id: '1',
    userCode: 'EMP001',
    userName: 'Zhang San',
    departmentId: '10',
    departmentName: 'Manufacturing',
    employmentStatus: 'ACTIVE',
    loginName: 'zhangsan',
    accountStatus: 'ENABLED'
  },
  {
    id: '2',
    userCode: 'EMP002',
    userName: 'Li Si',
    departmentId: '20',
    departmentName: 'Quality',
    employmentStatus: 'LEAVE',
    loginName: 'lisi',
    accountStatus: 'DISABLED'
  }
]

const departments: DepartmentItem[] = [
  {
    id: '10',
    departmentName: 'Manufacturing',
    managerUserName: 'Manager A',
    status: 'ACTIVE'
  },
  {
    id: '20',
    departmentName: 'Quality',
    managerUserName: 'Manager B',
    status: 'ACTIVE'
  }
]

function mountWithLocale(locale: Locale = 'zh-CN') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)

  return mount(UserListView, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('UserListView', () => {
  beforeEach(() => {
    vi.mocked(fetchUsers).mockReset()
    vi.mocked(createUser).mockReset()
    vi.mocked(updateUser).mockReset()
    vi.mocked(updateUserStatus).mockReset()
    vi.mocked(deleteUser).mockReset()
    vi.mocked(fetchDepartments).mockReset()
    vi.restoreAllMocks()

    vi.mocked(fetchUsers).mockResolvedValue(users)
    vi.mocked(fetchDepartments).mockResolvedValue(departments)
    vi.mocked(createUser).mockResolvedValue(users[0])
    vi.mocked(updateUser).mockResolvedValue(users[0])
    vi.mocked(updateUserStatus).mockResolvedValue(users[0])
    vi.mocked(deleteUser).mockResolvedValue()
  })

  it('renders loaded user rows and primary management affordances', async () => {
    const wrapper = mountWithLocale('zh-CN')
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(fetchDepartments).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('用户管理工作台')
    expect(wrapper.text()).toContain('新建用户')
    expect(wrapper.text()).toContain('EMP001')
    expect(wrapper.text()).toContain('zhangsan')
    expect(wrapper.text()).toContain('Manufacturing')
    expect(wrapper.find('input[name="keyword"]').exists()).toBe(true)
    expect(wrapper.find('select[name="accountStatusFilter"]').exists()).toBe(true)
  })

  it('opens create drawer and submits normalized create payload', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="create-user"]').trigger('click')

    await wrapper.get('input[name="userCode"]').setValue(' EMP003 ')
    await wrapper.get('input[name="userName"]').setValue(' Wang Wu ')
    await wrapper.get('select[name="departmentId"]').setValue('20')
    await wrapper.get('select[name="employmentStatus"]').setValue('ACTIVE')
    await wrapper.get('input[name="loginName"]').setValue(' wangwu ')
    await wrapper.get('select[name="accountStatus"]').setValue('ENABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createUser).toHaveBeenCalledWith({
      userCode: 'EMP003',
      userName: 'Wang Wu',
      departmentId: 20,
      employmentStatus: 'ACTIVE',
      loginName: 'wangwu',
      accountStatus: 'ENABLED'
    })
  })

  it('prevents overlapping drawer submits while a save is pending', async () => {
    let resolveCreate: (value: UserItem) => void = () => undefined
    const pendingCreate = new Promise<UserItem>((resolve) => {
      resolveCreate = resolve
    })
    vi.mocked(createUser).mockReturnValueOnce(pendingCreate)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="create-user"]').trigger('click')
    await wrapper.get('input[name="userCode"]').setValue('EMP003')
    await wrapper.get('input[name="userName"]').setValue('Wang Wu')
    await wrapper.get('select[name="departmentId"]').setValue('20')
    await wrapper.get('select[name="employmentStatus"]').setValue('ACTIVE')
    await wrapper.get('input[name="loginName"]').setValue('wangwu')
    await wrapper.get('select[name="accountStatus"]').setValue('ENABLED')

    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(createUser).toHaveBeenCalledTimes(1)

    resolveCreate({
      id: '3',
      userCode: 'EMP003',
      userName: 'Wang Wu',
      departmentId: '20',
      departmentName: 'Quality',
      employmentStatus: 'ACTIVE',
      loginName: 'wangwu',
      accountStatus: 'ENABLED'
    })
    await flushPromises()
  })

  it('opens edit drawer and submits update payload', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="edit-1"]').trigger('click')
    await wrapper.get('input[name="userName"]').setValue(' Zhang San Updated ')
    await wrapper.get('select[name="accountStatus"]').setValue('DISABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateUser).toHaveBeenCalledWith('1', {
      userCode: 'EMP001',
      userName: 'Zhang San Updated',
      departmentId: 10,
      employmentStatus: 'ACTIVE',
      loginName: 'zhangsan',
      accountStatus: 'DISABLED'
    })
  })

  it('toggles account status through the status action', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="status-1"]').trigger('click')
    await flushPromises()

    expect(updateUserStatus).toHaveBeenCalledWith('1', {
      accountStatus: 'DISABLED'
    })
  })

  it('deletes a user after confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="delete-2"]').trigger('click')
    await flushPromises()

    expect(deleteUser).toHaveBeenCalledWith('2')
  })

  it('shows backend message from an axios-style rejected mutation', async () => {
    vi.mocked(createUser).mockRejectedValueOnce({
      response: {
        data: {
          message: 'User code already exists'
        }
      }
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="create-user"]').trigger('click')
    await wrapper.get('input[name="userCode"]').setValue('EMP003')
    await wrapper.get('input[name="userName"]').setValue('Wang Wu')
    await wrapper.get('select[name="departmentId"]').setValue('20')
    await wrapper.get('select[name="employmentStatus"]').setValue('ACTIVE')
    await wrapper.get('input[name="loginName"]').setValue('wangwu')
    await wrapper.get('select[name="accountStatus"]').setValue('ENABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('User code already exists')
  })

  it('prevents overlapping row actions while one row mutation is pending', async () => {
    let resolveStatus: (value: UserItem) => void = () => undefined
    const pendingStatus = new Promise<UserItem>((resolve) => {
      resolveStatus = resolve
    })
    vi.mocked(updateUserStatus).mockReturnValueOnce(pendingStatus)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="status-1"]').trigger('click')
    await nextTick()

    expect(wrapper.get('button[data-testid="status-1"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('button[data-testid="status-2"]').attributes('disabled')).toBeDefined()

    await wrapper.get('button[data-testid="status-2"]').trigger('click')

    expect(updateUserStatus).toHaveBeenCalledTimes(1)

    resolveStatus({
      ...users[0],
      accountStatus: 'DISABLED'
    })
    await flushPromises()
  })

  it('keeps local ui state correct after a successful status mutation without a follow-up fetch', async () => {
    vi.mocked(updateUserStatus).mockResolvedValueOnce({
      ...users[0],
      accountStatus: 'DISABLED'
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="status-1"]').trigger('click')
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(wrapper.get('button[data-testid="status-1"]').text()).toBe('启用')
  })

  it('appends a created user locally without a follow-up fetch', async () => {
    vi.mocked(createUser).mockResolvedValueOnce({
      id: '3',
      userCode: 'EMP003',
      userName: 'Wang Wu',
      departmentId: '20',
      departmentName: 'Quality',
      employmentStatus: 'ACTIVE',
      loginName: 'wangwu',
      accountStatus: 'ENABLED'
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="create-user"]').trigger('click')
    await wrapper.get('input[name="userCode"]').setValue('EMP003')
    await wrapper.get('input[name="userName"]').setValue('Wang Wu')
    await wrapper.get('select[name="departmentId"]').setValue('20')
    await wrapper.get('select[name="employmentStatus"]').setValue('ACTIVE')
    await wrapper.get('input[name="loginName"]').setValue('wangwu')
    await wrapper.get('select[name="accountStatus"]').setValue('ENABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('EMP003')
    expect(wrapper.text()).toContain('wangwu')
  })

  it('updates an edited user locally without a follow-up fetch', async () => {
    vi.mocked(updateUser).mockResolvedValueOnce({
      ...users[0],
      userName: 'Zhang San Updated',
      accountStatus: 'DISABLED'
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="edit-1"]').trigger('click')
    await wrapper.get('input[name="userName"]').setValue('Zhang San Updated')
    await wrapper.get('select[name="accountStatus"]').setValue('DISABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Zhang San Updated')
    expect(wrapper.get('button[data-testid="status-1"]').text()).toBe('启用')
  })

  it('removes a deleted user locally without a follow-up fetch', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="delete-2"]').trigger('click')
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).not.toContain('EMP002')
    expect(wrapper.text()).not.toContain('lisi')
  })

  it('guards overlapping refresh clicks and exposes refresh pending state', async () => {
    let resolveRefresh: (value: UserItem[]) => void = () => undefined
    const pendingRefresh = new Promise<UserItem[]>((resolve) => {
      resolveRefresh = resolve
    })
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockReturnValueOnce(pendingRefresh)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="refresh-users"]').trigger('click')
    await nextTick()

    expect(wrapper.get('button[data-testid="refresh-users"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('button[data-testid="refresh-users"]').text()).toBe('正在刷新...')

    await wrapper.get('button[data-testid="refresh-users"]').trigger('click')

    expect(fetchUsers).toHaveBeenCalledTimes(2)

    resolveRefresh([
      ...users,
      {
        id: '3',
        userCode: 'EMP003',
        userName: 'Wang Wu',
        departmentId: '20',
        departmentName: 'Quality',
        employmentStatus: 'ACTIVE',
        loginName: 'wangwu',
        accountStatus: 'ENABLED'
      }
    ])
    await flushPromises()

    expect(wrapper.get('button[data-testid="refresh-users"]').text()).toBe('刷新')
    expect(wrapper.text()).toContain('EMP003')
  })

  it('blocks row mutations while refresh is pending', async () => {
    let resolveRefresh: (value: UserItem[]) => void = () => undefined
    const pendingRefresh = new Promise<UserItem[]>((resolve) => {
      resolveRefresh = resolve
    })
    vi.mocked(fetchUsers)
      .mockResolvedValueOnce(users)
      .mockReturnValueOnce(pendingRefresh)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('button[data-testid="refresh-users"]').trigger('click')
    await nextTick()

    expect(wrapper.get('button[data-testid="status-1"]').attributes('disabled')).toBeDefined()

    await wrapper.get('button[data-testid="status-1"]').trigger('click')

    expect(updateUserStatus).not.toHaveBeenCalled()

    resolveRefresh(users)
    await flushPromises()
  })

  it('shows an error state when initial load fails and retries successfully', async () => {
    vi.mocked(fetchUsers)
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce(users)

    const wrapper = mountWithLocale()
    await flushPromises()

    expect(wrapper.text()).toContain('加载用户失败')
    expect(wrapper.text()).toContain('重试')

    await wrapper.get('button[data-testid="retry-load"]').trigger('click')
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('EMP001')
    expect(wrapper.text()).not.toContain('加载用户失败')
  })
})
