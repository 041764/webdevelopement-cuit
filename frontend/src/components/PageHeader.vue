<template>
  <div class="page-header">
    <div class="page-header__left">
      <div class="page-header__title">{{ title }}</div>
      <div v-if="description" class="page-header__description">{{ description }}</div>
    </div>
    <div class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; description?: string }>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--s-4);
  margin-bottom: var(--s-4);
}

.page-header__title {
  font-size: 20px;
  font-weight: 650;
  letter-spacing: 0.2px;
}

.page-header__description {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.page-header__actions {
  display: flex;
  gap: var(--s-2);
}
</style>
