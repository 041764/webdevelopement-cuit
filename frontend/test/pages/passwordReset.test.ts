import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import userEvent from '@testing-library/user-event'
import { waitFor } from '@testing-library/vue'

import PasswordResetPage from '@/pages/users/PasswordResetPage.vue'

import { server } from '../mswServer'
import { renderWithProviders } from '../utils/renderWithProviders'

describe('PasswordResetPage', () => {
  it('posts reset payload', async () => {
    let received: unknown = null

    server.use(
      http.post('/api/users/1/password:reset', async ({ request }) => {
        received = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    const { getByPlaceholderText, getByRole } = await renderWithProviders(PasswordResetPage)

    await user.type(getByPlaceholderText('例如：2020123456'), '1001')
    await user.type(getByPlaceholderText('请输入新密码'), 'pw')

    await user.click(getByRole('button', { name: '重置' }))

    await waitFor(() => {
      expect(received && typeof received === 'object').toBe(true)
    })
  })
})
