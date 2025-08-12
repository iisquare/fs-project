<script setup lang="ts">
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
  <div class="node">
    <div class="box">
      <LayoutIcon :name="data.icon" color="#4A93FF" size="24" />
    </div>
    <div class="title">{{ data.name }}</div>
  </div>
</template>

<style lang="scss" scoped>
.node {
  width: 100%;
  height: 100%;
  overflow: visible;
}
.box {
  width: 100%;
  height: 100%;
  border: solid 1px rgb(204, 204, 204);
  box-sizing: border-box;
  border-radius: 10px;
  background-color: #fff;
  @include flex-center();
}
.title {
  height: 30px;
  line-height: 30px;
  font-size: 12px;
  text-align: center;
  white-space: nowrap; /* 保持文本在一行内 */
  overflow: hidden; /* 隐藏溢出的文本 */
  text-overflow: ellipsis; /* 使用省略号表示文本溢出 */
}
.x6-node-selected {
  .box {
    border-color: #1890ff;
    background-color: #F4F9FF;
  }
}
</style>
