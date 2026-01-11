import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import userEvent from '@testing-library/user-event'

import PlanCompletionReportPage from '@/pages/reports/PlanCompletionReportPage.vue'

import { server } from '../mswServer'
import { renderWithProviders } from '../utils/renderWithProviders'

describe('PlanCompletionReportPage', () => {
  it('loads report after clicking query', async () => {
    server.use(
      http.get('/api/reports/plan-completion', ({ request }) => {
        const url = new URL(request.url)
        expect(url.searchParams.get('term')).toBe('2026-02-23-1')

        return HttpResponse.json({
          term: '2026-02-23-1',
          items: [{ scope: '学院:A', doneCount: 1, totalCount: 2, completionRate: 0.5 }],
        })
      }),
    )

    const user = userEvent.setup()
    const { getByPlaceholderText, getByRole, findByText } = await renderWithProviders(PlanCompletionReportPage)

    await user.type(getByPlaceholderText('term（必填）'), '2026-02-23-1')
    await user.click(getByRole('button', { name: '查询' }))

    await expect(findByText('学院:A')).resolves.toBeTruthy()
  })
})
