<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button type="success" :icon="ElementPlusIcons.Plus" :loading="loading">新增</el-button>
</template>

<style lang="scss" scoped>
</style>
