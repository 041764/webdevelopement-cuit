import { getApiClient } from './client'
import type { ImportUsersResult, UserType } from './schema'

type ImportUsersParams = {
  userType: UserType
}

export async function importUsersCsv(params: ImportUsersParams, file: File): Promise<ImportUsersResult> {
  const api = getApiClient()
  const form = new FormData()
  form.append('file', file)

  const res = await api.post<ImportUsersResult>('users/import', form, {
    params,
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  return res.data
}

export async function resetUserPassword(
  userId: number,
  body: { clientSalt: string; clientHash: string },
): Promise<void> {
  const api = getApiClient()
  await api.post(`users/${userId}/password:reset`, body)
}

export async function resetUserPasswordByNo(
  body: { userType: UserType; userNo: string; clientSalt: string; clientHash: string },
): Promise<void> {
  const api = getApiClient()
  await api.post('users/password:reset-by-no', body)
}
