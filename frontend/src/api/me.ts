import { getApiClient } from './client'
import type { UserMe } from './schema'

export async function getMe(): Promise<UserMe> {
  const api = getApiClient()
  const res = await api.get<UserMe>('auth/me')
  return res.data
}
