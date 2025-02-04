<script setup lang="ts">
const {
  layout = { lg: 6, md: 8, sm: 12 },
} = defineProps({
  label: { type: String, required: false },
  prop: { type: String, required: false },
  layout: { type: Object, required: false },
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
