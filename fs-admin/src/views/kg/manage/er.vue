<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute()
const router = useRouter()
</script>

<template>
  <el-card :bordered="false" shadow="never" class="toolbar">
    <el-space>
      <button-add />
      <button-delete />
    </el-space>
    <el-space>
      <el-button type="primary">保存</el-button>
      <el-button @click="router.go(-1)">返回</el-button>
    </el-space>
  </el-card>
  <el-card :bordered="false" shadow="never" class="container"></el-card>
</template>

<style lang="scss" scoped>
.toolbar {
  :deep(.el-card__body) {
    @include flex-between();
  }
}
.container {
  margin-top: 20px;
  height: calc(100vh - 140px - var(--fs-layout-header-height));
}
</style>
