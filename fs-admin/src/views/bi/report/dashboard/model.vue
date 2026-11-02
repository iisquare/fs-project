<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  FullScreen,
  Grid,
  Iphone,
  Minus,
  Plus,
  Refresh,
  RefreshLeft,
  RefreshRight,
  View,
} from '@element-plus/icons-vue'
import LayoutBack from '@/components/Layout/LayoutBack.vue'
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import LayoutWidget from '@/components/Layout/LayoutWidget.vue'
import DashboardApi from '@/api/bi/DashboardApi'
import ApiUtil from '@/utils/ApiUtil'
import DashboardInspector from '@/views/bi/components/dashboard/DashboardInspector.vue'
import DashboardRenderer from '@/views/bi/components/dashboard/DashboardRenderer.vue'
import {
  createWidget,
  defaultDashboard,
  findParent,
  findWidget,
  removeWidget,
  styleDefaults,
  widgetContentSize,
  walkWidgets,
  widgetGroups,
  type DashboardModel,
  type DashboardWidget,
} from '@/views/bi/components/dashboard/model'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const model = ref<DashboardModel>(defaultDashboard())
const title = ref('未命名大屏')
const selectedId = ref('')
const refreshToken = ref(0)
const zoom = ref(1)
const autoFit = ref(true)
const snap = ref(true)
const leftTab = ref<'components' | 'layers'>('components')
const stageViewportRef = ref<HTMLElement>()
const stageCenterRef = ref<HTMLElement>()
let resizeObserver: ResizeObserver | null = null
let historyTimer: any = null
let lastGeometryAt = 0
let lastPanAt = 0
let canvasPanState: {
  pointerId: number
  startX: number
  startY: number
  scrollLeft: number
  scrollTop: number
  moved: boolean
} | null = null
const canvasPanning = ref(false)
const history = ref<string[]>([])
const historyIndex = ref(-1)
const restoring = ref(false)
const placementContexts = new Map<string, any>()
const savedSnapshot = ref('')

const selectedWidget = computed(() => selectedId.value ? findWidget(model.value.widgets, selectedId.value) : null)
const stageStyle = computed(() => ({
  width: `${model.value.canvas.width * zoom.value}px`,
  height: `${model.value.canvas.height * zoom.value}px`,
}))
const formatGeometry = (value: number, unit: string) => `${value}${unit === '%' ? '%' : 'px'}`
const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value >= 0 && historyIndex.value < history.value.length - 1)
const dirty = computed(() => Boolean(savedSnapshot.value) && (
  savedSnapshot.value !== snapshot() || title.value !== savedTitle.value
))
const savedTitle = ref('')
const layoutLabels: Record<string, string> = {
  absolute: '绝对定位',
  relative: '相对定位',
  fixed: '固定定位',
  flex: '弹性布局',
  grid: '栅格布局',
}
const layers = computed(() => {
  const result: Array<{ widget: DashboardWidget; depth: number }> = []
  const visit = (items: DashboardWidget[], depth: number) => {
    ;[...items]
      .sort((left, right) => (Number(right.style?.zIndex) || 1) - (Number(left.style?.zIndex) || 1))
      .forEach((widget) => {
      result.push({ widget, depth })
      visit(widget.children || [], depth + 1)
      })
  }
  visit(model.value.widgets, 0)
  return result
})
const layoutSignature = computed(() => {
  const parts = [`root:${model.value.canvas.layout}:${model.value.canvas.columns}:${model.value.canvas.rows}:${model.value.canvas.fill}:${model.value.canvas.rowHeight}:${model.value.canvas.gap}`]
  walkWidgets(model.value.widgets, (widget) => {
    if (widget.type === 'Container') {
      parts.push(`${widget.id}:${widget.layout}:${widget.gridColumns}:${widget.gridRows}:${widget.gridFill}:${widget.gridRowHeight}:${widget.gridGap}`)
    }
  })
  return parts.join('|')
})

const normalizeWidget = (source: any): DashboardWidget => {
  const widget = createWidget(source.type || 'Container', source)
  widget.style = Object.assign(styleDefaults(), source.style || {})
  widget.data = {
    datasetId: source.data?.datasetId,
    filter: source.data?.filter || [],
    axis: {
      metrics: source.data?.axis?.metrics || [],
      buckets: source.data?.axis?.buckets || [],
    },
  }
  widget.children = Array.isArray(source.children) ? source.children.map(normalizeWidget) : []
  return widget
}

const parseContent = (content: any) => {
  if (!content) return defaultDashboard()
  try {
    const parsed = typeof content === 'string' ? JSON.parse(content) : content
    const defaults = defaultDashboard()
    const result: DashboardModel = {
      version: 2,
      canvas: Object.assign(defaults.canvas, parsed.canvas || {}),
      widgets: Array.isArray(parsed.widgets) ? parsed.widgets.map(normalizeWidget) : [],
    }
    model.value = result
  } catch (error: any) {
    ElMessage.error('大屏配置解析失败：' + error.message)
    model.value = defaultDashboard()
  }
}

const load = () => {
  if (!route.query.id) {
    syncPlacementContexts()
    initHistory()
    savedSnapshot.value = snapshot()
    savedTitle.value = title.value
    return
  }
  loading.value = true
  DashboardApi.info(route.query.id).then((result: any) => {
    if (!ApiUtil.succeed(result)) return
    title.value = result.data?.name || title.value
    parseContent(result.data?.content)
  }).catch(() => {}).finally(async () => {
    loading.value = false
    await nextTick()
    fitCanvas()
    syncPlacementContexts()
    initHistory()
    savedSnapshot.value = snapshot()
    savedTitle.value = title.value
  })
}

const snapshot = () => JSON.stringify(model.value)
const initHistory = () => {
  history.value = [snapshot()]
  historyIndex.value = 0
}
const commitHistory = () => {
  if (restoring.value) return
  clearTimeout(historyTimer)
  historyTimer = setTimeout(() => {
    const current = snapshot()
    if (history.value[historyIndex.value] === current) return
    history.value = history.value.slice(0, historyIndex.value + 1)
    history.value.push(current)
    if (history.value.length > 60) history.value.shift()
    historyIndex.value = history.value.length - 1
  }, 180)
}
watch(model, commitHistory, { deep: true })
const restore = (index: number) => {
  if (index < 0 || index >= history.value.length) return
  restoring.value = true
  model.value = JSON.parse(history.value[index])
  historyIndex.value = index
  selectedId.value = ''
  nextTick(() => {
    restoring.value = false
    refreshToken.value++
  })
}
const undo = () => restore(historyIndex.value - 1)
const redo = () => restore(historyIndex.value + 1)

const fitCanvas = () => {
  if (!stageViewportRef.value) return
  const availableWidth = Math.max(320, stageViewportRef.value.clientWidth - 80)
  const availableHeight = Math.max(220, stageViewportRef.value.clientHeight - 70)
  const displayWidth = model.value.canvas.displayWidthUnit === '%'
    ? availableWidth * model.value.canvas.displayWidth / 100
    : model.value.canvas.displayWidth
  const displayHeight = model.value.canvas.displayHeightUnit === '%'
    ? availableHeight * model.value.canvas.displayHeight / 100
    : model.value.canvas.displayHeight
  zoom.value = Math.max(0.15, Math.min(
    availableWidth / model.value.canvas.width,
    availableHeight / model.value.canvas.height,
    displayWidth / model.value.canvas.width,
    displayHeight / model.value.canvas.height,
  ))
}

const syncPlacementContexts = () => {
  placementContexts.clear()
  placementContexts.set('root', {
    layout: model.value.canvas.layout,
    width: model.value.canvas.width,
    height: model.value.canvas.height,
    columns: model.value.canvas.columns,
    rows: model.value.canvas.rows,
    fill: model.value.canvas.fill,
    rowHeight: model.value.canvas.rowHeight,
    gap: model.value.canvas.gap,
  })
  walkWidgets(model.value.widgets, (widget) => {
    if (widget.type !== 'Container') return
    const size = widgetContentSize(widget, model.value.widgets, model.value.canvas)
    placementContexts.set(widget.id, {
      layout: widget.layout,
      width: Math.max(40, size.width),
      height: Math.max(40, size.height),
      columns: widget.gridColumns,
      rows: widget.gridRows,
      fill: widget.gridFill,
      rowHeight: widget.gridRowHeight,
      gap: widget.gridGap,
    })
  })
}

type LayoutContext = {
  layout: DashboardWidget['layout']
  width: number
  height: number
  columns: number
  rows: number
  fill: boolean
  rowHeight: number
  gap: number
}

const reflowChildren = (children: DashboardWidget[], from: LayoutContext, to: LayoutContext) => {
  if (!from || !to || from.layout === to.layout) return
  const fromColumns = Math.max(1, from.columns)
  const toColumns = Math.max(1, to.columns)
  const fromRows = Math.max(1, from.rows)
  const toRows = Math.max(1, to.rows)
  const fromCell = Math.max(1, (from.width - from.gap * (fromColumns - 1)) / fromColumns)
  const toCell = Math.max(1, (to.width - to.gap * (toColumns - 1)) / toColumns)
  const fromCellHeight = from.fill
    ? Math.max(1, (from.height - from.gap * (fromRows - 1)) / fromRows)
    : Math.max(1, from.rowHeight)
  const toCellHeight = to.fill
    ? Math.max(1, (to.height - to.gap * (toRows - 1)) / toRows)
    : Math.max(1, to.rowHeight)
  const fromColumnStep = fromCell + from.gap
  const fromRowStep = fromCellHeight + from.gap
  const toColumnStep = toCell + to.gap
  const toRowStep = toCellHeight + to.gap
  const toAvailableRows = to.fill
    ? toRows
    : Math.max(1, Math.floor((to.height + to.gap) / toRowStep))

  children.forEach((widget) => {
    const width = from.layout === 'grid'
      ? widget.w * fromCell + Math.max(0, widget.w - 1) * from.gap
      : widget.wUnit === '%' ? widget.w * from.width / 100 : widget.w
    const height = from.layout === 'grid'
      ? widget.h * fromCellHeight + Math.max(0, widget.h - 1) * from.gap
      : widget.hUnit === '%' ? widget.h * from.height / 100 : widget.h
    const offsetX = from.layout === 'grid'
      ? widget.x * fromColumnStep
      : widget.xUnit === '%' ? widget.x * from.width / 100 : widget.x
    const offsetY = from.layout === 'grid'
      ? widget.y * fromRowStep
      : widget.yUnit === '%' ? widget.y * from.height / 100 : widget.y
    const x = from.layout === 'grid'
      ? offsetX
      : widget.horizontalAnchor === 'right' ? from.width - offsetX - width : offsetX
    const y = from.layout === 'grid'
      ? offsetY
      : widget.verticalAnchor === 'bottom' ? from.height - offsetY - height : offsetY

    if (to.layout === 'grid') {
      const column = Math.min(toColumns - 1, Math.max(0, Math.round(x / toColumnStep)))
      const row = Math.min(toAvailableRows - 1, Math.max(0, Math.round(y / toRowStep)))
      widget.x = column
      widget.y = row
      widget.w = Math.min(
        toColumns - column,
        Math.max(1, Math.round((width + to.gap) / toColumnStep)),
      )
      widget.h = Math.min(
        toAvailableRows - row,
        Math.max(1, Math.round((height + to.gap) / toRowStep)),
      )
    } else {
      widget.w = Math.max(1, Math.round(width))
      widget.h = Math.max(1, Math.round(height))
      widget.x = widget.horizontalAnchor === 'right'
        ? Math.max(0, Math.round(to.width - x - widget.w))
        : Math.round(x)
      widget.y = widget.verticalAnchor === 'bottom'
        ? Math.max(0, Math.round(to.height - y - widget.h))
        : Math.round(y)
    }
    widget.xUnit = 'px'
    widget.yUnit = 'px'
    widget.wUnit = 'px'
    widget.hUnit = 'px'
  })
}

const reflowLayouts = () => {
  if (restoring.value) {
    syncPlacementContexts()
    return
  }
  const rootBefore = placementContexts.get('root')
  if (rootBefore && rootBefore.layout !== model.value.canvas.layout) {
    reflowChildren(model.value.widgets, rootBefore, {
      layout: model.value.canvas.layout,
      width: model.value.canvas.width,
      height: model.value.canvas.height,
      columns: model.value.canvas.columns,
      rows: model.value.canvas.rows,
      fill: model.value.canvas.fill,
      rowHeight: model.value.canvas.rowHeight,
      gap: model.value.canvas.gap,
    })
  }
  walkWidgets(model.value.widgets, (widget) => {
    if (widget.type !== 'Container') return
    const before = placementContexts.get(widget.id)
    if (!before || before.layout === widget.layout) return
    const size = widgetContentSize(widget, model.value.widgets, model.value.canvas)
    reflowChildren(widget.children || [], before, {
      layout: widget.layout,
      width: Math.max(40, size.width),
      height: Math.max(40, size.height),
      columns: widget.gridColumns,
      rows: widget.gridRows,
      fill: widget.gridFill,
      rowHeight: widget.gridRowHeight,
      gap: widget.gridGap,
    })
  })
  syncPlacementContexts()
  refreshToken.value++
  commitHistory()
}

watch(layoutSignature, reflowLayouts, { flush: 'post' })
watch(
  () => [
    model.value.canvas.displayWidth,
    model.value.canvas.displayHeight,
    model.value.canvas.displayWidthUnit,
    model.value.canvas.displayHeightUnit,
    model.value.canvas.width,
    model.value.canvas.height,
  ],
  () => {
    nextTick(fitCanvas)
  },
  { flush: 'post' },
)
watch(
  () => model.value.canvas.layout,
  (layout) => {
    if (layout === 'grid') model.value.canvas.showGrid = true
  },
)

const onPaletteDragStart = (event: DragEvent, item: any) => {
  event.dataTransfer?.setData('application/x-dashboard-widget', item.type)
  event.dataTransfer?.setData('text/plain', item.type)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'copy'
}

const positionNewWidget = (
  widget: DashboardWidget,
  parent: DashboardWidget | null,
  position?: { x: number; y: number },
) => {
  const context: LayoutContext = parent
    ? {
        layout: parent.layout,
        width: widgetContentSize(parent, model.value.widgets, model.value.canvas).width,
        height: widgetContentSize(parent, model.value.widgets, model.value.canvas).height,
        columns: parent.gridColumns,
        rows: parent.gridRows,
        fill: parent.gridFill,
        rowHeight: parent.gridRowHeight,
        gap: parent.gridGap,
      }
    : {
        layout: model.value.canvas.layout,
        width: model.value.canvas.width,
        height: model.value.canvas.height,
        columns: model.value.canvas.columns,
        rows: model.value.canvas.rows,
        fill: model.value.canvas.fill,
        rowHeight: model.value.canvas.rowHeight,
        gap: model.value.canvas.gap,
      }
  const columns = Math.max(1, context.columns)
  const rows = Math.max(1, context.rows)
  const gap = Math.max(0, context.gap)
  const cellWidth = Math.max(1, (context.width - gap * (columns - 1)) / columns)
  const cellHeight = context.fill
    ? Math.max(1, (context.height - gap * (rows - 1)) / rows)
    : Math.max(1, context.rowHeight)
  const columnStep = cellWidth + gap
  const rowStep = cellHeight + gap

  if (context.layout === 'grid') {
    const availableRows = context.fill
      ? rows
      : Math.max(1, Math.floor((context.height + gap) / rowStep))
    widget.w = context.fill
      ? 1
      : Math.max(1, Math.min(columns, Math.round(parent ? columns / 2 : 7)))
    widget.h = context.fill
      ? 1
      : Math.max(1, Math.round((parent ? 250 : 300) / rowStep))
    widget.w = Math.min(widget.w, columns)
    widget.h = Math.min(widget.h, availableRows)

    if (position) {
      const renderedWidth = widget.w * cellWidth + Math.max(0, widget.w - 1) * gap
      const renderedHeight = widget.h * cellHeight + Math.max(0, widget.h - 1) * gap
      widget.x = Math.min(
        Math.max(0, columns - widget.w),
        Math.max(0, Math.round((position.x - renderedWidth / 2) / columnStep)),
      )
      widget.y = Math.min(
        Math.max(0, availableRows - widget.h),
        Math.max(0, Math.round((position.y - renderedHeight / 2) / rowStep)),
      )
    } else if (parent) {
      const index = parent.children.length
      const perRow = Math.max(1, Math.floor(columns / widget.w))
      widget.x = (index % perRow) * widget.w
      widget.y = Math.floor(index / perRow) * widget.h
    } else {
      widget.x = Math.max(0, Math.round(30 / columnStep))
      widget.y = Math.max(0, Math.round(30 / rowStep))
    }
  } else {
    widget.w = Math.min(widget.w, Math.max(1, context.width))
    widget.h = Math.min(widget.h, Math.max(1, context.height))
    if (position) {
      const step = snap.value ? Math.max(1, model.value.canvas.gridSize) : 1
      widget.x = Math.max(0, Math.min(
        Math.max(0, context.width - widget.w),
        Math.round((position.x - widget.w / 2) / step) * step,
      ))
      widget.y = Math.max(0, Math.min(
        Math.max(0, context.height - widget.h),
        Math.round((position.y - widget.h / 2) / step) * step,
      ))
    } else if (parent) {
      const index = parent.children.length
      widget.x = Math.min(Math.max(0, context.width - widget.w), 24 + (index % 2) * 24)
      widget.y = Math.min(Math.max(0, context.height - widget.h), 48 + Math.floor(index / 2) * 28)
    } else {
      widget.x = 30
      widget.y = 30
    }
  }

  widget.xUnit = 'px'
  widget.yUnit = 'px'
  widget.wUnit = 'px'
  widget.hUnit = 'px'
  widget.horizontalAnchor = 'left'
  widget.verticalAnchor = 'top'
}

const addWidget = (type: string, parentId = '', position?: { x: number; y: number }) => {
  const parent = parentId ? findWidget(model.value.widgets, parentId) : null
  if (parent && parent.type !== 'Container') return
  const widget = createWidget(type as any)
  positionNewWidget(widget, parent, position)
  const siblings = parent ? parent.children : model.value.widgets
  widget.style.zIndex = Math.max(0, ...siblings.map((item: DashboardWidget) => Number(item.style?.zIndex) || 0)) + 1
  if (parent) parent.children.push(widget)
  else model.value.widgets.push(widget)
  selectedId.value = widget.id
  refreshToken.value++
  commitHistory()
}

const handleDropStage = (position: { x: number; y: number }) => addWidget(dragType.value || 'Container', '', position)
const handleDropContainer = (payload: { id: string; position: { x: number; y: number } }) => (
  addWidget(dragType.value || 'Container', payload.id, payload.position)
)

const dragType = ref('')
const currentDragStart = (event: DragEvent, item: any) => {
  dragType.value = item.type
  onPaletteDragStart(event, item)
}

const currentWidgetClick = (item: any) => {
  const selected = selectedWidget.value
  let parentId = ''
  if (selected?.type === 'Container') {
    parentId = selected.id
  } else if (selected) {
    const parent = findParent(model.value.widgets, selected.id)
    if (parent?.type === 'Container') parentId = parent.id
  }
  addWidget(item.type, parentId)
}

const handleGeometry = (id: string, value: any) => {
  const widget = findWidget(model.value.widgets, id)
  if (!widget) return
  const parent = findParent(model.value.widgets, id)
  const layout = parent ? parent.layout : model.value.canvas.layout
  if (['absolute', 'fixed', 'relative'].includes(layout) && snap.value) {
    if ('x' in value && widget.xUnit !== '%') value.x = Math.round(value.x / model.value.canvas.gridSize) * model.value.canvas.gridSize
    if ('y' in value && widget.yUnit !== '%') value.y = Math.round(value.y / model.value.canvas.gridSize) * model.value.canvas.gridSize
  }
  Object.assign(widget, value)
  lastGeometryAt = Date.now()
  commitHistory()
}
const handleReorder = (payload: { id: string; targetId: string; after?: boolean }) => {
  if (!payload.id || payload.id === payload.targetId) return
  const parent = findParent(model.value.widgets, payload.id)
  const siblings = parent ? parent.children : model.value.widgets
  const sourceIndex = siblings.findIndex((widget) => widget.id === payload.id)
  if (sourceIndex < 0) return
  const moved = siblings.splice(sourceIndex, 1)[0]
  const targetIndex = payload.targetId
    ? siblings.findIndex((widget) => widget.id === payload.targetId)
    : payload.after ? 0 : siblings.length
  const insertIndex = targetIndex < 0
    ? siblings.length
    : payload.targetId && payload.after ? targetIndex + 1 : targetIndex
  siblings.splice(insertIndex, 0, moved)
  siblings.forEach((widget, index) => {
    widget.flexOrder = index
  })
  selectedId.value = moved.id
  lastGeometryAt = Date.now()
  commitHistory()
}
const handleStageClick = () => {
  if (Date.now() - lastGeometryAt < 180 || Date.now() - lastPanAt < 180) return
  selectedId.value = ''
}

const onCanvasBackgroundPointerDown = (event: PointerEvent) => {
  const viewport = stageViewportRef.value
  const target = event.target
  if (!viewport || !(target instanceof HTMLElement) || event.button !== 0 || !event.isPrimary) return
  const isBackground = target === viewport
    || target === stageCenterRef.value
    || (Boolean(target.closest('.dashboard-stage')) && !target.closest('.dashboard-node'))
  if (!isBackground) return

  event.preventDefault()
  canvasPanState = {
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    scrollLeft: viewport.scrollLeft,
    scrollTop: viewport.scrollTop,
    moved: false,
  }
  viewport.setPointerCapture(event.pointerId)
}

const onCanvasPanMove = (event: PointerEvent) => {
  if (!canvasPanState || event.pointerId !== canvasPanState.pointerId || !stageViewportRef.value) return
  const dx = event.clientX - canvasPanState.startX
  const dy = event.clientY - canvasPanState.startY
  if (!canvasPanState.moved && Math.hypot(dx, dy) < 3) return
  canvasPanState.moved = true
  canvasPanning.value = true
  stageViewportRef.value.scrollLeft = canvasPanState.scrollLeft - dx
  stageViewportRef.value.scrollTop = canvasPanState.scrollTop - dy
}

const onCanvasPanEnd = (event: PointerEvent) => {
  if (!canvasPanState || event.pointerId !== canvasPanState.pointerId) return
  const viewport = stageViewportRef.value
  if (canvasPanState.moved) lastPanAt = Date.now()
  if (viewport?.hasPointerCapture(event.pointerId)) viewport.releasePointerCapture(event.pointerId)
  canvasPanState = null
  canvasPanning.value = false
}

const handleRemove = (id = selectedId.value) => {
  if (!id) return
  removeWidget(model.value.widgets, id)
  selectedId.value = ''
  refreshToken.value++
  commitHistory()
}

const cloneWidget = (source: DashboardWidget): DashboardWidget => {
  const result = JSON.parse(JSON.stringify(source))
  const renew = (widget: DashboardWidget) => {
    widget.id = `widget-${Date.now()}-${Math.random().toString(16).slice(2)}`
    widget.children?.forEach(renew)
  }
  renew(result)
  return result
}

const handleClone = () => {
  const source = selectedWidget.value
  if (!source) return
  const parent = findParent(model.value.widgets, source.id)
  const copy = cloneWidget(source)
  copy.name = `${source.name} 副本`
  const offsetX = source.xUnit === '%' ? 2 : 20
  const offsetY = source.yUnit === '%' ? 2 : 20
  copy.x = Math.max(0, copy.x + (source.horizontalAnchor === 'right' ? -offsetX : offsetX))
  copy.y = Math.max(0, copy.y + (source.verticalAnchor === 'bottom' ? -offsetY : offsetY))
  const siblings = parent ? parent.children : model.value.widgets
  copy.style.zIndex = Math.max(0, ...siblings.map((item: DashboardWidget) => Number(item.style?.zIndex) || 0)) + 1
  if (parent) parent.children.push(copy)
  else model.value.widgets.push(copy)
  selectedId.value = copy.id
  commitHistory()
}

const handleSave = () => {
  if (saving.value) return
  if (!title.value.trim()) {
    ElMessage.warning('请输入大屏名称')
    return
  }
  saving.value = true
  const data = {
    id: route.query.id,
    name: title.value,
    content: JSON.stringify(model.value),
    status: 1,
    sort: 0,
  }
  DashboardApi.save(data, { success: true }).then((result: any) => {
    if (ApiUtil.succeed(result)) {
      savedSnapshot.value = snapshot()
      savedTitle.value = title.value
      if (!route.query.id && result.data?.id) {
        router.replace({ path: route.path, query: { id: result.data.id } }).catch(() => {})
      }
    }
  }).catch(() => {}).finally(() => {
    saving.value = false
  })
}

const preview = () => {
  if (!route.query.id) {
    ElMessage.warning('请先保存大屏')
    return
  }
  const target = router.resolve({ path: '/bi/dashboard/view', query: { id: route.query.id } })
  window.open(target.href, '_blank')
}

const zoomIn = () => {
  autoFit.value = false
  zoom.value = Math.min(2, Math.round((zoom.value + 0.1) * 10) / 10)
}
const zoomOut = () => {
  autoFit.value = false
  zoom.value = Math.max(0.15, Math.round((zoom.value - 0.1) * 10) / 10)
}
const clampCanvasZoom = (value: number) => Math.min(2, Math.max(0.15, Math.round(value * 1000) / 1000))
const onCanvasWheel = (event: WheelEvent) => {
  if (!event.ctrlKey || !stageViewportRef.value) return
  event.preventDefault()

  const viewport = stageViewportRef.value
  const stage = stageCenterRef.value
  const previousZoom = zoom.value
  const delta = event.deltaY * (
    event.deltaMode === WheelEvent.DOM_DELTA_LINE
      ? 16
      : event.deltaMode === WheelEvent.DOM_DELTA_PAGE ? viewport.clientHeight : 1
  )
  const nextZoom = clampCanvasZoom(previousZoom * Math.exp(-Math.max(-120, Math.min(120, delta)) * 0.0015))
  if (nextZoom === previousZoom) return

  const stageRect = stage?.getBoundingClientRect()
  const localX = stageRect ? (event.clientX - stageRect.left) / previousZoom : 0
  const localY = stageRect ? (event.clientY - stageRect.top) / previousZoom : 0
  autoFit.value = false
  zoom.value = nextZoom

  if (!stage || !stageRect) return
  nextTick(() => {
    const nextRect = stage.getBoundingClientRect()
    viewport.scrollLeft += (nextRect.left + localX * nextZoom) - (stageRect.left + localX * previousZoom)
    viewport.scrollTop += (nextRect.top + localY * nextZoom) - (stageRect.top + localY * previousZoom)
  })
}
const fit = () => {
  autoFit.value = true
  fitCanvas()
}

const onKeydown = (event: KeyboardEvent) => {
  const target = event.target as HTMLElement
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) || target.isContentEditable) return
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z') {
    event.preventDefault()
    event.shiftKey ? redo() : undo()
  } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'y') {
    event.preventDefault()
    redo()
  } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'd') {
    event.preventDefault()
    handleClone()
  } else if (event.key === 'Delete' || event.key === 'Backspace') {
    event.preventDefault()
    handleRemove()
  }
}
const onBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}
onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前大屏有未保存的修改，确定离开吗？', '未保存提醒', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
})

onMounted(async () => {
  load()
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('beforeunload', onBeforeUnload)
  await nextTick()
  resizeObserver = new ResizeObserver(() => autoFit.value && fitCanvas())
  if (stageViewportRef.value) resizeObserver.observe(stageViewportRef.value)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('beforeunload', onBeforeUnload)
  resizeObserver?.disconnect()
  clearTimeout(historyTimer)
  canvasPanState = null
  canvasPanning.value = false
})
</script>

<template>
  <LayoutDesigner splitter :left-size="240" :right-size="360" :left-min="210" :right-min="320">
    <template #left>
      <div class="palette-head">
        <div class="palette-brand">
          <span class="palette-brand__mark">BI</span>
          <div class="palette-brand__meta">
            <strong>可视化组件</strong>
            <el-input
              v-model="title"
              class="palette-name"
              size="small"
              placeholder="输入大屏名称" />
          </div>
        </div>
      </div>
      <el-tabs v-model="leftTab" class="left-tabs">
        <el-tab-pane label="组件" name="components">
          <LayoutWidget
            :widgets="widgetGroups"
            native
            @drag-start="currentDragStart"
            @click-widget="currentWidgetClick" />
          <div class="palette-tip">
            <el-icon><Iphone /></el-icon>
            <span>单击添加到选中容器，拖拽可定位；画布按 {{ model.canvas.width }} × {{ model.canvas.height }} 自适应缩放，按住 Ctrl + 滚轮可缩放。</span>
          </div>
        </el-tab-pane>
        <el-tab-pane name="layers">
          <template #label>
            <span class="tab-label">图层 <i v-if="layers.length">{{ layers.length }}</i></span>
          </template>
          <section class="layers-panel">
            <LayoutHeading title="画布图层" description="从上到下位于同层" />
            <div v-if="layers.length" class="layer-list">
              <button
                type="button"
                v-for="item in layers"
                :key="item.widget.id"
                class="layer-item"
                :class="{
                  'is-active': selectedId === item.widget.id,
                  'is-container-layer': item.widget.type === 'Container',
                }"
                :style="{ paddingLeft: `${8 + item.depth * 14}px` }"
                @click="selectedId = item.widget.id">
                <span class="layer-item__type">{{ item.widget.type.slice(0, 1) }}</span>
                <span class="layer-item__name">{{ item.widget.name }}</span>
                <span class="layer-item__z" title="图层层级">层级 {{ item.widget.style.zIndex ?? 1 }}</span>
              </button>
            </div>
            <div v-else class="layer-empty">
              <span class="layer-empty__icon"></span>
              <strong>暂无图层</strong>
              <small>从“组件”页签添加一个组件</small>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </template>

    <template #top>
      <div class="designer-topbar">
        <div class="designer-topbar__left">
          <LayoutBack to="/bi/olap/dashboard" title="返回" />
          <el-button text :icon="RefreshLeft" :disabled="!canUndo" title="撤销（Ctrl+Z）" @click="undo" />
          <el-button text :icon="RefreshRight" :disabled="!canRedo" title="重做（Ctrl+Y）" @click="redo" />
          <el-button text :icon="Refresh" title="刷新全部数据" @click="refreshToken++" />
          <span class="toolbar-divider"></span>
          <el-button text :icon="Minus" title="缩小" @click="zoomOut" />
          <span class="zoom-value" title="画布缩放（Ctrl + 滚轮）">{{ Math.round(zoom * 100) }}%</span>
          <el-button text :icon="Plus" title="放大" @click="zoomIn" />
          <el-button text :icon="Grid" title="重置画布适配" @click="fit" />
          <el-switch v-model="snap" size="small" inline-prompt active-text="吸附" inactive-text="自由" />
        </div>
        <div class="designer-topbar__right">
          <span v-if="dirty" class="dirty-mark">未保存</span>
          <el-button text :icon="View" title="预览大屏" @click="preview" />
          <el-button type="primary" :icon="Check" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </div>
    </template>

    <template #default>
      <div
        ref="stageViewportRef"
        class="stage-viewport"
        :class="{ 'is-panning': canvasPanning }"
        @click.self="handleStageClick"
        @wheel="onCanvasWheel"
        @pointerdown="onCanvasBackgroundPointerDown"
        @pointermove="onCanvasPanMove"
        @pointerup="onCanvasPanEnd"
        @pointercancel="onCanvasPanEnd">
        <div ref="stageCenterRef" class="stage-center" :style="stageStyle">
          <DashboardRenderer
            v-loading="loading"
            :model="model"
            :scale="zoom"
            design
            :selected-id="selectedId"
            :refresh-token="refreshToken"
            @select="(id: string) => selectedId = id"
            @geometry="handleGeometry"
            @drop-widget="handleDropContainer"
            @drop-stage="handleDropStage"
            @reorder="handleReorder"
            @blank-click="handleStageClick" />
        </div>
      </div>
    </template>

    <template #right>
      <DashboardInspector
        :widget="selectedWidget"
        :model="model"
        @refresh="refreshToken++"
        @remove="handleRemove()"
        @clone="handleClone" />
    </template>

    <template #footer>
      <div class="designer-footer">
        <span><el-icon><Grid /></el-icon>{{ layoutLabels[model.canvas.layout] || '自定义布局' }}</span>
        <span v-if="selectedWidget">
          <el-icon><FullScreen /></el-icon>
          {{ selectedWidget.name }} ·
          {{ selectedWidget.horizontalAnchor === 'right' ? '右侧' : '左侧' }}
          {{ formatGeometry(selectedWidget.x, selectedWidget.xUnit) }},
          {{ selectedWidget.verticalAnchor === 'bottom' ? '底部' : '顶部' }}
          {{ formatGeometry(selectedWidget.y, selectedWidget.yUnit) }} ·
          {{ formatGeometry(selectedWidget.w, selectedWidget.wUnit) }} ×
          {{ formatGeometry(selectedWidget.h, selectedWidget.hUnit) }}
        </span>
        <span v-else>点击画布空白处可设置全局样式</span>
        <span class="designer-footer__save">保存后即可获取独立预览页面</span>
      </div>
    </template>
  </LayoutDesigner>
</template>

<style lang="scss" scoped>
.palette-head {
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.palette-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  &__mark {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: 8px;
    color: #fff;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 1px;
    background: linear-gradient(145deg, #196dff, #16c5df);
    box-shadow: 0 5px 14px rgba(23, 139, 237, 0.25);
  }
  &__meta {
    flex: 1;
    min-width: 0;
  }
  strong {
    display: block;
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}
.palette-name {
  width: 100%;
  margin-top: 1px;
  :deep(.el-input__wrapper) {
    padding: 0 3px;
    box-shadow: none;
    background: transparent;
  }
  :deep(.el-input__wrapper:hover) {
    background: var(--el-fill-color-light);
  }
  :deep(.el-input__wrapper.is-focus) {
    background: var(--el-fill-color-lighter);
    box-shadow: inset 0 0 0 1px var(--el-color-primary-light-5);
  }
  :deep(.el-input__inner) {
    height: 20px;
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
}
.palette-tip {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: 12px;
  padding: 10px;
  border-radius: 6px;
  color: #7b8fa1;
  background: #f1f6fa;
  font-size: 11px;
  line-height: 1.6;
  .el-icon {
    flex: none;
    margin-top: 2px;
    color: #2d93cf;
  }
}
.left-tabs {
  :deep(.el-tabs__header) {
    position: sticky;
    top: 0;
    z-index: 6;
    margin: 0;
    padding: 0 12px;
    background: #fff;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  :deep(.el-tabs__nav) {
    width: 100%;
  }
  :deep(.el-tabs__item) {
    width: 50%;
    justify-content: center;
    font-size: 12px;
  }
}
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  i {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 16px;
    height: 15px;
    padding: 0 4px;
    box-sizing: border-box;
    border-radius: 8px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color);
    font-size: 9px;
    font-style: normal;
  }
}
.layers-panel {
  padding: 14px 12px 0;
  :deep(.fs-heading) {
    margin-bottom: 8px;
  }
  :deep(.fs-heading__title) {
    font-size: 13px;
  }
  :deep(.fs-heading__title::before) {
    height: 14px;
  }
}
.layer-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.layer-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
  height: 28px;
  padding-right: 8px;
  border: 0;
  border-radius: 4px;
  color: var(--el-text-color-regular);
  background: transparent;
  font: inherit;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  &:hover {
    color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }
  &.is-active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
  &__type {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 18px;
    height: 18px;
    border-radius: 4px;
    color: #fff;
    background: #82a8c6;
    font-size: 10px;
  }
  &.is-active &__type {
    background: var(--el-color-primary);
  }
  &__name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__z {
    flex: none;
    margin-left: auto;
    padding: 0 4px;
    border-radius: 3px;
    color: var(--el-text-color-placeholder);
    background: var(--el-fill-color-light);
    font-size: 9px;
    line-height: 16px;
  }
  &--container-layer {
    .layer-item__type {
      background: #659ec3;
    }
  }
}
.layer-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 28px 12px;
  border-radius: 6px;
  color: var(--el-text-color-placeholder);
  background: var(--el-fill-color-light);
  text-align: center;
  strong {
    color: var(--el-text-color-secondary);
    font-size: 11px;
    font-weight: 500;
  }
  small {
    font-size: 9px;
  }
  &__icon {
    width: 24px;
    height: 18px;
    margin-bottom: 4px;
    border: 1px solid var(--el-border-color);
    border-radius: 3px;
    box-shadow: 4px 4px 0 var(--el-fill-color-darker);
  }
}
.designer-topbar {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  gap: 8px;
  &__left,
  &__right {
    display: flex;
    align-items: center;
    min-width: 0;
  }
  &__left {
    flex: 1;
    overflow: hidden;
    :deep(.el-button) {
      flex: none;
      margin-left: 0;
      padding-left: 7px;
      padding-right: 7px;
    }
    :deep(.layout-back) {
      margin-right: 2px;
    }
  }
  &__right {
    flex: none;
    gap: 4px;
  }
}
.toolbar-divider {
  flex: none;
  width: 1px;
  height: 18px;
  margin: 0 2px;
  background: var(--el-border-color-light);
}
.dirty-mark {
  display: inline-flex;
  align-items: center;
  flex: none;
  color: #d48400;
  font-size: 11px;
  white-space: nowrap;
  &::before {
    content: '';
    width: 5px;
    height: 5px;
    margin-right: 4px;
    border-radius: 50%;
    background: currentColor;
  }
}
.zoom-value {
  min-width: 38px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  text-align: center;
}
.stage-viewport {
  width: 100%;
  height: 100%;
  padding: 35px;
  box-sizing: border-box;
  overflow: auto;
  background-color: #e9eef3;
  background-image:
    linear-gradient(45deg, #e2e8ee 25%, transparent 25%),
    linear-gradient(-45deg, #e2e8ee 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #e2e8ee 75%),
    linear-gradient(-45deg, transparent 75%, #e2e8ee 75%);
  background-size: 20px 20px;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0;
  cursor: grab;
  &.is-panning {
    cursor: grabbing;
    user-select: none;
  }
}
.stage-center {
  margin: 0 auto;
  position: relative;
  box-shadow: 0 12px 40px rgba(30, 63, 90, 0.24);
}
.designer-footer {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 18px;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    white-space: nowrap;
  }
  &__save {
    margin-left: auto;
  }
}
</style>
