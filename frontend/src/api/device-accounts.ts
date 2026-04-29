import axios from 'axios'

export interface DeviceAccountItem {
  id: string
  deviceNodeId: number
  deviceName: string
  userId: string | null
  userName: string | null
  accountName: string
  accountStatus: string
  sourceType: string
  remark: string
  roles: string[]
}

export interface DeviceAccountMutationPayload {
  deviceNodeId: number
  userId: number | null
  accountName: string
  accountStatus: string
  sourceType: string
  remark: string
}

interface ApiResponse<T> {
  code?: number
  message?: string
  data?: T
}

interface ListPayload<T> {
  list?: T[]
  total?: number
}

interface DeviceAccountRowRaw {
  id?: string | number
  deviceNodeId?: number
  deviceName?: string
  userId?: string | number | null
  userName?: string | null
  accountName?: string
  accountStatus?: string
  sourceType?: string
  remark?: string | null
  roles?: string[]
}

function assertRecord(value: unknown, context: string): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`Invalid ${context}`)
  }
  return value as Record<string, unknown>
}

function readRequiredString(value: unknown, context: string): string {
  if (typeof value !== 'string') {
    throw new Error(`Invalid ${context}`)
  }
  return value
}

function readRequiredNumber(value: unknown, context: string): number {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    throw new Error(`Invalid ${context}`)
  }
  return value
}

function readRequiredId(value: unknown, context: string): string {
  if (typeof value === 'string' || typeof value === 'number') {
    return String(value)
  }
  throw new Error(`Invalid ${context}`)
}

function readNullableId(value: unknown, context: string): string | null {
  if (value === null) {
    return null
  }
  return readRequiredId(value, context)
}

function readNullableString(value: unknown, context: string): string | null {
  if (value === null) {
    return null
  }
  return readRequiredString(value, context)
}

function parseEnvelopeData<T>(value: unknown, context: string): T {
  const envelope = assertRecord(value, `${context} response`)
  if (!('data' in envelope)) {
    throw new Error(`Invalid ${context} response`)
  }
  return envelope.data as T
}

function normalizeDeviceAccount(raw: unknown): DeviceAccountItem {
  const row = assertRecord(raw, 'device account row')
  return {
    id: readRequiredId(row.id, 'device account id'),
    deviceNodeId: readRequiredNumber(row.deviceNodeId, 'deviceNodeId'),
    deviceName: readRequiredString(row.deviceName, 'deviceName'),
    userId: readNullableId(row.userId, 'userId'),
    userName: readNullableString(row.userName, 'userName'),
    accountName: readRequiredString(row.accountName, 'accountName'),
    accountStatus: readRequiredString(row.accountStatus, 'accountStatus'),
    sourceType: readRequiredString(row.sourceType, 'sourceType'),
    remark: row.remark == null ? '' : readRequiredString(row.remark, 'remark'),
    roles: Array.isArray(row.roles)
      ? row.roles.map((role, index) => readRequiredString(role, `role[${index}]`))
      : []
  }
}

function normalizeListPayload(value: unknown, context: string) {
  const payload = parseEnvelopeData<ListPayload<DeviceAccountRowRaw>>(value, context)
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error(`Invalid ${context} response`)
  }
  return payload.list.map(normalizeDeviceAccount)
}

export async function fetchDeviceAccounts(): Promise<DeviceAccountItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<DeviceAccountRowRaw>>>('/api/device-accounts')
  return normalizeListPayload(data, 'device account list')
}

export async function fetchDeviceAccountsByDevice(deviceNodeId: string): Promise<DeviceAccountItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<DeviceAccountRowRaw>>>('/api/device-accounts/by-device', {
    params: { deviceNodeId }
  })
  return normalizeListPayload(data, 'device account by device list')
}

export async function createDeviceAccount(
  payload: DeviceAccountMutationPayload
): Promise<DeviceAccountItem> {
  const { data } = await axios.post<ApiResponse<DeviceAccountRowRaw>>('/api/device-accounts', payload)
  return normalizeDeviceAccount(parseEnvelopeData<DeviceAccountRowRaw>(data, 'create device account'))
}

export async function updateDeviceAccount(
  deviceAccountId: string,
  payload: DeviceAccountMutationPayload
): Promise<DeviceAccountItem> {
  const { data } = await axios.put<ApiResponse<DeviceAccountRowRaw>>(
    `/api/device-accounts/${deviceAccountId}`,
    payload
  )
  return normalizeDeviceAccount(parseEnvelopeData<DeviceAccountRowRaw>(data, 'update device account'))
}

export async function deleteDeviceAccount(deviceAccountId: string): Promise<void> {
  const { data } = await axios.delete<ApiResponse<null>>(`/api/device-accounts/${deviceAccountId}`)
  parseEnvelopeData<null>(data, 'delete device account')
}
