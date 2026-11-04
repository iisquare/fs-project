import UIUtil from '@/utils/UIUtil'

export type DashboardWidgetType =
  | 'Container'
  | 'Text'
  | 'Metric'
  | 'Table'
  | 'Line'
  | 'Bar'
  | 'Area'
  | 'Pie'
  | 'Funnel'
  | 'Radar'
  | 'Gauge'
  | 'DecorationHeader'
  | 'DecorationScan'
  | 'DecorationGrid'
  | 'DecorationDivider'
  | 'Border'

export type GeometryUnit = 'px' | '%'
export type DashboardLayout = 'absolute' | 'relative' | 'fixed' | 'flex' | 'grid'

export interface DashboardStyle {
  backgroundColor: string
  backgroundImage: string
  color: string
  borderWidth: number
  borderStyle: string
  borderColor: string
  borderRadius: number
  paddingTop: number
  paddingRight: number
  paddingBottom: number
  paddingLeft: number
  marginTop: number
  marginRight: number
  marginBottom: number
  marginLeft: number
  align: 'left' | 'center' | 'right'
  verticalAlign: 'top' | 'middle' | 'bottom'
  opacity: number
  overflow: 'hidden' | 'auto' | 'visible'
  titleShow: boolean
  titleText: string
  titleSize: number
  titleColor: string
  titleAlign: 'left' | 'center' | 'right'
  fontSize: number
  fontFamily: string
  fontWeight: number
  lineHeight: number
  letterSpacing: number
  showLegend: boolean
  showLabel: boolean
  smooth: boolean
  borderVariant: 'tech' | 'simple' | 'corner'
  borderAccent: string
  decorationPattern: 'hud' | 'grid' | 'wave' | 'radar'
  animation: boolean
  glow: number
  shadowBlur: number
  shadowColor: string
  shadowOffsetX: number
  shadowOffsetY: number
  backdropBlur: number
  backgroundSize: 'cover' | 'contain' | '100% 100%'
  zIndex: number
}

export type DashboardComponentStyles = Partial<Record<DashboardWidgetType, Partial<DashboardStyle>>>

export interface DashboardData {
  datasetId?: number
  filter: any[]
  axis: {
    metrics: any[]
    buckets: any[]
  }
}

export interface DashboardWidget {
  id: string
  type: DashboardWidgetType
  name: string
  x: number
  y: number
  w: number
  h: number
  xUnit: GeometryUnit
  yUnit: GeometryUnit
  wUnit: GeometryUnit
  hUnit: GeometryUnit
  horizontalAnchor: 'left' | 'right'
  verticalAnchor: 'top' | 'bottom'
  layout: DashboardLayout
  gridColumns: number
  gridRows: number
  gridFill: boolean
  gridRowHeight: number
  gridGap: number
  flexDirection: 'row' | 'column' | 'row-reverse' | 'column-reverse'
  flexWrap: 'nowrap' | 'wrap' | 'wrap-reverse'
  justifyContent: 'flex-start' | 'center' | 'flex-end' | 'space-between' | 'space-around' | 'space-evenly'
  alignItems: 'stretch' | 'flex-start' | 'center' | 'flex-end' | 'baseline'
  flexGrow: number
  flexShrink: number
  flexBasis: string
  alignSelf: 'auto' | 'stretch' | 'flex-start' | 'center' | 'flex-end' | 'baseline'
  flexOrder: number
  content: string
  style: DashboardStyle
  data: DashboardData
  children: DashboardWidget[]
}

export interface DashboardModel {
  version: number
  canvas: {
    width: number
    height: number
    background: string
    backgroundImage: string
    gridSize: number
    showGrid: boolean
    displayWidth: number
    displayHeight: number
    displayWidthUnit: GeometryUnit
    displayHeightUnit: GeometryUnit
    layout: DashboardLayout
    columns: number
    rows: number
    fill: boolean
    rowHeight: number
    gap: number
    flexDirection: 'row' | 'column' | 'row-reverse' | 'column-reverse'
    flexWrap: 'nowrap' | 'wrap' | 'wrap-reverse'
    justifyContent: 'flex-start' | 'center' | 'flex-end' | 'space-between' | 'space-around' | 'space-evenly'
    alignItems: 'stretch' | 'flex-start' | 'center' | 'flex-end' | 'baseline'
  }
  /**
   * 模板通用组件样式，对所有组件生效。
   */
  commonComponentStyle?: Partial<DashboardStyle>
  /**
   * 模板显式定义的组件类型默认样式。创建大屏时复制，后续新增组件按类型应用。
   */
  componentDefaults?: DashboardComponentStyles
  widgets: DashboardWidget[]
}

export const styleDefaults = (): DashboardStyle => ({
  backgroundColor: 'rgba(8, 28, 57, 0.82)',
  backgroundImage: '',
  color: '#e9f7ff',
  borderWidth: 0,
  borderStyle: 'solid',
  borderColor: '#26b8f5',
  borderRadius: 8,
  paddingTop: 12,
  paddingRight: 14,
  paddingBottom: 12,
  paddingLeft: 14,
  marginTop: 0,
  marginRight: 0,
  marginBottom: 0,
  marginLeft: 0,
  align: 'left',
  verticalAlign: 'middle',
  opacity: 1,
  overflow: 'hidden',
  titleShow: false,
  titleText: '',
  titleSize: 15,
  titleColor: '#9fddff',
  titleAlign: 'left',
  fontSize: 16,
  fontFamily: '"Microsoft YaHei", "PingFang SC", sans-serif',
  fontWeight: 400,
  lineHeight: 1.6,
  letterSpacing: 0,
  showLegend: true,
  showLabel: false,
  smooth: true,
  borderVariant: 'tech',
  borderAccent: '#1ac8ff',
  decorationPattern: 'hud',
  animation: true,
  glow: 16,
  shadowBlur: 0,
  shadowColor: 'rgba(13, 188, 255, 0.35)',
  shadowOffsetX: 0,
  shadowOffsetY: 10,
  backdropBlur: 0,
  backgroundSize: 'cover',
  zIndex: 1,
})

export const dataDefaults = (): DashboardData => ({
  datasetId: undefined,
  filter: [],
  axis: {
    metrics: [],
    buckets: [],
  },
})

const titleFor = (type: DashboardWidgetType) => ({
  Container: '布局容器',
  Text: '文本',
  Metric: '指标卡',
  Table: '数据表格',
  Line: '折线图',
  Bar: '柱状图',
  Area: '区域图',
  Pie: '饼图',
  Funnel: '漏斗图',
  Radar: '雷达图',
  Gauge: '仪表盘',
  DecorationHeader: '科技标题',
  DecorationScan: '扫描光效',
  DecorationGrid: '网格背景',
  DecorationDivider: '分割装饰',
  Border: '科技边框',
}[type])

export const createWidget = (
  type: DashboardWidgetType,
  position: Partial<DashboardWidget> = {},
  componentStyle?: Partial<DashboardStyle>,
): DashboardWidget => {
  const style = styleDefaults()
  if (type === 'Text') {
    Object.assign(style, {
      backgroundColor: 'transparent',
      color: '#e9f7ff',
      fontSize: 28,
      fontWeight: 600,
      align: 'center',
    })
  } else if (type === 'Metric') {
    Object.assign(style, {
      backgroundColor: 'rgba(7, 34, 70, 0.92)',
      borderWidth: 1,
      align: 'center',
      titleShow: true,
    })
  } else if (type === 'Border') {
    Object.assign(style, {
      backgroundColor: 'rgba(2, 19, 42, 0.35)',
      borderWidth: 1,
    })
  } else if (type === 'DecorationHeader') {
    Object.assign(style, {
      backgroundColor: 'transparent',
      color: '#eaffff',
      fontSize: 28,
      fontWeight: 600,
      paddingTop: 0,
      paddingRight: 0,
      paddingBottom: 0,
      paddingLeft: 0,
    })
  } else if (type === 'DecorationScan' || type === 'DecorationGrid') {
    Object.assign(style, {
      backgroundColor: 'rgba(3, 24, 48, 0.2)',
      paddingTop: 0,
      paddingRight: 0,
      paddingBottom: 0,
      paddingLeft: 0,
    })
  } else if (type === 'DecorationDivider') {
    Object.assign(style, {
      backgroundColor: 'transparent',
      paddingTop: 0,
      paddingRight: 0,
      paddingBottom: 0,
      paddingLeft: 0,
    })
  } else if (type === 'Container') {
    Object.assign(style, {
      backgroundColor: 'rgba(4, 22, 46, 0.32)',
      borderWidth: 1,
      titleShow: true,
      titleText: '布局容器',
    })
  }
  const result: DashboardWidget = {
    id: UIUtil.uuid('widget-'),
    type,
    name: titleFor(type),
    x: 40,
    y: 40,
    w: 420,
    h: 260,
    xUnit: 'px',
    yUnit: 'px',
    wUnit: 'px',
    hUnit: 'px',
    horizontalAnchor: 'left',
    verticalAnchor: 'top',
    layout: 'absolute',
    gridColumns: 12,
    gridRows: 4,
    gridFill: false,
    gridRowHeight: 40,
    gridGap: 12,
    flexDirection: 'row',
    flexWrap: 'nowrap',
    justifyContent: 'flex-start',
    alignItems: 'stretch',
    flexGrow: 0,
    flexShrink: 1,
    flexBasis: 'auto',
    alignSelf: 'auto',
    flexOrder: 0,
    content: type === 'Text'
      ? '双击或在右侧修改文本'
      : type === 'DecorationHeader'
        ? '数据可视化'
        : '',
    style,
    data: dataDefaults(),
    children: [],
    ...position,
  }
  Object.assign(result.style, componentStyle || {}, position.style || {})
  return result
}

export const defaultDashboard = (): DashboardModel => ({
  version: 2,
  canvas: {
    width: 1920,
    height: 1080,
    background: 'linear-gradient(135deg, #020b1c 0%, #041d39 52%, #020c1e 100%)',
    backgroundImage: '',
    gridSize: 10,
    showGrid: true,
    displayWidth: 100,
    displayHeight: 100,
    displayWidthUnit: '%',
    displayHeightUnit: '%',
    layout: 'absolute',
    columns: 24,
    rows: 4,
    fill: false,
    rowHeight: 20,
    gap: 10,
    flexDirection: 'row',
    flexWrap: 'nowrap',
    justifyContent: 'flex-start',
    alignItems: 'stretch',
  },
  widgets: [],
})

export const widgetGroups = [
  {
    id: 'layout',
    name: '布局',
    children: [
      { id: 'Container', type: 'Container', label: '布局容器', icon: 'Grid' },
      { id: 'Text', type: 'Text', label: '文本', icon: 'Document' },
    ],
  },
  {
    id: 'decorations',
    name: '大屏装饰',
    children: [
      { id: 'DecorationHeader', type: 'DecorationHeader', label: '科技标题', icon: 'EditPen' },
      { id: 'Border', type: 'Border', label: '科技边框', icon: 'Crop' },
      { id: 'DecorationScan', type: 'DecorationScan', label: '扫描光效', icon: 'Aim' },
      { id: 'DecorationGrid', type: 'DecorationGrid', label: '网格背景', icon: 'Grid' },
      { id: 'DecorationDivider', type: 'DecorationDivider', label: '分割装饰', icon: 'Minus' },
    ],
  },
  {
    id: 'data',
    name: '数据指标',
    children: [
      { id: 'Metric', type: 'Metric', label: '指标卡', icon: 'DataLine' },
      { id: 'Table', type: 'Table', label: '数据表格', icon: 'Tickets' },
    ],
  },
  {
    id: 'charts',
    name: '常用图表',
    children: [
      { id: 'Line', type: 'Line', label: '折线图', icon: 'TrendCharts' },
      { id: 'Bar', type: 'Bar', label: '柱状图', icon: 'Histogram' },
      { id: 'Area', type: 'Area', label: '区域图', icon: 'DataAnalysis' },
      { id: 'Pie', type: 'Pie', label: '饼图', icon: 'PieChart' },
      { id: 'Funnel', type: 'Funnel', label: '漏斗图', icon: 'Filter' },
      { id: 'Radar', type: 'Radar', label: '雷达图', icon: 'Aim' },
      { id: 'Gauge', type: 'Gauge', label: '仪表盘', icon: 'Odometer' },
    ],
  },
]

export const findWidget = (widgets: DashboardWidget[], id: string): DashboardWidget | null => {
  for (const widget of widgets) {
    if (widget.id === id) return widget
    const child = findWidget(widget.children || [], id)
    if (child) return child
  }
  return null
}

export const findParent = (widgets: DashboardWidget[], id: string, parent: DashboardWidget | null = null): DashboardWidget | null | undefined => {
  for (const widget of widgets) {
    if (widget.id === id) return parent
    const result = findParent(widget.children || [], id, widget)
    if (result !== undefined) return result
  }
  return undefined
}

export const removeWidget = (widgets: DashboardWidget[], id: string): DashboardWidget | null => {
  const index = widgets.findIndex((widget) => widget.id === id)
  if (index >= 0) return widgets.splice(index, 1)[0]
  for (const widget of widgets) {
    const removed = removeWidget(widget.children || [], id)
    if (removed) return removed
  }
  return null
}

export const walkWidgets = (widgets: DashboardWidget[], callback: (widget: DashboardWidget) => void) => {
  widgets.forEach((widget) => {
    callback(widget)
    walkWidgets(widget.children || [], callback)
  })
}

export const componentStyleDefaults = (
  model: DashboardModel,
  type: DashboardWidgetType,
): Partial<DashboardStyle> => ({
  ...(model.commonComponentStyle || {}),
  ...(model.componentDefaults?.[type] || {}),
})

export const widgetTitleHeight = (widget: DashboardWidget) => (
  widget.style.titleShow ? Math.max(22, widget.style.titleSize * 1.25) + 6 : 0
)

export const widgetContentSize = (
  widget: DashboardWidget | null,
  widgets: DashboardWidget[],
  canvas: DashboardModel['canvas'],
): { width: number; height: number } => {
  if (!widget) return { width: canvas.width, height: canvas.height }
  const parent = findParent(widgets, widget.id)
  const available = parent ? widgetContentSize(parent, widgets, canvas) : { width: canvas.width, height: canvas.height }
  const parentLayout = parent?.layout || canvas.layout
  const columns = Math.max(1, parent?.gridColumns || canvas.columns)
  const rows = Math.max(1, parent?.gridRows || canvas.rows)
  const gridFill = parent?.gridFill ?? canvas.fill
  const rowHeight = Math.max(1, parent?.gridRowHeight || canvas.rowHeight)
  const gap = Math.max(0, parent?.gridGap ?? canvas.gap)
  const cellWidth = Math.max(1, (available.width - gap * (columns - 1)) / columns)
  const cellHeight = gridFill
    ? Math.max(1, (available.height - gap * (rows - 1)) / rows)
    : rowHeight
  const width = parentLayout === 'grid'
    ? Math.max(1, widget.w * cellWidth + Math.max(0, widget.w - 1) * gap)
    : widget.wUnit === '%' ? available.width * widget.w / 100 : widget.w
  const height = parentLayout === 'grid'
    ? Math.max(1, widget.h * cellHeight + Math.max(0, widget.h - 1) * gap)
    : widget.hUnit === '%' ? available.height * widget.h / 100 : widget.h
  return {
    width: Math.max(1, width - widget.style.paddingLeft - widget.style.paddingRight - widget.style.borderWidth * 2),
    height: Math.max(1, height - widget.style.paddingTop - widget.style.paddingBottom - widget.style.borderWidth * 2 - widgetTitleHeight(widget)),
  }
}

export const commonStyle = (style: DashboardStyle) => ({
  backgroundColor: style.backgroundColor,
  backgroundImage: style.backgroundImage ? `url("${style.backgroundImage}")` : undefined,
  backgroundSize: style.backgroundSize,
  backgroundPosition: 'center',
  color: style.color,
  borderWidth: `${style.borderWidth}px`,
  borderStyle: style.borderStyle,
  borderColor: style.borderColor,
  borderRadius: `${style.borderRadius}px`,
  padding: `${style.paddingTop}px ${style.paddingRight}px ${style.paddingBottom}px ${style.paddingLeft}px`,
  margin: `${style.marginTop}px ${style.marginRight}px ${style.marginBottom}px ${style.marginLeft}px`,
  opacity: style.opacity,
  overflow: style.overflow,
  fontFamily: style.fontFamily,
  boxShadow: style.shadowBlur > 0
    ? `${style.shadowOffsetX}px ${style.shadowOffsetY}px ${style.shadowBlur}px ${style.shadowColor}`
    : undefined,
  backdropFilter: style.backdropBlur > 0 ? `blur(${style.backdropBlur}px)` : undefined,
})
