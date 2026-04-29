import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RouterView } from 'vue-router'
import { submitExecution } from '../api/executions'
import { fetchRequests } from '../api/requests'
import { approveRequest } from '../api/tasks'
import router from '../router'
import { usePreferencesStore } from '../stores/preferences'

vi.mock('../api/requests', () => ({
  fetchRequests: vi.fn(),
  createRequest: vi.fn()
}))

vi.mock(
  '../api/tasks',
  () => ({
    approveRequest: vi.fn()
  }),
  { virtual: true }
)

vi.mock(
  '../api/executions',
  () => ({
    submitExecution: vi.fn()
  }),
  { virtual: true }
)

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

describe('task target pages', () => {
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
        status: 'WAIT_DEPT_MANAGER',
        createdAt: '2026-04-29 09:00:00'
      },
      {
        id: '1002',
        requestNo: 'REQ1002',
        requestType: 'ROLE_ADD',
        targetAccountName: 'device_b_lisi',
        status: 'WAIT_QI_EXECUTE',
        createdAt: '2026-04-29 11:00:00'
      },
      {
        id: '1003',
        requestNo: 'REQ1003',
        requestType: 'ROLE_ADD',
        targetAccountName: 'device_c_zhaoliu',
        status: 'COMPLETED',
        createdAt: '2026-04-28 08:00:00'
      }
    ])

    vi.mocked(approveRequest).mockResolvedValue({
      requestId: 1001,
      currentStatus: 'WAIT_QA',
      currentStatusLabel: '待QA审批'
    })

    vi.mocked(submitExecution).mockResolvedValue({
      requestId: 1002,
      currentStatus: 'COMPLETED'
    })
  })

  it('renders accessApproval with live queue rows and approve action', async () => {
    const wrapper = await renderAt('/task/accessApproval')
    await flushPromises()

    expect(wrapper.text()).toContain('审批任务')
    expect(wrapper.text()).toContain('REQ1001')
    expect(wrapper.text()).toContain('待处理队列')

    await wrapper.get('[data-testid="approve-request-1001"]').trigger('click')
    await flushPromises()

    expect(approveRequest).toHaveBeenCalledWith('1001')
  })

  it('renders accessOperation with executable rows and complete action', async () => {
    const wrapper = await renderAt('/task/accessOperation')
    await flushPromises()

    expect(wrapper.text()).toContain('操作任务')
    expect(wrapper.text()).toContain('待操作')
    expect(wrapper.text()).toContain('REQ1002')

    await wrapper.get('[data-testid="complete-request-1002"]').trigger('click')
    await flushPromises()

    expect(submitExecution).toHaveBeenCalledWith('1002')
  })
})
