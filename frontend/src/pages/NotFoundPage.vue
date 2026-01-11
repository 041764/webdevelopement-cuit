<template>
  <n-result status="404" title="页面不存在" description="请检查地址是否正确。">
    <template #footer>
      <n-button type="primary" @click="goHome">回到首页</n-button>
    </template>
  </n-result>
</template>

<script setup lang="ts">
import { NButton, NResult } from 'naive-ui'
import { useRouter } from 'vue-router'

const router = useRouter()

async function goHome() {
  await router.push({ name: 'dashboard' })
}
</script>
