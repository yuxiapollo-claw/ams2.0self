import { createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { targetCloneEntries } from '../config/target-clone'
import AppLayout from '../layouts/AppLayout.vue'
import { usePreferencesStore } from '../stores/preferences'

describe('AppLayout target shell', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
  })

  function createShellRouter() {
    const DummyView = { template: '<div>Ok</div>' }

    return createRouter({
      history: createMemoryHistory(),
      routes: targetCloneEntries.map((entry) => ({
        path: entry.path,
        component: DummyView,
        meta: {
          sectionKey: entry.sectionKey,
          pagePath: entry.path
        }
      }))
    })
  }

  it('renders the target-system top navigation and account meta', async () => {
    const pinia = createPinia()
    const router = createShellRouter()
    usePreferencesStore(pinia).initializePreferences()

    router.push('/dashboard')
    await router.isReady()

    const wrapper = mount(AppLayout, {
      global: {
        plugins: [pinia, router]
      }
    })

    expect(wrapper.text()).toContain('控制台')
    expect(wrapper.text()).toContain('权限管理')
    expect(wrapper.text()).toContain('任务管理')
    expect(wrapper.text()).toContain('报告')
    expect(wrapper.text()).toContain('AMSAdmin')
    expect(wrapper.text()).toContain('中文')
    expect(wrapper.text()).toContain('English')
    expect(wrapper.text()).toContain('版本: 1.0.0')
  })

  it('shows contextual sidebar entries for the active primary section', async () => {
    const pinia = createPinia()
    const router = createShellRouter()
    usePreferencesStore(pinia).initializePreferences()

    router.push('/access/myRequest')
    await router.isReady()

    const wrapper = mount(AppLayout, {
      global: {
        plugins: [pinia, router]
      }
    })

    expect(wrapper.text()).toContain('申请权限')
    expect(wrapper.text()).toContain('删除权限')
    expect(wrapper.text()).toContain('账号列表')
    expect(wrapper.text()).not.toContain('账号历史')
  })
})
