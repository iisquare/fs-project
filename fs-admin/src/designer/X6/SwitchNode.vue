<script setup lang="ts">
/**
 * 条件分支节点 - switch-case-default 形式，每行分支右侧对应一个可连线的输出锚点。
 */
import { computed, inject, onMounted, ref } from 'vue'
import * as X6 from '@antv/x6'
import DesignUtil from '@/utils/DesignUtil'
import SwitchLayout, { defaultPortId } from './switch'

const node = (inject('getNode') as any)() as X6.Node
const data: any = ref(node.getData())

const rows = computed(() => SwitchLayout.rows(data.value))

onMounted(() => {
  node.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
    data.value = current
  }))
})
</script>

<template>
  <div class="agent-switch">
    <div class="header" :style="{ height: SwitchLayout.header + 'px' }">
      <div class="icon">
        <LayoutIcon :name="data.icon" size="16" />
      </div>
      <div class="title">{{ data.name }}</div>
    </div>
    <div class="body" :style="{ padding: SwitchLayout.padding + 'px 0' }">
      <div
        class="row"
        :class="{ 'is-default': defaultPortId === item.id }"
        :key="item.id"
        :style="{ height: SwitchLayout.row + 'px' }"
        v-for="item in rows">
        <span class="dot"></span>
        <span class="name">{{ item.name }}</span>
        <span class="summary">{{ item.summary }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.agent-switch {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  border: solid 1px #d5dae0;
  border-radius: 8px;
  background-color: #fff;
  overflow: hidden;
  .header {
    padding: 0 12px;
    box-sizing: border-box;
    border-bottom: solid 1px #eef0f3;
    background: #f7f9fc;
    @include flex-start();
    gap: 8px;
    .icon {
      flex: none;
      width: 22px;
      height: 22px;
      border-radius: 5px;
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
      @include flex-center();
    }
    .title {
      flex: 1;
      min-width: 0;
      font-size: 13px;
      color: #333;
      @include text-wrap();
    }
  }
  .body {
    box-sizing: border-box;
    .row {
      padding: 0 12px;
      box-sizing: border-box;
      font-size: 12px;
      color: #4b5563;
      @include flex-start();
      gap: 6px;
      .dot {
        flex: none;
        width: 5px;
        height: 5px;
        border-radius: 50%;
        background: var(--el-color-primary-light-5);
      }
      .name {
        flex: 1;
        min-width: 0;
        @include text-wrap();
      }
      .summary {
        flex: none;
        font-size: 12px;
        color: #9aa4b2;
      }
      &.is-default {
        color: #6b7280;
        background: #fafbfc;
        .dot {
          background: #cbd5e1;
        }
      }
    }
  }
}

:global(.x6-node-selected .agent-switch) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
}
</style>
