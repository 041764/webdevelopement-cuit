<template>
  <PageHeader title="活动报名统计" description="对接 GET /reports/activity-stats。" />

  <n-card>
    <n-space wrap :size="12" align="center">
      <n-input v-model:value="term" placeholder="term（必填）" style="width: 220px" />
      <n-button type="primary" :loading="loading" :disabled="term.trim().length === 0" @click="fetchReport">查询</n-button>
    </n-space>
  </n-card>

  <n-card style="margin-top: var(--s-4)">
    <AsyncState :loading="loading" :error="error" :empty="!data" @retry="fetchReport">
      <n-data-table :columns="columns" :data="data?.items ?? []" :bordered="false" />
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'

import { NButton, NCard, NDataTable, NInput, NSpace, useMessage } from 'naive-ui'
import { ref } from 'vue'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { getReportActivityStats } from '@/api/reports'
import type { ReportActivityStats } from '@/api/schema'

const message = useMessage()

const loading = ref(false)
const error = ref<unknown>(null)
const data = ref<ReportActivityStats | null>(null)

const term = ref('')

const columns: DataTableColumns<{ activityId: number; title: string; appliedCount: number; approvedCount: number }> = [
  { title: 'activityId', key: 'activityId', width: 120 },
  { title: 'title', key: 'title' },
  { title: 'appliedCount', key: 'appliedCount', width: 140 },
  { title: 'approvedCount', key: 'approvedCount', width: 140 },
]

async function fetchReport() {
  if (term.value.trim().length === 0) return

  loading.value = true
  error.value = null
  try {
    data.value = await getReportActivityStats({ term: term.value.trim() })
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    loading.value = false
  }
}
</script>
