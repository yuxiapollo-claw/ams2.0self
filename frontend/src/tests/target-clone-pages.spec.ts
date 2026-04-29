import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import router from '../router'
import TargetPageView from '../views/TargetPageView.vue'

describe('target clone routing', () => {
  it('registers the researched target-system routes', () => {
    const paths = router.getRoutes().map((route) => route.path)

    expect(paths).toContain('/dashboard')
    expect(paths).toContain('/access/myRequest')
    expect(paths).toContain('/task/accessApproval')
    expect(paths).toContain('/report/accountHistory')
    expect(paths).toContain('/systemAdmin/mailTemplateCfg')
    expect(paths).toContain('/hrAdmin/hrManagement')
    expect(router.resolve('/dashboard').href).toContain('#/dashboard')
  })

  it('renders configured target page actions and table headers', async () => {
    const localRouter = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/access/accountManagement',
          component: TargetPageView,
          meta: {
            sectionKey: 'access',
            pagePath: '/access/accountManagement'
          }
        }
      ]
    })

    await localRouter.push('/access/accountManagement')
    await localRouter.isReady()

    const wrapper = mount(TargetPageView, {
      global: {
        plugins: [localRouter]
      }
    })

    expect(wrapper.text()).toContain('账号列表')
    expect(wrapper.text()).toContain('申请权限')
    expect(wrapper.text()).toContain('重置密码')
    expect(wrapper.text()).toContain('删除权限')
    expect(wrapper.text()).toContain('账号名称')
  })
})
