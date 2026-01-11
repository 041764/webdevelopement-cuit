<template>
  <PageHeader title="用户 CSV 导入" description="对接 POST /users/import（multipart/form-data）。" />

  <n-card>
    <n-space vertical :size="16">
      <n-space wrap :size="12" align="center">
        <n-select v-model:value="userType" :options="userTypeOptions" style="width: 180px" />
        <n-upload :max="1" :default-upload="false" accept=".csv" @update:file-list="onFileListUpdate">
          <n-button secondary>选择 CSV</n-button>
        </n-upload>
        <n-button type="primary" :loading="uploading" :disabled="!canSubmit" @click="onSubmit">导入</n-button>
      </n-space>

      <n-alert type="info" :show-icon="true">
        CSV 为 UTF-8 逗号分隔，可包含表头，字段：id,name,collegeName
      </n-alert>

      <AsyncState :loading="loading" :error="error" :empty="result === null" @retry="onSubmit">
        <n-grid :cols="24" :x-gap="16" :y-gap="16">
          <n-grid-item :span="8"><n-statistic label="created" :value="result?.created ?? 0" /></n-grid-item>
          <n-grid-item :span="8"><n-statistic label="updated" :value="result?.updated ?? 0" /></n-grid-item>
          <n-grid-item :span="8"><n-statistic label="failed" :value="result?.failed ?? 0" /></n-grid-item>
        </n-grid>

        <div style="margin-top: var(--s-4)">
          <n-data-table v-if="(result?.failures?.length ?? 0) > 0" :columns="columns" :data="result?.failures ?? []" :bordered="false" />
        </div>
      </AsyncState>
    </n-space>
  </n-card>
</template>

<script setup lang="ts">
import type { DataTableColumns, UploadFileInfo } from 'naive-ui'

import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NGrid,
  NGridItem,
  NSelect,
  NSpace,
  NStatistic,
  NUpload,
  useMessage,
} from 'naive-ui'
import { computed, ref } from 'vue'

import AsyncState from '@/components/AsyncState.vue'
import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { importUsersCsv } from '@/api/users'
import type { ImportUsersResult, UserType } from '@/api/schema'

const message = useMessage()

const userType = ref<UserType>('STUDENT')
const userTypeOptions = [
  { label: 'STUDENT', value: 'STUDENT' },
  { label: 'TEACHER', value: 'TEACHER' },
]

const file = ref<File | null>(null)

const uploading = ref(false)
const loading = ref(false)
const error = ref<unknown>(null)
const result = ref<ImportUsersResult | null>(null)

const canSubmit = computed(() => file.value !== null)

const columns: DataTableColumns<{ row: number; reason: string }> = [
  { title: 'row', key: 'row', width: 120 },
  { title: 'reason', key: 'reason' },
]

function onFileListUpdate(files: UploadFileInfo[]) {
  const first = files[0]
  const raw = first?.file
  file.value = raw ?? null
}

async function onSubmit() {
  if (!file.value) return

  uploading.value = true
  loading.value = true
  error.value = null
  try {
    result.value = await importUsersCsv({ userType: userType.value }, file.value)
    message.success('导入完成')
  } catch (e) {
    error.value = e
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    uploading.value = false
    loading.value = false
  }
}
</script>
