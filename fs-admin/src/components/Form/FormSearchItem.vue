<script setup lang="ts">
import { computed } from 'vue';

const {
  span = 6,
  offset = 0,
  push = 0,
  pull = 0,
} = defineProps({
  label: { type: String, required: false },
  prop: { type: String, required: false },
  span: { type: Number, required: false },
  offset: { type: Number, required: false },
  push: { type: Number, required: false },
  pull: { type: Number, required: false },
})

const layout = computed(() => {
  return Object.assign({}, { span, offset, push, pull })
})
</script>

<template>
  <el-col v-bind="layout">
    <el-form-item :label="label" :prop="prop">
      <slot></slot>
    </el-form-item>
  </el-col>
</template>

<style lang="scss" scoped>
</style>
