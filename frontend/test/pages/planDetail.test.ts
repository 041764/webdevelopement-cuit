import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import PlanDetailPage from '@/pages/plans/PlanDetailPage.vue'

import { server } from '../mswServer'
import { renderWithProviders } from '../utils/renderWithProviders'

describe('PlanDetailPage', () => {
  it('loads plan and items from GET /plans/{id}', async () => {
    server.use(
      http.get('/api/plans/1', () => {
        return HttpResponse.json({
          id: 1,
          ownerType: 'USER',
          ownerUserId: 1,
          ownerClassId: null,
          term: '2026-02-23-1',
          title: 'P1',
          createdAt: '2026-01-10T12:00:00Z',
          items: [
            {
              id: 10,
              planId: 1,
              title: 'I1',
              status: 'todo',
              sortOrder: 1,
              dueDate: null,
              createdAt: '2026-01-10T12:00:00Z',
              updatedAt: '2026-01-10T12:00:00Z',
            },
          ],
          progress: {
            planId: 1,
            doneCount: 0,
            totalCount: 1,
            completionRate: 0,
            calculatedAt: '2026-01-10T12:00:00Z',
          },
        })
      }),
    )

    const { findByText } = await renderWithProviders(PlanDetailPage, {
      routes: [
        { path: '/login', name: 'login', component: { template: '<div />' } },
        { path: '/plans/:planId', name: 'plans-detail', component: PlanDetailPage },
        { path: '/plan-items/:itemId/progress', name: 'plan-item-progress', component: { template: '<div />' } },
      ],
      initialPath: '/plans/1',
    })

    await expect(findByText('P1')).resolves.toBeTruthy()
    await expect(findByText('I1')).resolves.toBeTruthy()
  })
})
