import type { UserType } from '@/api/schema'

import { sha256Base64Url } from './sha256'

export async function deriveClientCredentials(userType: UserType, id: string, password: string) {
  const normalizedId = id.trim()
  const clientSalt = await sha256Base64Url(`${userType}:${normalizedId}`)
  const clientHash = await sha256Base64Url(`${clientSalt}:${password}`)
  return { clientSalt, clientHash }
}
