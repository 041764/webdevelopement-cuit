<template>
  <PageHeader title="OpenAPI" description="调用 GET /openapi 获取 Swagger UI 与 /v3/api-docs 地址。" />

  <n-card>
    <AsyncState :loading="loading" :error="error" :empty="!data" @retry="fetchOpenApi">
      <n-space vertical :size="12">
        <n-a :href="data?.swaggerUi" target="_blank">Swagger UI</n-a>
        <n-a :href="data?.apiDocs" target="_blank">/v3/api-docs</n-a>
        <n-button secondary @click="fetchOpenApi">刷新</n-button>
      </n-space>
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import { NA, NButton, NCard, NSpace } from 'naive-ui'
import { onMounted, ref } from 'vue'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const error = ref<unknown>(null)
const data = ref<{ swaggerUi?: string; apiDocs?: string } | null>(null)

async function fetchOpenApi() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch('/api/openapi')
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    data.value = (await res.json()) as { swaggerUi?: string; apiDocs?: string }
  } catch (e) {
    error.value = e
    data.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchOpenApi()
})
</script>
