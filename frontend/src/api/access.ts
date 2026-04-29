import axios from 'axios'

export interface AccessSystemItem {
  id: string
  systemName: string
  systemDescription: string
}

export interface AccessPermissionTreeNode {
  key: string
  entityId: string
  nodeType: 'SYSTEM' | 'PERMISSION'
  systemId: string
  parentPermissionId: string | null
  label: string
  fullPath: string
  enabled: boolean
  level: number
  leaf: boolean
  children: AccessPermissionTreeNode[]
}

export interface PermissionRequestItem {
  id: string
  requestNo: string
  requestType: string
  permissionPath: string
  currentStatus: string
  requestReason: string
  createdAt: string
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

interface AccessSystemRowRaw {
  id?: string | number
  systemName?: string
  systemDescription?: string | null
}

interface PermissionTreeNodeRaw {
  key?: string
  entityId?: string | number
  nodeType?: 'SYSTEM' | 'PERMISSION'
  systemId?: string | number
  parentPermissionId?: string | number | null
  label?: string
  fullPath?: string
  enabled?: boolean
  level?: number
  leaf?: boolean
  children?: PermissionTreeNodeRaw[]
}

interface PermissionRequestRowRaw {
  id?: string | number
  requestNo?: string
  requestType?: string
  permissionPath?: string
  currentStatus?: string
  requestReason?: string
  createdAt?: string
}

function assertRecord(value: unknown, context: string): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`Invalid ${context}`)
  }
  return value as Record<string, unknown>
}

function parseEnvelopeData<T>(value: unknown, context: string): T {
  const envelope = assertRecord(value, `${context} response`)
  if (!('data' in envelope)) {
    throw new Error(`Invalid ${context} response`)
  }
  return envelope.data as T
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

function readNullableId(value: unknown, context: string): string | null {
  if (value === null) {
    return null
  }
  return readRequiredId(value, context)
}

function normalizeSystem(raw: unknown): AccessSystemItem {
  const row = assertRecord(raw, 'system row')
  return {
    id: readRequiredId(row.id, 'system id'),
    systemName: readRequiredString(row.systemName, 'systemName'),
    systemDescription: typeof row.systemDescription === 'string' ? row.systemDescription : ''
  }
}

function normalizePermissionTree(raw: unknown): AccessPermissionTreeNode {
  const row = assertRecord(raw, 'permission tree row')
  return {
    key: readRequiredString(row.key, 'tree key'),
    entityId: readRequiredId(row.entityId, 'entityId'),
    nodeType: readRequiredString(row.nodeType, 'nodeType') as 'SYSTEM' | 'PERMISSION',
    systemId: readRequiredId(row.systemId, 'systemId'),
    parentPermissionId: readNullableId(row.parentPermissionId, 'parentPermissionId'),
    label: readRequiredString(row.label, 'label'),
    fullPath: readRequiredString(row.fullPath, 'fullPath'),
    enabled: Boolean(row.enabled),
    level: typeof row.level === 'number' ? row.level : 0,
    leaf: Boolean(row.leaf),
    children: Array.isArray(row.children) ? row.children.map(normalizePermissionTree) : []
  }
}

function normalizeRequest(raw: unknown): PermissionRequestItem {
  const row = assertRecord(raw, 'request row')
  return {
    id: readRequiredId(row.id, 'request id'),
    requestNo: readRequiredString(row.requestNo, 'requestNo'),
    requestType: readRequiredString(row.requestType, 'requestType'),
    permissionPath: readRequiredString(row.permissionPath, 'permissionPath'),
    currentStatus: readRequiredString(row.currentStatus, 'currentStatus'),
    requestReason: readRequiredString(row.requestReason, 'requestReason'),
    createdAt: readRequiredString(row.createdAt, 'createdAt')
  }
}

async function fetchTree(url: string) {
  const { data } = await axios.get<ApiResponse<ListPayload<PermissionTreeNodeRaw>>>(url)
  const payload = parseEnvelopeData<ListPayload<PermissionTreeNodeRaw>>(data, 'permission tree')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid permission tree response')
  }
  return payload.list.map(normalizePermissionTree)
}

export async function fetchAccessSystems(): Promise<AccessSystemItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<AccessSystemRowRaw>>>('/api/access/systems')
  const payload = parseEnvelopeData<ListPayload<AccessSystemRowRaw>>(data, 'system list')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid system list response')
  }
  return payload.list.map(normalizeSystem)
}

export async function createAccessSystem(payload: { systemName: string; systemDescription: string }) {
  const { data } = await axios.post<ApiResponse<AccessSystemRowRaw>>('/api/access/systems', payload)
  return normalizeSystem(parseEnvelopeData<AccessSystemRowRaw>(data, 'create system'))
}

export async function updateAccessSystem(systemId: string, payload: { systemName: string; systemDescription: string }) {
  const { data } = await axios.put<ApiResponse<AccessSystemRowRaw>>(`/api/access/systems/${systemId}`, payload)
  return normalizeSystem(parseEnvelopeData<AccessSystemRowRaw>(data, 'update system'))
}

export async function deleteAccessSystem(systemId: string) {
  const { data } = await axios.delete<ApiResponse<null>>(`/api/access/systems/${systemId}`)
  parseEnvelopeData<null>(data, 'delete system')
}

export async function fetchPermissionTree() {
  return fetchTree('/api/access/permissions/tree')
}

export async function fetchMyPermissionTree() {
  return fetchTree('/api/access/permissions/my-tree')
}

export async function createPermissionNode(payload: {
  systemId: number
  parentPermissionId: number | null
  permissionName: string
  enabled: boolean
}) {
  const { data } = await axios.post<ApiResponse<PermissionTreeNodeRaw>>('/api/access/permissions', payload)
  return normalizePermissionTree(parseEnvelopeData<PermissionTreeNodeRaw>(data, 'create permission'))
}

export async function updatePermissionNode(
  permissionId: string,
  payload: {
    systemId: number
    parentPermissionId: number | null
    permissionName: string
    enabled: boolean
  }
) {
  const { data } = await axios.put<ApiResponse<PermissionTreeNodeRaw>>(
    `/api/access/permissions/${permissionId}`,
    payload
  )
  return normalizePermissionTree(parseEnvelopeData<PermissionTreeNodeRaw>(data, 'update permission'))
}

export async function deletePermissionNode(permissionId: string) {
  const { data } = await axios.delete<ApiResponse<null>>(`/api/access/permissions/${permissionId}`)
  parseEnvelopeData<null>(data, 'delete permission')
}

export async function fetchPermissionRequests(): Promise<PermissionRequestItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<PermissionRequestRowRaw>>>('/api/access/requests')
  const payload = parseEnvelopeData<ListPayload<PermissionRequestRowRaw>>(data, 'request list')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid request list response')
  }
  return payload.list.map(normalizeRequest)
}

export async function createPermissionRequest(payload: {
  requestType: string
  permissionId: number
  reason: string
}) {
  const { data } = await axios.post<ApiResponse<PermissionRequestRowRaw>>('/api/access/requests', payload)
  return normalizeRequest(parseEnvelopeData<PermissionRequestRowRaw>(data, 'create request'))
}
