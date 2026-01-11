<template>
  <PageHeader title="计划完成率报表" description="对接 GET /reports/plan-completion。" />

  <n-card>
    <n-space wrap :size="12" align="center">
      <n-input v-model:value="term" placeholder="term（必填）" style="width: 220px" />
      <n-input-number v-model:value="collegeId" :min="1" style="width: 220px" placeholder="collegeId（可选）" />
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

import { NButton, NCard, NDataTable, NInput, NInputNumber, NSpace, useMessage } from 'naive-ui'
import { ref } from 'vue'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { getReportPlanCompletion } from '@/api/reports'
import type { ReportPlanCompletion } from '@/api/schema'

const message = useMessage()

const loading = ref(false)
const error = ref<unknown>(null)
const data = ref<ReportPlanCompletion | null>(null)

const term = ref('')
const collegeId = ref<number | null>(null)

const columns: DataTableColumns<{ scope: string; doneCount: number; totalCount: number; completionRate: number }> = [
  { title: 'scope', key: 'scope' },
  { title: 'doneCount', key: 'doneCount', width: 120 },
  { title: 'totalCount', key: 'totalCount', width: 120 },
  { title: 'completionRate', key: 'completionRate', width: 160 },
]

async function fetchReport() {
  if (term.value.trim().length === 0) return

  loading.value = true
  error.value = null
  try {
    data.value = await getReportPlanCompletion({
      term: term.value.trim(),
      collegeId: collegeId.value ?? undefined,
    })
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    loading.value = false
  }
}
</script>
