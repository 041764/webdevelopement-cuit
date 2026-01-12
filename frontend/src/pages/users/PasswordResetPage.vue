<template>
  <PageHeader title="重置用户密码" description="管理员重置用户密码。" />

  <n-card>
    <n-form :model="model" label-placement="top">
      <n-grid :cols="24" :x-gap="16" :y-gap="12">
        <n-form-item-gi :span="12" label="用户类型">
          <n-select v-model:value="model.userType" :options="userTypeOptions" style="width: 100%" />
        </n-form-item-gi>
        <n-form-item-gi :span="12" label="账号（学号/工号）">
          <n-input v-model:value="model.userNo" placeholder="例如：2020123456" />
        </n-form-item-gi>
        <n-form-item-gi :span="12" label="新密码">
          <n-input v-model:value="model.password" type="password" placeholder="请输入新密码" />
        </n-form-item-gi>
      </n-grid>

      <n-space justify="end" style="margin-top: var(--s-4)">
        <n-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="onSubmit">重置</n-button>
      </n-space>
    </n-form>
  </n-card>
</template>

<script setup lang="ts">
import {
  NButton,
  NCard,
  NForm,
  NFormItemGi,
  NGrid,
  NInput,
  NSelect,
  NSpace,
  useMessage,
} from 'naive-ui'
import { computed, reactive, ref } from 'vue'

import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { resetUserPasswordByNo } from '@/api/users'
import type { UserType } from '@/api/schema'
import { deriveClientCredentials } from '@/utils/credentials'

const message = useMessage()

const submitting = ref(false)

const model = reactive({
  userType: 'STUDENT' as UserType,
  userNo: '',
  password: '',
})

const userTypeOptions = [
  { label: '学生', value: 'STUDENT' },
  { label: '教师', value: 'TEACHER' },
]

const canSubmit = computed(() => model.userNo.trim().length > 0 && model.password.trim().length > 0)

async function onSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const { clientSalt, clientHash } = await deriveClientCredentials(model.userType, model.userNo, model.password)
    await resetUserPasswordByNo({
      userType: model.userType,
      userNo: model.userNo,
      clientSalt,
      clientHash,
    })
    message.success('已重置')
    model.password = ''
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    submitting.value = false
  }
}
</script>
