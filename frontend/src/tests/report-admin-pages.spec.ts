import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchApplicationConfigs, fetchMailTemplates } from '../api/admin-config'
import { RouterView } from 'vue-router'
import { fetchAssetTree } from '../api/assets'
import { fetchAuditLogs } from '../api/audit'
import { fetchDepartments } from '../api/departments'
import { fetchDeviceAccounts } from '../api/device-accounts'
import { fetchDevicePermissions } from '../api/queries'
import { fetchRequests } from '../api/requests'
import router from '../router'
import { usePreferencesStore } from '../stores/preferences'
import { fetchUsers } from '../api/users'

vi.mock('../api/requests', () => ({
  fetchRequests: vi.fn(),
  createRequest: vi.fn()
}))

vi.mock('../api/users', () => ({
  fetchUsers: vi.fn()
}))

vi.mock('../api/departments', () => ({
  fetchDepartments: vi.fn()
}))

vi.mock('../api/device-accounts', () => ({
  fetchDeviceAccounts: vi.fn(),
  fetchDeviceAccountsByDevice: vi.fn(),
  createDeviceAccount: vi.fn(),
  updateDeviceAccount: vi.fn(),
  deleteDeviceAccount: vi.fn()
}))

vi.mock('../api/assets', () => ({
  fetchAssetTree: vi.fn()
}))

vi.mock('../api/audit', () => ({
  fetchAuditLogs: vi.fn()
}))

vi.mock('../api/queries', () => ({
  fetchDevicePermissions: vi.fn()
}))

vi.mock('../api/admin-config', () => ({
  fetchApplicationConfigs: vi.fn(),
  fetchMailTemplates: vi.fn(),
  createApplicationConfig: vi.fn(),
  updateApplicationConfig: vi.fn(),
  deleteApplicationConfig: vi.fn(),
  createMailTemplate: vi.fn(),
  updateMailTemplate: vi.fn(),
  deleteMailTemplate: vi.fn()
}))

async function renderAt(path: string) {
  window.sessionStorage.setItem('ams_auth_token', 'test-token')

  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale('en-US')

  await router.push(path)
  await router.isReady()

  return mount(RouterView, {
    global: {
      plugins: [pinia, router]
    }
  })
}

describe('report and admin target pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
    document.body.innerHTML = ''

    vi.mocked(fetchRequests).mockResolvedValue([
      {
        id: '1001',
        requestNo: 'REQ1001',
        requestType: 'ROLE_ADD',
        targetAccountName: 'device_a_wangwu',
        status: 'WAIT_QA',
        createdAt: '2026-04-29 09:00:00'
      }
    ])

    vi.mocked(fetchUsers).mockResolvedValue([
      {
        id: '4',
        userCode: 'U004',
        userName: 'Wang Wu',
        departmentId: '1',
        departmentName: 'Quality Dept',
        employmentStatus: 'ACTIVE',
        loginName: 'wangwu',
        accountStatus: 'ENABLED'
      }
    ])

    vi.mocked(fetchDepartments).mockResolvedValue([
      {
        id: '1',
        departmentName: 'Quality Dept',
        managerUserId: '4',
        managerUserName: 'Wang Wu',
        memberCount: 5,
        status: 'ENABLED',
        description: 'Quality management',
        updatedAt: '2026-04-29 08:00:00'
      }
    ])

    vi.mocked(fetchDeviceAccounts).mockResolvedValue([
      {
        id: '900',
        deviceNodeId: 100,
        deviceName: 'DMS',
        userId: '4',
        userName: 'Wang Wu',
        accountName: 'device_a_wangwu',
        accountStatus: 'ENABLED',
        sourceType: 'MANUAL',
        remark: 'Document account',
        roles: ['Archive Role']
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
            name: 'Archive Role',
            type: 'ROLE',
            children: []
          }
        ]
      }
    ])

    vi.mocked(fetchAuditLogs).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 'A1',
          operatorName: 'AMSAdmin',
          action: 'CREATE_REQUEST (DMS)',
          createdAt: '2026-04-29 10:00:00'
        }
      ]
    })

    vi.mocked(fetchDevicePermissions).mockResolvedValue({
      deviceNodeId: '100',
      roles: [
        {
          roleName: 'Archive Role',
          deviceAccounts: ['device_a_wangwu (Wang Wu)']
        }
      ]
    })

    vi.mocked(fetchApplicationConfigs).mockResolvedValue([
      {
        id: '1',
        applicationName: 'DMS',
        applicationCode: 'APP-DMS',
        description: 'Document management system',
        status: 'ENABLED',
        updatedAt: '2026-04-29T09:00:00'
      }
    ])

    vi.mocked(fetchMailTemplates).mockResolvedValue([
      {
        id: '1',
        templateName: 'Request Created',
        description: 'Notify new requests',
        subject: '[AMS] New Request',
        body: 'A new request has been submitted.',
        status: 'ENABLED',
        updatedAt: '2026-04-29T09:00:00'
      }
    ])
  })

  it('renders accountHistory as a real report view with request rows', async () => {
    const wrapper = await renderAt('/report/accountHistory')
    await flushPromises()

    expect(wrapper.text()).toContain('REQ1001')
    expect(wrapper.text()).toContain('device_a_wangwu')
  })

  it('renders manager access management with account and role data', async () => {
    const wrapper = await renderAt('/sysAccessManager/empAccessManagement')
    await flushPromises()

    expect(wrapper.text()).toContain('Archive Role')
    expect(wrapper.text()).toContain('device_a_wangwu')
  })

  it('renders system admin application config with real application rows', async () => {
    const wrapper = await renderAt('/systemAdmin/applicationConfig')
    await flushPromises()

    expect(wrapper.text()).toContain('DMS')
    expect(wrapper.text()).toContain('APP-DMS')
    expect(wrapper.text()).toContain('Document management system')
  })

  it('renders hr management with real employee information', async () => {
    const wrapper = await renderAt('/hrAdmin/hrManagement')
    await flushPromises()

    expect(wrapper.text()).toContain('Wang Wu')
    expect(wrapper.text()).toContain('Quality Dept')
  })
})
