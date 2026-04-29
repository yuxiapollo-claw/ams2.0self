import axios from 'axios'

export interface DepartmentItem {
  id: string
  departmentName: string
  managerUserId: string | null
  managerUserName: string | null
  description: string | null
  memberCount: number
  status: string
  updatedAt: string
}

export interface DepartmentMutationPayload {
  departmentName: string
  managerUserId: number | null
  description: string
  status: string
}

export interface DepartmentMemberItem {
  id: string
  userName: string
  loginName: string
  departmentId: string
  departmentName: string
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

interface DepartmentRowRaw {
  id?: string | number
  departmentName?: string
  managerUserId?: string | number | null
  managerUserName?: string | null
  description?: string | null
  memberCount?: number
  status?: string
  updatedAt?: string
}

interface DepartmentMemberRowRaw {
  id?: string | number
  userName?: string
  loginName?: string
  departmentId?: string | number
  departmentName?: string
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

function normalizeDepartment(raw: unknown): DepartmentItem {
  const row = assertRecord(raw, 'department row')
  return {
    id: readRequiredId(row.id, 'department id'),
    departmentName: readRequiredString(row.departmentName, 'departmentName'),
    managerUserId: readNullableId(row.managerUserId, 'managerUserId'),
    managerUserName: readNullableString(row.managerUserName, 'managerUserName'),
    description: readNullableString(row.description, 'description'),
    memberCount: readRequiredNumber(row.memberCount, 'memberCount'),
    status: readRequiredString(row.status, 'status'),
    updatedAt: readRequiredString(row.updatedAt, 'updatedAt')
  }
}

function normalizeDepartmentMember(raw: unknown): DepartmentMemberItem {
  const row = assertRecord(raw, 'department member row')
  return {
    id: readRequiredId(row.id, 'member id'),
    userName: readRequiredString(row.userName, 'userName'),
    loginName: readRequiredString(row.loginName, 'loginName'),
    departmentId: readRequiredId(row.departmentId, 'departmentId'),
    departmentName: readRequiredString(row.departmentName, 'departmentName')
  }
}

export async function fetchDepartments(): Promise<DepartmentItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<DepartmentRowRaw>>>('/api/departments')
  const payload = parseEnvelopeData<ListPayload<DepartmentRowRaw>>(data, 'department list')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid department list response')
  }
  return payload.list.map(normalizeDepartment)
}

export async function createDepartment(payload: DepartmentMutationPayload): Promise<DepartmentItem> {
  const { data } = await axios.post<ApiResponse<DepartmentRowRaw>>('/api/departments', payload)
  return normalizeDepartment(parseEnvelopeData<DepartmentRowRaw>(data, 'create department'))
}

export async function updateDepartment(
  departmentId: string,
  payload: DepartmentMutationPayload
): Promise<DepartmentItem> {
  const { data } = await axios.put<ApiResponse<DepartmentRowRaw>>(`/api/departments/${departmentId}`, payload)
  return normalizeDepartment(parseEnvelopeData<DepartmentRowRaw>(data, 'update department'))
}

export async function deleteDepartment(departmentId: string): Promise<void> {
  const { data } = await axios.delete<ApiResponse<null>>(`/api/departments/${departmentId}`)
  parseEnvelopeData<null>(data, 'delete department')
}

export async function fetchDepartmentMembers(departmentId: string): Promise<DepartmentMemberItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<DepartmentMemberRowRaw>>>(`/api/departments/${departmentId}/members`)
  const payload = parseEnvelopeData<ListPayload<DepartmentMemberRowRaw>>(data, 'department member list')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid department member list response')
  }
  return payload.list.map(normalizeDepartmentMember)
}

export async function bindDepartmentMembers(
  departmentId: string,
  memberUserIds: number[]
): Promise<DepartmentMemberItem[]> {
  const { data } = await axios.put<ApiResponse<ListPayload<DepartmentMemberRowRaw>>>(
    `/api/departments/${departmentId}/members`,
    { memberUserIds }
  )
  const payload = parseEnvelopeData<ListPayload<DepartmentMemberRowRaw>>(data, 'bind department members')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid bind department members response')
  }
  return payload.list.map(normalizeDepartmentMember)
}
