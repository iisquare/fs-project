<script setup lang="ts">
/**
 * 编辑按钮 - 封装了 Element Plus 的 el-button，预设为警告色(橙色)、带编辑图标和"编辑"文字。
 *
 * @prop {Boolean}  loading  - 加载中状态，绑至 v-model:loading，默认 false
 *
 * @example
 * <!-- 基础用法 -->
 * <button-edit @click="handleEdit" />
 *
 * <!-- 带加载状态 -->
 * <button-edit v-model:loading="isEditing" @click="handleEdit" />
 */
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button type="warning" :icon="ElementPlusIcons.Edit" :loading="loading">编辑</el-button>
</template>

<style lang="scss" scoped>
</style>
