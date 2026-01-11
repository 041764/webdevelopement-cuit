import { getApiClient } from './client'
import type { LoginRequest, LogoutRequest, RefreshRequest, TokenPair } from './types'

export async function login(req: LoginRequest): Promise<TokenPair> {
  const api = getApiClient()
  const res = await api.post<TokenPair>('auth/login', req)
  return res.data
}

export async function refresh(req: RefreshRequest): Promise<TokenPair> {
  const api = getApiClient()
  const res = await api.post<TokenPair>('auth/refresh', req)
  return res.data
}

export async function logout(req: LogoutRequest): Promise<void> {
  const api = getApiClient()
  await api.post('auth/logout', req)
}
