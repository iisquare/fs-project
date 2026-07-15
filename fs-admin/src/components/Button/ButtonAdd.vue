<script setup lang="ts">
/**
 * 新增按钮 - 封装了 Element Plus 的 el-button，预设为成功色(绿色)、带加号图标和"新增"文字。
 *
 * @prop {Boolean}  loading  - 加载中状态，绑至 v-model:loading，默认 false
 *
 * @example
 * <!-- 基础用法 -->
 * <button-add @click="handleAdd" />
 *
 * <!-- 带加载状态 -->
 * <button-add v-model:loading="isAdding" @click="handleAdd" />
 */
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button type="success" :icon="ElementPlusIcons.Plus" :loading="loading">新增</el-button>
</template>

<style lang="scss" scoped>
</style>
