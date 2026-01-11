<template>
  <n-card class="login-card" size="large" bordered>
    <div data-testid="login-page">
      <div class="login-title">登录</div>
      <n-form :model="model" label-placement="top" size="medium">
        <n-form-item label="身份">
          <n-radio-group v-model:value="model.userType">
            <n-radio-button value="STUDENT">学生</n-radio-button>
            <n-radio-button value="TEACHER">教师</n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="账号（学号/工号）">
          <n-input v-model:value="model.id" placeholder="例如：2020123456" />
        </n-form-item>
        <n-form-item label="密码">
          <n-input v-model:value="model.password" type="password" placeholder="请输入密码" />
        </n-form-item>

        <n-button type="primary" block @click="onSubmit" :disabled="!canSubmit" :loading="loading">
          进入系统
        </n-button>

        <n-space justify="space-between" style="margin-top: var(--s-3);">
          <n-button text @click="goDevHealth">后端健康检查</n-button>
          <n-button text @click="goDevOpenApi">OpenAPI</n-button>
        </n-space>
      </n-form>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { NButton, NCard, NForm, NFormItem, NInput, NRadioButton, NRadioGroup, NSpace, useMessage } from 'naive-ui'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { login } from '@/api/auth'
import type { UserType } from '@/api/schema'
import { toApiError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'
import { useMeStore } from '@/stores/me'
import { deriveClientCredentials } from '@/utils/credentials'
import { getDeviceId } from '@/utils/deviceId'

const router = useRouter()
const message = useMessage()
const auth = useAuthStore()
const meStore = useMeStore()

const loading = ref(false)

const model = reactive<{ userType: UserType; id: string; password: string }>({
  userType: 'STUDENT',
  id: '',
  password: '',
})

const canSubmit = computed(() => model.id.trim().length > 0 && model.password.trim().length > 0)

async function onSubmit() {
  if (!canSubmit.value) return

  loading.value = true
  try {
    const { clientSalt, clientHash } = await deriveClientCredentials(model.userType, model.id, model.password)
    const tokenPair = await login({
      userType: model.userType,
      id: model.id.trim(),
      clientSalt,
      clientHash,
      deviceId: getDeviceId(),
    })

    auth.setTokenPair(tokenPair)
    await meStore.load()

    const redirect = typeof router.currentRoute.value.query.redirect === 'string' ? router.currentRoute.value.query.redirect : '/'
    await router.push(redirect)
  } catch (e) {
    const err = toApiError(e)
    const text = err.code ? `${err.code}: ${err.message}` : err.message
    message.error(text)
  } finally {
    loading.value = false
  }
}

async function goDevHealth() {
  await router.push({ name: 'dev-health' })
}

async function goDevOpenApi() {
  await router.push({ name: 'dev-openapi' })
}
</script>

<style scoped>
.login-card {
  width: min(420px, 100vw);
}

.login-title {
  font-size: 20px;
  font-weight: 650;
  margin-bottom: var(--s-4);
}
</style>
