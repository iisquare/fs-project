<script setup lang="ts">
const props = defineProps({
  title: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
});
</script>

<template>
  <div class="fs-heading">
    <span class="fs-heading__title">{{ props.title }}</span>
    <span class="fs-heading__description">{{ props.description }}</span>
    <div class="fs-heading__extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.fs-heading {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 15px;
  &__title {
    display: inline-flex; align-items: center; gap: 10px;
    font-size: 15px; font-weight: 600;
    color: var(--el-text-color-primary);
    &::before {
      content: ''; display: block;
      width: 3px; height: 18px; border-radius: 2px; flex-shrink: 0;
      background: var(--el-color-primary);
    }
  }
  &__extra {
    margin-left: auto;
  }
  &__description {
    font-weight: 400; font-size: 12px; color: var(--el-text-color-placeholder);
  }
}
</style>
