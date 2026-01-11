<template>
  <div class="side-nav">
    <div class="side-nav__brand">
      <div class="side-nav__mark" aria-hidden="true" />
      <div class="side-nav__name">班导师</div>
    </div>

    <n-menu class="side-nav__menu" :options="options" :value="selectedKey" @update:value="onSelect" />
  </div>
</template>

<script setup lang="ts">
import type { MenuOption } from 'naive-ui'

import { NMenu } from 'naive-ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const emit = defineEmits<{ navigate: [] }>()

const router = useRouter()
const route = useRoute()

const options: MenuOption[] = [
  { label: '首页', key: 'dashboard', to: { name: 'dashboard' } },
  { label: '我的信息', key: 'me', to: { name: 'me' } },

  { label: '活动', key: 'activities', to: { name: 'activities' } },
  { label: '计划', key: 'plans', to: { name: 'plans' } },
  { label: '评价', key: 'evaluations', to: { name: 'evaluations' } },

  { label: '报表 / 计划完成率', key: 'report-plan-completion', to: { name: 'report-plan-completion' } },
  { label: '报表 / 活动统计', key: 'report-activity-stats', to: { name: 'report-activity-stats' } },

  { label: '管理 / 用户导入', key: 'users-import', to: { name: 'users-import' } },
  { label: '管理 / 重置密码', key: 'password-reset', to: { name: 'password-reset' } },

  { label: '开发 / 健康检查', key: 'dev-health', to: { name: 'dev-health' } },
  { label: '开发 / OpenAPI', key: 'dev-openapi', to: { name: 'dev-openapi' } },
]

const selectedKey = computed(() => {
  if (route.name && typeof route.name === 'string') return route.name
  return 'dashboard'
})

function onSelect(_key: string, option: MenuOption) {
  if (option.to) {
    void router.push(option.to)
  }
  emit('navigate')
}
</script>

<style scoped>
.side-nav {
  display: flex;
  flex-direction: column;
  gap: var(--s-4);
  height: 100%;
}

.side-nav__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.42));
  border: 1px solid var(--border);
  box-shadow: var(--shadow-1);
}

.side-nav__mark {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0) 55%),
    linear-gradient(135deg, var(--primary), rgba(245, 158, 11, 0.9));
  box-shadow: 0 10px 24px rgba(14, 165, 183, 0.22);
}

.side-nav__name {
  font-family: var(--font-display);
  font-weight: 700;
  letter-spacing: 0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.n-menu-item-content) {
  position: relative;
  border-radius: var(--radius);
  transition: background-color 140ms ease, transform 140ms ease;
}

:deep(.n-menu-item-content:hover) {
  background-color: rgba(14, 165, 183, 0.08);
}

:deep(.n-menu-item-content--selected) {
  background-color: var(--primary-soft);
  transform: translateX(2px);
}

:deep(.n-menu-item-content--selected::after) {
  content: '';
  position: absolute;
  left: 10px;
  top: 50%;
  width: 6px;
  height: 22px;
  transform: translateY(-50%);
  border-radius: 999px;
  background: var(--primary);
  box-shadow: 0 10px 22px rgba(14, 165, 183, 0.28);
}

:deep(.n-menu-item-content__icon) {
  opacity: 0.9;
}

:deep(.n-menu-item-content__text) {
  letter-spacing: 0.1px;
}

:deep(.n-menu-item-content--selected .n-menu-item-content__text) {
  font-weight: 650;
}

:deep(.n-menu-item-content) {
  padding-left: 26px;
}
</style>
