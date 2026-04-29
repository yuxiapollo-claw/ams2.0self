import axios from 'axios'

export interface DashboardMetrics {
  userTotal: number
  departmentTotal: number
  deviceAccountTotal: number
  pendingRequestTotal: number
}

export interface DashboardAlert {
  alertKey: string
  title: string
  description: string
  count: number
}

export interface DashboardRecentRequest {
  id: string
  requestNo: string
  requestType: string
  currentStatus: string
  createdAt: string
}

export interface DashboardQuickAction {
  actionKey: string
  title: string
  description: string
}

export interface DashboardSummary {
  metrics: DashboardMetrics
  alerts: DashboardAlert[]
  recentRequests: DashboardRecentRequest[]
  quickActions: DashboardQuickAction[]
}

interface WrappedResponse<T> {
  code?: number
  message?: string
  data?: T
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export async function fetchDashboardSummary(): Promise<DashboardSummary> {
  const { data } = await axios.get<WrappedResponse<unknown>>('/api/dashboard/summary')
  const raw = (data as WrappedResponse<unknown>).data

  if (!isRecord(raw)) {
    throw new Error('Invalid dashboard summary payload: missing data')
  }

  const metrics = raw.metrics
  if (!isRecord(metrics)) {
    throw new Error('Invalid dashboard summary payload: missing metrics')
  }

  const { userTotal, departmentTotal, deviceAccountTotal, pendingRequestTotal } = metrics
  const numbers = [userTotal, departmentTotal, deviceAccountTotal, pendingRequestTotal]
  if (!numbers.every((value) => typeof value === 'number' && Number.isFinite(value))) {
    throw new Error('Invalid dashboard summary payload: invalid metrics')
  }

  if (!Array.isArray(raw.alerts) || !Array.isArray(raw.recentRequests) || !Array.isArray(raw.quickActions)) {
    throw new Error('Invalid dashboard summary payload: missing list fields')
  }

  // Light validation to keep UI honest. We don't try to coerce backend mistakes into "valid" values.
  for (const alert of raw.alerts) {
    if (
      !isRecord(alert) ||
      typeof alert.alertKey !== 'string' ||
      typeof alert.title !== 'string' ||
      typeof alert.description !== 'string' ||
      typeof alert.count !== 'number'
    ) {
      throw new Error('Invalid dashboard summary payload: invalid alerts')
    }
  }

  for (const action of raw.quickActions) {
    if (
      !isRecord(action) ||
      typeof action.actionKey !== 'string' ||
      typeof action.title !== 'string' ||
      typeof action.description !== 'string'
    ) {
      throw new Error('Invalid dashboard summary payload: invalid quickActions')
    }
  }

  for (const request of raw.recentRequests) {
    if (
      !isRecord(request) ||
      typeof request.id !== 'string' ||
      typeof request.requestNo !== 'string' ||
      typeof request.requestType !== 'string' ||
      typeof request.currentStatus !== 'string' ||
      typeof request.createdAt !== 'string'
    ) {
      throw new Error('Invalid dashboard summary payload: invalid recentRequests')
    }
  }

  return raw as DashboardSummary
}
