<template>
  <PageHeader title="我的信息" description="对接 GET /auth/me。" />

  <n-card>
    <AsyncState :loading="loading" :error="error" :empty="!me" @retry="fetchMe">
      <n-descriptions :column="2" bordered>
        <n-descriptions-item label="userId">{{ me?.userId }}</n-descriptions-item>
        <n-descriptions-item label="userType">{{ me?.userType }}</n-descriptions-item>
        <n-descriptions-item label="id">{{ me?.id }}</n-descriptions-item>
        <n-descriptions-item label="name">{{ me?.name }}</n-descriptions-item>
        <n-descriptions-item label="status">{{ me?.status }}</n-descriptions-item>
        <n-descriptions-item label="collegeId">{{ me?.collegeId ?? '-' }}</n-descriptions-item>
        <n-descriptions-item label="roles" :span="2">{{ rolesText }}</n-descriptions-item>
      </n-descriptions>
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import { NCard, NDescriptions, NDescriptionsItem, useMessage } from 'naive-ui'
import { computed, onMounted, ref } from 'vue'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { getMe } from '@/api/me'
import type { UserMe } from '@/api/schema'

const message = useMessage()

const loading = ref(false)
const error = ref<unknown>(null)
const me = ref<UserMe | null>(null)

const rolesText = computed(() => {
  const roles = me.value?.roles ?? []
  if (roles.length === 0) return '-'
  return roles.join(', ')
})

async function fetchMe() {
  loading.value = true
  error.value = null
  try {
    me.value = await getMe()
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchMe()
})
</script>
