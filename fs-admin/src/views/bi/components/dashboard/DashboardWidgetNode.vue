<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, type CSSProperties } from 'vue'
import DashboardContent from './DashboardContent.vue'
import { commonStyle } from './model'

defineOptions({ name: 'DashboardWidgetNode' })

const props = defineProps({
  widget: { type: Object, required: true },
  parentLayout: { type: String, default: 'absolute' },
  parentWidth: { type: Number, default: 1920 },
  parentHeight: { type: Number, default: 1080 },
  parentColumns: { type: Number, default: 24 },
  parentRows: { type: Number, default: 4 },
  parentGridFill: { type: Boolean, default: false },
  parentFlexDirection: { type: String, default: 'row' },
  parentRowHeight: { type: Number, default: 20 },
  parentGap: { type: Number, default: 10 },
  showGrid: { type: Boolean, default: true },
  scale: { type: Number, default: 1 },
  design: { type: Boolean, default: false },
  selectedId: { type: String, default: '' },
  refreshToken: { type: Number, default: 0 },
})

const emit = defineEmits([
  'select',
  'geometry',
  'drop-widget',
  'reorder',
])

const rootRef = ref<HTMLDivElement>()
const childrenRef = ref<HTMLDivElement>()
const childrenSize = ref({ width: 0, height: 0 })
const dragging = ref(false)
let childrenResizeObserver: ResizeObserver | null = null
let dragState: any = null
let pointerMove: ((event: PointerEvent) => void) | null = null
let pointerUp: (() => void) | null = null
const resizeHandles = ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w'] as const
type ResizeDirection = typeof resizeHandles[number]

const selected = computed(() => props.selectedId === props.widget.id)
const childWidth = computed(() => Math.max(40, props.widget.w - props.widget.style.paddingLeft - props.widget.style.paddingRight))
const childHeight = computed(() => Math.max(40, props.widget.h - props.widget.style.paddingTop - props.widget.style.paddingBottom))
const geometryValue = (value: number, unit: string) => `${Number(value) || 0}${unit === '%' ? '%' : 'px'}`
const geometryText = (value: number, unit: string) => {
  const number = Number(value) || 0
  return `${unit === '%' ? Math.round(number * 100) / 100 : Math.round(number)}${unit === '%' ? '%' : 'px'}`
}
const controlStyle = computed(() => {
  const scale = Math.max(props.scale, 0.01)
  return {
    '--editor-control-size': `${8 / scale}px`,
    '--editor-control-offset': `${-4 / scale}px`,
    '--editor-grip-width': `${30 / scale}px`,
    '--editor-grip-height': `${18 / scale}px`,
    '--editor-font-size': `${10 / scale}px`,
    '--editor-line-height': `${16 / scale}px`,
    '--editor-pad-x': `${6 / scale}px`,
    '--editor-pad-y': `${2 / scale}px`,
    '--editor-grip-bar-width': `${2 / scale}px`,
    '--editor-grip-bar-height': `${8 / scale}px`,
    '--editor-border-width': `${1 / scale}px`,
  }
})
const titleStyle = computed(() => ({
  justifyContent: props.widget.style.titleAlign === 'center' ? 'center' : props.widget.style.titleAlign === 'right' ? 'flex-end' : 'flex-start',
  color: props.widget.style.titleColor,
  fontSize: `${props.widget.style.titleSize}px`,
}))
const nodeStyle = computed(() => {
  const common: any = commonStyle(props.widget.style)
  if (props.design) common.overflow = 'visible'
  const position = props.parentLayout === 'fixed'
    ? 'fixed'
    : props.parentLayout === 'relative' ? 'relative' : 'absolute'
  const size = {
    width: geometryValue(props.widget.w, props.widget.wUnit),
    height: geometryValue(props.widget.h, props.widget.hUnit),
    zIndex: props.design && selected.value ? 9999 : props.widget.style.zIndex,
  }
  if (props.parentLayout === 'grid') {
    return {
      ...common,
      gridColumn: `${Math.max(0, props.widget.x) + 1} / span ${Math.max(1, props.widget.w)}`,
      gridRow: `${Math.max(0, props.widget.y) + 1} / span ${Math.max(1, props.widget.h)}`,
      zIndex: props.design && selected.value ? 9999 : props.widget.style.zIndex,
    }
  }
  if (props.parentLayout === 'flex') {
    return {
      ...common,
      ...size,
      position: 'relative',
      flexGrow: props.widget.flexGrow,
      flexShrink: props.widget.flexShrink,
      flexBasis: props.widget.flexBasis || 'auto',
      alignSelf: props.widget.alignSelf || 'auto',
      order: props.widget.flexOrder,
    }
  }
  return {
    ...common,
    ...size,
    position,
    ...(props.widget.horizontalAnchor === 'right'
      ? { right: geometryValue(props.widget.x, props.widget.xUnit) }
      : { left: geometryValue(props.widget.x, props.widget.xUnit) }),
    ...(props.widget.verticalAnchor === 'bottom'
      ? { bottom: geometryValue(props.widget.y, props.widget.yUnit) }
      : { top: geometryValue(props.widget.y, props.widget.yUnit) }),
  }
})
const bodyStyle = computed(() => ({
  overflow: props.widget.style.overflow,
}))
const childrenStyle = computed<CSSProperties>(() => {
  if (props.widget.layout === 'grid') {
    const columns = Math.max(1, props.widget.gridColumns)
    const rows = Math.max(1, props.widget.gridRows)
    const gap = Math.max(0, props.widget.gridGap)
    const guideWidth = 1 / Math.max(props.scale, 0.01)
    const renderedWidth = childrenSize.value.width || childWidth.value
    const renderedHeight = childrenSize.value.height || childHeight.value
    const cellWidth = Math.max(1, (renderedWidth - gap * (columns - 1)) / columns)
    const cellHeight = props.widget.gridFill
      ? Math.max(1, (renderedHeight - gap * (rows - 1)) / rows)
      : Math.max(1, props.widget.gridRowHeight)
    const columnStep = cellWidth + gap
    const rowStep = cellHeight + gap
    const columnGuideEnd = Math.min(columnStep, cellWidth + guideWidth / 2)
    const rowGuideEnd = Math.min(rowStep, cellHeight + guideWidth / 2)
    const line = 'rgba(83, 205, 255, 0.66)'
    return {
      display: 'grid',
      gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`,
      ...(props.widget.gridFill
        ? { gridTemplateRows: `repeat(${rows}, minmax(0, 1fr))` }
        : { gridAutoRows: `${props.widget.gridRowHeight}px` }),
      gap: `${gap}px`,
      position: 'relative',
      backgroundImage: props.design && props.showGrid
        ? [
            `repeating-linear-gradient(to right, ${line} 0 ${guideWidth}px, transparent ${guideWidth}px ${columnStep}px)`,
            `repeating-linear-gradient(to right, transparent 0 ${columnGuideEnd}px, ${line} ${columnGuideEnd}px ${Math.min(columnStep, columnGuideEnd + guideWidth)}px, transparent ${Math.min(columnStep, columnGuideEnd + guideWidth)}px ${columnStep}px)`,
            `repeating-linear-gradient(to bottom, ${line} 0 ${guideWidth}px, transparent ${guideWidth}px ${rowStep}px)`,
            `repeating-linear-gradient(to bottom, transparent 0 ${rowGuideEnd}px, ${line} ${rowGuideEnd}px ${Math.min(rowStep, rowGuideEnd + guideWidth)}px, transparent ${Math.min(rowStep, rowGuideEnd + guideWidth)}px ${rowStep}px)`,
          ].join(',')
        : undefined,
    }
  }
  if (props.widget.layout === 'flex') {
    return {
      display: 'flex',
      flexDirection: props.widget.flexDirection,
      flexWrap: props.widget.flexWrap,
      justifyContent: props.widget.justifyContent,
      alignItems: props.widget.alignItems,
      gap: `${props.widget.gridGap}px`,
      position: 'relative',
    }
  }
  return {
    display: 'block',
    position: 'relative',
  }
})

const startInteraction = (event: PointerEvent, mode: 'move' | ResizeDirection) => {
  if (!props.design) return
  event.preventDefault()
  event.stopPropagation()
  emit('select', props.widget.id)
  dragging.value = true
  if (props.parentLayout === 'flex' && mode === 'move') {
    const parentElement = rootRef.value?.parentElement
    if (!parentElement) {
      dragging.value = false
      return
    }
    let lastTargetId = props.widget.id
    pointerMove = (moveEvent: PointerEvent) => {
      const siblings = Array.from(parentElement.children)
        .filter((element): element is HTMLElement => element instanceof HTMLElement && Boolean(element.dataset.widgetId))
        .map((element) => ({
          id: element.dataset.widgetId || '',
          rect: element.getBoundingClientRect(),
        }))
        .sort((left, right) => props.parentFlexDirection.includes('row')
          ? left.rect.left - right.rect.left
          : left.rect.top - right.rect.top)
      const point = props.parentFlexDirection.includes('row') ? moveEvent.clientX : moveEvent.clientY
      const target = siblings.find((item) => {
        if (item.id === props.widget.id) return false
        const center = props.parentFlexDirection.includes('row')
          ? item.rect.left + item.rect.width / 2
          : item.rect.top + item.rect.height / 2
        return point < center
      })
      const targetId = target?.id || ''
      if (targetId === lastTargetId) return
      lastTargetId = targetId
      emit('reorder', {
        id: props.widget.id,
        targetId,
        after: props.parentFlexDirection.includes('reverse'),
      })
    }
    pointerUp = () => {
      dragging.value = false
      if (pointerMove) document.removeEventListener('pointermove', pointerMove)
      if (pointerUp) {
        document.removeEventListener('pointerup', pointerUp)
        document.removeEventListener('pointercancel', pointerUp)
      }
      pointerMove = null
      pointerUp = null
    }
    document.addEventListener('pointermove', pointerMove)
    document.addEventListener('pointerup', pointerUp)
    document.addEventListener('pointercancel', pointerUp)
    return
  }
  dragState = {
    mode,
    startX: event.clientX,
    startY: event.clientY,
    x: props.widget.x,
    y: props.widget.y,
    w: props.widget.w,
    h: props.widget.h,
    horizontalAnchor: props.widget.horizontalAnchor || 'left',
    verticalAnchor: props.widget.verticalAnchor || 'top',
  }
  pointerMove = (moveEvent: PointerEvent) => {
    if (!dragState) return
    const scale = Math.max(props.scale, 0.01)
    const dx = (moveEvent.clientX - dragState.startX) / scale
    const dy = (moveEvent.clientY - dragState.startY) / scale
    const parentElement = rootRef.value?.parentElement
    const renderedParentWidth = Math.max(1, parentElement?.clientWidth || props.parentWidth)
    const renderedParentHeight = Math.max(1, parentElement?.clientHeight || props.parentHeight)
    if (props.parentLayout === 'grid') {
      const gap = props.parentGap
      const cellWidth = (renderedParentWidth - gap * (props.parentColumns - 1)) / props.parentColumns
      const colStep = Math.max(1, cellWidth + gap)
      const rowStep = props.parentGridFill
        ? Math.max(1, (renderedParentHeight - gap * (props.parentRows - 1)) / props.parentRows + gap)
        : Math.max(1, props.parentRowHeight + gap)
      const availableRows = props.parentGridFill
        ? Math.max(1, props.parentRows)
        : Math.max(1, Math.floor((renderedParentHeight + gap) / rowStep))
      if (dragState.mode === 'move') {
        emit('geometry', {
          x: Math.min(
            Math.max(0, props.parentColumns - dragState.w),
            Math.max(0, dragState.x + Math.round(dx / colStep)),
          ),
          y: Math.min(
            Math.max(0, availableRows - dragState.h),
            Math.max(0, dragState.y + Math.round(dy / rowStep)),
          ),
          w: dragState.w,
          h: dragState.h,
        })
      } else {
        const direction = dragState.mode as ResizeDirection
        const deltaX = Math.round(dx / colStep)
        const deltaY = Math.round(dy / rowStep)
        let x = dragState.x
        let y = dragState.y
        let w = dragState.w
        let h = dragState.h
        if (direction.includes('e')) w += deltaX
        if (direction.includes('s')) h += deltaY
        if (direction.includes('w')) {
          x += deltaX
          w -= deltaX
        }
        if (direction.includes('n')) {
          y += deltaY
          h -= deltaY
        }
        if (x < 0) {
          w += x
          x = 0
        }
        if (y < 0) {
          h += y
          y = 0
        }
        if (w < 1) {
          if (direction.includes('w')) x -= 1 - w
          w = 1
        }
        if (h < 1) {
          if (direction.includes('n')) y -= 1 - h
          h = 1
        }
        w = Math.min(Math.max(1, props.parentColumns - x), w)
        h = Math.min(Math.max(1, availableRows - y), h)
        emit('geometry', {
          x,
          y,
          w,
          h,
        })
      }
      return
    }
    const fromUnit = (value: number, unit: string, dimension: number) => unit === '%'
      ? value * dimension / 100
      : value
    const toUnit = (value: number, unit: string, dimension: number) => unit === '%'
      ? Math.round(value / Math.max(1, dimension) * 10000) / 100
      : Math.round(value)
    const startW = fromUnit(dragState.w, props.widget.wUnit, renderedParentWidth)
    const startH = fromUnit(dragState.h, props.widget.hUnit, renderedParentHeight)
    const startOffsetX = fromUnit(dragState.x, props.widget.xUnit, renderedParentWidth)
    const startOffsetY = fromUnit(dragState.y, props.widget.yUnit, renderedParentHeight)
    const startLeft = dragState.horizontalAnchor === 'right'
      ? renderedParentWidth - startOffsetX - startW
      : startOffsetX
    const startTop = dragState.verticalAnchor === 'bottom'
      ? renderedParentHeight - startOffsetY - startH
      : startOffsetY
    const geometry = (left: number, top: number, w: number, h: number) => {
      const offsetX = dragState.horizontalAnchor === 'right'
        ? renderedParentWidth - left - w
        : left
      const offsetY = dragState.verticalAnchor === 'bottom'
        ? renderedParentHeight - top - h
        : top
      return {
        x: toUnit(Math.max(0, offsetX), props.widget.xUnit, renderedParentWidth),
        y: toUnit(Math.max(0, offsetY), props.widget.yUnit, renderedParentHeight),
        w: toUnit(w, props.widget.wUnit, renderedParentWidth),
        h: toUnit(h, props.widget.hUnit, renderedParentHeight),
      }
    }
    if (dragState.mode === 'move') {
      emit('geometry', geometry(
        Math.min(Math.max(0, renderedParentWidth - startW), Math.max(0, startLeft + dx)),
        Math.min(Math.max(0, renderedParentHeight - startH), Math.max(0, startTop + dy)),
        startW,
        startH,
      ))
    } else {
      const direction = dragState.mode as ResizeDirection
      let left = startLeft
      let top = startTop
      let w = startW
      let h = startH
      const minWidth = Math.min(renderedParentWidth, props.widget.wUnit === '%' ? renderedParentWidth / 100 : 60)
      const minHeight = Math.min(renderedParentHeight, props.widget.hUnit === '%' ? renderedParentHeight / 100 : 40)
      if (direction.includes('e')) {
        w = Math.min(renderedParentWidth - left, Math.max(minWidth, startW + dx))
      }
      if (direction.includes('s')) {
        h = Math.min(renderedParentHeight - top, Math.max(minHeight, startH + dy))
      }
      if (direction.includes('w')) {
        const right = startLeft + startW
        left = Math.min(Math.max(0, right - minWidth), Math.max(0, startLeft + dx))
        w = right - left
      }
      if (direction.includes('n')) {
        const bottom = startTop + startH
        top = Math.min(Math.max(0, bottom - minHeight), Math.max(0, startTop + dy))
        h = bottom - top
      }
      emit('geometry', geometry(left, top, w, h))
    }
  }
  pointerUp = () => {
    dragState = null
    dragging.value = false
    if (pointerMove) document.removeEventListener('pointermove', pointerMove)
    if (pointerUp) {
      document.removeEventListener('pointerup', pointerUp)
      document.removeEventListener('pointercancel', pointerUp)
    }
    pointerMove = null
    pointerUp = null
  }
  document.addEventListener('pointermove', pointerMove)
  document.addEventListener('pointerup', pointerUp)
  document.addEventListener('pointercancel', pointerUp)
}

const onDrop = (event: DragEvent) => {
  if (!props.design || props.widget.type !== 'Container') return
  if (!event.dataTransfer?.types.includes('application/x-dashboard-widget')) return
  event.preventDefault()
  event.stopPropagation()
  const target = childrenRef.value
  if (!target) return
  const rect = target.getBoundingClientRect()
  const scale = Math.max(props.scale, 0.01)
  emit('drop-widget', {
    id: props.widget.id,
    position: {
      x: Math.max(0, Math.min(target.clientWidth, (event.clientX - rect.left) / scale)),
      y: Math.max(0, Math.min(target.clientHeight, (event.clientY - rect.top) / scale)),
    },
  })
}
const onDragOver = (event: DragEvent) => {
  if (!props.design || props.widget.type !== 'Container') return
  if (!event.dataTransfer?.types.includes('application/x-dashboard-widget')) return
  event.preventDefault()
  event.stopPropagation()
  event.dataTransfer.dropEffect = 'copy'
}

onMounted(() => {
  if (!childrenRef.value) return
  childrenResizeObserver = new ResizeObserver(() => {
    if (!childrenRef.value) return
    childrenSize.value = {
      width: childrenRef.value.clientWidth,
      height: childrenRef.value.clientHeight,
    }
  })
  childrenResizeObserver.observe(childrenRef.value)
  childrenSize.value = {
    width: childrenRef.value.clientWidth,
    height: childrenRef.value.clientHeight,
  }
})

onBeforeUnmount(() => {
  childrenResizeObserver?.disconnect()
  if (pointerMove) document.removeEventListener('pointermove', pointerMove)
  if (pointerUp) document.removeEventListener('pointerup', pointerUp)
  if (pointerUp) document.removeEventListener('pointercancel', pointerUp)
})
</script>

<template>
  <div
    ref="rootRef"
    class="dashboard-node"
    :class="{
      'is-selected': selected && design,
      'is-dragging': dragging,
      'is-container': widget.type === 'Container',
      'is-decoration': widget.type === 'Border',
    }"
    :style="[nodeStyle, controlStyle]"
    :data-widget-id="widget.id"
    @click.stop="design && emit('select', widget.id)"
    @dragover="onDragOver"
    @drop="onDrop">
    <div v-if="widget.style.titleShow && widget.type !== 'Text'" class="dashboard-node__title" :style="titleStyle">
      {{ widget.style.titleText || widget.name }}
    </div>
    <div class="dashboard-node__body" :style="bodyStyle">
      <DashboardContent :widget="widget" :refresh-token="refreshToken">
        <div ref="childrenRef" class="dashboard-node__children" :style="childrenStyle">
          <DashboardWidgetNode
            v-for="child in widget.children"
            :key="child.id"
            :widget="child"
            :parent-layout="widget.layout"
            :parent-width="childWidth"
            :parent-height="childHeight"
            :parent-columns="widget.gridColumns"
            :parent-rows="widget.gridRows"
            :parent-grid-fill="widget.gridFill"
            :parent-flex-direction="widget.flexDirection"
            :parent-row-height="widget.gridRowHeight"
            :parent-gap="widget.gridGap"
            :show-grid="showGrid"
            :scale="scale"
            :design="design"
            :selected-id="selectedId"
            :refresh-token="refreshToken"
            @select="(id: string) => emit('select', id)"
            @geometry="(value: any) => emit('geometry', child.id, value)"
            @drop-widget="(payload: any) => emit('drop-widget', payload)"
            @reorder="(payload: any) => emit('reorder', payload)" />
        </div>
      </DashboardContent>
    </div>
    <template v-if="design">
      <div
        v-if="widget.type === 'Container'"
        class="container-drag-handle"
        :title="parentLayout === 'flex' ? '拖动调整弹性顺序' : '拖动布局容器'"
        @pointerdown="startInteraction($event, 'move')">
        <span></span><span></span><span></span>
      </div>
      <div
        v-else
        class="node-hit"
        :class="{ 'is-reorder': parentLayout === 'flex' }"
        @pointerdown="startInteraction($event, 'move')">
        <span class="node-hit__label">{{ widget.name }}</span>
        <span class="node-hit__tip">{{ parentLayout === 'flex' ? '拖动调整顺序' : '拖动定位' }}</span>
      </div>
      <span v-if="selected" class="node-size">
        {{ geometryText(widget.w, widget.wUnit) }} × {{ geometryText(widget.h, widget.hUnit) }}
      </span>
      <span
        v-for="direction in resizeHandles"
        :key="direction"
        class="resize-handle"
        :class="`resize-handle--${direction}`"
        @pointerdown="startInteraction($event, direction)"></span>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.dashboard-node {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  position: relative;
  transition: outline-color 0.15s, box-shadow 0.15s;
  &.is-dragging {
    opacity: 0.78;
    outline: 1px dashed #7be5ff;
  }
  &.is-selected {
    outline: 2px solid #32bfff;
    outline-offset: 1px;
    box-shadow: 0 0 0 4px rgba(50, 191, 255, 0.14);
    z-index: 8;
  }
  &__title {
    flex: none;
    display: flex;
    align-items: center;
    min-height: 22px;
    margin-bottom: 6px;
    font-weight: 600;
  }
  &__body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }
  &__children {
    flex: 1;
    width: 100%;
    height: 100%;
    min-height: 0;
    box-sizing: border-box;
  }
}
.node-hit {
  position: absolute;
  inset: 0;
  z-index: 5;
  cursor: move;
  user-select: none;
  &__label {
    position: absolute;
    left: 0;
    top: 0;
    max-width: 80%;
    padding: var(--editor-pad-y) var(--editor-pad-x);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: var(--editor-font-size);
    line-height: var(--editor-line-height);
    color: #e6f8ff;
    background: rgba(13, 114, 174, 0.88);
    border-radius: 0 0 4px 0;
    opacity: 0;
    transition: opacity 0.15s;
  }
  &__tip {
    position: absolute;
    left: 50%;
    top: 50%;
    padding: var(--editor-pad-y) var(--editor-pad-x);
    font-size: var(--editor-font-size);
    white-space: nowrap;
    color: #dff8ff;
    background: rgba(4, 35, 67, 0.78);
    border: 1px solid rgba(56, 190, 255, 0.45);
    border-radius: 3px;
    opacity: 0;
    transform: translate(-50%, -50%);
    transition: opacity 0.15s;
  }
  &:hover {
    background: rgba(18, 156, 220, 0.06);
    .node-hit__label, .node-hit__tip { opacity: 1; }
  }
  &.is-reorder {
    cursor: grab;
    &:active {
      cursor: grabbing;
    }
  }
}
.resize-handle {
  position: absolute;
  z-index: 7;
  width: var(--editor-control-size);
  height: var(--editor-control-size);
  background: #fff;
  border: var(--editor-border-width) solid #20b8ef;
  border-radius: 1px;
  opacity: 0;
  &--nw { left: var(--editor-control-offset); top: var(--editor-control-offset); cursor: nwse-resize; }
  &--n { left: 50%; top: var(--editor-control-offset); cursor: ns-resize; transform: translateX(-50%); }
  &--ne { right: var(--editor-control-offset); top: var(--editor-control-offset); cursor: nesw-resize; }
  &--e { right: var(--editor-control-offset); top: 50%; cursor: ew-resize; transform: translateY(-50%); }
  &--se { right: var(--editor-control-offset); bottom: var(--editor-control-offset); cursor: nwse-resize; }
  &--s { left: 50%; bottom: var(--editor-control-offset); cursor: ns-resize; transform: translateX(-50%); }
  &--sw { left: var(--editor-control-offset); bottom: var(--editor-control-offset); cursor: nesw-resize; }
  &--w { left: var(--editor-control-offset); top: 50%; cursor: ew-resize; transform: translateY(-50%); }
}
.node-size {
  position: absolute;
  left: 50%;
  bottom: 5px;
  z-index: 7;
  padding: var(--editor-pad-y) var(--editor-pad-x);
  font-size: var(--editor-font-size);
  line-height: var(--editor-line-height);
  color: #fff;
  white-space: nowrap;
  background: rgba(17, 116, 170, 0.82);
  border-radius: 2px;
  pointer-events: none;
  transform: translateX(-50%);
}
.is-selected > .resize-handle { opacity: 1; }
.container-drag-handle {
  position: absolute;
  top: 4px;
  right: 5px;
  z-index: 7;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: var(--editor-grip-width);
  height: var(--editor-grip-height);
  border-radius: 3px;
  background: rgba(15, 118, 177, 0.72);
  cursor: move;
  opacity: 0;
  transition: opacity 0.15s;
  span {
    width: var(--editor-grip-bar-width);
    height: var(--editor-grip-bar-height);
    border-radius: 1px;
    background: #dff8ff;
  }
}
.is-container:hover > .container-drag-handle,
.is-container.is-selected > .container-drag-handle {
  opacity: 1;
}
</style>
