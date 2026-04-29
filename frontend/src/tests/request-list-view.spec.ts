import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchRequests } from '../api/requests'
import { usePreferencesStore, type Locale } from '../stores/preferences'
import RequestListView from '../views/requests/RequestListView.vue'

vi.mock('../api/requests', () => ({
  fetchRequests: vi.fn()
}))

function mountWithLocale(locale: Locale = 'zh-CN') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)

  return mount(RequestListView, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('RequestListView cockpit shell', () => {
  beforeEach(() => {
    vi.mocked(fetchRequests).mockResolvedValue([
      {
        id: '1',
        requestNo: 'REQ1',
        requestType: 'ROLE_ADD',
        targetAccountName: 'device_a_wangwu',
        status: 'WAIT_QA',
        createdAt: '2026-04-24 09:00:00'
      },
      {
        id: '2',
        requestNo: 'REQ2',
        requestType: 'PASSWORD_RESET',
        targetAccountName: 'device_b_lisi',
        status: 'WAIT_QM',
        createdAt: '2026-04-23 11:30:00'
      }
    ])
  })

  it('renders cockpit header, expanded filters, metrics and quick detail entry', async () => {
    const wrapper = mountWithLocale()
    await flushPromises()

    expect(wrapper.text()).toContain('申请管理工作台')
    expect(wrapper.text()).toContain('筛选条件')
    expect(wrapper.text()).toContain('申请类型')
    expect(wrapper.text()).toContain('创建日期')
    expect(wrapper.text()).toContain('待处理申请')
    expect(wrapper.text()).toContain('REQ1')
    expect(wrapper.text()).toContain('查看详情')

    await wrapper.get('[data-testid="view-request-1"]').trigger('click')

    expect(wrapper.text()).toContain('申请详情')
    expect(wrapper.text()).toContain('device_a_wangwu')
  })
})
