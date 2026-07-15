<script setup lang="ts">
/**
 * 刷新按钮 - 圆形图标按钮，点击可重新加载分页数据。
 *
 * @prop {Boolean}  loading  - 加载中状态，绑至 v-model:loading，默认 false
 *
 * @example
 * <button-refresh v-model:loading="refreshing" @click="handleRefresh" />
 */
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button :icon="ElementPlusIcons.Refresh" circle title="重新加载分页数据" :loading="loading" />
</template>

<style lang="scss" scoped>
</style>
