import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { NConfigProvider, NDialogProvider, NMessageProvider } from 'naive-ui'
import { h, type Component } from 'vue'
import { render } from '@testing-library/vue'
import type { RenderResult } from '@testing-library/vue'
import { createRouter, createMemoryHistory, type RouteRecordRaw, type Router } from 'vue-router'

import { initApiClient } from '@/api/client'

export function createTestPinia(): Pinia {
  const pinia = createPinia()
  setActivePinia(pinia)
  return pinia
}

export function createTestRouter(routes: RouteRecordRaw[]): Router {
  return createRouter({ history: createMemoryHistory(), routes })
}

export async function renderWithProviders(component: Component, options: { routes?: RouteRecordRaw[]; initialPath?: string } = {}): Promise<
  RenderResult & { pinia: Pinia; router: Router }
> {
  const pinia = createTestPinia()
  const routes =
    options.routes ??
    ([
      { path: '/login', name: 'login', component: { render: () => h('div') } },
      { path: '/', name: 'dashboard', component: { render: () => h('div') } },
    ] as RouteRecordRaw[])

  const router = createTestRouter(routes)
  initApiClient(pinia, router)

  if (options.initialPath) {
    await router.push(options.initialPath)
    await router.isReady()
  }

  const wrapper = {
    setup() {
      return () =>
        h(NConfigProvider, {}, {
          default: () =>
            h(NMessageProvider, {}, {
              default: () =>
                h(NDialogProvider, {}, {
                  default: () => h(component),
                }),
            }),
        })
    },
  }

  const result = render(wrapper, {
    global: {
      plugins: [pinia, router],
    },
  })

  return Object.assign(result, { pinia, router })
}
