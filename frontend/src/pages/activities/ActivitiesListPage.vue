<template>
  <PageHeader title="活动" description="对接 /activities 列表与筛选。">
    <template #actions>
      <n-button type="primary" @click="goCreate">创建活动</n-button>
    </template>
  </PageHeader>

  <n-card>
    <n-space wrap :size="12" align="center">
      <n-input v-model:value="term" placeholder="term（可选）" clearable style="width: 220px" />
      <n-select v-model:value="status" :options="statusOptions" clearable placeholder="状态" style="width: 180px" />
      <n-button secondary @click="fetchList">查询</n-button>
    </n-space>
  </n-card>

  <n-card style="margin-top: var(--s-4)">
    <AsyncState :loading="loading" :error="error" :empty="items.length === 0" @retry="fetchList">
      <n-data-table :columns="columns" :data="items" :bordered="false" />
      <div style="margin-top: var(--s-3); display: flex; justify-content: flex-end;">
        <n-pagination v-model:page="page" v-model:page-size="size" :item-count="total" @update:page="fetchList" />
      </div>
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'

import { NButton, NCard, NDataTable, NInput, NPagination, NSelect, NSpace, useMessage } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { listActivities } from '@/api/activities'
import type { Activity, ActivityStatus } from '@/api/schema'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const error = ref<unknown>(null)
const items = ref<Activity[]>([])
const total = ref(0)

const term = ref<string | null>(null)
const status = ref<ActivityStatus | null>(null)

const page = ref(1)
const size = ref(20)

const statusOptions = [
  { label: 'DRAFT', value: 'DRAFT' },
  { label: 'PUBLISHED', value: 'PUBLISHED' },
  { label: 'CLOSED', value: 'CLOSED' },
]

const columns: DataTableColumns<Activity> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '标题', key: 'title' },
  { title: '学期', key: 'term', width: 160 },
  { title: '状态', key: 'status', width: 120 },
  { title: '容量', key: 'capacity', width: 100 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (row) =>
      h(
        NButton,
        {
          size: 'small',
          secondary: true,
          onClick: () => {
            void router.push({ name: 'activities-detail', params: { activityId: row.id } })
          },
        },
        { default: () => '详情' },
      ),
  },
]

async function fetchList() {
  loading.value = true
  error.value = null

  try {
    const res = await listActivities({
      page: page.value,
      size: size.value,
      term: term.value || undefined,
      status: status.value || undefined,
    })
    items.value = res.items
    total.value = res.total
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    loading.value = false
  }
}

async function goCreate() {
  await router.push({ name: 'activities-new' })
}

onMounted(() => {
  void fetchList()
})
</script>
