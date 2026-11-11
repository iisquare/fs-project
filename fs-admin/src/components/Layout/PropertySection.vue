<script setup lang="ts">
/**
 * 属性分组 - 抽屉式标题 + 可折叠内容，供各类设计器的属性面板复用。
 *
 * 标题横向铺满属性面板，内容保持 12px 左右留白；分组默认展开，可通过 resetKey 在切换对象时恢复。
 */
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  description?: string
  open?: boolean
  resetKey?: string | number
}>(), {
  open: true,
})

const expanded = ref(props.open)
watch(() => props.resetKey, () => {
  expanded.value = props.open
}, { immediate: true })
</script>

<template>
  <section class="property-section" :class="{ 'is-expanded': expanded }">
    <button
      type="button"
      class="property-section__header"
      :aria-expanded="expanded"
      @click="expanded = !expanded">
      <span class="property-section__chevron"></span>
      <strong class="property-section__title">
        <slot name="title">{{ title }}</slot>
      </strong>
      <small v-if="description">{{ description }}</small>
      <span class="property-section__extra" @click.stop>
        <slot name="extra"></slot>
      </span>
    </button>
    <el-collapse-transition>
      <div v-show="expanded" class="property-section__content">
        <slot></slot>
      </div>
    </el-collapse-transition>
  </section>
</template>

<style lang="scss" scoped>
.property-section {
  /* 抵消属性表单的左右留白，使标题和悬浮背景铺满面板；内容仍单独保留留白 */
  margin: 0 -12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: transparent;

  &__header {
    display: flex;
    align-items: center;
    gap: 7px;
    width: 100%;
    height: 38px;
    padding: 0 12px;
    box-sizing: border-box;
    border: 0;
    color: var(--el-text-color-primary);
    background: transparent;
    font: inherit;
    text-align: left;
    cursor: pointer;

    &:hover {
      color: var(--el-color-primary);
      background: var(--el-fill-color-lighter);
    }

    strong {
      font-size: 13px;
      font-weight: 600;
    }

    small {
      margin-left: 2px;
      color: var(--el-text-color-placeholder);
      font-size: 10px;
    }
  }

  &__chevron {
    flex: none;
    width: 6px;
    height: 6px;
    border-right: 1px solid var(--el-text-color-secondary);
    border-bottom: 1px solid var(--el-text-color-secondary);
    transform: rotate(45deg) translateY(-2px);
    transition: transform 0.18s ease;
  }

  &:not(.is-expanded) &__chevron {
    transform: rotate(-45deg) translateX(-1px);
  }

  &__extra {
    margin-left: auto;
    display: inline-flex;
    align-items: center;
  }

  &__content {
    padding: 2px 12px 12px;
    background: transparent;
  }
}
</style>
