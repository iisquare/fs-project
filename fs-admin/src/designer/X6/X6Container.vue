<script lang="ts">
</script>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import Flow from './flow'

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const emit = defineEmits(['update:activeItem'])
const { // 在defineProps的回调函数中，不允许引用setup本地变量作为默认值或直接赋值本地变量
  options = {} as any,
  activeItem = {} as any,
} = defineProps<{
  options: { type: null, required: false },
  activeItem: { type: null, required: true },
}>()
if (!options.onBlankClick) options.onBlankClick = () => {
  emit('update:activeItem', {})
  tips.value.text = '选中画布'
  flow.value.select()
}
if (!options.onCellClick) options.onCellClick = (data: any) => {
  const cell = flow.value.cell2meta(data.cell)
  emit('update:activeItem', cell)
  tips.value.text = `选中 ${cell.shape} ${cell.data.name}, ID: ${cell.id}`
  flow.value.select(data.cell)
}
if (!options.onNodeAdded) options.onNodeAdded = (data: any) => {
  const cell = flow.value.cell2meta(data.node)
  emit('update:activeItem', cell)
  tips.value.text = `选中 ${cell.shape} ${cell.data.name}, ID: ${cell.id}`
  flow.value.select(data.node)
}
if (!options.onEdgeConnected) options.onEdgeConnected = (data: any) => {
  const cell = flow.value.cell2meta(data.edge)
  emit('update:activeItem', cell)
  tips.value.text = `选中 ${cell.shape} ${cell.data.name}, ID: ${cell.id}`
  flow.value.select(data.edge)
}

const flow: any = ref()
const containerRef = ref<HTMLDivElement>()
const TeleportContainer = Flow.X6VueShape.getTeleport()

watch(() => activeItem, (cell: any) => {
  flow.value.updateCell(cell)
}, { deep: true })

onMounted(() => {
  flow.value = new Flow(containerRef.value, options)
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
