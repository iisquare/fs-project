<script setup lang="ts">
const model: any = defineModel()

</script>

<template>
  <el-input-number v-model="model" controls-position="right" />
</template>

<style lang="scss" scoped>
.el-input-number {
  :deep(.el-input__wrapper) {
    padding-left: 42px;
  }
  :deep(.el-input-number__decrease), :deep(.el-input-number__increase) {
    visibility: hidden;
  }
  &:hover {
    :deep(.el-input-number__decrease), :deep(.el-input-number__increase) {
      visibility: visible;
    }
  }
}
</style>
