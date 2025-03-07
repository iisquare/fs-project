<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button :icon="ElementPlusIcons.Refresh" circle title="重新加载分页数据" :loading="loading" />
</template>

<style lang="scss" scoped>
</style>
