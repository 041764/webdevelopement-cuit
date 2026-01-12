<template>
  <PageHeader title="报名列表" description="查看和审核活动报名。" />

  <n-card>
    <n-space wrap :size="12" align="center">
      <n-select v-model:value="status" :options="statusOptions" clearable placeholder="状态" style="width: 220px" />
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

import { NButton, NCard, NDataTable, NInput, NPagination, NSelect, NSpace, useDialog, useMessage } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { approveSignup, listActivitySignups, rejectSignup } from '@/api/activities'
import { toApiError } from '@/api/errors'
import type { ActivitySignup, SignupStatus } from '@/api/schema'

const route = useRoute()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const error = ref<unknown>(null)
const items = ref<ActivitySignup[]>([])
const total = ref(0)

const status = ref<SignupStatus | null>(null)
const page = ref(1)
const size = ref(20)

const statusOptions = [
  { label: '已申请', value: 'APPLIED' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELED' },
]

function readActivityId(): number {
  const raw = route.params.activityId
  const value = typeof raw === 'string' ? Number(raw) : Number(String(raw))
  return value
}

const columns: DataTableColumns<ActivitySignup> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '学号', key: 'userNo', width: 120 },
  { title: '姓名', key: 'userName', width: 120 },
  { title: '状态', key: 'status', width: 140 },
  { title: '创建时间', key: 'createdAt' },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    render: (row) =>
      h('div', { style: { display: 'flex', gap: '8px' } }, [
        h(
          NButton,
          {
            size: 'small',
            type: 'primary',
            disabled: row.status !== 'APPLIED',
            onClick: () => void onApprove(row.id),
          },
          { default: () => '通过' },
        ),
        h(
          NButton,
          {
            size: 'small',
            secondary: true,
            disabled: row.status !== 'APPLIED',
            onClick: () => void onReject(row.id),
          },
          { default: () => '拒绝' },
        ),
      ]),
  },
]

async function fetchList() {
  loading.value = true
  error.value = null

  try {
    const res = await listActivitySignups(readActivityId(), {
      page: page.value,
      size: size.value,
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

async function onApprove(signupId: number) {
  try {
    await approveSignup(readActivityId(), signupId)
    message.success('已通过')
    await fetchList()
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  }
}

async function onReject(signupId: number) {
  const reason = ref('')

  dialog.warning({
    title: '拒绝报名',
    content: () =>
      h('div', { style: { display: 'flex', flexDirection: 'column', gap: '8px' } }, [
        h('div', '确认拒绝该报名？'),
        h(NInput, {
          value: reason.value,
          placeholder: '拒绝原因（可选）',
          clearable: true,
          onUpdateValue: (v) => {
            reason.value = v
          },
        }),
      ]),
    positiveText: '拒绝',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await rejectSignup(readActivityId(), signupId, reason.value.trim().length > 0 ? reason.value.trim() : undefined)
        message.success('已拒绝')
        await fetchList()
      } catch (e) {
        const err = toApiError(e)
        message.error(err.code ? `${err.code}: ${err.message}` : err.message)
      }
    },
  })
}

onMounted(() => {
  void fetchList()
})
</script>
