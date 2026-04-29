import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginView from '../views/LoginView.vue'

const pushMock = vi.fn()
const loginMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock
  })
}))

vi.mock('../stores/auth', () => ({
  useAuthStore: () => ({
    login: loginMock
  })
}))

function mountLogin() {
  return mount(LoginView, {
    global: {
      plugins: [createPinia()]
    }
  })
}

describe('LoginView target clone', () => {
  beforeEach(() => {
    pushMock.mockReset()
    loginMock.mockReset()
    loginMock.mockResolvedValue(undefined)
  })

  it('renders the target-system title and minimal login form', () => {
    const wrapper = mountLogin()

    expect(wrapper.text()).toContain('权限管理系统')
    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.text()).toContain('登录')
  })

  it('submits credentials and routes to the dashboard', async () => {
    const wrapper = mountLogin()

    await wrapper.get('input[name="loginName"]').setValue('AMSAdmin')
    await wrapper.get('input[name="password"]').setValue('Admin.23')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(loginMock).toHaveBeenCalledTimes(1)
    expect(loginMock).toHaveBeenCalledWith('AMSAdmin', 'Admin.23')
    expect(pushMock).toHaveBeenCalledWith('/')
  })

  it('shows an inline error when authentication fails', async () => {
    loginMock.mockRejectedValue({ response: { status: 401 } })

    const wrapper = mountLogin()

    await wrapper.get('input[name="loginName"]').setValue('AMSAdmin')
    await wrapper.get('input[name="password"]').setValue('bad')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(pushMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('用户名或密码不正确')
  })
})
