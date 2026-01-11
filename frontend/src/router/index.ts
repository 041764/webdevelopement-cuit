import type { Pinia } from 'pinia'

import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

import { routes } from './routes'

export function createAppRouter(pinia: Pinia, history: RouterHistory = createWebHistory()) {
  const router = createRouter({
    history,
    routes,
  })

  router.beforeEach((to) => {
    if (to.meta.public) return true

    const requiresAuth = Boolean(to.meta.requiresAuth)
    if (!requiresAuth) return true

    const auth = useAuthStore(pinia)
    if (auth.isAuthenticated) return true

    return { name: 'login', query: { redirect: to.fullPath } }
  })

  router.afterEach((to) => {
    const title = typeof to.meta.title === 'string' ? to.meta.title : ''
    document.title = title ? `${title} - 班导师管理系统` : '班导师管理系统'
  })

  return router
}
