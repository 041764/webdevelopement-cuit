<template>
  <PageHeader title="创建评价" description="对接 POST /evaluations。" />

  <n-card>
    <n-form :model="model" label-placement="top">
      <n-grid :cols="24" :x-gap="16" :y-gap="12">
        <n-form-item-gi :span="12" label="evaluateeUserId">
          <n-input-number v-model:value="model.evaluateeUserId" :min="1" style="width: 100%" />
        </n-form-item-gi>
        <n-form-item-gi :span="12" label="term">
          <n-input v-model:value="model.term" placeholder="2026-02-23-1" />
        </n-form-item-gi>
        <n-form-item-gi :span="12" label="scoreTotal">
          <n-input-number v-model:value="model.scoreTotal" :min="0" style="width: 100%" />
        </n-form-item-gi>
        <n-form-item-gi :span="24" label="comment（可选）">
          <n-input v-model:value="model.comment" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" />
        </n-form-item-gi>
      </n-grid>

      <n-divider />

      <n-space vertical :size="12">
        <n-space justify="space-between" align="center">
          <div style="font-weight: 650;">明细（可选）</div>
          <n-button secondary size="small" @click="addDetail">新增一行</n-button>
        </n-space>

        <n-empty v-if="details.length === 0" description="暂无明细" />

        <n-grid v-else :cols="24" :x-gap="16" :y-gap="12">
          <template v-for="(d, idx) in details" :key="idx">
            <n-grid-item :span="6">
              <n-input v-model:value="d.itemKey" placeholder="itemKey" />
            </n-grid-item>
            <n-grid-item :span="6">
              <n-input-number v-model:value="d.score" :min="0" style="width: 100%" />
            </n-grid-item>
            <n-grid-item :span="10">
              <n-input v-model:value="d.comment" placeholder="comment（可选）" />
            </n-grid-item>
            <n-grid-item :span="2" style="display: flex; align-items: center; justify-content: flex-end;">
              <n-button size="small" tertiary @click="removeDetail(idx)">删</n-button>
            </n-grid-item>
          </template>
        </n-grid>
      </n-space>

      <n-space justify="end" style="margin-top: var(--s-4)">
        <n-button secondary @click="goBack">取消</n-button>
        <n-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="onSubmit">创建</n-button>
      </n-space>
    </n-form>
  </n-card>
</template>

<script setup lang="ts">
import {
  NButton,
  NCard,
  NDivider,
  NEmpty,
  NForm,
  NFormItemGi,
  NGrid,
  NGridItem,
  NInput,
  NInputNumber,
  NSpace,
  useMessage,
} from 'naive-ui'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import PageHeader from '@/components/PageHeader.vue'
import { toApiError } from '@/api/errors'
import { createEvaluation } from '@/api/evaluations'

const router = useRouter()
const message = useMessage()

const submitting = ref(false)

const model = reactive({
  evaluateeUserId: 1 as number,
  term: '',
  scoreTotal: 0 as number,
  comment: '' as string,
})

type DetailDraft = { itemKey: string; score: number | null; comment: string }
const details = ref<DetailDraft[]>([])

const canSubmit = computed(() => model.evaluateeUserId > 0 && model.term.trim().length > 0)

function addDetail() {
  details.value.push({ itemKey: '', score: 0, comment: '' })
}

function removeDetail(index: number) {
  details.value.splice(index, 1)
}

async function onSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const body = {
      evaluateeUserId: model.evaluateeUserId,
      term: model.term.trim(),
      scoreTotal: model.scoreTotal,
      comment: model.comment.trim().length > 0 ? model.comment.trim() : undefined,
      details:
        details.value.length > 0
          ? details.value
              .filter((d) => d.itemKey.trim().length > 0 && d.score !== null)
              .map((d) => ({
                itemKey: d.itemKey.trim(),
                score: d.score ?? 0,
                comment: d.comment.trim().length > 0 ? d.comment.trim() : undefined,
              }))
          : undefined,
    }

    const res = await createEvaluation(body)
    message.success('创建成功')
    await router.push({ name: 'evaluations-detail', params: { evaluationId: res.id } })
  } catch (e) {
    const err = toApiError(e)
    message.error(err.code ? `${err.code}: ${err.message}` : err.message)
  } finally {
    submitting.value = false
  }
}

async function goBack() {
  await router.push({ name: 'evaluations' })
}
</script>
