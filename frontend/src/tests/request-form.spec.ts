import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useRoute } from 'vue-router'
import { fetchAssetTree } from '../api/assets'
import { fetchDeviceAccountsByDevice } from '../api/device-accounts'
import { createRequest } from '../api/requests'
import { usePreferencesStore, type Locale } from '../stores/preferences'
import { fetchUsers } from '../api/users'
import RequestFormView from '../views/requests/RequestFormView.vue'

vi.mock('../api/assets', () => ({
  fetchAssetTree: vi.fn()
}))

vi.mock('../api/device-accounts', () => ({
  fetchDeviceAccountsByDevice: vi.fn()
}))

vi.mock('../api/requests', () => ({
  createRequest: vi.fn()
}))

vi.mock('../api/users', () => ({
  fetchUsers: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: vi.fn()
}))

const routeState = reactive<{ query: Record<string, string> }>({
  query: {}
})

function mountWithLocale(locale: Locale = 'zh-CN') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)

  return mount(RequestFormView, {
    global: {
      plugins: [pinia]
    }
  })
}

function getOptionValues(wrapper: ReturnType<typeof mount>, selector: string) {
  return wrapper
    .get(selector)
    .findAll('option')
    .map((option) => (option.element as HTMLOptionElement).value)
}

function createDeferred<T>() {
  let resolve: (value: T) => void = () => undefined
  const promise = new Promise<T>((nextResolve) => {
    resolve = nextResolve
  })
  return { promise, resolve }
}

type MockDeviceAccountRow = {
  id: string
  deviceNodeId: number
  deviceName: string
  userId: string | null
  userName: string | null
  accountName: string
  accountStatus: string
  sourceType: string
  remark: string
  roles: string[]
}

describe('RequestFormView', () => {
  beforeEach(() => {
    vi.mocked(fetchUsers).mockReset()
    vi.mocked(fetchAssetTree).mockReset()
    vi.mocked(fetchDeviceAccountsByDevice).mockReset()
    vi.mocked(createRequest).mockReset()
    vi.mocked(useRoute).mockReset()
    routeState.query = {}
    vi.mocked(useRoute).mockReturnValue(routeState as never)

    vi.mocked(fetchUsers).mockResolvedValue([
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
    ])
    vi.mocked(fetchAssetTree).mockResolvedValue([
      {
        id: '100',
        name: 'Device A',
        type: 'DEVICE',
        children: [{ id: '302', name: 'Inspector', type: 'ROLE', children: [] }]
      },
      {
        id: '200',
        name: 'Device B',
        type: 'DEVICE',
        children: [{ id: '402', name: 'Maintainer', type: 'ROLE', children: [] }]
      }
    ])
    vi.mocked(fetchDeviceAccountsByDevice).mockImplementation(async (deviceNodeId: string) => {
      if (deviceNodeId === '200') {
        return [
          {
            id: 'DA-200-5',
            deviceNodeId: 200,
            deviceName: 'Device B',
            userId: '5',
            userName: 'Li Si',
            accountName: 'device_b_lisi',
            accountStatus: 'ACTIVE',
            sourceType: 'MANUAL',
            remark: '',
            roles: []
          }
        ]
      }

      return [
        {
          id: 'DA-100-4',
          deviceNodeId: 100,
          deviceName: 'Device A',
          userId: '4',
          userName: 'Wang Wu',
          accountName: 'device_a_wangwu',
          accountStatus: 'ACTIVE',
          sourceType: 'MANUAL',
          remark: '',
          roles: []
        }
      ]
    })
    vi.mocked(createRequest).mockResolvedValue({ id: 'REQ-1' })
  })

  it('loads user, device and account options on mount', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(fetchAssetTree).toHaveBeenCalledTimes(1)
    expect(fetchDeviceAccountsByDevice).toHaveBeenCalledWith('100')
    expect(wrapper.text()).toContain('申请发起台')
    expect(wrapper.text()).toContain('审批路径')
    expect(wrapper.text()).toContain('Wang Wu')
    expect(wrapper.text()).toContain('device_a_wangwu')
  })

  it('submits the backend-compatible request payload', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('select[name="targetUserId"]').setValue('4')
    await wrapper.get('select[name="targetDeviceNodeId"]').setValue('100')
    await wrapper.get('select[name="targetAccountName"]').setValue('device_a_wangwu')
    await wrapper.get('select[name="roleNodeId"]').setValue('302')
    await wrapper.get('textarea[name="reason"]').setValue('Position adjustment')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createRequest).toHaveBeenCalledTimes(1)
    expect(createRequest).toHaveBeenCalledWith({
      requestType: 'ROLE_ADD',
      targetUserId: 4,
      targetDeviceNodeId: 100,
      targetAccountName: 'device_a_wangwu',
      reason: 'Position adjustment',
      items: [{ roleNodeId: 302 }]
    })
    expect(wrapper.text()).toContain('打开申请列表')
  })

  it('shows the submit failure reason when request creation is rejected', async () => {
    vi.mocked(createRequest).mockRejectedValueOnce(new Error('request rejected'))

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('select[name="targetUserId"]').setValue('4')
    await wrapper.get('select[name="targetDeviceNodeId"]').setValue('100')
    await wrapper.get('select[name="targetAccountName"]').setValue('device_a_wangwu')
    await wrapper.get('select[name="roleNodeId"]').setValue('302')
    await wrapper.get('textarea[name="reason"]').setValue('Position adjustment')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createRequest).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('request rejected')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  it('prevents duplicate submits while request is pending', async () => {
    let resolveRequest: (value: { id: string }) => void = () => undefined
    const pendingRequest = new Promise<{ id: string }>((resolve) => {
      resolveRequest = resolve
    })
    vi.mocked(createRequest).mockReturnValueOnce(pendingRequest)

    const wrapper = mountWithLocale()
    await flushPromises()
    await wrapper.get('textarea[name="reason"]').setValue('test reason')

    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(createRequest).toHaveBeenCalledTimes(1)
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    resolveRequest({ id: 'REQ-2' })
    await flushPromises()

    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    await wrapper.get('textarea[name="reason"]').setValue('another reason')
    await nextTick()

    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  it('honors route query prefills while preserving default load and submit behavior', async () => {
    routeState.query = {
      requestType: 'ROLE_ADD',
      targetUserId: '5',
      targetDeviceNodeId: '200',
      targetAccountName: 'device_b_lisi'
    }

    const wrapper = mountWithLocale()
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(fetchAssetTree).toHaveBeenCalledTimes(1)
    expect(fetchDeviceAccountsByDevice).toHaveBeenCalledWith('200')

    expect((wrapper.get('select[name="requestType"]').element as HTMLSelectElement).value).toBe('ROLE_ADD')
    expect((wrapper.get('select[name="targetUserId"]').element as HTMLSelectElement).value).toBe('5')
    expect((wrapper.get('select[name="targetDeviceNodeId"]').element as HTMLSelectElement).value).toBe('200')
    expect((wrapper.get('select[name="targetAccountName"]').element as HTMLSelectElement).value).toBe('device_b_lisi')

    await wrapper.get('select[name="roleNodeId"]').setValue('402')
    await wrapper.get('textarea[name="reason"]').setValue('Prefilled flow')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createRequest).toHaveBeenCalledWith({
      requestType: 'ROLE_ADD',
      targetUserId: 5,
      targetDeviceNodeId: 200,
      targetAccountName: 'device_b_lisi',
      reason: 'Prefilled flow',
      items: [{ roleNodeId: 402 }]
    })
  })

  it('falls back to loaded defaults when the route query is cleared', async () => {
    routeState.query = {
      requestType: 'ROLE_ADD',
      targetUserId: '5',
      targetDeviceNodeId: '200',
      targetAccountName: 'device_b_lisi'
    }

    const wrapper = mountWithLocale()
    await flushPromises()

    routeState.query = {}
    await nextTick()
    await flushPromises()

    expect((wrapper.get('select[name="requestType"]').element as HTMLSelectElement).value).toBe('ROLE_ADD')
    expect((wrapper.get('select[name="targetUserId"]').element as HTMLSelectElement).value).toBe('4')
    expect((wrapper.get('select[name="targetDeviceNodeId"]').element as HTMLSelectElement).value).toBe('100')
    expect((wrapper.get('select[name="targetAccountName"]').element as HTMLSelectElement).value).toBe('device_a_wangwu')
  })

  it('filters device accounts by stable userId instead of userName', async () => {
    vi.mocked(fetchUsers).mockResolvedValue([
      {
        id: '4',
        userCode: 'EMP004',
        userName: 'Alex Chen',
        departmentId: '10',
        departmentName: 'Assembly Dept',
        employmentStatus: 'ACTIVE',
        loginName: 'alex.one',
        accountStatus: 'ENABLED'
      },
      {
        id: '5',
        userCode: 'EMP005',
        userName: 'Alex Chen',
        departmentId: '20',
        departmentName: 'Quality Dept',
        employmentStatus: 'ACTIVE',
        loginName: 'alex.two',
        accountStatus: 'ENABLED'
      }
    ])
    vi.mocked(fetchDeviceAccountsByDevice).mockResolvedValue([
      {
        id: 'DA-100-4',
        deviceNodeId: 100,
        deviceName: 'Device A',
        userId: '4',
        userName: 'Alex Chen',
        accountName: 'device_a_alex_one',
        accountStatus: 'ACTIVE',
        sourceType: 'MANUAL',
        remark: '',
        roles: []
      },
      {
        id: 'DA-100-5',
        deviceNodeId: 100,
        deviceName: 'Device A',
        userId: '5',
        userName: 'Alex Chen',
        accountName: 'device_a_alex_two',
        accountStatus: 'ACTIVE',
        sourceType: 'MANUAL',
        remark: '',
        roles: []
      }
    ])

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('select[name="targetUserId"]').setValue('5')
    await nextTick()

    expect(getOptionValues(wrapper, 'select[name="targetAccountName"]')).toEqual(['device_a_alex_two'])
  })

  it('blocks submit when no matching device account exists for the selected user', async () => {
    vi.mocked(fetchDeviceAccountsByDevice).mockResolvedValue([
      {
        id: 'DA-100-4',
        deviceNodeId: 100,
        deviceName: 'Device A',
        userId: '4',
        userName: 'Wang Wu',
        accountName: 'device_a_wangwu',
        accountStatus: 'ACTIVE',
        sourceType: 'MANUAL',
        remark: '',
        roles: []
      }
    ])

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('select[name="targetUserId"]').setValue('5')
    await wrapper.get('textarea[name="reason"]').setValue('Need access for shift change')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect((wrapper.get('select[name="targetAccountName"]').element as HTMLSelectElement).value).toBe('')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    expect(createRequest).not.toHaveBeenCalled()
  })

  it('blocks submit when the selected device does not expose any role options', async () => {
    vi.mocked(fetchAssetTree).mockResolvedValue([
      {
        id: '100',
        name: 'Device A',
        type: 'DEVICE',
        children: []
      }
    ])

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('textarea[name="reason"]').setValue('Need access for shift change')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect((wrapper.get('select[name="roleNodeId"]').element as HTMLSelectElement).value).toBe('')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    expect(createRequest).not.toHaveBeenCalled()
  })

  it('ignores stale device-account responses when devices are switched quickly', async () => {
    const deviceARequest = createDeferred<MockDeviceAccountRow[]>()
    const deviceBRequest = createDeferred<MockDeviceAccountRow[]>()

    vi.mocked(fetchDeviceAccountsByDevice).mockImplementation((deviceNodeId: string) => {
      if (deviceNodeId === '200') {
        return deviceBRequest.promise
      }
      return deviceARequest.promise
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('select[name="targetDeviceNodeId"]').setValue('200')
    await nextTick()

    deviceBRequest.resolve([
      {
        id: 'DA-200-4',
        deviceNodeId: 200,
        deviceName: 'Device B',
        userId: '4',
        userName: 'Wang Wu',
        accountName: 'device_b_wangwu',
        accountStatus: 'ACTIVE',
        sourceType: 'MANUAL',
        remark: '',
        roles: []
      }
    ])
    await flushPromises()

    expect((wrapper.get('select[name="targetAccountName"]').element as HTMLSelectElement).value).toBe('device_b_wangwu')

    deviceARequest.resolve([
      {
        id: 'DA-100-4',
        deviceNodeId: 100,
        deviceName: 'Device A',
        userId: '4',
        userName: 'Wang Wu',
        accountName: 'device_a_wangwu',
        accountStatus: 'ACTIVE',
        sourceType: 'MANUAL',
        remark: '',
        roles: []
      }
    ])
    await flushPromises()

    expect((wrapper.get('select[name="targetDeviceNodeId"]').element as HTMLSelectElement).value).toBe('200')
    expect(getOptionValues(wrapper, 'select[name="targetAccountName"]')).toEqual(['device_b_wangwu'])
  })

  it('shows a load error when the request form bootstrap data cannot be loaded', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    vi.mocked(fetchUsers).mockRejectedValueOnce(new Error('users unavailable'))

    try {
      const wrapper = mountWithLocale()
      await flushPromises()

      expect(wrapper.text()).toContain('users unavailable')
      expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    } finally {
      consoleErrorSpy.mockRestore()
    }
  })

  it('blocks submit while accounts for the newly selected device are still loading', async () => {
    const deviceBRequest = createDeferred<MockDeviceAccountRow[]>()

    vi.mocked(fetchDeviceAccountsByDevice).mockImplementation((deviceNodeId: string) => {
      if (deviceNodeId === '200') {
        return deviceBRequest.promise
      }

      return Promise.resolve([
        {
          id: 'DA-100-4',
          deviceNodeId: 100,
          deviceName: 'Device A',
          userId: '4',
          userName: 'Wang Wu',
          accountName: 'device_a_wangwu',
          accountStatus: 'ACTIVE',
          sourceType: 'MANUAL',
          remark: '',
          roles: []
        }
      ])
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('textarea[name="reason"]').setValue('Need access for shift change')
    await wrapper.get('select[name="targetDeviceNodeId"]').setValue('200')
    await nextTick()

    expect((wrapper.get('select[name="targetAccountName"]').element as HTMLSelectElement).value).toBe('')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createRequest).not.toHaveBeenCalled()

    deviceBRequest.resolve([
      {
        id: 'DA-200-4',
        deviceNodeId: 200,
        deviceName: 'Device B',
        userId: '4',
        userName: 'Wang Wu',
        accountName: 'device_b_wangwu',
        accountStatus: 'ACTIVE',
        sourceType: 'MANUAL',
        remark: '',
        roles: []
      }
    ])
    await flushPromises()
  })

  it('clears stale accounts and blocks submit when the latest device-account request fails', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)

    vi.mocked(fetchDeviceAccountsByDevice).mockImplementation((deviceNodeId: string) => {
      if (deviceNodeId === '200') {
        return Promise.reject(new Error('device fetch failed'))
      }

      return Promise.resolve([
        {
          id: 'DA-100-4',
          deviceNodeId: 100,
          deviceName: 'Device A',
          userId: '4',
          userName: 'Wang Wu',
          accountName: 'device_a_wangwu',
          accountStatus: 'ACTIVE',
          sourceType: 'MANUAL',
          remark: '',
          roles: []
        }
      ])
    })

    try {
      const wrapper = mountWithLocale()
      await flushPromises()

      await wrapper.get('textarea[name="reason"]').setValue('Need access for shift change')
      await wrapper.get('select[name="targetDeviceNodeId"]').setValue('200')
      await flushPromises()

      expect((wrapper.get('select[name="targetAccountName"]').element as HTMLSelectElement).value).toBe('')
      expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

      await wrapper.get('form').trigger('submit')
      await flushPromises()

      expect(createRequest).not.toHaveBeenCalled()
    } finally {
      consoleErrorSpy.mockRestore()
    }
  })
})

