import axios from 'axios'

export interface UserItem {
  id: string
  userCode: string
  userName: string
  departmentId: string
  departmentName: string
  employmentStatus: string
  loginName: string
  accountStatus: string
  systemAdmin: boolean
}

export interface UserMutationPayload {
  userCode?: string
  userName: string
  departmentId: number
  employmentStatus: string
  loginName: string
  accountStatus: string
}

export interface UserStatusPayload {
  accountStatus: string
}

export interface PasswordActionResponse {
  message: string
  defaultPassword: string | null
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
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

interface UserRowRaw {
  id?: string | number
  userCode?: string
  userName?: string
  departmentId?: string | number
  departmentName?: string
  employmentStatus?: string
  loginName?: string
  accountStatus?: string
  systemAdmin?: boolean
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

function readRequiredId(value: unknown, context: string): string {
  if (typeof value === 'string' || typeof value === 'number') {
    return String(value)
  }
  throw new Error(`Invalid ${context}`)
}

function parseEnvelopeData<T>(value: unknown, context: string): T {
  const envelope = assertRecord(value, `${context} response`)
  if (!('data' in envelope)) {
    throw new Error(`Invalid ${context} response`)
  }
  return envelope.data as T
}

function normalizeUser(raw: unknown): UserItem {
  const row = assertRecord(raw, 'user row')
  return {
    id: readRequiredId(row.id, 'user id'),
    userCode: typeof row.userCode === 'string' && row.userCode.trim().length > 0 ? row.userCode : readRequiredString(row.loginName, 'loginName'),
    userName: readRequiredString(row.userName, 'userName'),
    departmentId: readRequiredId(row.departmentId, 'departmentId'),
    departmentName: readRequiredString(row.departmentName, 'departmentName'),
    employmentStatus: readRequiredString(row.employmentStatus, 'employmentStatus'),
    loginName: readRequiredString(row.loginName, 'loginName'),
    accountStatus: readRequiredString(row.accountStatus, 'accountStatus'),
    systemAdmin: Boolean(row.systemAdmin)
  }
}

function normalizePasswordAction(raw: unknown): PasswordActionResponse {
  const row = assertRecord(raw, 'password response')
  return {
    message: typeof row.message === 'string' ? row.message : '',
    defaultPassword: typeof row.defaultPassword === 'string' ? row.defaultPassword : null
  }
}

export async function fetchUsers(): Promise<UserItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<UserRowRaw>>>('/api/users')
  const payload = parseEnvelopeData<ListPayload<UserRowRaw>>(data, 'user list')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid user list response')
  }
  return payload.list.map(normalizeUser)
}

export async function createUser(payload: UserMutationPayload): Promise<UserItem> {
  const { data } = await axios.post<ApiResponse<UserRowRaw>>('/api/users', {
    userCode: payload.userCode,
    userName: payload.userName,
    departmentId: payload.departmentId,
    employmentStatus: payload.employmentStatus,
    loginName: payload.loginName,
    accountStatus: payload.accountStatus
  })
  return normalizeUser(parseEnvelopeData<UserRowRaw>(data, 'create user'))
}

export async function updateUser(userId: string, payload: UserMutationPayload): Promise<UserItem> {
  const { data } = await axios.put<ApiResponse<UserRowRaw>>(`/api/users/${userId}`, {
    userCode: payload.userCode,
    userName: payload.userName,
    departmentId: payload.departmentId,
    employmentStatus: payload.employmentStatus,
    loginName: payload.loginName,
    accountStatus: payload.accountStatus
  })
  return normalizeUser(parseEnvelopeData<UserRowRaw>(data, 'update user'))
}

export async function updateUserStatus(userId: string, payload: UserStatusPayload): Promise<UserItem> {
  const { data } = await axios.patch<ApiResponse<UserRowRaw>>(`/api/users/${userId}/status`, payload)
  return normalizeUser(parseEnvelopeData<UserRowRaw>(data, 'update user status'))
}

export async function deleteUser(userId: string): Promise<void> {
  const { data } = await axios.delete<ApiResponse<null>>(`/api/users/${userId}`)
  parseEnvelopeData<null>(data, 'delete user')
}

export async function resetUserPassword(userId: string): Promise<PasswordActionResponse> {
  const { data } = await axios.post<ApiResponse<Record<string, unknown>>>(`/api/users/${userId}/reset-password`)
  return normalizePasswordAction(parseEnvelopeData<Record<string, unknown>>(data, 'reset password'))
}

export async function changeMyPassword(payload: ChangePasswordPayload): Promise<PasswordActionResponse> {
  const { data } = await axios.post<ApiResponse<Record<string, unknown>>>('/api/users/change-password', payload)
  return normalizePasswordAction(parseEnvelopeData<Record<string, unknown>>(data, 'change password'))
}
