import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { fetchAuditLogs } from '../api/audit'
import { usePreferencesStore, type Locale } from '../stores/preferences'
import AuditLogView from '../views/audit/AuditLogView.vue'

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

  return mount(AuditLogView, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('AuditLogView', () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset()
  })

  it('maps backend audit rows from actionType/objectType to frontend action display', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        code: 0,
        message: 'ok',
        data: {
          total: 1,
          list: [
            {
              actionType: 'GRANT_PERMISSION',
              operatorName: 'admin',
              objectType: 'DEVICE_ACCOUNT',
              createdAt: '2026-04-23 09:00:00'
            }
          ]
        }
      }
    })

    const result = await fetchAuditLogs()

    expect(axios.get).toHaveBeenCalledWith('/api/audit/logs')
    expect(result.total).toBe(1)
    expect(result.list).toEqual([
      {
        id: 'AUDIT-1',
        operatorName: 'admin',
        action: 'GRANT_PERMISSION (DEVICE_ACCOUNT)',
        createdAt: '2026-04-23 09:00:00'
      }
    ])
  })

  it('surfaces audit endpoint failures instead of returning placeholder logs', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('503'))

    await expect(fetchAuditLogs()).rejects.toThrow('503')
  })

  it('throws the backend business message when the audit envelope reports failure', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        code: 400,
        message: 'audit query failed',
        data: null
      }
    })

    await expect(fetchAuditLogs()).rejects.toThrow('audit query failed')
  })

  it('renders cockpit shell filters and dense audit headings', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: {
          total: 1,
          list: [
            {
              id: 'AUDIT-100',
              actionType: 'REVOKE_PERMISSION',
              operatorName: 'ops_admin',
              objectType: 'DEVICE_ACCOUNT',
              createdAt: '2026-04-23 10:00:00'
            }
          ]
        }
      }
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    expect(wrapper.text()).toContain('审计日志工作台')
    expect(wrapper.text()).toContain('时间范围')
    expect(wrapper.text()).toContain('目标对象')
    expect(wrapper.text()).toContain('设备筛选')
    expect(wrapper.text()).toContain('操作筛选')
    expect(wrapper.text()).toContain('ops_admin')
    expect(wrapper.text()).toContain('REVOKE_PERMISSION')
  })

  it('treats date filters as real dates for ISO audit timestamps', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        data: {
          total: 1,
          list: [
            {
              id: 'AUDIT-ISO-1',
              actionType: 'GRANT_PERMISSION',
              operatorName: 'iso_admin',
              objectType: 'DEVICE_ACCOUNT',
              createdAt: '2026-04-23T10:00:00Z'
            }
          ]
        }
      }
    })

    const wrapper = mountWithLocale()
    await flushPromises()

    await wrapper.get('#audit-from').setValue('2026-04-23')
    await wrapper.get('#audit-to').setValue('2026-04-23')

    expect(wrapper.text()).toContain('iso_admin')
    expect(wrapper.text()).toContain('GRANT_PERMISSION')
  })

  it('shows an error state when audit logs fail to load', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('audit unavailable'))

    const wrapper = mountWithLocale()
    await flushPromises()

    expect(wrapper.text()).toContain('审计日志暂不可用')
    expect(wrapper.text()).toContain('audit unavailable')
    expect(wrapper.text()).toContain('当前筛选条件下暂无审计记录。')
  })
})
