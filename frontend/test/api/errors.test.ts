import { describe, expect, it } from 'vitest'

import { toApiError } from '@/api/errors'

type MinimalAxiosError = {
  isAxiosError: true
  message: string
  response?: {
    status?: number
    data?: unknown
  }
}

describe('toApiError', () => {
  it('handles nullish and non-object errors', () => {
    expect(toApiError(null)).toEqual({ status: null, code: null, message: 'unknown error' })
    expect(toApiError(undefined)).toEqual({ status: null, code: null, message: 'unknown error' })
  })

  it('handles native Error', () => {
    expect(toApiError(new Error('boom'))).toEqual({ status: null, code: null, message: 'boom' })
  })

  it('extracts structured api error response when present', () => {
    const err: MinimalAxiosError = {
      isAxiosError: true,
      message: 'Request failed with status code 400',
      response: {
        status: 400,
        data: { code: 'BAD', message: 'Nope', requestId: 'req-1', details: { field: 'x' } },
      },
    }

    expect(toApiError(err)).toEqual({
      status: 400,
      code: 'BAD',
      message: 'Nope',
      requestId: 'req-1',
      details: { field: 'x' },
    })
  })

  it('falls back to axios message when response is not parseable', () => {
    const err: MinimalAxiosError = {
      isAxiosError: true,
      message: 'Network Error',
      response: {
        status: 500,
        data: { nope: true },
      },
    }

    expect(toApiError(err)).toEqual({ status: 500, code: null, message: 'Network Error' })
  })
})
