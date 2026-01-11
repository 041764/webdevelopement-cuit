import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import EvaluationsListPage from '@/pages/evaluations/EvaluationsListPage.vue'

import { server } from '../mswServer'
import { renderWithProviders } from '../utils/renderWithProviders'

describe('EvaluationsListPage', () => {
  it('renders items from GET /evaluations', async () => {
    server.use(
      http.get('/api/evaluations', () => {
        return HttpResponse.json({
          page: 1,
          size: 20,
          total: 1,
          items: [
            {
              id: 1,
              evaluatorUserId: 2,
              evaluateeUserId: 3,
              term: '2026-02-23-1',
              scoreTotal: 10,
              comment: null,
              createdAt: '2026-01-10T12:00:00Z',
            },
          ],
        })
      }),
    )

    const { findByText } = await renderWithProviders(EvaluationsListPage)

    await expect(findByText('2026-02-23-1')).resolves.toBeTruthy()
  })
})
