<script setup lang="ts">
/**
 * 数字输入框 - 基于 el-input-number 封装，右侧控制按钮默认隐藏，hover 时显示。
 *
 * @v-model  {Number}   数字值（双向绑定主值）
 *
 * @example
 * <form-input-number v-model="count" />
 */
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
