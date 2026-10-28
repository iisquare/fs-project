<script setup lang="ts">
import LayoutBack from '@/components/Layout/LayoutBack.vue'

const props = withDefaults(defineProps<{
  title: string
  description?: string
  back?: string | Record<string, any>
  backLabel?: string
  sticky?: boolean
}>(), {
  description: '',
  back: '',
  backLabel: '返回',
  sticky: false,
})
</script>

<template>
  <section class="govern-page">
    <header :class="['govern-page__header', { 'govern-page__header--sticky': props.sticky }]">
      <LayoutBack
        v-if="back"
        :to="back"
        :title="backLabel"
        variant="text"
        :label="backLabel"
        :aria-label="backLabel"
      />
      <div class="govern-page__titles">
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="govern-page__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="govern-page__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.govern-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  padding-bottom: 24px;
}
.govern-page__header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}
.govern-page__header--sticky {
  position: sticky;
  top: -20px;
  z-index: 20;
  margin: -20px -20px 0;
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: color-mix(in srgb, var(--fs-layout-background-color) 94%, transparent);
  backdrop-filter: blur(8px);
}
.govern-page__titles {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}
.govern-page h2 {
  color: var(--el-text-color-primary);
  font-size: 20px;
  font-weight: 650;
  line-height: 28px;
}
.govern-page p {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}
.govern-page__actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 8px;
  padding-top: 15px;
}
.govern-page__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.govern-page__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.govern-page__body > * {
  margin-top: 0 !important;
  margin-bottom: 0 !important;
}
.govern-page__body :deep(.fs-table-search) {
  margin-bottom: 0;
}
.govern-page__body :deep(.el-card) {
  border-color: var(--el-border-color-lighter);
  border-radius: 10px;
}
.govern-page__body :deep(.el-card__header) {
  padding: 14px 18px;
}
.govern-page__body :deep(.search-card .el-card__body) {
  padding: 16px 18px;
}
.govern-page__body :deep(.search-card .el-form-item) {
  margin-bottom: 0;
}
.govern-page__body :deep(.fs-table-toolbar) {
  gap: 12px;
  min-height: 58px;
  height: auto;
  padding: 10px 0;
}
.govern-page__body :deep(.fs-table-toolbar > .el-space) {
  flex-wrap: wrap;
}
.govern-page__body :deep(.fs-table-card .el-card__body) {
  padding-bottom: 16px;
}
.govern-page__body :deep(.el-table) {
  border-radius: 7px;
}
.govern-page__body :deep(.el-descriptions__label) {
  color: var(--el-text-color-secondary);
  font-weight: 500;
}
@media (max-width: 900px) {
  .govern-page__header {
    flex-wrap: wrap;
  }
  .govern-page__actions {
    width: 100%;
    padding-top: 0;
    flex-wrap: wrap;
    justify-content: flex-end;
  }
  .govern-page__body :deep(.search-card .el-col-6) {
    max-width: 50%;
    flex: 0 0 50%;
  }
}
@media (max-width: 640px) {
  .govern-page__body :deep(.search-card .el-col-6) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
@media (max-width: 900px) {
  .govern-page__header--sticky {
    top: -20px;
    margin: -20px -20px 0;
    padding: 14px 20px 11px;
  }
}
</style>
