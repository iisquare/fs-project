<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Flow from './flow'

const props = withDefaults(defineProps<{
  grid?: boolean,
  autoResize?: boolean,
  panning?: boolean,
  readonly?: boolean,
  mousewheel?: boolean,
  resizing?: boolean,
  rotating?: boolean,
  snapline?: boolean,
  clipboard?: boolean,
  keyboard?: boolean,
  selection?: boolean,
  history?: boolean,
  scroller?: boolean,
  minimap?: HTMLElement,
  onCellClick?: () => void,
  onCellDoubleClick?: () => void,
  onCellContextmenu?: () => void,
  onBlankClick?: () => void,
  onBlankDoubleClick?: () => void,
  onBlankContextmenu?: () => void,
  onNodeAdded?: () => void,
  onEdgeConnected?: () => void,
}>(), Flow.defaults)

const flow: any = ref()
const containerRef = ref<HTMLDivElement>()
const TeleportContainer = Flow.X6VueShape.getTeleport()

onMounted(() => {
  flow.value = new Flow(containerRef.value, props)
})

defineExpose({ flow })
</script>

<template>
  <div ref="containerRef" class="viewport"></div>
  <TeleportContainer />
</template>

<style lang="scss" scoped>
.viewport {
  width: 100%;
  height: 100%;
}
</style>
