import type { Pinia } from 'pinia'

import { createRouter, createWebHistory, type RouterHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { useMeStore } from '@/stores/me'

import { routes } from './routes'

export function createAppRouter(pinia: Pinia, history: RouterHistory = createWebHistory()) {
  const router = createRouter({
    history,
    routes,
  })

  router.beforeEach(async (to) => {
    if (to.meta.public) return true

    const requiresAuth = Boolean(to.meta.requiresAuth)
    if (!requiresAuth) return true

    const auth = useAuthStore(pinia)
    if (!auth.isAuthenticated) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    // 检查是否需要特定角色权限
    if (to.meta.requiresTeacher || to.meta.requiresRole) {
      const meStore = useMeStore(pinia)
      // 如果还没有加载用户信息，等待加载
      if (!meStore.me) {
        try {
          await meStore.load()
        } catch {
          return { name: 'login', query: { redirect: to.fullPath } }
        }
      }
      // 学生不能访问教师页面
      if (meStore.me?.userType !== 'TEACHER') {
        return { name: 'dashboard' }
      }
      // 如果需要角色，检查是否有 ADMIN_SCHOOL/ADMIN_COLLEGE/TUTOR 角色
      if (to.meta.requiresRole) {
        const roles = meStore.me?.roles ?? []
        const hasRole = roles.includes('ADMIN_SCHOOL') || roles.includes('ADMIN_COLLEGE') || roles.includes('TUTOR')
        if (!hasRole) {
          return { name: 'dashboard' }
        }
      }
    }

    return true
  })

  router.afterEach((to) => {
    const title = typeof to.meta.title === 'string' ? to.meta.title : ''
    document.title = title ? `${title} - 班导师管理系统` : '班导师管理系统'
  })

  return router
}
