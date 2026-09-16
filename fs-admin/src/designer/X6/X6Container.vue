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
  options?: Record<string, any>,
  activeItem?: Record<string, any>,
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
/**
 * X6 Vue 节点宿主（teleport 宿主）。
 *
 * @antv/x6-vue-shape 的节点内容默认由「宿主组件」统一渲染：首次调用 getTeleport() 时返回
 * 宿主组件并把模块级 active 置为 true，此后所有节点视图改为 connect() 注册到一个模块级
 * items 表，由宿主把这些组件 teleport 回各自的 foreignObject 根节点。注意两点：
 * 1. getTeleport() 只在首次调用返回宿主组件，之后再调用一律返回 null，且 active 只置真不复位；
 * 2. 宿主组件卸载时会清空库内部对宿主的引用（teleport.js 的 onBeforeUnmount）。
 *
 * 因此「active 为真但没有任何宿主实例挂载」是致命状态：画布节点与拖拽 ghost 的内容都不再
 * 渲染（graph 里有数据、节点却是空白，拖拽时看不到 ghost），刷新页面才能恢复。本组件把宿主
 * 挂在自己页面内，正常路径（旧页面先卸载、新页面再挂载）没问题，但依赖挂载时序：一旦出现
 * 「新实例先挂载拿到 null（v-if 为假，没有宿主）、旧宿主随后卸载」——例如 HMR 改本目录文件、
 * 或页面内同时存在多个设计器、或外层路由加了 keep-alive/transition——该页面就会停在这个
 * 状态且不会自愈（TeleportContainer 是 setup 中取一次即固定的常量，不会再求值）。
 *
 * 遇到上述现象时的处理：在 App.vue 里全局挂载一次宿主，即
 *   import { x6Teleport } from '@/designer/X6/teleport'
 *   const TeleportContainer = x6Teleport()   // 模块级缓存宿主组件定义
 *   <TeleportContainer v-if="TeleportContainer" />  // 与 <router-view /> 同级
 * 这样宿主随应用常驻，且各页面此处 getTeleport() 返回 null、v-if 保持为假，全局恰好一个宿主。
 * 不要在各页面做「缓存宿主组件再自行挂载」的兜底：那会让新旧宿主同时存在，节点内容重复渲染。
 */
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
  <TeleportContainer v-if="TeleportContainer" />
</template>

<style lang="scss" scoped>
.viewport {
  width: 100%;
  height: 100%;
}
</style>
