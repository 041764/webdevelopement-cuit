<template>
  <PageHeader title="后端健康检查" description="调用 GET /health 验证后端是否可用。" />

  <n-card>
    <AsyncState :loading="loading" :error="error" :empty="false" @retry="fetchHealth">
      <n-space align="center" :size="12">
        <n-tag type="success" v-if="status === 'ok'">OK</n-tag>
        <n-tag type="warning" v-else>Unknown</n-tag>
        <div style="color: var(--muted);">status: {{ status }}</div>
        <div style="flex: 1;" />
        <n-button secondary @click="fetchHealth">刷新</n-button>
      </n-space>
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import { NButton, NCard, NSpace, NTag } from 'naive-ui'
import { onMounted, ref } from 'vue'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const error = ref<unknown>(null)
const status = ref('')

async function fetchHealth() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch('/api/health')
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const json = (await res.json()) as { status?: string }
    status.value = json.status ?? ''
  } catch (e) {
    error.value = e
    status.value = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchHealth()
})
</script>
