import axios from 'axios'

export interface DevicePermissionRole {
  roleName: string
  deviceAccounts: string[]
}

export interface DevicePermissionResult {
  deviceNodeId: string
  roles: DevicePermissionRole[]
}

interface DevicePermissionRoleRaw {
  roleName?: string
  role?: string
  deviceAccounts?: string[]
  accounts?: Array<string | { userName?: string; accountName?: string }>
  loginNames?: string[]
}

interface WrappedResponse<T> {
  code?: number
  message?: string
  data?: T
}

function readEnvelopeData<T>(value: WrappedResponse<T>, fallbackMessage: string) {
  if (typeof value.code === 'number' && value.code !== 0) {
    throw new Error(value.message?.trim() || fallbackMessage)
  }

  if (value.data == null && value.message?.trim()) {
    throw new Error(value.message.trim())
  }

  return value.data
}

function normalizeDeviceAccounts(raw: DevicePermissionRoleRaw) {
  if (Array.isArray(raw.deviceAccounts)) {
    return raw.deviceAccounts
  }
  if (Array.isArray(raw.accounts)) {
    return raw.accounts
      .map((account) => {
        if (typeof account === 'string') {
          return account
        }
        if (!account || typeof account !== 'object') {
          return ''
        }

        const accountName = account.accountName?.trim() ?? ''
        const userName = account.userName?.trim() ?? ''
        if (accountName && userName) {
          return `${accountName} (${userName})`
        }
        if (accountName) {
          return accountName
        }
        return userName
      })
      .filter((value) => value.length > 0)
  }
  if (Array.isArray(raw.loginNames)) {
    return raw.loginNames
  }
  return []
}

function normalizeRole(raw: DevicePermissionRoleRaw): DevicePermissionRole {
  return {
    roleName: raw.roleName ?? raw.role ?? 'Unnamed Role',
    deviceAccounts: normalizeDeviceAccounts(raw)
  }
}

export async function fetchDevicePermissions(
  deviceNodeId: string
): Promise<DevicePermissionResult> {
  const { data } = await axios.get<WrappedResponse<{ deviceNodeId?: string; roles?: DevicePermissionRoleRaw[] }>>(
    '/api/queries/device-permissions',
    {
      params: { deviceNodeId }
    }
  )

  const payload = readEnvelopeData(data, 'Permission query failed')
  const roles = Array.isArray(payload?.roles) ? payload.roles.map(normalizeRole) : []
  return {
    deviceNodeId: payload?.deviceNodeId ?? deviceNodeId,
    roles
  }
}
