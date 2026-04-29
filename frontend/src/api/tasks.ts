import axios from 'axios'

export interface TaskAdvanceResponse {
  requestId: number
  currentStatus: string
  currentStatusLabel: string
}

interface ApiEnvelope<T> {
  data?: T
}

export async function approveRequest(requestId: string): Promise<TaskAdvanceResponse> {
  const { data } = await axios.post<ApiEnvelope<TaskAdvanceResponse>>(`/api/requests/${requestId}/approve`)
  return data.data ?? { requestId: Number(requestId), currentStatus: '', currentStatusLabel: '' }
}
