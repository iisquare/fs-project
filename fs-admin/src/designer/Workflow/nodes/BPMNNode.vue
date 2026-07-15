<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'
import * as X6 from '@antv/x6'
import DesignUtil from '@/utils/DesignUtil'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

const node = (inject('getNode') as any)() as X6.Node
const data: any = ref(node.getData())

onMounted(() => {
  node.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
    data.value = current
  }))
})
</script>

<template>
  <div v-if="data.type === 'bpmn:StartEvent'" class="bpmn-event start-event">
    <div class="event-circle start-circle">
      <LayoutIcon :name="data.icon" color="#4CAF50" size="18" />
    </div>
    <div class="bpmn-label">{{ data.name }}</div>
  </div>
  <div v-else-if="data.type === 'bpmn:EndEvent'" class="bpmn-event end-event">
    <div class="event-circle end-circle">
      <LayoutIcon :name="data.icon" color="#F44336" size="18" />
    </div>
    <div class="bpmn-label">{{ data.name }}</div>
  </div>
  <div v-else class="bpmn-task">
    <div class="task-box">
      <LayoutIcon :name="data.icon" color="#4A93FF" size="24" />
      <span class="task-name">{{ data.name }}</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.bpmn-event {
  width: 100%;
  height: 100%;
  @include flex-center;
  flex-direction: column;
}

.event-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  @include flex-center;
  background: #fff;
}

.start-circle {
  border: 2px solid #4CAF50;
}

.end-circle {
  border: 3px solid #F44336;
}

.bpmn-label {
  font-size: 11px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80px;
  margin-top: 2px;
}

.bpmn-task {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.task-box {
  width: 100%;
  height: 100%;
  border: solid 1px rgb(204, 204, 204);
  box-sizing: border-box;
  border-radius: 12px;
  background-color: #fff;
  @include flex-center;
  gap: 6px;
  padding: 4px 12px;
}

.task-name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.x6-node-selected {
  .start-circle {
    box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.3);
  }
  .end-circle {
    box-shadow: 0 0 0 2px rgba(244, 67, 54, 0.3);
  }
  .task-box {
    border-color: #1890ff;
    background-color: #F4F9FF;
  }
}
</style>
