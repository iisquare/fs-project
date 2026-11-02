<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  open: { type: Boolean, default: true },
})

const expanded = ref(props.open)
</script>

<template>
  <section class="property-block" :class="{ 'is-expanded': expanded }">
    <button type="button" class="property-block__header" @click="expanded = !expanded">
      <span class="property-block__chevron"></span>
      <strong>{{ title }}</strong>
      <small v-if="description">{{ description }}</small>
      <span class="property-block__extra" @click.stop><slot name="extra" /></span>
    </button>
    <el-collapse-transition>
      <div v-show="expanded" class="property-block__content">
        <slot />
      </div>
    </el-collapse-transition>
  </section>
</template>

<style lang="scss" scoped>
.property-block {
  border-bottom: 1px solid var(--el-border-color-lighter);
  &__header {
    display: flex;
    align-items: center;
    gap: 7px;
    width: 100%;
    height: 38px;
    padding: 0 12px;
    border: 0;
    color: var(--el-text-color-primary);
    background: transparent;
    font: inherit;
    text-align: left;
    cursor: pointer;
    &:hover {
      background: var(--el-fill-color-lighter);
    }
    strong {
      font-size: 12px;
      font-weight: 600;
    }
    small {
      margin-left: 2px;
      color: var(--el-text-color-placeholder);
      font-size: 10px;
    }
  }
  &__chevron {
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
  }
  &__content {
    padding: 0 12px 12px;
  }
}
</style>
