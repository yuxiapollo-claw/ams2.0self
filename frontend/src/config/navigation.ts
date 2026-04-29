import type { MessageKey } from '../i18n/messages'

export interface NavigationItem {
  labelKey: MessageKey
  to: string
  descriptionKey?: MessageKey
}

export const primaryNavigation: NavigationItem[] = [
  { labelKey: 'nav.dashboard', to: '/' },
  { labelKey: 'nav.users', to: '/users' },
  { labelKey: 'nav.departments', to: '/departments' },
  { labelKey: 'nav.deviceAccounts', to: '/device-accounts' },
  { labelKey: 'nav.requests', to: '/requests' },
  { labelKey: 'nav.devicePermissions', to: '/queries/device-permissions' },
  { labelKey: 'nav.audit', to: '/audit/logs' }
]

export const quickActions: NavigationItem[] = [
  {
    labelKey: 'nav.quick.newRequest',
    to: '/requests/new',
    descriptionKey: 'nav.quick.newRequestDescription'
  }
]
