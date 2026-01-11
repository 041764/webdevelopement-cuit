export type UserType = 'STUDENT' | 'TEACHER'

export type TokenPair = {
  accessToken: string
  refreshToken: string
  accessExpiresAt: string
  refreshExpiresAt: string
}

export type LoginRequest = {
  userType: UserType
  id: string
  clientSalt: string
  clientHash: string
  deviceId?: string
}

export type RefreshRequest = {
  refreshToken: string
}

export type LogoutRequest = {
  refreshToken: string
}

export type ErrorResponse = {
  code: string
  message: string
  requestId?: string
  details?: unknown
}
