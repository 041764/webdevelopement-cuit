<template>
  <PageHeader title="创建活动" description="填写活动信息并创建新活动。" />

  <n-card>
    <n-form :model="model" label-placement="top">
      <n-grid :cols="24" :x-gap="16" :y-gap="12">
        <n-form-item-gi :span="12" label="班级">
          <n-select
            v-model:value="model.classId"
            :options="classOptions"
            :loading="loadingClasses"
            filterable
            placeholder="请选择班级"
            style="width: 100%"
          />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="学期">
          <n-input v-model:value="model.term" placeholder="例如：2026-02-23-1" />
        </n-form-item-gi>

        <n-form-item-gi :span="24" label="标题">
          <n-input v-model:value="model.title" placeholder="请输入活动标题" />
        </n-form-item-gi>

        <n-form-item-gi :span="24" label="描述">
          <n-input v-model:value="model.description" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="容量（可选）">
          <n-input-number v-model:value="model.capacity" :min="1" :show-button="true" style="width: 100%" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="是否需要审核">
          <n-switch v-model:value="model.requiresReview" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="开始时间（可选）">
          <n-input v-model:value="model.startsAt" placeholder="ISO-8601" />
        </n-form-item-gi>

        <n-form-item-gi :span="12" label="结束时间（可选）">
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
import { NButton, NCard, NForm, NFormItemGi, NGrid, NInput, NInputNumber, NSelect, NSpace, NSwitch, useMessage } from 'naive-ui'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import PageHeader from '@/components/PageHeader.vue'
import { createActivity } from '@/api/activities'
import { toApiError } from '@/api/errors'
import { fetchClasses, type ClassOption } from '@/api/lookup'

const router = useRouter()
const message = useMessage()

const submitting = ref(false)
const loadingClasses = ref(false)
const classes = ref<ClassOption[]>([])

const classOptions = computed(() =>
  classes.value.map((c) => ({
    label: `${c.name} (${c.collegeName})`,
    value: c.id,
  }))
)

const model = reactive({
  classId: null as number | null,
  term: '',
  title: '',
  description: '',
  capacity: null as number | null,
  requiresReview: false,
  startsAt: '' as string,
  endsAt: '' as string,
})

const canSubmit = computed(() => model.classId !== null && model.classId > 0 && model.term.trim().length > 0 && model.title.trim().length > 0)

onMounted(async () => {
  loadingClasses.value = true
  try {
    classes.value = await fetchClasses()
  } catch (e) {
    const err = toApiError(e)
    message.error(`加载班级列表失败: ${err.message}`)
  } finally {
    loadingClasses.value = false
  }
})

async function onSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const res = await createActivity({
      classId: model.classId as number,
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
