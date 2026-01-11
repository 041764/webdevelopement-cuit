import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import MePage from '@/pages/MePage.vue'

import { server } from '../mswServer'
import { renderWithProviders } from '../utils/renderWithProviders'

describe('MePage', () => {
  it('renders user info from GET /auth/me', async () => {
    server.use(
      http.get('/api/auth/me', () => {
        return HttpResponse.json({
          userId: 1,
          userType: 'TEACHER',
          id: '1001',
          name: 'T1',
          status: 'ACTIVE',
          collegeId: null,
          roles: ['TUTOR'],
        })
      }),
    )

    const { findByText } = await renderWithProviders(MePage)

    await expect(findByText('T1')).resolves.toBeTruthy()
    await expect(findByText('TUTOR')).resolves.toBeTruthy()
  })
})
