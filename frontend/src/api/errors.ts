import type { AxiosError } from 'axios'

import type { ErrorResponse } from './types'

export type ApiError = {
  status: number | null
  code: string | null
  message: string
  requestId?: string
  details?: unknown
}

export function toApiError(err: unknown): ApiError {
  if (!err || typeof err !== 'object') {
    return { status: null, code: null, message: 'unknown error' }
  }

  const maybeAxios = err as AxiosError
  if (!maybeAxios.isAxiosError) {
    const msg = err instanceof Error ? err.message : 'unknown error'
    return { status: null, code: null, message: msg }
  }

  const status = maybeAxios.response?.status ?? null
  const data = maybeAxios.response?.data as unknown
  const parsed = parseErrorResponse(data)

  if (parsed) {
    return {
      status,
      code: parsed.code,
      message: parsed.message,
      requestId: parsed.requestId,
      details: parsed.details,
    }
  }

  return {
    status,
    code: null,
    message: maybeAxios.message || 'request failed',
  }
}

function parseErrorResponse(data: unknown): ErrorResponse | null {
  if (!data || typeof data !== 'object') return null
  if (!('code' in data) || !('message' in data)) return null

  const code = (data as { code?: unknown }).code
  const message = (data as { message?: unknown }).message
  const requestId = (data as { requestId?: unknown }).requestId
  const details = (data as { details?: unknown }).details

  if (typeof code !== 'string' || typeof message !== 'string') return null

  const out: ErrorResponse = { code, message }
  if (typeof requestId === 'string') out.requestId = requestId
  if (details !== undefined) out.details = details
  return out
}
