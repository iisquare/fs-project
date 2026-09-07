<script setup lang="ts">
/**
 * Trino 运行状态维护
 *
 * 左侧维护项导航，右侧展示当前维护任务的执行面板。
 */
import MaintainApi from '@/api/bi/MaintainApi';
import FormMaintainBox from '@/components/Form/FormMaintainBox.vue';

const items = [
  {
    key: 'catalog',
    title: '目录重载',
    description: '重新加载 Trino 目录，清理并重建内置目录和数据源目录。',
    api: MaintainApi.catalog,
    params: () => ({}),
  },
  {
    key: 'dataset',
    title: '数据集重建',
    description: '删除数据集 Schema 后重建所有视图，并同步维护定时任务。',
    api: MaintainApi.dataset,
    params: () => ({}),
  },
]
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="maintain-header">
        <span>Trino 运行状态维护</span>
        <span class="maintain-tip">选择左侧维护项执行，执行过程会以 SSE 方式实时展示。</span>
      </div>
    </template>
    <FormMaintainBox :items="items" height="320px" />
  </el-card>
</template>

<style lang="scss" scoped>
.maintain-header {
  @include flex-between();

  .maintain-tip {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    font-weight: normal;
  }
}
</style>
