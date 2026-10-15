<script setup lang="ts">
/**
 * 本地访问策略图标 - 盾牌打底，按当前策略换标记与配色，一眼看出松紧：
 * ask 待确认（感叹号 / 琥珀）、auto 直接放行（对勾 / 绿）、deny 一律拒绝（叉 / 红）、
 * full 完全放行（双层盾 / 主色）。
 */
defineProps<{
  mode: string
}>()
</script>

<template>
  <svg
    class="policy-icon"
    :class="`is-${mode}`"
    viewBox="0 0 16 16"
    width="14"
    height="14"
    aria-hidden="true"
  >
    <path
      d="M8 1.7 13 3.5v4.2c0 3.1-2.1 5.5-5 6.8-2.9-1.3-5-3.7-5-6.8V3.5L8 1.7Z"
      fill="none"
      stroke="currentColor"
      stroke-width="1.2"
      stroke-linejoin="round" />
    <template v-if="'ask' === mode">
      <path
        d="M8 5.4v2.9"
        stroke="currentColor"
        stroke-width="1.3"
        stroke-linecap="round" />
      <circle cx="8" cy="10.5" r="0.8" fill="currentColor" />
    </template>
    <path
      v-else-if="'auto' === mode"
      d="M5.7 7.9 7.3 9.5l3-3.2"
      fill="none"
      stroke="currentColor"
      stroke-width="1.3"
      stroke-linecap="round"
      stroke-linejoin="round" />
    <path
      v-else-if="'deny' === mode"
      d="M6.2 6.2l3.6 3.6M9.8 6.2l-3.6 3.6"
      stroke="currentColor"
      stroke-width="1.3"
      stroke-linecap="round" />
    <path
      v-else
      d="M8 3.9 11.2 5.1v3c0 2.1-1.3 3.7-3.2 4.7-1.9-1-3.2-2.6-3.2-4.7v-3L8 3.9Z"
      fill="none"
      stroke="currentColor"
      stroke-width="1"
      stroke-linejoin="round" />
  </svg>
</template>

<style lang="scss" scoped>
.policy-icon {
  display: block;
  flex: none;
}

.is-ask {
  color: var(--el-color-warning);
}

.is-auto {
  color: var(--el-color-success);
}

.is-deny {
  color: var(--el-color-danger);
}

.is-full {
  color: var(--el-color-primary);
}
</style>
