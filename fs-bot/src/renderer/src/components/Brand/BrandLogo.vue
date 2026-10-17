<script setup lang="ts">
import logo from '@renderer/assets/images/app-loading-static.png'

withDefaults(
  defineProps<{
    size?: number
  }>(),
  {
    size: 28
  }
)
</script>

<template>
  <img
    class="brand-logo"
    :src="logo"
    :width="size"
    :height="size"
    alt=""
    aria-hidden="true"
    draggable="false"
  />
</template>

<style lang="scss" scoped>
.brand-logo {
  display: block;
  flex-shrink: 0;
  object-fit: contain;
  user-select: none;
  pointer-events: none;
}
</style>
