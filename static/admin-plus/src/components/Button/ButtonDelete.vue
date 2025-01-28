<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button type="danger" :icon="ElementPlusIcons.Delete" :loading="loading">删除</el-button>
</template>

<style lang="scss" scoped>
</style>
