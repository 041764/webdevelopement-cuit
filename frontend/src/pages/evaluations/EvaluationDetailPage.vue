<template>
  <PageHeader title="评价详情" description="查看评价详细信息。" />

  <n-card>
    <AsyncState :loading="loading" :error="error" :empty="!evaluation" @retry="fetchDetail">
      <n-descriptions :column="2" bordered>
        <n-descriptions-item label="ID">{{ evaluation?.id }}</n-descriptions-item>
        <n-descriptions-item label="学期">{{ evaluation?.term }}</n-descriptions-item>
        <n-descriptions-item label="评价人">{{ evaluation?.evaluatorUserName || evaluation?.evaluatorUserNo || evaluation?.evaluatorUserId }}</n-descriptions-item>
        <n-descriptions-item label="被评价人">{{ evaluation?.evaluateeUserName || evaluation?.evaluateeUserNo || evaluation?.evaluateeUserId }}</n-descriptions-item>
        <n-descriptions-item label="总分">{{ evaluation?.scoreTotal }}</n-descriptions-item>
        <n-descriptions-item label="创建时间">{{ evaluation?.createdAt }}</n-descriptions-item>
        <n-descriptions-item label="评语" :span="2">{{ evaluation?.comment || '-' }}</n-descriptions-item>
      </n-descriptions>

      <div style="margin-top: var(--s-4)">
        <n-data-table :columns="columns" :data="evaluation?.details ?? []" :bordered="false" />
      </div>
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'

import { NCard, NDataTable, NDescriptions, NDescriptionsItem, useMessage } from 'naive-ui'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { getEvaluation } from '@/api/evaluations'
import type { EvaluationDetail, EvaluationDetailItem } from '@/api/schema'

const route = useRoute()
const message = useMessage()

const loading = ref(false)
const error = ref<unknown>(null)
const evaluation = ref<EvaluationDetail | null>(null)

const columns: DataTableColumns<EvaluationDetailItem> = [
  { title: '评价项', key: 'itemKey', width: 180 },
  { title: '分数', key: 'score', width: 120 },
  { title: '评语', key: 'comment' },
]

function readId(): number {
  const raw = route.params.evaluationId
  const value = typeof raw === 'string' ? Number(raw) : Number(String(raw))
  return value
}

async function fetchDetail() {
  loading.value = true
  error.value = null
  try {
    evaluation.value = await getEvaluation(readId())
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchDetail()
})
</script>
