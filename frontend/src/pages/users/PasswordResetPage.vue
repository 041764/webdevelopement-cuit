<template>
  <PageHeader title="重置用户密码" description="对接 POST /users/{userId}/password:reset。" />

  <n-card>
    <n-form :model="model" label-placement="top">
      <n-grid :cols="24" :x-gap="16" :y-gap="12">
        <n-form-item-gi :span="12" label="userId">
          <n-input-number v-model:value="model.userId" :min="1" style="width: 100%" />
        </n-form-item-gi>
        <n-form-item-gi :span="12" label="userType">
          <n-select v-model:value="model.userType" :options="userTypeOptions" style="width: 100%" />
        </n-form-item-gi>
        <n-form-item-gi :span="12" label="账号（id=学号/工号）">
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
  NInputNumber,
  NSelect,
  NSpace,
  useMessage,
} from 'naive-ui'
import { computed, reactive, ref } from 'vue'

import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { resetUserPassword } from '@/api/users'
import type { UserType } from '@/api/schema'
import { deriveClientCredentials } from '@/utils/credentials'

const message = useMessage()

const submitting = ref(false)

const model = reactive({
  userId: 1 as number,
  userType: 'STUDENT' as UserType,
  userNo: '',
  password: '',
})

const userTypeOptions = [
  { label: 'STUDENT', value: 'STUDENT' },
  { label: 'TEACHER', value: 'TEACHER' },
]

const canSubmit = computed(() => model.userId > 0 && model.userNo.trim().length > 0 && model.password.trim().length > 0)

async function onSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const { clientSalt, clientHash } = await deriveClientCredentials(model.userType, model.userNo, model.password)
    await resetUserPassword(model.userId, { clientSalt, clientHash })
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
