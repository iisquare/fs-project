<script setup lang="ts">
/**
 * 流程图查看器 - 基于 bpmn-js 的只读查看器
 *
 * @prop {String} bpmnXML    - BPMN 2.0 XML
 * @prop {Object} highlights - 节点着色配置 { activityId: color }
 * @prop {Array}  activities - 历史活动实例，按 已完成-绿/已删除-红/进行中-橙 着色
 */
import { nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import BpmnModeler from './bpmn/BpmnModeler'
import config from './config'

const props = defineProps<{
  bpmnXML: string,
  highlights?: Record<string, string>,
  activities?: any[],
}>()

const containerRef = ref<HTMLDivElement>()
const bpmn = shallowRef<any>(null)

const applyHighlights = () => {
  if (!bpmn.value) return
  if (props.highlights && Object.keys(props.highlights).length > 0) {
    bpmn.value.highlight(props.highlights)
    return
  }
  if (props.activities && props.activities.length > 0) {
    bpmn.value.colorful(props.activities)
  }
}

const importXML = () => {
  if (!bpmn.value || !props.bpmnXML) return
  bpmn.value.modeler.importXML(props.bpmnXML).then(() => {
    // 第二个参数传 'auto' 才会在适配视口的同时把图形居中，否则小图会贴左上角
    bpmn.value.canvas.zoom('fit-viewport', 'auto')
    nextTick(applyHighlights)
    return true
  }).catch(() => {
    return false
  })
}

onMounted(() => {
  bpmn.value = new BpmnModeler(containerRef.value, config, false)
  importXML()
})

watch(() => props.bpmnXML, () => {
  importXML()
})

watch([() => props.highlights, () => props.activities], () => {
  applyHighlights()
}, { deep: true })

onBeforeUnmount(() => {
  bpmn.value = null
})
</script>

<template>
  <div ref="containerRef" class="process-viewer">
    <el-empty description="无流程图" v-if="!bpmnXML" />
  </div>
</template>

<style lang="scss" scoped>
.process-viewer {
  width: 100%;
  height: 100%;
  min-height: 400px;
  :deep(.djs-palette) {
    display: none;
  }
}
</style>
