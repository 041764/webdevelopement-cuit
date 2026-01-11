import { describe, expect, it } from 'vitest'
import { h } from 'vue'

import AsyncState from '@/components/AsyncState.vue'

import { renderWithProviders } from '../utils/renderWithProviders'

describe('AsyncState', () => {
  it('shows empty text when empty', async () => {
    const Wrapper = {
      setup() {
        return () => h(AsyncState, { loading: false, empty: true, error: null })
      },
    }

    const { findByText } = await renderWithProviders(Wrapper)

    await expect(findByText('暂无数据')).resolves.toBeTruthy()
  })

  it('shows error title when error', async () => {
    const Wrapper = {
      setup() {
        return () => h(AsyncState, { loading: false, empty: false, error: new Error('boom') })
      },
    }

    const { findByText } = await renderWithProviders(Wrapper)

    await expect(findByText('请求失败')).resolves.toBeTruthy()
  })
})
