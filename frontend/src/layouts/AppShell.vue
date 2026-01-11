<template>
  <n-layout has-sider>
    <n-layout-sider
      v-if="!isMobile"
      class="app-sider"
      bordered
      collapse-mode="width"
      :collapsed-width="76"
      :width="264"
      show-trigger
    >
      <AppSideNav />
    </n-layout-sider>

    <n-layout>
      <n-layout-header class="app-header">
        <AppTopBar @toggleNav="navOpen = true" />
      </n-layout-header>

      <n-layout-content class="app-content">
        <router-view />
      </n-layout-content>
    </n-layout>

    <n-drawer v-if="isMobile" v-model:show="navOpen" placement="left" :width="300">
      <n-drawer-content body-content-style="padding: 12px;">
        <AppSideNav @navigate="navOpen = false" />
      </n-drawer-content>
    </n-drawer>
  </n-layout>
</template>

<script setup lang="ts">
import { NDrawer, NDrawerContent, NLayout, NLayoutContent, NLayoutHeader, NLayoutSider } from 'naive-ui'
import { onMounted, ref } from 'vue'

import AppSideNav from '@/components/AppSideNav.vue'
import AppTopBar from '@/components/AppTopBar.vue'
import { useViewport } from '@/composables/useViewport'
import { useAuthStore } from '@/stores/auth'
import { useMeStore } from '@/stores/me'

const navOpen = ref(false)
const { isMobile } = useViewport()

const auth = useAuthStore()
const meStore = useMeStore()

onMounted(() => {
  if (auth.isAuthenticated) {
    void meStore.load()
  }
})
</script>
