import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { fetchDevicePermissions } from '../api/queries'
import { usePreferencesStore, type Locale } from '../stores/preferences'
import DevicePermissionView from '../views/queries/DevicePermissionView.vue'

vi.mock('axios', () => ({
  default: {
    get: vi.fn()
  }
}))

function mountWithLocale(locale: Locale = 'zh-CN') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)

  return mount(DevicePermissionView, {
    global: {
      plugins: [pinia]
    }
  })
}

function createDeferred<T>() {
  let resolve: (value: T) => void = () => undefined
  const promise = new Promise<T>((nextResolve) => {
    resolve = nextResolve
  })
  return { promise, resolve }
}

describe('DevicePermissionView', () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset()
  })

  it('normalizes backend role accounts objects into displayable device accounts', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        code: 0,
        message: 'ok',
        data: {
          deviceNodeId: 'NODE-1',
          roles: [
            {
              roleName: 'Operator Role',
              accounts: [
                { userName: 'ops_user', accountName: 'ops_root' },
                { userName: 'readonly_user', accountName: 'ops_ro' }
              ]
            }
          ]
        }
      }
    })

    const result = await fetchDevicePermissions('NODE-1')

    expect(axios.get).toHaveBeenCalledWith('/api/queries/device-permissions', {
      params: { deviceNodeId: 'NODE-1' }
    })
    expect(result).toEqual({
      deviceNodeId: 'NODE-1',
      roles: [
        {
          roleName: 'Operator Role',
          deviceAccounts: ['ops_root (ops_user)', 'ops_ro (readonly_user)']
        }
      ]
    })
  })

  it('surfaces endpoint failures instead of fabricating placeholder permissions', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('404'))

    await expect(fetchDevicePermissions('NODE-404')).rejects.toThrow('404')
  })

  it('throws the backend business message when the permission envelope reports failure', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        code: 400,
        message: 'permission query failed',
        data: null
      }
    })

    await expect(fetchDevicePermissions('NODE-500')).rejects.toThrow('permission query failed')
  })

  it('renders cockpit shell headings, filters and mapped account values', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: {
          deviceNodeId: 'NODE-1',
          roles: [
            {
              roleName: 'Read Only Role',
              accounts: [{ userName: 'demo_user', accountName: 'readonly_demo' }]
            }
          ]
        }
      }
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    expect(wrapper.text()).toContain('设备权限查询工作台')
    expect(wrapper.text()).toContain('设备筛选')
    expect(wrapper.text()).toContain('角色覆盖')
    expect(wrapper.text()).toContain('readonly_demo (demo_user)')
  })

  it('keeps the latest query result when earlier device lookups finish later', async () => {
    const device100Request = createDeferred<{
      data: {
        data: {
          deviceNodeId: string
          roles: Array<{ roleName: string; accounts: Array<{ userName: string; accountName: string }> }>
        }
      }
    }>()
    const device200Request = createDeferred<{
      data: {
        data: {
          deviceNodeId: string
          roles: Array<{ roleName: string; accounts: Array<{ userName: string; accountName: string }> }>
        }
      }
    }>()

    vi.mocked(axios.get).mockImplementation((_url, config) => {
      const deviceNodeId = config?.params?.deviceNodeId
      if (deviceNodeId === '200') {
        return device200Request.promise
      }
      return device100Request.promise
    })

    const wrapper = mountWithLocale()
    await wrapper.get('#device-node-id').setValue('200')
    await wrapper.get('form').trigger('submit')

    device200Request.resolve({
      data: {
        data: {
          deviceNodeId: '200',
          roles: [
            {
              roleName: 'Device B Role',
              accounts: [{ userName: 'target_user', accountName: 'device_b_ops' }]
            }
          ]
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Device B Role')
    expect(wrapper.text()).toContain('device_b_ops (target_user)')

    device100Request.resolve({
      data: {
        data: {
          deviceNodeId: '100',
          roles: [
            {
              roleName: 'Device A Role',
              accounts: [{ userName: 'legacy_user', accountName: 'device_a_ops' }]
            }
          ]
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Device B Role')
    expect(wrapper.text()).not.toContain('Device A Role')
  })

  it('shows an error state when the device query fails', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('query failed'))

    const wrapper = mountWithLocale()
    await flushPromises()

    expect(wrapper.text()).toContain('查询失败')
    expect(wrapper.text()).toContain('query failed')
    expect(wrapper.text()).toContain('当前设备暂无角色覆盖信息。')
  })
})
