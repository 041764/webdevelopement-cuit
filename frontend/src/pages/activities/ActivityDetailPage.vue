<template>
  <PageHeader title="活动详情" description="查看活动详细信息并进行报名管理。">
    <template #actions>
      <n-button secondary @click="goSignups">报名列表</n-button>
    </template>
  </PageHeader>

  <n-card>
    <AsyncState :loading="loading" :error="error" :empty="!activity" @retry="fetchDetail">
      <n-descriptions :column="2" bordered>
        <n-descriptions-item label="ID">{{ activity?.id }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ activity?.status }}</n-descriptions-item>
        <n-descriptions-item label="标题">{{ activity?.title }}</n-descriptions-item>
        <n-descriptions-item label="学期">{{ activity?.term }}</n-descriptions-item>
        <n-descriptions-item label="班级">{{ activity?.className || `班级ID: ${activity?.classId}` }}</n-descriptions-item>
        <n-descriptions-item label="容量">{{ activity?.capacity ?? '∞' }}</n-descriptions-item>
        <n-descriptions-item label="需要审核">{{ activity?.requiresReview }}</n-descriptions-item>
        <n-descriptions-item label="创建时间">{{ activity?.createdAt }}</n-descriptions-item>
        <n-descriptions-item label="描述" :span="2">
          {{ activity?.description || '-' }}
        </n-descriptions-item>
      </n-descriptions>

      <n-space wrap style="margin-top: var(--s-4)" :size="12" align="center">
        <n-button secondary :loading="publishing" @click="onPublish" :disabled="activity?.status !== 'DRAFT'">
          发布
        </n-button>

        <n-input v-model:value="signupNote" placeholder="报名备注（可选）" clearable style="width: 240px" />
        <n-button type="primary" :loading="signing" @click="onSignup">报名</n-button>
        <n-button secondary :loading="canceling" @click="onCancel">取消报名</n-button>
      </n-space>
    </AsyncState>
  </n-card>
</template>

<script setup lang="ts">
import {
  NButton,
  NCard,
  NDescriptions,
  NDescriptionsItem,
  NInput,
  NSpace,
  useMessage,
} from 'naive-ui'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { cancelMySignup, getActivity, publishActivity, signupActivity } from '@/api/activities'
import { toApiError } from '@/api/errors'
import type { Activity } from '@/api/schema'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(false)
const error = ref<unknown>(null)
const activity = ref<Activity | null>(null)

const publishing = ref(false)
const signing = ref(false)
const canceling = ref(false)

const signupNote = ref('')

function readId(): number {
  const raw = route.params.activityId
  const value = typeof raw === 'string' ? Number(raw) : Number(String(raw))
  return value
}

async function fetchDetail() {
  loading.value = true
  error.value = null
  try {
    activity.value = await getActivity(readId())
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    loading.value = false
  }
}

async function onPublish() {
  if (!activity.value) return
  publishing.value = true
  try {
    await publishActivity(activity.value.id)
    message.success('已发布')
    await fetchDetail()
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    publishing.value = false
  }
}

async function onSignup() {
  if (!activity.value) return
  signing.value = true
  try {
    await signupActivity(activity.value.id, signupNote.value.trim().length > 0 ? signupNote.value.trim() : undefined)
    signupNote.value = ''
    message.success('报名成功')
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    signing.value = false
  }
}

async function onCancel() {
  if (!activity.value) return
  canceling.value = true
  try {
    await cancelMySignup(activity.value.id)
    message.success('已取消')
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    canceling.value = false
  }
}

async function goSignups() {
  await router.push({ name: 'activities-signups', params: { activityId: readId() } })
}

onMounted(() => {
  void fetchDetail()
})
</script>
