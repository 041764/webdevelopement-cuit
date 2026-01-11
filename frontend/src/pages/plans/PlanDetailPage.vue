<template>
  <PageHeader title="计划详情" description="对接 /plans/{id} + items 操作。" />

  <n-card>
    <AsyncState :loading="loading" :error="error" :empty="!plan" @retry="fetchDetail">
      <n-descriptions :column="2" bordered>
        <n-descriptions-item label="ID">{{ plan?.id }}</n-descriptions-item>
        <n-descriptions-item label="ownerType">{{ plan?.ownerType }}</n-descriptions-item>
        <n-descriptions-item label="title">{{ plan?.title }}</n-descriptions-item>
        <n-descriptions-item label="term">{{ plan?.term }}</n-descriptions-item>
        <n-descriptions-item label="progress" :span="2">
          {{ progressText }}
        </n-descriptions-item>
      </n-descriptions>

      <n-divider />

      <n-space wrap :size="12" align="center">
        <n-input v-model:value="newTitle" placeholder="新增条目标题" style="width: 260px" />
        <n-input v-model:value="newDueDate" placeholder="dueDate（YYYY-MM-DD，可选）" style="width: 220px" clearable />
        <n-button type="primary" :loading="adding" :disabled="newTitle.trim().length === 0" @click="onAddItem">新增条目</n-button>
      </n-space>

      <div style="margin-top: var(--s-4)">
        <n-empty v-if="(plan?.items.length ?? 0) === 0" description="暂无条目" />

        <n-list v-else bordered>
          <n-list-item v-for="item in plan?.items" :key="item.id">
            <n-thing :title="item.title" :description="`status=${item.status}  dueDate=${item.dueDate ?? '-'}`">
              <template #action>
                <n-space :size="8">
                  <n-select
                    v-model:value="statusDraft[item.id]"
                    :options="statusOptions"
                    style="width: 120px"
                    size="small"
                    @update:value="(v) => onChangeStatus(item.id, v)"
                  />
                  <n-button size="small" secondary @click="onEdit(item.id)">编辑</n-button>
                  <n-button size="small" secondary @click="goProgress(item.id)">进度</n-button>
                  <n-button size="small" secondary :loading="deletingId === item.id" @click="onDelete(item.id)">删除</n-button>
                </n-space>
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </div>
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import {
  NButton,
  NCard,
  NDescriptions,
  NDescriptionsItem,
  NDivider,
  NEmpty,
  NInput,
  NList,
  NListItem,
  NSelect,
  NSpace,
  NThing,
  useDialog,
  useMessage,
} from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { addPlanItem, deletePlanItem, getPlan, updatePlanItem } from '@/api/plans'
import { toApiError } from '@/api/errors'
import type { PlanDetail, PlanItem, PlanItemStatus } from '@/api/schema'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const error = ref<unknown>(null)
const plan = ref<PlanDetail | null>(null)

const adding = ref(false)
const deletingId = ref<number | null>(null)

const newTitle = ref('')
const newDueDate = ref('')

const statusOptions = [
  { label: 'todo', value: 'todo' },
  { label: 'doing', value: 'doing' },
  { label: 'done', value: 'done' },
]

const statusDraft = reactive<Record<number, PlanItemStatus>>({})

function readPlanId(): number {
  const raw = route.params.planId
  const value = typeof raw === 'string' ? Number(raw) : Number(String(raw))
  return value
}

const progressText = computed(() => {
  if (!plan.value?.progress) return '-'
  const p = plan.value.progress
  const rate = Number.isFinite(p.completionRate) ? `${Math.round(p.completionRate * 100)}%` : '-'
  return `${p.doneCount}/${p.totalCount} (${rate})`
})

async function fetchDetail() {
  loading.value = true
  error.value = null
  try {
    plan.value = await getPlan(readPlanId())
    for (const item of plan.value.items) {
      statusDraft[item.id] = item.status
    }
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    loading.value = false
  }
}

async function onAddItem() {
  if (!plan.value) return
  if (newTitle.value.trim().length === 0) return

  adding.value = true
  try {
    await addPlanItem(plan.value.id, {
      title: newTitle.value.trim(),
      dueDate: newDueDate.value.trim().length > 0 ? newDueDate.value.trim() : null,
    })
    newTitle.value = ''
    newDueDate.value = ''
    await fetchDetail()
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    adding.value = false
  }
}

async function onChangeStatus(itemId: number, status: PlanItemStatus) {
  if (!plan.value) return

  try {
    await updatePlanItem(plan.value.id, itemId, { status })
    await fetchDetail()
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
    await fetchDetail()
  }
}

async function onEdit(itemId: number) {
  if (!plan.value) return

  const item = (plan.value.items as PlanItem[]).find((x) => x.id === itemId)
  if (!item) return

  const title = ref(item.title)
  const dueDate = ref(item.dueDate ?? '')

  dialog.info({
    title: '编辑条目',
    content: () =>
      h('div', { style: { display: 'flex', flexDirection: 'column', gap: '12px' } }, [
        h(NInput, {
          value: title.value,
          placeholder: '标题',
          onUpdateValue: (v) => {
            title.value = v
          },
        }),
        h(NInput, {
          value: dueDate.value,
          placeholder: 'dueDate（YYYY-MM-DD，可选）',
          clearable: true,
          onUpdateValue: (v) => {
            dueDate.value = v
          },
        }),
      ]),
    positiveText: '保存',
    negativeText: '取消',
    onPositiveClick: async () => {
      if (!plan.value) return

      try {
        await updatePlanItem(plan.value.id, itemId, {
          title: title.value.trim().length > 0 ? title.value.trim() : item.title,
          dueDate: dueDate.value.trim().length > 0 ? dueDate.value.trim() : null,
        })
        await fetchDetail()
      } catch (e) {
        const err = toApiError(e)
        message.error(err.code ? `${err.code}: ${err.message}` : err.message)
        await fetchDetail()
      }
    },
  })
}

async function onDelete(itemId: number) {
  if (!plan.value) return

  deletingId.value = itemId
  try {
    await deletePlanItem(plan.value.id, itemId)
    await fetchDetail()
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    deletingId.value = null
  }
}

async function goProgress(itemId: number) {
  await router.push({ name: 'plan-item-progress', params: { itemId } })
}

onMounted(() => {
  void fetchDetail()
})
</script>
