<template>
  <n-spin :show="loading">
    <slot v-if="!loading && !error && !empty" />

    <n-empty v-else-if="!loading && !error && empty" description="暂无数据" />

    <n-result v-else-if="error" status="error" title="请求失败" :description="errorDescription">
      <template #footer>
        <n-button @click="$emit('retry')">重试</n-button>
      </template>
    </n-result>
  </n-spin>
</template>

<script setup lang="ts">
import { NButton, NEmpty, NResult, NSpin } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps<{ loading: boolean; empty: boolean; error: unknown }>()

defineEmits<{ retry: [] }>()

const errorDescription = computed(() => {
  if (!props.error) return ''
  if (props.error instanceof Error) return props.error.message
  if (typeof props.error === 'string') return props.error
  return '未知错误'
})
</script>
