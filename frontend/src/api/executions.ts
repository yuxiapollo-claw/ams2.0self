import axios from 'axios'

export interface ExecutionSubmitPayload {
  requestId: number
  currentStatus: string
}

interface ApiEnvelope<T> {
  data?: T
}

export async function submitExecution(requestId: string): Promise<ExecutionSubmitPayload> {
  const { data } = await axios.post<ApiEnvelope<ExecutionSubmitPayload>>(`/api/executions/${requestId}/submit`)
  return data.data ?? { requestId: Number(requestId), currentStatus: '' }
}
