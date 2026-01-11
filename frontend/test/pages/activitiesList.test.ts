import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import ActivitiesListPage from '@/pages/activities/ActivitiesListPage.vue'
import { useAuthStore } from '@/stores/auth'

import { server } from '../mswServer'
import { renderWithProviders } from '../utils/renderWithProviders'

describe('ActivitiesListPage', () => {
  it('renders items from GET /activities', async () => {
    server.use(
      http.get('/api/activities', () => {
        return HttpResponse.json({
          page: 1,
          size: 20,
          total: 1,
          items: [
            {
              id: 1,
              classId: 1,
              term: '2026-02-23-1',
              title: 'A1',
              requiresReview: false,
              status: 'DRAFT',
              capacity: null,
              description: null,
              createdByUserId: 1,
              createdAt: '2026-01-10T12:00:00Z',
            },
          ],
        })
      }),
    )

    const { pinia, findByText } = await renderWithProviders(ActivitiesListPage)

    const auth = useAuthStore(pinia)
    auth.setTokenPair({
      accessToken: 'access',
      refreshToken: 'refresh',
      accessExpiresAt: new Date(Date.now() + 60_000).toISOString(),
      refreshExpiresAt: new Date(Date.now() + 120_000).toISOString(),
    })

    await expect(findByText('A1')).resolves.toBeTruthy()
  })

  it('shows empty state when list is empty', async () => {
    server.use(
      http.get('/api/activities', () => {
        return HttpResponse.json({ page: 1, size: 20, total: 0, items: [] })
      }),
    )

    const { findByText } = await renderWithProviders(ActivitiesListPage)

    await expect(findByText('暂无数据')).resolves.toBeTruthy()
  })
})
