import axios from 'axios'
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const AUTH_TOKEN_KEY = 'ams_auth_token'
export const AUTH_USER_KEY = 'ams_auth_user'
export const AUTH_ROLES_KEY = 'ams_auth_roles'

export interface AuthUser {
  id: string
  loginName: string
  userName: string
  systemAdmin: boolean
}

interface LoginResponseEnvelope {
  data?: {
    token?: string
    roles?: string[]
    user?: {
      id?: string | number
      loginName?: string
      userName?: string
      systemAdmin?: boolean
    }
  }
}

function readToken() {
  if (typeof window === 'undefined') {
    return null
  }
  return window.sessionStorage.getItem(AUTH_TOKEN_KEY)
}

function readUser() {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.sessionStorage.getItem(AUTH_USER_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as AuthUser
    if (!parsed || typeof parsed !== 'object') {
      return null
    }
    if (typeof parsed.id !== 'string' || typeof parsed.loginName !== 'string' || typeof parsed.userName !== 'string') {
      return null
    }
    return {
      id: parsed.id,
      loginName: parsed.loginName,
      userName: parsed.userName,
      systemAdmin: Boolean(parsed.systemAdmin)
    }
  } catch {
    return null
  }
}

function readRoles() {
  if (typeof window === 'undefined') {
    return []
  }

  const raw = window.sessionStorage.getItem(AUTH_ROLES_KEY)
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []
  } catch {
    return []
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(readToken())
  const user = ref<AuthUser | null>(readUser())
  const roles = ref<string[]>(readRoles())
  const isAuthenticated = computed(() => Boolean(token.value))
  const isSystemAdmin = computed(() => Boolean(user.value?.systemAdmin))

  async function login(loginName: string, password: string) {
    const response = await axios.post<LoginResponseEnvelope>('/api/auth/login', { loginName, password })
    const payload = response.data?.data
    token.value = payload?.token ?? 'session-authenticated'
    roles.value = Array.isArray(payload?.roles) ? payload.roles : []
    user.value = normalizeUser(payload?.user)

    if (typeof window !== 'undefined') {
      window.sessionStorage.setItem(AUTH_TOKEN_KEY, token.value)
      if (user.value) {
        window.sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(user.value))
      } else {
        window.sessionStorage.removeItem(AUTH_USER_KEY)
      }
      window.sessionStorage.setItem(AUTH_ROLES_KEY, JSON.stringify(roles.value))
    }
  }

  function logout() {
    token.value = null
    user.value = null
    roles.value = []
    if (typeof window !== 'undefined') {
      window.sessionStorage.removeItem(AUTH_TOKEN_KEY)
      window.sessionStorage.removeItem(AUTH_USER_KEY)
      window.sessionStorage.removeItem(AUTH_ROLES_KEY)
    }
  }

  function normalizeUser(raw: LoginResponseEnvelope['data']['user']): AuthUser | null {
    if (!raw) {
      return null
    }

    const id = typeof raw.id === 'string' || typeof raw.id === 'number' ? String(raw.id) : ''
    const loginName = typeof raw.loginName === 'string' ? raw.loginName : ''
    const userName = typeof raw.userName === 'string' ? raw.userName : ''
    if (!id || !loginName || !userName) {
      return null
    }

    return {
      id,
      loginName,
      userName,
      systemAdmin: Boolean(raw.systemAdmin)
    }
  }

  return {
    token,
    user,
    roles,
    isAuthenticated,
    isSystemAdmin,
    login,
    logout
  }
})
