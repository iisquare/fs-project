<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button :icon="ElementPlusIcons.Search" circle title="展示/隐藏搜索栏" :loading="loading" />
</template>

<style lang="scss" scoped>
</style>
