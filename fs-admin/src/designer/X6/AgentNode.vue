<script setup lang="ts">
/**
 * 智能体编排节点 - 图标 + 名称 + 描述的横向卡片，用于智能体编排画布。
 */
import { inject, onMounted, ref } from 'vue'
import * as X6 from '@antv/x6'
import DesignUtil from '@/utils/DesignUtil'

const node = (inject('getNode') as any)() as X6.Node
const data: any = ref(node.getData())

onMounted(() => {
  node.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
    data.value = current
  }))
})
</script>

<template>
  <div class="agent-node">
    <div class="icon">
      <LayoutIcon :name="data.icon" size="18" />
    </div>
    <div class="content">
      <div class="title">{{ data.name }}</div>
      <div class="description">{{ data.description || data.type }}</div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.agent-node {
  width: 100%;
  height: 100%;
  padding: 0 12px;
  box-sizing: border-box;
  border: solid 1px #d5dae0;
  border-radius: 8px;
  background-color: #fff;
  @include flex-start();
  gap: 10px;
  .icon {
    flex: none;
    width: 28px;
    height: 28px;
    border-radius: 6px;
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    @include flex-center();
  }
  .content {
    flex: 1;
    min-width: 0;
    .title {
      font-size: 13px;
      color: #333;
      @include text-wrap();
    }
    .description {
      margin-top: 2px;
      font-size: 12px;
      color: #909399;
      @include text-wrap();
    }
  }
}

:global(.x6-node-selected .agent-node) {
  border-color: var(--el-color-primary);
  background-color: #f4f9ff;
}
</style>
