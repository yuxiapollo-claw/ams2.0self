import { createRouter, createWebHashHistory } from 'vue-router'
import { targetCloneEntries } from '../config/target-clone'
import AccessAuditView from '../views/access/AccessAuditView.vue'
import AccessDirectoryView from '../views/access/AccessDirectoryView.vue'
import AccessWorkbenchView from '../views/access/AccessWorkbenchView.vue'
import HrWorkbenchView from '../views/hr-admin/HrWorkbenchView.vue'
import AppLayout from '../layouts/AppLayout.vue'
import ApplicationConfigListView from '../views/system-admin/ApplicationConfigListView.vue'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import ManagerWorkbenchView from '../views/manager/ManagerWorkbenchView.vue'
import MailTemplateListView from '../views/system-admin/MailTemplateListView.vue'
import ReportCenterView from '../views/report/ReportCenterView.vue'
import SystemAdminWorkbenchView from '../views/system-admin/SystemAdminWorkbenchView.vue'
import TaskQueueView from '../views/task/TaskQueueView.vue'
import TargetPageView from '../views/TargetPageView.vue'

function resolveTargetCloneComponent(path: string) {
  if (['/access/myRequest', '/access/myRemove', '/access/myChangepd'].includes(path)) {
    return AccessWorkbenchView
  }

  if (['/access/myView', '/access/accountManagement'].includes(path)) {
    return AccessDirectoryView
  }

  if (['/access/reviewCheck', '/access/bedelegation'].includes(path)) {
    return AccessAuditView
  }

  if (
    ['/task/accessApproval', '/task/accessOperation', '/task/reviewApproval', '/task/revocation'].includes(path)
  ) {
    return TaskQueueView
  }

  if (
    [
      '/report/accountHistory',
      '/report/reviewReport',
      '/report/empAccess',
      '/report/systemAccess',
      '/report/sysconfig',
      '/report/variationReport'
    ].includes(path)
  ) {
    return ReportCenterView
  }

  if (['/sysAccessManager/empAccessManagement', '/sysAccessManager/reviewCheck'].includes(path)) {
    return ManagerWorkbenchView
  }

  if (path === '/systemAdmin/applicationConfig') {
    return ApplicationConfigListView
  }

  if (path === '/systemAdmin/mailTemplateCfg') {
    return MailTemplateListView
  }

  if (
    [
      '/systemAdmin/userAccessManagement',
      '/systemAdmin/empAccessManagement',
      '/systemAdmin/copyProfile',
      '/systemAdmin/sysAccessAdmin',
      '/systemAdmin/reviewManagement',
      '/systemAdmin/admindelegation',
      '/systemAdmin/adminworkflow'
    ].includes(path)
  ) {
    return SystemAdminWorkbenchView
  }

  if (['/hrAdmin/hrManagement', '/hrAdmin/enterprisemaintain'].includes(path)) {
    return HrWorkbenchView
  }

  if (path === '/dashboard') {
    return DashboardView
  }

  return TargetPageView
}

const contentRoutes = targetCloneEntries.map((entry) => ({
  path: entry.path.replace(/^\//, ''),
  name: entry.path,
  component: resolveTargetCloneComponent(entry.path),
  meta: {
    sectionKey: entry.sectionKey,
    pagePath: entry.path,
    title: entry.title,
    sectionLabel: entry.sectionLabel
  }
}))

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          redirect: {
            path: '/dashboard'
          }
        },
        ...contentRoutes
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path === '/login') {
    return true
  }

  const token =
    typeof window === 'undefined' ? null : window.sessionStorage.getItem('ams_auth_token')

  if (!token) {
    return '/login'
  }

  return true
})

export default router
