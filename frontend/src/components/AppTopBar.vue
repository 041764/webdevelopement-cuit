<template>
  <div class="topbar">
    <n-button quaternary class="topbar__icon" @click="$emit('toggleNav')">菜单</n-button>
    <div class="topbar__title">班导师管理系统</div>

    <div class="topbar__spacer" />

    <div v-if="isAuthenticated" class="topbar__user">{{ me?.name ?? '-' }}</div>
    <n-button v-if="isAuthenticated" secondary @click="onLogout" :loading="loggingOut">退出</n-button>
  </div>
</template>

<script setup lang="ts">
import { NButton, useMessage } from 'naive-ui'
import { storeToRefs } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { logout } from '@/api/auth'
import { toApiError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'
import { useMeStore } from '@/stores/me'

defineEmits<{ toggleNav: [] }>()

const router = useRouter()
const message = useMessage()

const auth = useAuthStore()
const meStore = useMeStore()

const { isAuthenticated } = storeToRefs(auth)
const { me } = storeToRefs(meStore)

const loggingOut = ref(false)

async function onLogout() {
  if (loggingOut.value) return
  loggingOut.value = true

  const refreshToken = auth.tokenPair?.refreshToken
  auth.clear()
  meStore.clear()

  if (refreshToken) {
    try {
      await logout({ refreshToken })
    } catch (e) {
      const err = toApiError(e)
      message.warning(err.code ? `${err.code}: ${err.message}` : err.message)
    }
  }

  await router.push({ name: 'login' })
  loggingOut.value = false
}
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  gap: var(--s-3);
  width: 100%;
}

.topbar__title {
  font-family: var(--font-display);
  font-weight: 720;
  letter-spacing: 0.2px;
}

.topbar__spacer {
  flex: 1;
}

@media (min-width: 960px) {
  .topbar__icon {
    display: none;
  }
}
</style>
