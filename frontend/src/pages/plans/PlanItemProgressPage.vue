<template>
  <PageHeader title="条目进度" description="对接 /plan-items/{itemId}/progress。" />

  <n-card>
    <n-space wrap :size="12" align="center">
      <n-input-number v-model:value="percent" :min="0" :max="100" style="width: 180px" />
      <n-input v-model:value="note" placeholder="note（可选）" style="width: 320px" clearable />
      <n-button type="primary" :loading="adding" @click="onAdd">追加</n-button>
      <n-button secondary @click="fetchList">刷新</n-button>
    </n-space>
  </n-card>

  <n-card style="margin-top: var(--s-4)">
    <AsyncState :loading="loading" :error="error" :empty="items.length === 0" @retry="fetchList">
      <n-data-table :columns="columns" :data="items" :bordered="false" />
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'

import { NButton, NCard, NDataTable, NInput, NInputNumber, NSpace, useMessage } from 'naive-ui'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { addPlanItemProgress, listPlanItemProgress } from '@/api/plans'
import { toApiError } from '@/api/errors'
import type { PlanItemProgress } from '@/api/schema'

const route = useRoute()
const message = useMessage()

const loading = ref(false)
const adding = ref(false)
const error = ref<unknown>(null)

const items = ref<PlanItemProgress[]>([])

const percent = ref<number | null>(0)
const note = ref('')

const columns: DataTableColumns<PlanItemProgress> = [
  { title: 'id', key: 'id', width: 80 },
  { title: 'percent', key: 'percent', width: 100 },
  { title: 'note', key: 'note' },
  { title: 'createdAt', key: 'createdAt' },
]

function readItemId(): number {
  const raw = route.params.itemId
  const value = typeof raw === 'string' ? Number(raw) : Number(String(raw))
  return value
}

async function fetchList() {
  loading.value = true
  error.value = null

  try {
    items.value = await listPlanItemProgress(readItemId())
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    loading.value = false
  }
}

async function onAdd() {
  if (percent.value === null) return
  if (percent.value < 0 || percent.value > 100) return

  adding.value = true
  try {
    await addPlanItemProgress(readItemId(), {
      percent: percent.value,
      note: note.value.trim().length > 0 ? note.value.trim() : null,
    })
    note.value = ''
    await fetchList()
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    adding.value = false
  }
}

onMounted(() => {
  void fetchList()
})
</script>
