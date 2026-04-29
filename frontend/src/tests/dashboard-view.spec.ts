import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DashboardView from '../views/DashboardView.vue'

describe('DashboardView target clone', () => {
  it('renders the researched welcome copy and todo buckets', () => {
    const wrapper = mount(DashboardView)

    expect(wrapper.text()).toContain('你好, AMSAdmin')
    expect(wrapper.text()).toContain('欢迎使用权限通')
    expect(wrapper.text()).toContain('系统公告')
    expect(wrapper.text()).toContain('待办事项')
    expect(wrapper.text()).toContain('待审批')
    expect(wrapper.text()).toContain('待操作')
    expect(wrapper.text()).toContain('待审核')
    expect(wrapper.text()).toContain('待申诉')
  })
})
