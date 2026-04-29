import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createApplicationConfig,
  createMailTemplate,
  deleteApplicationConfig,
  deleteMailTemplate,
  fetchApplicationConfigs,
  fetchMailTemplates,
  updateApplicationConfig,
  updateMailTemplate,
  type ApplicationConfigItem,
  type MailTemplateItem
} from '../api/admin-config'
import { usePreferencesStore, type Locale } from '../stores/preferences'
import ApplicationConfigListView from '../views/system-admin/ApplicationConfigListView.vue'
import MailTemplateListView from '../views/system-admin/MailTemplateListView.vue'

vi.mock('../api/admin-config', () => ({
  fetchApplicationConfigs: vi.fn(),
  createApplicationConfig: vi.fn(),
  updateApplicationConfig: vi.fn(),
  deleteApplicationConfig: vi.fn(),
  fetchMailTemplates: vi.fn(),
  createMailTemplate: vi.fn(),
  updateMailTemplate: vi.fn(),
  deleteMailTemplate: vi.fn()
}))

const applicationConfigs: ApplicationConfigItem[] = [
  {
    id: '1',
    applicationName: 'DMS',
    applicationCode: 'APP-DMS',
    description: 'Document management system',
    status: 'ENABLED',
    updatedAt: '2026-04-29T10:00:00'
  },
  {
    id: '2',
    applicationName: 'QMS',
    applicationCode: 'APP-QMS',
    description: 'Quality management system',
    status: 'DISABLED',
    updatedAt: '2026-04-29T11:00:00'
  }
]

const mailTemplates: MailTemplateItem[] = [
  {
    id: '1',
    templateName: '申请创建通知',
    description: '新申请提交通知',
    subject: '【AMS】新申请通知',
    body: '申请单已提交，请尽快处理。',
    status: 'ENABLED',
    updatedAt: '2026-04-29T10:00:00'
  },
  {
    id: '2',
    templateName: '执行完成通知',
    description: '权限执行完成通知',
    subject: '【AMS】权限执行完成',
    body: '您的权限申请已执行完成。',
    status: 'DISABLED',
    updatedAt: '2026-04-29T11:00:00'
  }
]

function mountWithLocale(component: object, locale: Locale = 'zh-CN') {
  const pinia = createPinia()
  const preferences = usePreferencesStore(pinia)
  preferences.initializePreferences()
  preferences.setLocale(locale)

  return mount(component, {
    global: {
      plugins: [pinia]
    }
  })
}

describe('system admin config pages', () => {
  beforeEach(() => {
    vi.mocked(fetchApplicationConfigs).mockReset()
    vi.mocked(createApplicationConfig).mockReset()
    vi.mocked(updateApplicationConfig).mockReset()
    vi.mocked(deleteApplicationConfig).mockReset()
    vi.mocked(fetchMailTemplates).mockReset()
    vi.mocked(createMailTemplate).mockReset()
    vi.mocked(updateMailTemplate).mockReset()
    vi.mocked(deleteMailTemplate).mockReset()
    vi.restoreAllMocks()

    vi.mocked(fetchApplicationConfigs).mockResolvedValue(applicationConfigs)
    vi.mocked(createApplicationConfig).mockResolvedValue(applicationConfigs[0])
    vi.mocked(updateApplicationConfig).mockResolvedValue(applicationConfigs[0])
    vi.mocked(deleteApplicationConfig).mockResolvedValue()

    vi.mocked(fetchMailTemplates).mockResolvedValue(mailTemplates)
    vi.mocked(createMailTemplate).mockResolvedValue(mailTemplates[0])
    vi.mocked(updateMailTemplate).mockResolvedValue(mailTemplates[0])
    vi.mocked(deleteMailTemplate).mockResolvedValue()
  })

  it('renders application config rows and supports English copy', async () => {
    const wrapper = mountWithLocale(ApplicationConfigListView, 'en-US')
    await flushPromises()

    expect(fetchApplicationConfigs).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Application Configuration')
    expect(wrapper.text()).toContain('Create Application')
    expect(wrapper.text()).toContain('DMS')
    expect(wrapper.text()).toContain('APP-DMS')
  })

  it('creates and updates application configs with normalized payloads', async () => {
    const wrapper = mountWithLocale(ApplicationConfigListView)
    await flushPromises()

    await wrapper.get('button[data-testid="create-application-config"]').trigger('click')
    await wrapper.get('input[name="applicationName"]').setValue(' LIMS ')
    await wrapper.get('input[name="applicationCode"]').setValue(' APP-LIMS ')
    await wrapper.get('textarea[name="description"]').setValue(' Lab system ')
    await wrapper.get('select[name="status"]').setValue('ENABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createApplicationConfig).toHaveBeenCalledWith({
      applicationName: 'LIMS',
      applicationCode: 'APP-LIMS',
      description: 'Lab system',
      status: 'ENABLED'
    })

    await wrapper.get('button[data-testid="edit-1"]').trigger('click')
    await wrapper.get('input[name="applicationName"]').setValue(' DMS Core ')
    await wrapper.get('input[name="applicationCode"]').setValue(' APP-DMS-CORE ')
    await wrapper.get('textarea[name="description"]').setValue(' Updated description ')
    await wrapper.get('select[name="status"]').setValue('DISABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateApplicationConfig).toHaveBeenCalledWith('1', {
      applicationName: 'DMS Core',
      applicationCode: 'APP-DMS-CORE',
      description: 'Updated description',
      status: 'DISABLED'
    })
  })

  it('deletes application configs after confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountWithLocale(ApplicationConfigListView)
    await flushPromises()

    await wrapper.get('button[data-testid="delete-2"]').trigger('click')
    await flushPromises()

    expect(deleteApplicationConfig).toHaveBeenCalledWith('2')
  })

  it('renders mail templates and performs real create-update-delete actions', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountWithLocale(MailTemplateListView)
    await flushPromises()

    expect(fetchMailTemplates).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('邮件模板配置')
    expect(wrapper.text()).toContain('申请创建通知')

    await wrapper.get('button[data-testid="create-mail-template"]').trigger('click')
    await wrapper.get('input[name="templateName"]').setValue(' 执行结果通知 ')
    await wrapper.get('textarea[name="description"]').setValue(' 结果回执 ')
    await wrapper.get('input[name="subject"]').setValue(' 【AMS】执行结果 ')
    await wrapper.get('textarea[name="body"]').setValue(' 执行结果正文 ')
    await wrapper.get('select[name="status"]').setValue('ENABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createMailTemplate).toHaveBeenCalledWith({
      templateName: '执行结果通知',
      description: '结果回执',
      subject: '【AMS】执行结果',
      body: '执行结果正文',
      status: 'ENABLED'
    })

    await wrapper.get('button[data-testid="edit-1"]').trigger('click')
    await wrapper.get('input[name="templateName"]').setValue(' 申请创建通知标准版 ')
    await wrapper.get('textarea[name="description"]').setValue(' 新申请提交通知标准版 ')
    await wrapper.get('input[name="subject"]').setValue(' 【AMS】新申请通知标准版 ')
    await wrapper.get('textarea[name="body"]').setValue(' 新申请通知正文标准版 ')
    await wrapper.get('select[name="status"]').setValue('DISABLED')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateMailTemplate).toHaveBeenCalledWith('1', {
      templateName: '申请创建通知标准版',
      description: '新申请提交通知标准版',
      subject: '【AMS】新申请通知标准版',
      body: '新申请通知正文标准版',
      status: 'DISABLED'
    })

    await wrapper.get('button[data-testid="delete-2"]').trigger('click')
    await flushPromises()

    expect(deleteMailTemplate).toHaveBeenCalledWith('2')
  })
})
