import axios from 'axios'

export interface RequestItem {
  id: string
  requestNo: string
  requestType: string
  targetAccountName: string
  status: string
  createdAt: string
}

export interface CreateRequestPayload {
  requestType: string
  targetUserId: number
  targetDeviceNodeId: number
  targetAccountName: string
  reason: string
  items: Array<{ roleNodeId: number }>
}

interface RequestListRaw {
  id?: string | number
  requestNo?: string
  requestType?: string
  targetAccountName?: string
  currentStatus?: string
  status?: string
  createdAt?: string
}

interface RequestListResponse {
  data?: {
    list?: RequestListRaw[]
  }
}

function normalizeRequest(raw: RequestListRaw): RequestItem {
  return {
    id: String(raw.id ?? ''),
    requestNo: raw.requestNo ?? '',
    requestType: raw.requestType ?? '',
    targetAccountName: raw.targetAccountName ?? '',
    status: raw.status ?? raw.currentStatus ?? '',
    createdAt: raw.createdAt ?? ''
  }
}

export async function fetchRequests() {
  const { data } = await axios.get<RequestListResponse>('/api/requests')
  return Array.isArray(data.data?.list) ? data.data.list.map(normalizeRequest) : []
}

export async function createRequest(payload: CreateRequestPayload) {
  const { data } = await axios.post('/api/requests', payload)
  return data.data ?? data
}
