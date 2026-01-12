import type { RouteRecordRaw } from 'vue-router'

import AppShell from '@/layouts/AppShell.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import ActivitiesListPage from '@/pages/activities/ActivitiesListPage.vue'
import ActivityCreatePage from '@/pages/activities/ActivityCreatePage.vue'
import ActivityDetailPage from '@/pages/activities/ActivityDetailPage.vue'
import ActivitySignupsPage from '@/pages/activities/ActivitySignupsPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import HealthPage from '@/pages/dev/HealthPage.vue'
import LoginPage from '@/pages/LoginPage.vue'
import MePage from '@/pages/MePage.vue'
import NotFoundPage from '@/pages/NotFoundPage.vue'
import EvaluationCreatePage from '@/pages/evaluations/EvaluationCreatePage.vue'
import EvaluationDetailPage from '@/pages/evaluations/EvaluationDetailPage.vue'
import EvaluationsListPage from '@/pages/evaluations/EvaluationsListPage.vue'
import ActivityStatsReportPage from '@/pages/reports/ActivityStatsReportPage.vue'
import PlanCompletionReportPage from '@/pages/reports/PlanCompletionReportPage.vue'
import PlanCreatePage from '@/pages/plans/PlanCreatePage.vue'
import PasswordResetPage from '@/pages/users/PasswordResetPage.vue'
import UsersImportPage from '@/pages/users/UsersImportPage.vue'
import PlanDetailPage from '@/pages/plans/PlanDetailPage.vue'
import PlanItemProgressPage from '@/pages/plans/PlanItemProgressPage.vue'
import PlansListPage from '@/pages/plans/PlansListPage.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: AuthLayout,
    children: [{ path: '', name: 'login', component: LoginPage, meta: { public: true, title: '登录' } }],
  },
  {
    path: '/dev',
    component: AuthLayout,
    children: [
      { path: 'health', name: 'dev-health', component: HealthPage, meta: { public: true, title: '健康检查' } },
    ],
  },
  {
    path: '/',
    component: AppShell,
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'dashboard', component: DashboardPage, meta: { title: '首页' } },
      { path: 'me', name: 'me', component: MePage, meta: { title: '我的信息' } },
      { path: 'activities', name: 'activities', component: ActivitiesListPage, meta: { title: '活动' } },
      { path: 'activities/new', name: 'activities-new', component: ActivityCreatePage, meta: { title: '创建活动' } },
      {
        path: 'activities/:activityId',
        name: 'activities-detail',
        component: ActivityDetailPage,
        meta: { title: '活动详情' },
      },
      {
        path: 'activities/:activityId/signups',
        name: 'activities-signups',
        component: ActivitySignupsPage,
        meta: { title: '报名列表' },
      },
      { path: 'admin/users-import', name: 'users-import', component: UsersImportPage, meta: { title: '用户导入', requiresRole: true } },
      { path: 'admin/password-reset', name: 'password-reset', component: PasswordResetPage, meta: { title: '重置密码', requiresRole: true } },

      { path: 'evaluations', name: 'evaluations', component: EvaluationsListPage, meta: { title: '评价', requiresRole: true } },
      { path: 'evaluations/new', name: 'evaluations-new', component: EvaluationCreatePage, meta: { title: '创建评价', requiresRole: true } },
      {
        path: 'evaluations/:evaluationId',
        name: 'evaluations-detail',
        component: EvaluationDetailPage,
        meta: { title: '评价详情', requiresRole: true },
      },

      { path: 'reports/plan-completion', name: 'report-plan-completion', component: PlanCompletionReportPage, meta: { title: '计划完成率报表', requiresRole: true } },
      { path: 'reports/activity-stats', name: 'report-activity-stats', component: ActivityStatsReportPage, meta: { title: '活动统计报表', requiresRole: true } },

      { path: 'plans', name: 'plans', component: PlansListPage, meta: { title: '计划' } },
      { path: 'plans/new', name: 'plans-new', component: PlanCreatePage, meta: { title: '创建计划' } },
      { path: 'plans/:planId', name: 'plans-detail', component: PlanDetailPage, meta: { title: '计划详情' } },
      {
        path: 'plan-items/:itemId/progress',
        name: 'plan-item-progress',
        component: PlanItemProgressPage,
        meta: { title: '条目进度' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundPage, meta: { public: true, title: '页面不存在' } },
]
