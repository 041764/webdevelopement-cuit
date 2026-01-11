import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'

import axios, { type AxiosInstance } from 'axios'

import { useAuthStore } from '@/stores/auth'

import type { TokenPair } from './types'

let apiClient: AxiosInstance | null = null

export function initApiClient(pinia: Pinia, router: Pick<Router, 'push'>) {
  apiClient = createApiClient(pinia, router)
  return apiClient
}

export function getApiClient(): AxiosInstance {
  if (!apiClient) throw new Error('api client not initialized')
  return apiClient
}

export function createApiClient(pinia: Pinia, router: Pick<Router, 'push'>): AxiosInstance {
  const api = axios.create({
    baseURL: '/api',
    timeout: 15_000,
  })

  const refreshClient = axios.create({
    baseURL: '/api',
    timeout: 15_000,
  })

  let refreshPromise: Promise<void> | null = null
  const retriedConfigs = new WeakSet<object>()

  api.interceptors.request.use((config) => {
    const auth = useAuthStore(pinia)
    const accessToken = auth.tokenPair?.accessToken
    if (accessToken) {
      config.headers.set('Authorization', `Bearer ${accessToken}`)
    }
    return config
  })

  api.interceptors.response.use(
    (response) => response,
    async (error) => {
      if (!axios.isAxiosError(error)) throw error

      const status = error.response?.status
      const rawUrl = typeof error.config?.url === 'string' ? error.config.url : ''
      const url = rawUrl.startsWith('/') ? rawUrl.slice(1) : rawUrl
      const isAuthEndpoint = url.startsWith('auth/login') || url.startsWith('auth/refresh')

      if (status !== 401 || isAuthEndpoint || !error.config) {
        throw error
      }

      if (retriedConfigs.has(error.config)) {
        throw error
      }
      retriedConfigs.add(error.config)

      const auth = useAuthStore(pinia)
      const refreshToken = auth.tokenPair?.refreshToken
      if (!refreshToken) {
        auth.clear()
        await router.push({ name: 'login' })
        throw error
      }

      try {
        if (!refreshPromise) {
          refreshPromise = refreshOnce(pinia, refreshClient, refreshToken)
        }
        await refreshPromise
        refreshPromise = null

        const newAccessToken = auth.tokenPair?.accessToken
        if (newAccessToken) {
          error.config.headers.set('Authorization', `Bearer ${newAccessToken}`)
        }

        return await api.request(error.config)
      } catch {
        refreshPromise = null
        auth.clear()
        await router.push({ name: 'login' })
        throw error
      }
    },
  )

  return api
}

async function refreshOnce(pinia: Pinia, refreshClient: AxiosInstance, refreshToken: string) {
  const auth = useAuthStore(pinia)
  const res = await refreshClient.post<TokenPair>('auth/refresh', { refreshToken })
  auth.setTokenPair(res.data)
}
