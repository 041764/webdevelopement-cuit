import { describe, expect, it } from 'vitest'

import { deriveClientCredentials } from '@/utils/credentials'

describe('deriveClientCredentials', () => {
  it('is deterministic for same inputs', async () => {
    const a = await deriveClientCredentials('STUDENT', '2020123456', 'pw')
    const b = await deriveClientCredentials('STUDENT', '2020123456', 'pw')

    expect(a).toEqual(b)
  })

  it('changes when password changes', async () => {
    const a = await deriveClientCredentials('STUDENT', '2020123456', 'pw1')
    const b = await deriveClientCredentials('STUDENT', '2020123456', 'pw2')

    expect(a.clientHash).not.toEqual(b.clientHash)
    expect(a.clientSalt).toEqual(b.clientSalt)
  })

  it('trims id before hashing', async () => {
    const a = await deriveClientCredentials('TEACHER', ' 1001 ', 'pw')
    const b = await deriveClientCredentials('TEACHER', '1001', 'pw')

    expect(a).toEqual(b)
  })

  it('returns base64url-safe strings', async () => {
    const { clientSalt, clientHash } = await deriveClientCredentials('STUDENT', '2020123456', 'pw')

    expect(clientSalt).not.toMatch(/[+/=]/)
    expect(clientHash).not.toMatch(/[+/=]/)
  })
})
