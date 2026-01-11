<template>
  <PageHeader title="首页" description="快速入口与状态概览。">
    <template #actions>
      <n-button secondary @click="goDevHealth">后端健康检查</n-button>
    </template>
  </PageHeader>

  <n-grid :cols="24" :x-gap="16" :y-gap="16">
    <n-grid-item :span="24">
      <n-card>
        <div>从这里开始：后续会接入活动/计划/评价/报表等模块。</div>
      </n-card>
    </n-grid-item>
  </n-grid>
</template>

<script setup lang="ts">
import { NButton, NCard, NGrid, NGridItem } from 'naive-ui'
import { useRouter } from 'vue-router'

import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()

async function goDevHealth() {
  await router.push({ name: 'dev-health' })
}
</script>
