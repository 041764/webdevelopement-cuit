<template>
  <PageHeader title="计划完成率报表" description="查看计划完成率统计数据。" />

  <n-card>
    <n-space wrap :size="12" align="center">
      <n-input v-model:value="term" placeholder="学期（必填）" style="width: 220px" />
      <n-select
        v-model:value="collegeId"
        :options="collegeOptions"
        :loading="loadingColleges"
        clearable
        filterable
        placeholder="学院筛选（可选）"
        style="width: 220px"
      />
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

import { NButton, NCard, NDataTable, NInput, NSelect, NSpace, useMessage } from 'naive-ui'
import { computed, onMounted, ref } from 'vue'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { fetchColleges, type CollegeOption } from '@/api/lookup'
import { getReportPlanCompletion } from '@/api/reports'
import type { ReportPlanCompletion } from '@/api/schema'

const message = useMessage()

const loading = ref(false)
const error = ref<unknown>(null)
const data = ref<ReportPlanCompletion | null>(null)

const term = ref('')
const collegeId = ref<number | null>(null)

const loadingColleges = ref(false)
const colleges = ref<CollegeOption[]>([])

const collegeOptions = computed(() =>
  colleges.value.map((c) => ({
    label: c.name,
    value: c.id,
  }))
)

onMounted(async () => {
  loadingColleges.value = true
  try {
    colleges.value = await fetchColleges()
  } catch (e) {
    const err = toApiError(e)
    message.error(`加载学院列表失败: ${err.message}`)
  } finally {
    loadingColleges.value = false
  }
})

const columns: DataTableColumns<{ scope: string; doneCount: number; totalCount: number; completionRate: number }> = [
  { title: '范围', key: 'scope' },
  { title: '已完成数', key: 'doneCount', width: 120 },
  { title: '总数', key: 'totalCount', width: 120 },
  { title: '完成率', key: 'completionRate', width: 160 },
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
