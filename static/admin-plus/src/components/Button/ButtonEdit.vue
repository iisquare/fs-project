<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button type="warning" :icon="ElementPlusIcons.Edit" :loading="loading">编辑</el-button>
</template>

<style lang="scss" scoped>
</style>
