import axios from 'axios'

export interface AuditLogItem {
  id: string
  operatorName: string
  action: string
  createdAt: string
}

export interface AuditLogResult {
  list: AuditLogItem[]
  total: number
}

interface AuditLogRowRaw {
  id?: string
  actionType?: string
  operatorName?: string
  objectType?: string
  createdAt?: string
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

function formatAction(raw: AuditLogRowRaw) {
  const actionType = raw.actionType?.trim() ?? ''
  const objectType = raw.objectType?.trim() ?? ''

  if (!actionType && !objectType) {
    return 'Unspecified Action'
  }
  if (!objectType) {
    return actionType
  }
  if (!actionType) {
    return objectType
  }
  return `${actionType} (${objectType})`
}

function normalizeAuditRow(raw: AuditLogRowRaw, index: number): AuditLogItem {
  return {
    id: raw.id ?? `AUDIT-${index + 1}`,
    operatorName: raw.operatorName?.trim() || 'unknown',
    action: formatAction(raw),
    createdAt: raw.createdAt?.trim() || '-'
  }
}

export async function fetchAuditLogs(): Promise<AuditLogResult> {
  const { data } = await axios.get<WrappedResponse<{ list?: AuditLogRowRaw[]; total?: number }>>(
    '/api/audit/logs'
  )
  const payload = readEnvelopeData(data, 'Audit query failed')

  return {
    list: Array.isArray(payload?.list)
      ? payload.list.map((row, index) => normalizeAuditRow(row, index))
      : [],
    total: payload?.total ?? 0
  }
}
