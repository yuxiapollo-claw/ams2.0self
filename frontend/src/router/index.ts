import { createRouter, createWebHashHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import AuditLogView from '../views/audit/AuditLogView.vue'
import DepartmentListView from '../views/departments/DepartmentListView.vue'
import DevicePermissionView from '../views/queries/DevicePermissionView.vue'
import PermissionApplyView from '../views/requests/PermissionApplyView.vue'
import PermissionRemoveView from '../views/requests/PermissionRemoveView.vue'
import PermissionResetView from '../views/requests/PermissionResetView.vue'
import SystemPermissionAdminView from '../views/system-admin/SystemPermissionAdminView.vue'
import UserListView from '../views/users/UserListView.vue'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../stores/auth'

interface StoredAuthUser {
  systemAdmin?: boolean
}

function readStoredToken() {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.sessionStorage.getItem(AUTH_TOKEN_KEY) ?? ''
}

function readStoredUser() {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.sessionStorage.getItem(AUTH_USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as StoredAuthUser
  } catch {
    return null
  }
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        title: 'Sign In'
      }
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          redirect: '/dashboard'
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardView,
          meta: {
            title: 'Overview'
          }
        },
        {
          path: 'users',
          name: 'users',
          component: UserListView,
          meta: {
            title: 'User Management'
          }
        },
        {
          path: 'departments',
          name: 'departments',
          component: DepartmentListView,
          meta: {
            title: 'Department Management',
            requiresSystemAdmin: true
          }
        },
        {
          path: 'systemAdmin/sysAccessAdmin',
          name: 'system-permissions',
          component: SystemPermissionAdminView,
          meta: {
            title: 'System & Permission Management',
            requiresSystemAdmin: true
          }
        },
        {
          path: 'queries/device-permissions',
          name: 'device-permissions',
          component: DevicePermissionView,
          meta: {
            title: 'Device Permission Query'
          }
        },
        {
          path: 'audit/logs',
          name: 'audit-logs',
          component: AuditLogView,
          meta: {
            title: 'Audit Logs'
          }
        },
        {
          path: 'access/myRequest',
          name: 'permission-apply',
          component: PermissionApplyView,
          meta: {
            title: 'Permission Apply'
          }
        },
        {
          path: 'access/myRemove',
          name: 'permission-remove',
          component: PermissionRemoveView,
          meta: {
            title: 'Permission Remove Request'
          }
        },
        {
          path: 'access/myChangepd',
          name: 'permission-reset',
          component: PermissionResetView,
          meta: {
            title: 'Password Reset Request'
          }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path === '/login') {
    return true
  }

  if (!readStoredToken()) {
    return '/login'
  }

  if (to.meta.requiresSystemAdmin) {
    const user = readStoredUser()
    if (!user?.systemAdmin) {
      return '/dashboard'
    }
  }

  return true
})

export default router
