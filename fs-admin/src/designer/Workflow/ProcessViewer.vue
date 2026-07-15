<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch, nextTick } from 'vue'
import Flow from '@/designer/X6/flow'
import BPMNConverter from '@/designer/Workflow/bpmn/BPMNConverter'
import { registerBPMNNodes } from '@/designer/Workflow/nodes'

registerBPMNNodes()

const props = defineProps<{
  bpmnXML: string
  highlights?: Record<string, string>
}>()

const containerRef = ref<HTMLDivElement>()
const TeleportContainer = Flow.X6VueShape.getTeleport()
let flow: any = null

function initGraph() {
  if (!containerRef.value) return
  if (flow) flow.graph.dispose()

  flow = new Flow(containerRef.value, {
    ...Flow.defaults,
    readonly: true,
    keyboard: false,
    clipboard: false,
    history: false,
    panning: true,
    mousewheel: true,
    connecting: false as any,
    embedding: false,
    selection: false,
  })

  loadBPMN()
}

function loadBPMN() {
  if (!flow || !props.bpmnXML) return
  try {
    const graph = BPMNConverter.parse(props.bpmnXML)
    flow.fromJSON(graph.cells)
    applyHighlights()
  } catch (e) {
    // BPMN parse failed, silently ignore
  }
}

function applyHighlights() {
  if (!flow || !props.highlights) return
  nextTick(() => {
    flow.highlight(props.highlights || {})
  })
}

watch(() => props.bpmnXML, () => {
  loadBPMN()
})

watch(() => props.highlights, () => {
  applyHighlights()
}, { deep: true })

onMounted(() => {
  initGraph()
})

onBeforeUnmount(() => {
  if (flow) flow.graph.dispose()
})
</script>

<template>
  <div ref="containerRef" class="process-viewer"></div>
  <TeleportContainer />
</template>

<style lang="scss" scoped>
.process-viewer {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>
