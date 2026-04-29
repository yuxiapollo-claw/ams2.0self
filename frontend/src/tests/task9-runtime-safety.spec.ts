import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { fetchAssetTree } from '../api/assets'
import { fetchRequests } from '../api/requests'
import { usePreferencesStore } from '../stores/preferences'
import { fetchUsers } from '../api/users'
import RequestListView from '../views/requests/RequestListView.vue'

vi.mock('axios', () => ({
  default: {
    get: vi.fn()
  }
}))

function mountWithPreferences() {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()

  return mount(RequestListView, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('Task 9 runtime integration safety', () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset()
  })

  it('normalizes user list from backend response data.list', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: {
          list: [
            {
              id: 'U-1',
              userCode: 'EMP001',
              loginName: 'zhangsan',
              userName: 'Zhang San',
              departmentId: 10,
              departmentName: 'Assembly Dept',
              employmentStatus: 'ACTIVE',
              accountStatus: 'ENABLED'
            }
          ]
        }
      }
    })

    const result = await fetchUsers()

    expect(axios.get).toHaveBeenCalledWith('/api/users')
    expect(result).toEqual([
      {
        id: 'U-1',
        userCode: 'EMP001',
        loginName: 'zhangsan',
        userName: 'Zhang San',
        departmentId: '10',
        departmentName: 'Assembly Dept',
        employmentStatus: 'ACTIVE',
        accountStatus: 'ENABLED'
      }
    ])
  })

  it('normalizes asset tree fields from backend response data', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: [
          {
            id: 'A-1',
            nodeName: 'Device Catalog',
            nodeType: 'CATEGORY',
            children: [{ id: 'A-2', nodeName: 'Device A', nodeType: 'DEVICE' }]
          }
        ]
      }
    })

    const result = await fetchAssetTree()

    expect(axios.get).toHaveBeenCalledWith('/api/assets/tree')
    expect(result).toEqual([
      {
        id: 'A-1',
        name: 'Device Catalog',
        type: 'CATEGORY',
        children: [{ id: 'A-2', name: 'Device A', type: 'DEVICE', children: [] }]
      }
    ])
  })

  it('normalizes persisted request list from backend response', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: {
          list: [
            {
              id: 1,
              requestNo: 'REQ1',
              requestType: 'ROLE_ADD',
              targetAccountName: 'device_a_wangwu',
              currentStatus: 'WAIT_DEPT_MANAGER',
              createdAt: '2026-04-23T10:00:00Z'
            }
          ]
        }
      }
    })

    const result = await fetchRequests()

    expect(axios.get).toHaveBeenCalledWith('/api/requests')
    expect(result).toEqual([
      {
        id: '1',
        requestNo: 'REQ1',
        requestType: 'ROLE_ADD',
        targetAccountName: 'device_a_wangwu',
        status: 'WAIT_DEPT_MANAGER',
        createdAt: '2026-04-23T10:00:00Z'
      }
    ])
  })

  it('loads request list view from api on mount', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: {
          list: [
            {
              id: 1,
              requestNo: 'REQ1',
              requestType: 'ROLE_ADD',
              targetAccountName: 'device_a_wangwu',
              currentStatus: 'WAIT_DEPT_MANAGER',
              createdAt: '2026-04-23T10:00:00Z'
            }
          ]
        }
      }
    })

    const wrapper = mountWithPreferences()
    await flushPromises()

    expect(wrapper.text()).toContain('REQ1')
    expect(wrapper.text()).toContain('device_a_wangwu')
  })
})
