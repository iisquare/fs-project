<script setup lang="ts">
defineProps({
  label: { type: String, required: false },
  prop: { type: String, required: false },
})
</script>

<template>
  <el-col :lg="6" :md="8" :sm="12">
    <el-form-item :label="label" :prop="prop">
      <slot></slot>
    </el-form-item>
  </el-col>
</template>

<style lang="scss" scoped>
</style>
