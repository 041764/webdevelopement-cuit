import { bytesToBase64Url } from './base64url'

export async function sha256Base64Url(text: string): Promise<string> {
  const bytes = new TextEncoder().encode(text)
  const hash = await crypto.subtle.digest('SHA-256', bytes)
  return bytesToBase64Url(new Uint8Array(hash))
}
