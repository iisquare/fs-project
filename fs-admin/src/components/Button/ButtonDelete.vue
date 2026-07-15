<script setup lang="ts">
/**
 * 删除按钮 - 封装了 Element Plus 的 el-button，预设为危险色(红色)、带删除图标和"删除"文字。
 *
 * @prop {Boolean}  loading  - 加载中状态，绑至 v-model:loading，默认 false
 *
 * @example
 * <!-- 基础用法 -->
 * <button-delete @click="handleDelete" />
 *
 * <!-- 带加载状态 -->
 * <button-delete v-model:loading="isDeleting" @click="handleDelete" />
 */
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button type="danger" :icon="ElementPlusIcons.Delete" :loading="loading">删除</el-button>
</template>

<style lang="scss" scoped>
</style>
