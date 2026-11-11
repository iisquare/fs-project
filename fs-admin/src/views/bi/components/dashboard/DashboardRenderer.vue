<script setup lang="ts">
import { computed } from 'vue'
import { Grid } from '@element-plus/icons-vue'
import DashboardWidgetNode from './DashboardWidgetNode.vue'

const props = defineProps({
  model: { type: Object, required: true },
  scale: { type: Number, default: 1 },
  scaleX: { type: Number, default: undefined },
  scaleY: { type: Number, default: undefined },
  design: { type: Boolean, default: false },
  selectedId: { type: String, default: '' },
  refreshToken: { type: Number, default: 0 },
})

const emit = defineEmits([
  'select',
  'geometry',
  'drop-widget',
  'drop-stage',
  'reorder',
  'blank-click',
])

const canvasStyle = computed(() => ({
  width: `${props.model.canvas.width}px`,
  height: `${props.model.canvas.height}px`,
  background: props.model.canvas.background,
  backgroundImage: props.model.canvas.backgroundImage ? `url("${props.model.canvas.backgroundImage}")` : undefined,
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  transform: `scale(${props.scaleX ?? props.scale}, ${props.scaleY ?? props.scale})`,
}))

const rootGridStyle = computed(() => {
  const base = { position: 'relative' as const, width: '100%', height: '100%' }
  if (props.model.canvas.layout === 'grid') {
    return {
      ...base,
      display: 'grid',
      gridTemplateColumns: `repeat(${props.model.canvas.columns}, minmax(0, 1fr))`,
      ...(props.model.canvas.fill
        ? { gridTemplateRows: `repeat(${props.model.canvas.rows}, minmax(0, 1fr))` }
        : { gridAutoRows: `${props.model.canvas.rowHeight}px` }),
      gap: `${props.model.canvas.gap}px`,
    }
  }
  if (props.model.canvas.layout === 'flex') {
    return {
      ...base,
      display: 'flex',
      flexDirection: props.model.canvas.flexDirection,
      flexWrap: props.model.canvas.flexWrap,
      justifyContent: props.model.canvas.justifyContent,
      alignItems: props.model.canvas.alignItems,
      gap: `${props.model.canvas.gap}px`,
    }
  }
  return base
})

const layoutGuideStyle = computed(() => {
  if (!props.design || !props.model.canvas.showGrid) return {}
  const canvas = props.model.canvas
  const line = 'rgba(83, 205, 255, 0.72)'
  const guideWidth = 1 / Math.max(props.scale, 0.01)
  if (canvas.layout === 'grid') {
    const columns = Math.max(1, canvas.columns)
    const rows = Math.max(1, canvas.rows)
    const gap = Math.max(0, canvas.gap)
    const cellWidth = Math.max(1, (canvas.width - gap * (columns - 1)) / columns)
    const cellHeight = canvas.fill
      ? Math.max(1, (canvas.height - gap * (rows - 1)) / rows)
      : Math.max(1, canvas.rowHeight)
    const columnStep = cellWidth + gap
    const rowStep = cellHeight + gap
    const columnGuideEnd = Math.min(columnStep, cellWidth + guideWidth / 2)
    const rowGuideEnd = Math.min(rowStep, cellHeight + guideWidth / 2)
    return {
      backgroundImage: [
        `repeating-linear-gradient(to right, ${line} 0 ${guideWidth}px, transparent ${guideWidth}px ${columnStep}px)`,
        `repeating-linear-gradient(to right, transparent 0 ${columnGuideEnd}px, ${line} ${columnGuideEnd}px ${Math.min(columnStep, columnGuideEnd + guideWidth)}px, transparent ${Math.min(columnStep, columnGuideEnd + guideWidth)}px ${columnStep}px)`,
        `repeating-linear-gradient(to bottom, ${line} 0 ${guideWidth}px, transparent ${guideWidth}px ${rowStep}px)`,
        `repeating-linear-gradient(to bottom, transparent 0 ${rowGuideEnd}px, ${line} ${rowGuideEnd}px ${Math.min(rowStep, rowGuideEnd + guideWidth)}px, transparent ${Math.min(rowStep, rowGuideEnd + guideWidth)}px ${rowStep}px)`,
      ].join(','),
      boxShadow: `inset 0 0 0 ${guideWidth}px ${line}`,
    }
  }
  return {}
})

const onDropStage = (event: DragEvent) => {
  if (!props.design) return
  if (!event.dataTransfer?.types.includes('application/x-dashboard-widget')) return
  event.preventDefault()
  emit('select', '')
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const x = Math.max(0, (event.clientX - rect.left) / Math.max(props.scale, 0.01))
  const y = Math.max(0, (event.clientY - rect.top) / Math.max(props.scale, 0.01))
  emit('drop-stage', { x, y })
}
const onDragOverStage = (event: DragEvent) => {
  if (!props.design) return
  if (!event.dataTransfer?.types.includes('application/x-dashboard-widget')) return
  event.preventDefault()
  event.dataTransfer.dropEffect = 'copy'
}
</script>

<template>
  <div
    class="dashboard-stage"
    :class="{
      'has-grid': design && model.canvas.showGrid,
      'is-design': design,
    }"
    :style="canvasStyle"
    @click.self="design && emit('blank-click')"
    @dragover="onDragOverStage"
    @drop="onDropStage">
    <div
      v-if="design && model.canvas.showGrid && model.canvas.layout === 'grid'"
      class="layout-guides"
      :style="layoutGuideStyle" />
    <div class="dashboard-stage__root" :style="rootGridStyle">
      <DashboardWidgetNode
        v-for="widget in model.widgets"
        :key="widget.id"
        :widget="widget"
        :parent-layout="model.canvas.layout"
        :parent-width="model.canvas.width"
        :parent-height="model.canvas.height"
        :parent-columns="model.canvas.columns"
        :parent-rows="model.canvas.rows"
        :parent-grid-fill="model.canvas.fill"
        :parent-flex-direction="model.canvas.flexDirection"
        :parent-row-height="model.canvas.rowHeight"
        :parent-gap="model.canvas.gap"
        :show-grid="model.canvas.showGrid"
        :scale="scale"
        :design="design"
        :selected-id="selectedId"
        :refresh-token="refreshToken"
        @select="(id: string) => emit('select', id)"
        @geometry="(value: any) => emit('geometry', widget.id, value)"
        @drop-widget="(payload: any) => emit('drop-widget', payload)"
        @reorder="(payload: any) => emit('reorder', payload)" />
    </div>
    <div v-if="design && !model.widgets.length" class="stage-empty">
      <el-icon><Grid /></el-icon>
      <span>从左侧拖入组件，开始设计大屏</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.dashboard-stage {
  position: relative;
  isolation: isolate;
  box-sizing: border-box;
  transform-origin: top left;
  overflow: hidden;
  flex: none;
  &.is-design {
    overflow: visible;
  }
  &.has-grid {
    background-image:
      linear-gradient(rgba(55, 157, 210, 0.07) 1px, transparent 1px),
      linear-gradient(90deg, rgba(55, 157, 210, 0.07) 1px, transparent 1px);
    background-size: v-bind('`${model.canvas.gridSize}px ${model.canvas.gridSize}px`');
  }
  &__root {
    box-sizing: border-box;
    position: relative;
    z-index: 2;
  }
  .layout-guides {
    position: absolute;
    inset: 0;
    z-index: 3;
    overflow: hidden;
    pointer-events: none;
    background-repeat: repeat;
  }
}
.stage-empty {
  position: absolute;
  left: 50%;
  top: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: rgba(128, 188, 225, 0.55);
  font-size: 20px;
  pointer-events: none;
  transform: translate(-50%, -50%);
  .el-icon {
    font-size: 48px;
  }
}
</style>
