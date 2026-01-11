import { defineStore } from 'pinia'

export type AuthTokenPair = {
  accessToken: string
  refreshToken: string
  accessExpiresAt: string
  refreshExpiresAt: string
}

type AuthState = {
  tokenPair: AuthTokenPair | null
}

const STORAGE_KEY = 'tutor-management.auth'

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    tokenPair: readStoredTokenPair(),
  }),
  getters: {
    isAuthenticated(state): boolean {
      return Boolean(state.tokenPair?.accessToken)
    },
  },
  actions: {
    setTokenPair(tokenPair: AuthTokenPair) {
      this.tokenPair = tokenPair
      localStorage.setItem(STORAGE_KEY, JSON.stringify(tokenPair))
    },
    clear() {
      this.tokenPair = null
      localStorage.removeItem(STORAGE_KEY)
    },
  },
})

function readStoredTokenPair(): AuthTokenPair | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as unknown
    if (
      !parsed ||
      typeof parsed !== 'object' ||
      !('accessToken' in parsed) ||
      !('refreshToken' in parsed) ||
      !('accessExpiresAt' in parsed) ||
      !('refreshExpiresAt' in parsed)
    ) {
      return null
    }
    const tokenPair = parsed as AuthTokenPair
    if (
      typeof tokenPair.accessToken !== 'string' ||
      typeof tokenPair.refreshToken !== 'string' ||
      typeof tokenPair.accessExpiresAt !== 'string' ||
      typeof tokenPair.refreshExpiresAt !== 'string'
    ) {
      return null
    }
    return tokenPair
  } catch {
    return null
  }
}
