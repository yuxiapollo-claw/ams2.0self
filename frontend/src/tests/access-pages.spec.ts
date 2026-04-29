import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RouterView } from 'vue-router'
import { fetchAssetTree } from '../api/assets'
import { fetchDeviceAccountsByDevice } from '../api/device-accounts'
import { createRequest, fetchRequests } from '../api/requests'
import router from '../router'
import { usePreferencesStore } from '../stores/preferences'
import { fetchUsers } from '../api/users'

vi.mock('../api/users', () => ({
  fetchUsers: vi.fn()
}))

vi.mock('../api/assets', () => ({
  fetchAssetTree: vi.fn()
}))

vi.mock('../api/device-accounts', () => ({
  fetchDeviceAccountsByDevice: vi.fn()
}))

vi.mock('../api/requests', () => ({
  fetchRequests: vi.fn(),
  createRequest: vi.fn()
}))

async function renderAt(path: string) {
  window.sessionStorage.setItem('ams_auth_token', 'test-token')

  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale('zh-CN')

  await router.push(path)
  await router.isReady()

  return mount(RouterView, {
    global: {
      plugins: [pinia, router]
    }
  })
}

describe('access target pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
    document.body.innerHTML = ''

    vi.mocked(fetchUsers).mockResolvedValue([
      {
        id: '4',
        userCode: 'U004',
        userName: '王五',
        departmentId: '1',
        departmentName: '质量部',
        employmentStatus: 'ACTIVE',
        loginName: 'wangwu',
        accountStatus: 'ACTIVE'
      }
    ])

    vi.mocked(fetchAssetTree).mockResolvedValue([
      {
        id: '100',
        name: 'DMS',
        type: 'DEVICE',
        children: [
          {
            id: '300',
            name: '文件归档角色',
            type: 'ROLE',
            children: []
          }
        ]
      }
    ])

    vi.mocked(fetchDeviceAccountsByDevice).mockResolvedValue([
      {
        id: '900',
        deviceNodeId: 100,
        deviceName: 'DMS',
        userId: '4',
        userName: '王五',
        accountName: 'device_a_wangwu',
        accountStatus: 'ACTIVE',
        sourceType: 'MANUAL',
        remark: '',
        roles: ['文件归档角色']
      }
    ])

    vi.mocked(fetchRequests).mockResolvedValue([
      {
        id: '1001',
        requestNo: 'REQ1001',
        requestType: 'ROLE_ADD',
        targetAccountName: 'device_a_wangwu',
        status: 'WAIT_DEPT_MANAGER',
        createdAt: '2026-04-29 09:00:00'
      }
    ])

    vi.mocked(createRequest).mockResolvedValue({
      id: 1001,
      requestNo: 'REQ1001',
      currentStatus: 'WAIT_DEPT_MANAGER'
    })
  })

  it('renders myRequest as a real request form and submits a role-add request', async () => {
    const wrapper = await renderAt('/access/myRequest')
    await flushPromises()

    expect(wrapper.text()).toContain('权限申请')
    expect(wrapper.text()).toContain('目标用户')
    expect(wrapper.text()).toContain('申请原因')
    expect(wrapper.text()).toContain('提交申请')

    await wrapper.get('textarea[name="reason"]').setValue('需要新增文档归档权限')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(createRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        requestType: 'ROLE_ADD',
        targetAccountName: 'device_a_wangwu'
      })
    )
    expect(wrapper.text()).toContain('申请已提交')
  })

  it('renders myView with granted and pending permission perspectives', async () => {
    const wrapper = await renderAt('/access/myView')
    await flushPromises()

    expect(wrapper.text()).toContain('我的权限视图')
    expect(wrapper.text()).toContain('已获得权限')
    expect(wrapper.text()).toContain('申请记录')
    expect(wrapper.text()).toContain('REQ1001')
  })

  it('renders accountManagement with account rows and quick actions', async () => {
    const wrapper = await renderAt('/access/accountManagement')
    await flushPromises()

    expect(wrapper.text()).toContain('账号列表')
    expect(wrapper.text()).toContain('device_a_wangwu')
    expect(wrapper.text()).toContain('申请权限')
    expect(wrapper.text()).toContain('查看权限')
  })
})
