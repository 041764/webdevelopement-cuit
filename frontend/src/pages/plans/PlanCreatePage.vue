<template>
  <PageHeader title="创建计划" description="对接 POST /plans。" />

  <n-card>
    <n-form :model="model" label-placement="top">
      <n-grid :cols="24" :x-gap="16" :y-gap="12">
        <n-form-item-gi :span="12" label="ownerType">
          <n-select v-model:value="model.ownerType" :options="ownerTypeOptions" style="width: 100%" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="ownerClassId（ownerType=CLASS 时必填）">
          <n-input-number v-model:value="model.ownerClassId" :min="1" style="width: 100%" :disabled="model.ownerType !== 'CLASS'" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="term">
          <n-input v-model:value="model.term" placeholder="2026-02-23-1" />
        </n-form-item-gi>

        <n-form-item-gi :span="24" label="title">
          <n-input v-model:value="model.title" />
        </n-form-item-gi>
      </n-grid>

      <n-space justify="end" style="margin-top: var(--s-4)">
        <n-button secondary @click="goBack">取消</n-button>
        <n-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="onSubmit">
          创建
        </n-button>
      </n-space>
    </n-form>
  </n-card>
</template>

<script setup lang="ts">
import { NButton, NCard, NForm, NFormItemGi, NGrid, NInput, NInputNumber, NSelect, NSpace, useMessage } from 'naive-ui'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import PageHeader from '@/components/PageHeader.vue'
import { createPlan } from '@/api/plans'
import { toApiError } from '@/api/errors'

const router = useRouter()
const message = useMessage()

const submitting = ref(false)

const model = reactive({
  ownerType: 'USER' as 'USER' | 'CLASS',
  ownerClassId: null as number | null,
  term: '',
  title: '',
})

const ownerTypeOptions = [
  { label: 'USER', value: 'USER' },
  { label: 'CLASS', value: 'CLASS' },
]

const canSubmit = computed(() => {
  if (model.term.trim().length === 0) return false
  if (model.title.trim().length === 0) return false
  if (model.ownerType === 'CLASS' && !model.ownerClassId) return false
  return true
})

async function onSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const body =
      model.ownerType === 'CLASS'
        ? { ownerType: 'CLASS' as const, ownerClassId: model.ownerClassId as number, term: model.term.trim(), title: model.title.trim() }
        : { ownerType: 'USER' as const, term: model.term.trim(), title: model.title.trim() }

    const res = await createPlan(body)

    message.success('创建成功')
    await router.push({ name: 'plans-detail', params: { planId: res.id } })
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    submitting.value = false
  }
}

async function goBack() {
  await router.push({ name: 'plans' })
}
</script>
