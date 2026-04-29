import axios from 'axios'
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const AUTH_TOKEN_KEY = 'ams_auth_token'

function readToken() {
  if (typeof window === 'undefined') {
    return null
  }
  return window.sessionStorage.getItem(AUTH_TOKEN_KEY)
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(readToken())
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(loginName: string, password: string) {
    const response = await axios.post('/api/auth/login', { loginName, password })
    token.value = response.data?.data?.token ?? 'session-authenticated'
    if (typeof window !== 'undefined') {
      window.sessionStorage.setItem(AUTH_TOKEN_KEY, token.value)
    }
  }

  function logout() {
    token.value = null
    if (typeof window !== 'undefined') {
      window.sessionStorage.removeItem(AUTH_TOKEN_KEY)
    }
  }

  return {
    token,
    isAuthenticated,
    login,
    logout
  }
})
