<template>
  <PageHeader title="创建活动" description="对接 POST /activities。" />

  <n-card>
    <n-form :model="model" label-placement="top">
      <n-grid :cols="24" :x-gap="16" :y-gap="12">
        <n-form-item-gi :span="12" label="classId">
          <n-input-number v-model:value="model.classId" :min="1" style="width: 100%" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="term">
          <n-input v-model:value="model.term" placeholder="2026-02-23-1" />
        </n-form-item-gi>

        <n-form-item-gi :span="24" label="title">
          <n-input v-model:value="model.title" placeholder="标题" />
        </n-form-item-gi>

        <n-form-item-gi :span="24" label="description">
          <n-input v-model:value="model.description" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="capacity（可选）">
          <n-input-number v-model:value="model.capacity" :min="1" :show-button="true" style="width: 100%" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="requiresReview">
          <n-switch v-model:value="model.requiresReview" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="startsAt（可选）">
          <n-input v-model:value="model.startsAt" placeholder="ISO-8601" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="endsAt（可选）">
          <n-input v-model:value="model.endsAt" placeholder="ISO-8601" />
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
import { NButton, NCard, NForm, NFormItemGi, NGrid, NInput, NInputNumber, NSpace, NSwitch, useMessage } from 'naive-ui'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import PageHeader from '@/components/PageHeader.vue'
import { createActivity } from '@/api/activities'
import { toApiError } from '@/api/errors'

const router = useRouter()
const message = useMessage()

const submitting = ref(false)

const model = reactive({
  classId: 1 as number,
  term: '',
  title: '',
  description: '',
  capacity: null as number | null,
  requiresReview: false,
  startsAt: '' as string,
  endsAt: '' as string,
})

const canSubmit = computed(() => model.classId > 0 && model.term.trim().length > 0 && model.title.trim().length > 0)

async function onSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const res = await createActivity({
      classId: model.classId,
      term: model.term.trim(),
      title: model.title.trim(),
      description: model.description || undefined,
      capacity: model.capacity,
      requiresReview: model.requiresReview,
      startsAt: model.startsAt || null,
      endsAt: model.endsAt || null,
    })

    message.success('创建成功')
    await router.push({ name: 'activities-detail', params: { activityId: res.id } })
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    submitting.value = false
  }
}

async function goBack() {
  await router.push({ name: 'activities' })
}
</script>
