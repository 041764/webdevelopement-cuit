import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import userEvent from '@testing-library/user-event'

import ActivityCreatePage from '@/pages/activities/ActivityCreatePage.vue'

import { server } from '../mswServer'
import { renderWithProviders } from '../utils/renderWithProviders'

describe('ActivityCreatePage', () => {
  it('creates activity and navigates to detail', async () => {
    server.use(
      http.post('/api/activities', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>
        expect(body.term).toBe('2026-02-23-1')
        expect(body.title).toBe('T')

        return HttpResponse.json({
          id: 9,
          classId: 1,
          term: '2026-02-23-1',
          title: 'T',
          requiresReview: false,
          status: 'DRAFT',
          capacity: null,
          description: null,
          createdByUserId: 1,
          createdAt: '2026-01-10T12:00:00Z',
        })
      }),
    )

    const user = userEvent.setup()
    const { router, getByPlaceholderText, getByRole } = await renderWithProviders(ActivityCreatePage, {
      routes: [
        { path: '/login', name: 'login', component: { template: '<div />' } },
        { path: '/activities/new', name: 'activities-new', component: ActivityCreatePage },
        { path: '/activities/:activityId', name: 'activities-detail', component: { template: '<div />' } },
        { path: '/activities', name: 'activities', component: { template: '<div />' } },
      ],
      initialPath: '/activities/new',
    })

    await user.type(getByPlaceholderText('2026-02-23-1'), '2026-02-23-1')
    await user.type(getByPlaceholderText('标题'), 'T')

    await user.click(getByRole('button', { name: '创建' }))

    expect(router.currentRoute.value.name).toBe('activities-detail')
  })
})
