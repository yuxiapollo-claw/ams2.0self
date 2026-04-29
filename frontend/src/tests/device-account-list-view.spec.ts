import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createDeviceAccount,
  deleteDeviceAccount,
  fetchDeviceAccounts,
  updateDeviceAccount,
  type DeviceAccountItem
} from '../api/device-accounts'
import { fetchAssetTree } from '../api/assets'
import { fetchUsers, type UserItem } from '../api/users'
import { usePreferencesStore, type Locale } from '../stores/preferences'
import DeviceAccountListView from '../views/device-accounts/DeviceAccountListView.vue'

const pushMock = vi.fn()

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push: pushMock })
  }
})

vi.mock('../api/device-accounts', () => ({
  fetchDeviceAccounts: vi.fn(),
  createDeviceAccount: vi.fn(),
  updateDeviceAccount: vi.fn(),
  deleteDeviceAccount: vi.fn()
}))

vi.mock('../api/assets', () => ({
  fetchAssetTree: vi.fn()
}))

vi.mock('../api/users', () => ({
  fetchUsers: vi.fn()
}))

const deviceAccounts: DeviceAccountItem[] = [
  {
    id: '3',
    deviceNodeId: 100,
    deviceName: 'Device A',
    userId: '4',
    userName: 'Wang Wu',
    accountName: 'device_a_wangwu',
    accountStatus: 'ENABLED',
    sourceType: 'MANUAL',
    remark: 'Primary account',
    roles: ['Inspector']
  },
  {
    id: '4',
    deviceNodeId: 100,
    deviceName: 'Device A',
    userId: null,
    userName: null,
    accountName: 'device_a_spare_01',
    accountStatus: 'DISABLED',
    sourceType: 'IMPORTED',
    remark: 'Spare account',
    roles: []
  }
]

const users: UserItem[] = [
  {
    id: '4',
    userCode: 'EMP004',
    userName: 'Wang Wu',
    departmentId: '10',
    departmentName: 'Assembly Dept',
    employmentStatus: 'ACTIVE',
    loginName: 'wangwu',
    accountStatus: 'ENABLED'
  },
  {
    id: '5',
    userCode: 'EMP005',
    userName: 'Li Si',
    departmentId: '20',
    departmentName: 'Quality Dept',
    employmentStatus: 'ACTIVE',
    loginName: 'lisi',
    accountStatus: 'ENABLED'
  }
]

function mountWithLocale(locale: Locale = 'zh-CN') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)

  return mount(DeviceAccountListView, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('DeviceAccountListView', () => {
  beforeEach(() => {
    vi.mocked(fetchDeviceAccounts).mockReset()
    vi.mocked(createDeviceAccount).mockReset()
    vi.mocked(updateDeviceAccount).mockReset()
    vi.mocked(deleteDeviceAccount).mockReset()
    vi.mocked(fetchAssetTree).mockReset()
    vi.mocked(fetchUsers).mockReset()
    vi.restoreAllMocks()
    pushMock.mockReset()

    vi.mocked(fetchDeviceAccounts).mockResolvedValue(deviceAccounts)
    vi.mocked(createDeviceAccount).mockResolvedValue(deviceAccounts[0])
    vi.mocked(updateDeviceAccount).mockResolvedValue(deviceAccounts[0])
    vi.mocked(deleteDeviceAccount).mockResolvedValue()
    vi.mocked(fetchAssetTree).mockResolvedValue([
      {
        id: '100',
        name: 'Device A',
        type: 'DEVICE',
        children: []
      },
      {
        id: '200',
        name: 'Device B',
        type: 'DEVICE',
        children: []
      }
    ])
    vi.mocked(fetchUsers).mockResolvedValue(users)
  })

  it('renders loaded device-account rows and request affordances', async () => {
    const wrapper = mountWithLocale('zh-CN')
    await flushPromises()

    expect(fetchDeviceAccounts).toHaveBeenCalledTimes(1)
    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(fetchAssetTree).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('设备账号管理工作台')
    expect(wrapper.text()).toContain('device_a_wangwu')
    expect(wrapper.text()).toContain('Wang Wu')
    expect(wrapper.text()).toContain('Inspector')
    expect(wrapper.text()).toContain('未绑定')
    expect(wrapper.find('input[name="keyword"]').exists()).toBe(true)
    expect(wrapper.find('select[name="bindingFilter"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="action-request-role-add-4"]').attributes('disabled')).toBeDefined()
  })

  it('opens create drawer and submits normalized payload', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('[data-testid="create-device-account"]').trigger('click')
    await wrapper.get('select[name="deviceNodeId"]').setValue('100')
    await wrapper.get('select[name="userId"]').setValue('')
    await wrapper.get('input[name="accountName"]').setValue(' device_a_new ')
    await wrapper.get('select[name="accountStatus"]').setValue('ENABLED')
    await wrapper.get('select[name="sourceType"]').setValue('MANUAL')
    await wrapper.get('textarea[name="remark"]').setValue(' Fresh account ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createDeviceAccount).toHaveBeenCalledWith({
      deviceNodeId: 100,
      userId: null,
      accountName: 'device_a_new',
      accountStatus: 'ENABLED',
      sourceType: 'MANUAL',
      remark: 'Fresh account'
    })
  })

  it('opens edit drawer and submits an update payload', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('[data-testid="edit-4"]').trigger('click')
    await wrapper.get('select[name="userId"]').setValue('5')
    await wrapper.get('input[name="accountName"]').setValue(' device_a_spare_02 ')
    await wrapper.get('select[name="accountStatus"]').setValue('ENABLED')
    await wrapper.get('select[name="sourceType"]').setValue('IMPORTED')
    await wrapper.get('textarea[name="remark"]').setValue(' Assigned spare ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateDeviceAccount).toHaveBeenCalledWith('4', {
      deviceNodeId: 100,
      userId: 5,
      accountName: 'device_a_spare_02',
      accountStatus: 'ENABLED',
      sourceType: 'IMPORTED',
      remark: 'Assigned spare'
    })
  })

  it('navigates to the request form with prefilled account context', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('[data-testid="action-request-role-add-3"]').trigger('click')

    expect(pushMock).toHaveBeenCalledWith({
      name: 'request-form',
      query: {
        requestType: 'ROLE_ADD',
        targetUserId: '4',
        targetDeviceNodeId: '100',
        targetAccountName: 'device_a_wangwu'
      }
    })
  })

  it('deletes a device account after confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('[data-testid="delete-4"]').trigger('click')
    await flushPromises()

    expect(deleteDeviceAccount).toHaveBeenCalledWith('4')
  })
})
