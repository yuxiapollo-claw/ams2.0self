import axios from 'axios'

export interface ApplicationConfigItem {
  id: string
  applicationName: string
  applicationCode: string
  description: string
  status: string
  updatedAt: string
}

export interface ApplicationConfigMutationPayload {
  applicationName: string
  applicationCode: string
  description: string
  status: string
}

export interface MailTemplateItem {
  id: string
  templateName: string
  description: string
  subject: string
  body: string
  status: string
  updatedAt: string
}

export interface MailTemplateMutationPayload {
  templateName: string
  description: string
  subject: string
  body: string
  status: string
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

function readOptionalString(value: unknown, context: string): string {
  if (value === null || value === undefined) {
    return ''
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

function normalizeApplicationConfig(raw: unknown): ApplicationConfigItem {
  const row = assertRecord(raw, 'application config row')
  return {
    id: readRequiredId(row.id, 'application config id'),
    applicationName: readRequiredString(row.applicationName, 'applicationName'),
    applicationCode: readRequiredString(row.applicationCode, 'applicationCode'),
    description: readOptionalString(row.description, 'description'),
    status: readRequiredString(row.status, 'status'),
    updatedAt: readRequiredString(row.updatedAt, 'updatedAt')
  }
}

function normalizeMailTemplate(raw: unknown): MailTemplateItem {
  const row = assertRecord(raw, 'mail template row')
  return {
    id: readRequiredId(row.id, 'mail template id'),
    templateName: readRequiredString(row.templateName, 'templateName'),
    description: readOptionalString(row.description, 'description'),
    subject: readRequiredString(row.subject, 'subject'),
    body: readRequiredString(row.body, 'body'),
    status: readRequiredString(row.status, 'status'),
    updatedAt: readRequiredString(row.updatedAt, 'updatedAt')
  }
}

export async function fetchApplicationConfigs(): Promise<ApplicationConfigItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<unknown>>>('/api/admin/application-configs')
  const payload = parseEnvelopeData<ListPayload<unknown>>(data, 'application config list')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid application config list response')
  }
  return payload.list.map(normalizeApplicationConfig)
}

export async function createApplicationConfig(
  payload: ApplicationConfigMutationPayload
): Promise<ApplicationConfigItem> {
  const { data } = await axios.post<ApiResponse<unknown>>('/api/admin/application-configs', payload)
  return normalizeApplicationConfig(parseEnvelopeData<unknown>(data, 'create application config'))
}

export async function updateApplicationConfig(
  configId: string,
  payload: ApplicationConfigMutationPayload
): Promise<ApplicationConfigItem> {
  const { data } = await axios.put<ApiResponse<unknown>>(`/api/admin/application-configs/${configId}`, payload)
  return normalizeApplicationConfig(parseEnvelopeData<unknown>(data, 'update application config'))
}

export async function deleteApplicationConfig(configId: string): Promise<void> {
  const { data } = await axios.delete<ApiResponse<null>>(`/api/admin/application-configs/${configId}`)
  parseEnvelopeData<null>(data, 'delete application config')
}

export async function fetchMailTemplates(): Promise<MailTemplateItem[]> {
  const { data } = await axios.get<ApiResponse<ListPayload<unknown>>>('/api/admin/mail-templates')
  const payload = parseEnvelopeData<ListPayload<unknown>>(data, 'mail template list')
  if (!payload || !Array.isArray(payload.list)) {
    throw new Error('Invalid mail template list response')
  }
  return payload.list.map(normalizeMailTemplate)
}

export async function createMailTemplate(
  payload: MailTemplateMutationPayload
): Promise<MailTemplateItem> {
  const { data } = await axios.post<ApiResponse<unknown>>('/api/admin/mail-templates', payload)
  return normalizeMailTemplate(parseEnvelopeData<unknown>(data, 'create mail template'))
}

export async function updateMailTemplate(
  templateId: string,
  payload: MailTemplateMutationPayload
): Promise<MailTemplateItem> {
  const { data } = await axios.put<ApiResponse<unknown>>(`/api/admin/mail-templates/${templateId}`, payload)
  return normalizeMailTemplate(parseEnvelopeData<unknown>(data, 'update mail template'))
}

export async function deleteMailTemplate(templateId: string): Promise<void> {
  const { data } = await axios.delete<ApiResponse<null>>(`/api/admin/mail-templates/${templateId}`)
  parseEnvelopeData<null>(data, 'delete mail template')
}
