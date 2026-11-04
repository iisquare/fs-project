<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RefreshLeft, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import DashboardPropertySection from './DashboardPropertySection.vue'
import {
  componentStyleDefaults,
  createWidget,
  findParent,
  widgetContentSize,
  type DashboardModel,
} from './model'

const props = defineProps({
  widget: { type: Object, default: null },
  model: { type: Object, required: true },
})

const backgroundUploadRef = ref<HTMLInputElement>()
const fillMode = ref<'solid' | 'image'>('solid')
const isRoot = computed(() => !props.widget)
const isContainer = computed(() => props.widget?.type === 'Container')
const isText = computed(() => props.widget?.type === 'Text')
const isChart = computed(() => ['Line', 'Bar', 'Area', 'Pie', 'Funnel', 'Radar'].includes(props.widget?.type))
const isDecoration = computed(() => ['Border', 'DecorationHeader', 'DecorationScan', 'DecorationGrid', 'DecorationDivider'].includes(props.widget?.type))
const isHeaderDecoration = computed(() => props.widget?.type === 'DecorationHeader')
const isScanDecoration = computed(() => props.widget?.type === 'DecorationScan')
const hasImageFill = computed(() => Boolean(isRoot.value ? props.model.canvas.backgroundImage : props.widget?.style.backgroundImage))
const placementMode = computed(() => {
  if (!props.widget) return props.model.canvas.layout
  return findParent(props.model.widgets, props.widget.id)?.layout || props.model.canvas.layout
})
const supportsSizeUnit = computed(() => placementMode.value !== 'grid')
const supportsPosition = computed(() => ['absolute', 'fixed', 'relative'].includes(placementMode.value))
const parentBox = computed(() => {
  if (!props.widget) {
    return {
      layout: props.model.canvas.layout,
      width: props.model.canvas.width,
      height: props.model.canvas.height,
      columns: props.model.canvas.columns,
      rows: props.model.canvas.rows,
      fill: props.model.canvas.fill,
      rowHeight: props.model.canvas.rowHeight,
      gap: props.model.canvas.gap,
    }
  }
  const parent = findParent(props.model.widgets, props.widget.id)
  if (!parent) {
    return {
      layout: props.model.canvas.layout,
      width: props.model.canvas.width,
      height: props.model.canvas.height,
      columns: props.model.canvas.columns,
      rows: props.model.canvas.rows,
      fill: props.model.canvas.fill,
      rowHeight: props.model.canvas.rowHeight,
      gap: props.model.canvas.gap,
    }
  }
  const parentSize = widgetContentSize(parent, props.model.widgets, props.model.canvas)
  return {
    layout: parent.layout,
    width: Math.max(40, parentSize.width),
    height: Math.max(40, parentSize.height),
    columns: parent.gridColumns,
    rows: parent.gridRows,
    fill: parent.gridFill,
    rowHeight: parent.gridRowHeight,
    gap: parent.gridGap,
  }
})
const convertUnitValue = (value: number, from: string, to: string, dimension: number) => {
  if (from === to) return value
  const converted = to === '%' ? value / Math.max(1, dimension) * 100 : value * dimension / 100
  return to === '%' ? Math.round(converted * 100) / 100 : Math.round(converted)
}
const changeUnit = (field: 'x' | 'y' | 'w' | 'h', unit: 'px' | '%') => {
  if (!props.widget) return
  const valueKey = field
  const unitKey = `${field}Unit`
  const dimension = field === 'x' || field === 'w' ? parentBox.value.width : parentBox.value.height
  props.widget[valueKey] = convertUnitValue(props.widget[valueKey], props.widget[unitKey] || 'px', unit, dimension)
  props.widget[unitKey] = unit
}
const changeHorizontalAnchor = (anchor: 'left' | 'right') => {
  if (!props.widget || (props.widget.horizontalAnchor || 'left') === anchor) return
  const dimension = parentBox.value.width
  const width = convertUnitValue(props.widget.w, props.widget.wUnit || 'px', 'px', dimension)
  const offset = convertUnitValue(props.widget.x, props.widget.xUnit || 'px', 'px', dimension)
  const nextOffset = Math.max(0, dimension - width - offset)
  props.widget.x = convertUnitValue(nextOffset, 'px', props.widget.xUnit || 'px', dimension)
  props.widget.horizontalAnchor = anchor
}
const changeVerticalAnchor = (anchor: 'top' | 'bottom') => {
  if (!props.widget || (props.widget.verticalAnchor || 'top') === anchor) return
  const dimension = parentBox.value.height
  const height = convertUnitValue(props.widget.h, props.widget.hUnit || 'px', 'px', dimension)
  const offset = convertUnitValue(props.widget.y, props.widget.yUnit || 'px', 'px', dimension)
  const nextOffset = Math.max(0, dimension - height - offset)
  props.widget.y = convertUnitValue(nextOffset, 'px', props.widget.yUnit || 'px', dimension)
  props.widget.verticalAnchor = anchor
}
const fillParent = () => {
  if (!props.widget || placementMode.value !== 'absolute') return
  Object.assign(props.widget, {
    x: 0,
    y: 0,
    w: 100,
    h: 100,
    xUnit: '%',
    yUnit: '%',
    wUnit: '%',
    hUnit: '%',
    horizontalAnchor: 'left',
    verticalAnchor: 'top',
    marginTop: 0,
    marginRight: 0,
    marginBottom: 0,
    marginLeft: 0,
  })
}
const layerSiblings = computed(() => {
  if (!props.widget) return []
  const parent = findParent(props.model.widgets, props.widget.id)
  return parent ? parent.children : props.model.widgets
})
const layoutOptions = [
  { value: 'absolute', label: '自由定位', description: '绝对坐标定位' },
  { value: 'relative', label: '相对定位', description: '文档流内偏移' },
  { value: 'fixed', label: '固定定位', description: '固定在画布视口' },
  { value: 'flex', label: '弹性布局', description: '自动排列与伸缩' },
  { value: 'grid', label: '栅格布局', description: '按行列网格排列' },
] as const
const setCanvasLayout = (layout: typeof layoutOptions[number]['value']) => {
  props.model.canvas.layout = layout
  if (layout === 'grid') props.model.canvas.showGrid = true
}
const updateWidgetField = (field: string, value: any) => {
  const widget = props.widget
  if (!widget || value === undefined || value === null) return
  widget[field] = value
}
const flexDirectionOptions = [
  { label: '水平排列', value: 'row' },
  { label: '垂直排列', value: 'column' },
  { label: '水平反向', value: 'row-reverse' },
  { label: '垂直反向', value: 'column-reverse' },
] as const
const flexWrapOptions = [
  { label: '不换行', value: 'nowrap' },
  { label: '自动换行', value: 'wrap' },
  { label: '反向换行', value: 'wrap-reverse' },
] as const
const justifyContentOptions = [
  { label: '起点对齐', value: 'flex-start' },
  { label: '居中对齐', value: 'center' },
  { label: '终点对齐', value: 'flex-end' },
  { label: '两端对齐', value: 'space-between' },
  { label: '环绕分布', value: 'space-around' },
  { label: '均匀分布', value: 'space-evenly' },
] as const
const alignItemsOptions = [
  { label: '拉伸填满', value: 'stretch' },
  { label: '起点对齐', value: 'flex-start' },
  { label: '居中对齐', value: 'center' },
  { label: '终点对齐', value: 'flex-end' },
  { label: '基线与文字对齐', value: 'baseline' },
] as const
const paddingFields = [
  { key: 'paddingTop', label: '上边距' },
  { key: 'paddingRight', label: '右边距' },
  { key: 'paddingBottom', label: '下边距' },
  { key: 'paddingLeft', label: '左边距' },
] as const
const marginFields = [
  { key: 'marginTop', label: '上边距' },
  { key: 'marginRight', label: '右边距' },
  { key: 'marginBottom', label: '下边距' },
  { key: 'marginLeft', label: '左边距' },
] as const

const fontFamilies = [
  { label: '系统默认', value: '"Microsoft YaHei", "PingFang SC", sans-serif' },
  { label: '等宽数字', value: '"DIN Alternate", "Bahnschrift", Consolas, monospace' },
  { label: '思源黑体', value: '"Source Han Sans SC", "Noto Sans CJK SC", sans-serif' },
  { label: '宋体', value: 'SimSun, serif' },
]
const fontWeightOptions = [
  { label: '细体 300', value: 300 },
  { label: '常规 400', value: 400 },
  { label: '中等 500', value: 500 },
  { label: '半粗 600', value: 600 },
  { label: '粗体 700', value: 700 },
]

const applyAlign = (horizontal?: 'left' | 'center' | 'right', vertical?: 'top' | 'middle' | 'bottom') => {
  if (!props.widget) return
  if (parentBox.value.layout === 'grid' && horizontal) {
    const columns = Math.max(1, parentBox.value.columns)
    const width = Math.max(1, Math.min(props.widget.w, columns))
    const maxX = Math.max(0, columns - width)
    props.widget.x = horizontal === 'left' ? 0 : horizontal === 'right' ? maxX : Math.round(maxX / 2)
  } else if (horizontal) {
    const width = convertUnitValue(props.widget.w, props.widget.wUnit || 'px', props.widget.xUnit || 'px', parentBox.value.width)
    const total = props.widget.xUnit === '%' ? 100 : parentBox.value.width
    const maxX = Math.max(0, total - width)
    props.widget.horizontalAnchor = horizontal === 'right' ? 'right' : 'left'
    props.widget.x = horizontal === 'left' || horizontal === 'right' ? 0 : Math.round(maxX / 2)
  }
  if (parentBox.value.layout === 'grid' && vertical) {
    const rowStep = parentBox.value.fill
      ? Math.max(1, (parentBox.value.height - parentBox.value.gap * (parentBox.value.rows - 1)) / parentBox.value.rows + parentBox.value.gap)
      : Math.max(1, parentBox.value.rowHeight + parentBox.value.gap)
    const rows = parentBox.value.fill
      ? parentBox.value.rows
      : Math.max(1, Math.floor((parentBox.value.height + parentBox.value.gap) / rowStep))
    const height = Math.max(1, Math.min(props.widget.h, rows))
    const maxY = Math.max(0, rows - height)
    props.widget.y = vertical === 'top' ? 0 : vertical === 'bottom' ? maxY : Math.round(maxY / 2)
  } else if (vertical) {
    const height = convertUnitValue(props.widget.h, props.widget.hUnit || 'px', props.widget.yUnit || 'px', parentBox.value.height)
    const total = props.widget.yUnit === '%' ? 100 : parentBox.value.height
    const maxY = Math.max(0, total - height)
    props.widget.verticalAnchor = vertical === 'bottom' ? 'bottom' : 'top'
    props.widget.y = vertical === 'top' || vertical === 'bottom' ? 0 : Math.round(maxY / 2)
  }
}

const setFillMode = (mode: 'solid' | 'image') => {
  fillMode.value = mode
  if (mode === 'solid') {
    if (isRoot.value) props.model.canvas.backgroundImage = ''
    else props.widget.style.backgroundImage = ''
  }
}

watch(
  () => props.widget?.id,
  () => {
    fillMode.value = hasImageFill.value ? 'image' : 'solid'
  },
  { immediate: true },
)
watch(hasImageFill, (image) => {
  if (image) fillMode.value = 'image'
})

const uploadBackground = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    input.value = ''
    return
  }
  if (file.size > 1024 * 1024) {
    ElMessage.warning('背景图片请控制在 1MB 以内，较大的图片建议使用图片地址')
    input.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    if (isRoot.value) props.model.canvas.backgroundImage = String(reader.result || '')
    else props.widget.style.backgroundImage = String(reader.result || '')
  }
  reader.readAsDataURL(file)
  input.value = ''
}

const resetAppearance = () => {
  if (!props.widget) return
  const zIndex = props.widget.style.zIndex
  Object.assign(
    props.widget.style,
    createWidget(
      props.widget.type,
      {},
      componentStyleDefaults(props.model as DashboardModel, props.widget.type),
    ).style,
  )
  props.widget.style.zIndex = zIndex
}
const changeCanvasDisplayUnit = (field: 'width' | 'height', unit: 'px' | '%') => {
  const valueKey = field === 'width' ? 'displayWidth' : 'displayHeight'
  const unitKey = field === 'width' ? 'displayWidthUnit' : 'displayHeightUnit'
  const base = field === 'width' ? props.model.canvas.width : props.model.canvas.height
  const currentUnit = props.model.canvas[unitKey] || 'px'
  if (currentUnit === unit) return
  const current = Number(props.model.canvas[valueKey]) || 0
  props.model.canvas[valueKey] = unit === '%'
    ? Math.round(current / Math.max(1, base) * 10000) / 100
    : Math.round(current * base / 100)
  props.model.canvas[unitKey] = unit
}
const setCanvasDisplayPreset = (percent: number) => {
  props.model.canvas.displayWidth = percent
  props.model.canvas.displayHeight = percent
  props.model.canvas.displayWidthUnit = '%'
  props.model.canvas.displayHeightUnit = '%'
}
const setLayer = (position: 'front' | 'back') => {
  if (!props.widget) return
  const levels = layerSiblings.value.map((item: any) => Number(item.style?.zIndex) || 0)
  props.widget.style.zIndex = position === 'front'
    ? Math.max(0, ...levels) + 1
    : Math.min(0, ...levels) - 1
}
</script>

<template>
  <div class="style-panel">
    <template v-if="isRoot">
      <DashboardPropertySection title="画布基准" description="像素坐标基准">
        <div class="field-grid field-grid--two">
          <label class="field-cell">
            <span>设计宽度</span>
            <el-input-number v-model="model.canvas.width" :controls="false" :min="800" :max="7680" />
          </label>
          <label class="field-cell">
            <span>设计高度</span>
            <el-input-number v-model="model.canvas.height" :controls="false" :min="600" :max="4320" />
          </label>
        </div>
        <div class="layout-mode-grid">
          <button
            type="button"
            v-for="option in layoutOptions"
            :key="option.value"
            :class="{ 'is-active': model.canvas.layout === option.value }"
            @click="setCanvasLayout(option.value)">
            <span class="layout-preview" :class="`layout-preview--${option.value}`">
              <template v-if="option.value === 'grid'"><i v-for="index in 9" :key="index"></i></template>
              <template v-else-if="option.value === 'flex'"><i v-for="index in 3" :key="index"></i></template>
              <template v-else><i></i><i></i></template>
            </span>
            <b>{{ option.label }}</b>
            <small>{{ option.description }}</small>
          </button>
        </div>
        <div v-if="model.canvas.layout === 'grid'" class="grid-submode section-gap">
          <button type="button" :class="{ 'is-active': !model.canvas.fill }" @click="model.canvas.fill = false">固定行高</button>
          <button type="button" :class="{ 'is-active': model.canvas.fill }" @click="model.canvas.fill = true">行列等分填满</button>
        </div>
        <div v-if="model.canvas.layout === 'grid'" class="field-grid field-grid--two section-gap">
          <label class="field-cell">
            <span>列数</span>
            <el-input-number v-model="model.canvas.columns" :controls="false" :min="1" :max="48" />
          </label>
          <label v-if="model.canvas.fill" class="field-cell">
            <span>行数</span>
            <el-input-number v-model="model.canvas.rows" :controls="false" :min="1" :max="48" />
          </label>
          <label v-else class="field-cell">
            <span>行高</span>
            <el-input-number v-model="model.canvas.rowHeight" :controls="false" :min="8" :max="200" />
          </label>
          <label class="field-cell">
            <span>间距</span>
            <el-input-number v-model="model.canvas.gap" :controls="false" :min="0" :max="80" />
          </label>
          <label class="field-cell">
            <span>辅助线</span>
            <el-switch v-model="model.canvas.showGrid" size="small" />
          </label>
        </div>
        <div v-if="model.canvas.layout === 'grid' && model.canvas.fill" class="grid-fill-tip">
          {{ model.canvas.columns }} 列 × {{ model.canvas.rows }} 行，每个单元格自动均分可用宽高
        </div>
        <div v-if="model.canvas.layout === 'flex'" class="field-grid field-grid--two section-gap">
          <label class="field-cell">
            <span>排列方向</span>
            <el-select v-model="model.canvas.flexDirection">
              <el-option v-for="item in flexDirectionOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>换行方式</span>
            <el-select v-model="model.canvas.flexWrap">
              <el-option v-for="item in flexWrapOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>主轴对齐</span>
            <el-select v-model="model.canvas.justifyContent">
              <el-option v-for="item in justifyContentOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>交叉轴对齐</span>
            <el-select v-model="model.canvas.alignItems">
              <el-option v-for="item in alignItemsOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>子项间距</span>
            <el-input-number v-model="model.canvas.gap" :controls="false" :min="0" :max="200" />
          </label>
        </div>
      </DashboardPropertySection>

      <DashboardPropertySection title="画布显示" description="相对可用屏幕">
        <div class="field-grid field-grid--two">
          <label class="field-cell">
            <span>显示宽度</span>
            <div class="field-with-unit">
              <el-input-number
                v-model="model.canvas.displayWidth"
                :controls="false"
                :min="model.canvas.displayWidthUnit === '%' ? 1 : 100"
                :max="model.canvas.displayWidthUnit === '%' ? 100 : 10000"
                :precision="model.canvas.displayWidthUnit === '%' ? 2 : 0" />
              <el-select
                :model-value="model.canvas.displayWidthUnit"
                class="unit-select"
                @change="(unit: 'px' | '%') => changeCanvasDisplayUnit('width', unit)">
                <el-option label="像素" value="px" />
                <el-option label="百分比" value="%" />
              </el-select>
            </div>
          </label>
          <label class="field-cell">
            <span>显示高度</span>
            <div class="field-with-unit">
              <el-input-number
                v-model="model.canvas.displayHeight"
                :controls="false"
                :min="model.canvas.displayHeightUnit === '%' ? 1 : 100"
                :max="model.canvas.displayHeightUnit === '%' ? 100 : 10000"
                :precision="model.canvas.displayHeightUnit === '%' ? 2 : 0" />
              <el-select
                :model-value="model.canvas.displayHeightUnit"
                class="unit-select"
                @change="(unit: 'px' | '%') => changeCanvasDisplayUnit('height', unit)">
                <el-option label="像素" value="px" />
                <el-option label="百分比" value="%" />
              </el-select>
            </div>
          </label>
        </div>
        <div class="display-presets">
          <button type="button" @click="setCanvasDisplayPreset(100)">100%</button>
          <button type="button" @click="setCanvasDisplayPreset(75)">75%</button>
          <button type="button" @click="setCanvasDisplayPreset(50)">50%</button>
        </div>
      </DashboardPropertySection>

      <DashboardPropertySection title="填充">
        <div class="fill-mode">
          <button type="button" :class="{ 'is-active': fillMode === 'solid' }" @click="setFillMode('solid')">颜色</button>
          <button type="button" :class="{ 'is-active': fillMode === 'image' }" @click="setFillMode('image')">图片</button>
        </div>
        <div v-if="fillMode === 'solid'" class="fill-row">
          <el-color-picker v-model="model.canvas.background" show-alpha />
          <el-input v-model="model.canvas.background" placeholder="渐变或颜色值" />
        </div>
        <template v-else>
          <div class="image-row">
            <el-input v-model="model.canvas.backgroundImage" placeholder="图片地址" clearable />
            <el-button :icon="Upload" @click="backgroundUploadRef?.click()">上传</el-button>
          </div>
          <label class="field-row">
            <span>适应方式</span>
            <el-select v-model="model.canvas.backgroundSize" size="small" style="width: 130px">
              <el-option label="覆盖" value="cover" />
              <el-option label="完整显示" value="contain" />
              <el-option label="拉伸" value="100% 100%" />
            </el-select>
          </label>
        </template>
        <input ref="backgroundUploadRef" type="file" accept="image/*" hidden @change="uploadBackground" />
      </DashboardPropertySection>

      <DashboardPropertySection title="画布辅助" :open="false">
        <label class="field-row">
          <span>布局参考线</span>
          <el-switch v-model="model.canvas.showGrid" size="small" />
        </label>
        <label class="field-row">
          <span>网格大小</span>
          <el-input-number v-model="model.canvas.gridSize" :controls="false" :min="4" :max="80" size="small" />
        </label>
      </DashboardPropertySection>
    </template>

    <template v-else>
      <DashboardPropertySection title="位置与尺寸" description="布局">
        <template #extra>
          <el-button text size="small" :icon="RefreshLeft" title="恢复默认外观" @click="resetAppearance" />
        </template>
        <div class="field-grid field-grid--two">
          <label class="field-cell">
            <span>宽度</span>
            <div v-if="supportsSizeUnit" class="field-with-unit">
              <el-input-number
                v-model="widget.w"
                :controls="false"
                :min="widget.wUnit === '%' ? 1 : 20"
                :precision="widget.wUnit === '%' ? 2 : 0" />
              <el-select :model-value="widget.wUnit || 'px'" class="unit-select" @change="(unit: 'px' | '%') => changeUnit('w', unit)">
                <el-option label="像素" value="px" />
                <el-option label="百分比" value="%" />
              </el-select>
            </div>
            <el-input-number v-else v-model="widget.w" :controls="false" :min="1" />
          </label>
          <label class="field-cell">
            <span>高度</span>
            <div v-if="supportsSizeUnit" class="field-with-unit">
              <el-input-number
                v-model="widget.h"
                :controls="false"
                :min="widget.hUnit === '%' ? 1 : 20"
                :precision="widget.hUnit === '%' ? 2 : 0" />
              <el-select :model-value="widget.hUnit || 'px'" class="unit-select" @change="(unit: 'px' | '%') => changeUnit('h', unit)">
                <el-option label="像素" value="px" />
                <el-option label="百分比" value="%" />
              </el-select>
            </div>
            <el-input-number v-else v-model="widget.h" :controls="false" :min="1" />
          </label>
          <label v-if="supportsPosition" class="field-cell">
            <span>水平基准</span>
            <div class="grid-submode">
              <button
                type="button"
                :class="{ 'is-active': (widget.horizontalAnchor || 'left') === 'left' }"
                @click="changeHorizontalAnchor('left')">左侧</button>
              <button
                type="button"
                :class="{ 'is-active': widget.horizontalAnchor === 'right' }"
                @click="changeHorizontalAnchor('right')">右侧</button>
            </div>
          </label>
          <label v-if="supportsPosition" class="field-cell">
            <span>垂直基准</span>
            <div class="grid-submode">
              <button
                type="button"
                :class="{ 'is-active': (widget.verticalAnchor || 'top') === 'top' }"
                @click="changeVerticalAnchor('top')">顶部</button>
              <button
                type="button"
                :class="{ 'is-active': widget.verticalAnchor === 'bottom' }"
                @click="changeVerticalAnchor('bottom')">底部</button>
            </div>
          </label>
          <label v-if="supportsPosition" class="field-cell">
            <span>{{ widget.horizontalAnchor === 'right' ? '右侧偏移' : '左侧偏移' }}</span>
            <div class="field-with-unit">
              <el-input-number
                v-model="widget.x"
                :controls="false"
                :min="0"
                :precision="widget.xUnit === '%' ? 2 : 0" />
              <el-select :model-value="widget.xUnit || 'px'" class="unit-select" @change="(unit: 'px' | '%') => changeUnit('x', unit)">
                <el-option label="像素" value="px" />
                <el-option label="百分比" value="%" />
              </el-select>
            </div>
          </label>
          <label v-if="supportsPosition" class="field-cell">
            <span>{{ widget.verticalAnchor === 'bottom' ? '底部偏移' : '顶部偏移' }}</span>
            <div class="field-with-unit">
              <el-input-number
                v-model="widget.y"
                :controls="false"
                :min="0"
                :precision="widget.yUnit === '%' ? 2 : 0" />
              <el-select :model-value="widget.yUnit || 'px'" class="unit-select" @change="(unit: 'px' | '%') => changeUnit('y', unit)">
                <el-option label="像素" value="px" />
                <el-option label="百分比" value="%" />
              </el-select>
            </div>
          </label>
        </div>
        <button
          v-if="['absolute', 'fixed'].includes(placementMode)"
          type="button"
          class="fill-parent-action"
          @click="fillParent">
          撑满父容器
        </button>
        <div v-if="supportsPosition" class="alignment-panel">
          <div class="alignment-preview">
            <span></span>
            <small>{{ parentBox.layout === 'grid' ? '栅格内对齐' : '父容器内对齐' }}</small>
          </div>
          <div class="alignment-actions">
            <button type="button" title="左对齐" @click="applyAlign('left')"><i class="align-icon align-icon--left"></i></button>
            <button type="button" title="水平居中" @click="applyAlign('center')"><i class="align-icon align-icon--center"></i></button>
            <button type="button" title="右对齐" @click="applyAlign('right')"><i class="align-icon align-icon--right"></i></button>
            <button type="button" title="顶部对齐" @click="applyAlign(undefined, 'top')"><i class="align-icon align-icon--top"></i></button>
            <button type="button" title="垂直居中" @click="applyAlign(undefined, 'middle')"><i class="align-icon align-icon--middle"></i></button>
            <button type="button" title="底部对齐" @click="applyAlign(undefined, 'bottom')"><i class="align-icon align-icon--bottom"></i></button>
          </div>
        </div>
        <div class="layer-row">
          <span class="layer-row__label">叠放层级</span>
          <el-input-number v-model="widget.style.zIndex" size="small" :controls="false" :min="-9999" :max="9999" />
        </div>
        <div class="layer-actions">
          <button type="button" @click="setLayer('front')">置于顶层</button>
          <button type="button" @click="setLayer('back')">置于底层</button>
        </div>
        <label class="field-row field-row--stack">
          <span>组件名称</span>
          <el-input v-model="widget.name" size="small" />
        </label>
      </DashboardPropertySection>

      <DashboardPropertySection v-if="placementMode === 'flex'" title="弹性子项" description="当前组件">
        <div class="field-grid field-grid--two">
          <label class="field-cell">
            <span>排列顺序</span>
            <el-input-number v-model="widget.flexOrder" :controls="false" :min="-999" :max="999" />
          </label>
          <label class="field-cell">
            <span>交叉轴对齐</span>
            <el-select v-model="widget.alignSelf">
              <el-option label="继承父级" value="auto" />
              <el-option label="拉伸填满" value="stretch" />
              <el-option label="起点对齐" value="flex-start" />
              <el-option label="居中" value="center" />
              <el-option label="终点对齐" value="flex-end" />
              <el-option label="基线对齐" value="baseline" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>放大比例</span>
            <el-input-number v-model="widget.flexGrow" :controls="false" :min="0" :max="100" />
          </label>
          <label class="field-cell">
            <span>收缩比例</span>
            <el-input-number v-model="widget.flexShrink" :controls="false" :min="0" :max="100" />
          </label>
        </div>
        <label class="field-cell section-gap">
          <span>弹性基准</span>
          <el-input v-model="widget.flexBasis" placeholder="auto / 120px / 30%" />
        </label>
      </DashboardPropertySection>

      <DashboardPropertySection v-if="isContainer" title="容器布局" description="子组件排列">
        <div class="layout-mode-grid">
          <button
            type="button"
            v-for="option in layoutOptions"
            :key="option.value"
            :class="{ 'is-active': widget.layout === option.value }"
            @click="widget.layout = option.value">
            <span class="layout-preview" :class="`layout-preview--${option.value}`">
              <template v-if="option.value === 'grid'"><i v-for="index in 9" :key="index"></i></template>
              <template v-else-if="option.value === 'flex'"><i v-for="index in 3" :key="index"></i></template>
              <template v-else><i></i><i></i></template>
            </span>
            <b>{{ option.label }}</b>
            <small>{{ option.description }}</small>
          </button>
        </div>
        <div v-if="widget.layout === 'grid'" class="grid-submode section-gap">
          <button type="button" :class="{ 'is-active': !widget.gridFill }" @click="widget.gridFill = false">固定行高</button>
          <button type="button" :class="{ 'is-active': widget.gridFill }" @click="widget.gridFill = true">行列等分填满</button>
        </div>
        <div v-if="widget.layout === 'grid'" class="field-grid field-grid--two section-gap">
          <label class="field-cell">
            <span>列数</span>
            <el-input-number
              :model-value="widget.gridColumns"
              :controls="false"
              :min="1"
              :max="48"
              @update:model-value="(value?: number) => updateWidgetField('gridColumns', value)" />
          </label>
          <label v-if="widget.gridFill" class="field-cell">
            <span>行数</span>
            <el-input-number
              :model-value="widget.gridRows"
              :controls="false"
              :min="1"
              :max="48"
              @update:model-value="(value?: number) => updateWidgetField('gridRows', value)" />
          </label>
          <label v-else class="field-cell">
            <span>行高</span>
            <el-input-number
              :model-value="widget.gridRowHeight"
              :controls="false"
              :min="8"
              :max="200"
              @update:model-value="(value?: number) => updateWidgetField('gridRowHeight', value)" />
          </label>
          <label class="field-cell">
            <span>间距</span>
            <el-input-number
              :model-value="widget.gridGap"
              :controls="false"
              :min="0"
              :max="80"
              @update:model-value="(value?: number) => updateWidgetField('gridGap', value)" />
          </label>
        </div>
        <div v-if="widget.layout === 'grid' && widget.gridFill" class="grid-fill-tip">
          {{ widget.gridColumns }} 列 × {{ widget.gridRows }} 行，每个单元格自动均分可用宽高
        </div>
        <div v-if="widget.layout === 'flex'" class="field-grid field-grid--two section-gap">
          <label class="field-cell">
            <span>排列方向</span>
            <el-select v-model="widget.flexDirection">
              <el-option v-for="item in flexDirectionOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>换行方式</span>
            <el-select v-model="widget.flexWrap">
              <el-option v-for="item in flexWrapOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>主轴对齐</span>
            <el-select v-model="widget.justifyContent">
              <el-option v-for="item in justifyContentOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>交叉轴对齐</span>
            <el-select v-model="widget.alignItems">
              <el-option v-for="item in alignItemsOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>子项间距</span>
            <el-input-number
              :model-value="widget.gridGap"
              :controls="false"
              :min="0"
              :max="200"
              @update:model-value="(value?: number) => updateWidgetField('gridGap', value)" />
          </label>
        </div>
      </DashboardPropertySection>

      <DashboardPropertySection title="填充" description="背景">
        <div class="fill-mode">
          <button type="button" :class="{ 'is-active': fillMode === 'solid' }" @click="setFillMode('solid')">颜色</button>
          <button type="button" :class="{ 'is-active': fillMode === 'image' }" @click="setFillMode('image')">图片</button>
        </div>
        <div v-if="fillMode === 'solid'" class="fill-row">
          <el-color-picker v-model="widget.style.backgroundColor" show-alpha />
          <el-input v-model="widget.style.backgroundColor" size="small" />
        </div>
        <template v-else>
          <div class="image-row">
            <el-input v-model="widget.style.backgroundImage" size="small" placeholder="图片地址" clearable />
            <el-button :icon="Upload" size="small" @click="backgroundUploadRef?.click()">上传</el-button>
          </div>
          <label class="field-row">
            <span>适应方式</span>
            <el-select v-model="widget.style.backgroundSize" size="small" style="width: 130px">
              <el-option label="覆盖" value="cover" />
              <el-option label="完整显示" value="contain" />
              <el-option label="拉伸" value="100% 100%" />
            </el-select>
          </label>
        </template>
        <input ref="backgroundUploadRef" type="file" accept="image/*" hidden @change="uploadBackground" />
      </DashboardPropertySection>

      <DashboardPropertySection title="描边与圆角" :open="widget.style.borderWidth > 0">
        <div class="stroke-row">
          <label class="stroke-field">
            <span>颜色</span>
            <el-color-picker v-model="widget.style.borderColor" />
          </label>
          <label class="stroke-field">
            <span>宽度</span>
            <el-input-number v-model="widget.style.borderWidth" :controls="false" :min="0" :max="24" />
          </label>
          <label class="stroke-field">
            <span>线型</span>
            <el-select v-model="widget.style.borderStyle" size="small">
              <el-option label="实线" value="solid" />
              <el-option label="虚线" value="dashed" />
              <el-option label="点线" value="dotted" />
            </el-select>
          </label>
        </div>
        <div class="field-grid field-grid--two section-gap">
          <label class="field-cell">
            <span>圆角</span>
            <el-input-number v-model="widget.style.borderRadius" :controls="false" :min="0" :max="160" />
          </label>
          <label class="field-cell">
            <span>不透明度</span>
            <el-input-number v-model="widget.style.opacity" :controls="false" :min="0.05" :max="1" :step="0.05" />
          </label>
        </div>
      </DashboardPropertySection>

      <DashboardPropertySection title="文字" :open="isText">
        <div class="font-row">
          <el-select v-model="widget.style.fontFamily" size="small">
            <el-option v-for="font in fontFamilies" :key="font.value" :label="font.label" :value="font.value" />
          </el-select>
          <el-color-picker v-model="widget.style.color" />
        </div>
        <div class="field-grid field-grid--two section-gap">
          <label class="field-cell">
            <span>字号</span>
            <el-input-number v-model="widget.style.fontSize" :controls="false" :min="10" :max="200" />
          </label>
          <label class="field-cell">
            <span>字重</span>
            <el-select v-model="widget.style.fontWeight" size="small">
              <el-option v-for="item in fontWeightOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-cell">
            <span>行高</span>
            <el-input-number v-model="widget.style.lineHeight" :controls="false" :min="1" :max="4" :step="0.1" />
          </label>
          <label class="field-cell">
            <span>字距</span>
            <el-input-number v-model="widget.style.letterSpacing" :controls="false" :min="-5" :max="40" />
          </label>
        </div>
        <div class="text-align-row">
          <button type="button" :class="{ 'is-active': widget.style.align === 'left' }" title="左对齐" @click="widget.style.align = 'left'"><i class="align-icon align-icon--left"></i></button>
          <button type="button" :class="{ 'is-active': widget.style.align === 'center' }" title="居中" @click="widget.style.align = 'center'"><i class="align-icon align-icon--center"></i></button>
          <button type="button" :class="{ 'is-active': widget.style.align === 'right' }" title="右对齐" @click="widget.style.align = 'right'"><i class="align-icon align-icon--right"></i></button>
          <span class="mini-divider"></span>
          <button type="button" :class="{ 'is-active': widget.style.verticalAlign === 'top' }" title="顶部" @click="widget.style.verticalAlign = 'top'"><i class="align-icon align-icon--top"></i></button>
          <button type="button" :class="{ 'is-active': widget.style.verticalAlign === 'middle' }" title="垂直居中" @click="widget.style.verticalAlign = 'middle'"><i class="align-icon align-icon--middle"></i></button>
          <button type="button" :class="{ 'is-active': widget.style.verticalAlign === 'bottom' }" title="底部" @click="widget.style.verticalAlign = 'bottom'"><i class="align-icon align-icon--bottom"></i></button>
        </div>
      </DashboardPropertySection>

      <DashboardPropertySection title="内外边距">
        <div class="spacing-title"><span>内边距</span><small>内容与边框之间的距离</small></div>
        <div class="spacing-grid">
          <label v-for="item in paddingFields" :key="item.key">
            <el-input-number v-model="widget.style[item.key]" :controls="false" :min="0" :max="200" />
            <span>{{ item.label }}</span>
          </label>
        </div>
        <div class="spacing-title section-gap"><span>外边距</span><small>组件与其他元素之间的距离</small></div>
        <div class="spacing-grid">
          <label v-for="item in marginFields" :key="item.key">
            <el-input-number v-model="widget.style[item.key]" :controls="false" :min="0" :max="200" />
            <span>{{ item.label }}</span>
          </label>
        </div>
        <label class="field-row">
          <span>溢出处理</span>
          <el-select v-model="widget.style.overflow" size="small" style="width: 120px">
            <el-option label="隐藏" value="hidden" />
            <el-option label="滚动" value="auto" />
            <el-option label="可见" value="visible" />
          </el-select>
        </label>
      </DashboardPropertySection>

      <DashboardPropertySection title="阴影与滤镜" :open="widget.style.shadowBlur > 0 || widget.style.backdropBlur > 0">
        <div class="field-grid field-grid--two">
          <label class="field-cell">
            <span>阴影模糊</span>
            <el-input-number v-model="widget.style.shadowBlur" :controls="false" :min="0" :max="120" />
          </label>
          <label class="field-cell">
            <span>背景模糊</span>
            <el-input-number v-model="widget.style.backdropBlur" :controls="false" :min="0" :max="40" />
          </label>
          <label class="field-cell">
            <span>水平偏移</span>
            <el-input-number v-model="widget.style.shadowOffsetX" :controls="false" :min="-100" :max="100" />
          </label>
          <label class="field-cell">
            <span>垂直偏移</span>
            <el-input-number v-model="widget.style.shadowOffsetY" :controls="false" :min="-100" :max="100" />
          </label>
        </div>
        <div class="fill-row section-gap">
          <el-color-picker v-model="widget.style.shadowColor" show-alpha />
          <el-input v-model="widget.style.shadowColor" size="small" />
        </div>
      </DashboardPropertySection>

      <DashboardPropertySection title="标题" :open="widget.style.titleShow">
        <label class="field-row">
          <span>显示标题</span>
          <el-switch v-model="widget.style.titleShow" size="small" />
        </label>
        <template v-if="widget.style.titleShow">
          <label class="field-row field-row--stack section-gap">
            <span>标题文字</span>
            <el-input v-model="widget.style.titleText" size="small" :placeholder="widget.name" />
          </label>
          <div class="field-grid field-grid--two section-gap">
            <label class="field-cell">
              <span>字号</span>
              <el-input-number v-model="widget.style.titleSize" :controls="false" :min="10" :max="80" />
            </label>
            <label class="field-cell">
              <span>颜色</span>
              <el-color-picker v-model="widget.style.titleColor" />
            </label>
          </div>
        </template>
      </DashboardPropertySection>

      <DashboardPropertySection v-if="isChart" title="图表" :open="false">
        <label class="field-row">
          <span>显示图例</span>
          <el-switch v-model="widget.style.showLegend" size="small" />
        </label>
        <label class="field-row">
          <span>数值标签</span>
          <el-switch v-model="widget.style.showLabel" size="small" />
        </label>
        <label v-if="['Line', 'Area'].includes(widget.type)" class="field-row">
          <span>平滑曲线</span>
          <el-switch v-model="widget.style.smooth" size="small" />
        </label>
      </DashboardPropertySection>

      <DashboardPropertySection v-if="isDecoration" title="装饰样式" description="科技大屏风格">
        <label class="field-row">
          <span>强调色</span>
          <el-color-picker v-model="widget.style.borderAccent" />
        </label>
        <label class="field-row">
          <span>光晕强度</span>
          <el-slider v-model="widget.style.glow" :min="0" :max="40" :show-tooltip="false" />
        </label>
        <label class="field-row">
          <span>动态效果</span>
          <el-switch v-model="widget.style.animation" size="small" />
        </label>
        <label v-if="widget.type === 'Border'" class="field-row">
          <span>边框形态</span>
          <el-select v-model="widget.style.borderVariant" size="small" style="width: 120px">
            <el-option label="科技切角" value="tech" />
            <el-option label="四角描边" value="corner" />
            <el-option label="细线" value="simple" />
          </el-select>
        </label>
        <label v-if="isScanDecoration" class="field-row">
          <span>扫描形态</span>
          <el-select v-model="widget.style.decorationPattern" size="small" style="width: 120px">
            <el-option label="平视扫描" value="hud" />
            <el-option label="网格" value="grid" />
            <el-option label="波纹" value="wave" />
            <el-option label="雷达" value="radar" />
          </el-select>
        </label>
        <label v-if="isHeaderDecoration" class="field-row field-row--stack section-gap">
          <span>标题内容</span>
          <el-input v-model="widget.content" size="small" />
        </label>
      </DashboardPropertySection>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.style-panel {
  --property-label: var(--el-text-color-secondary);
  --panel-control-height: 28px;
  --el-component-size: var(--panel-control-height);
  --el-component-size-small: var(--panel-control-height);
  :deep(.el-form-item) {
    margin-bottom: 8px;
  }
  :deep(.el-input),
  :deep(.el-select),
  :deep(.el-button) {
    box-sizing: border-box;
    height: var(--panel-control-height);
    min-height: var(--panel-control-height);
  }
  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    height: 100%;
    min-height: 0;
    box-sizing: border-box;
  }
  :deep(.el-input-number) {
    width: 100%;
    height: var(--panel-control-height);
    .el-input__inner {
      text-align: left;
    }
  }
  :deep(.el-color-picker) {
    width: 32px;
    height: var(--panel-control-height);
    vertical-align: top;
  }
  :deep(.el-color-picker__trigger) {
    width: 100%;
    height: 100%;
  }
}
.field-grid {
  display: grid;
  gap: 8px;
  &--two {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
.field-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  > span {
    color: var(--property-label);
    font-size: 10px;
    line-height: 16px;
  }
}
.field-with-unit {
  display: flex;
  min-width: 0;
  :deep(.el-input-number) {
    flex: 1;
    min-width: 0;
  }
}
.unit-select {
  flex: none;
  width: 68px;
  margin-left: 3px;
  :deep(.el-select__wrapper) {
    min-height: var(--panel-control-height);
    padding: 0 3px 0 6px;
  }
  :deep(.el-select__selected-item) {
    overflow: visible;
    font-size: 10px;
    white-space: nowrap;
  }
}
.display-presets {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 8px;
  button {
    height: var(--panel-control-height);
    border: 1px solid var(--el-border-color-light);
    border-radius: 4px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-lighter);
    font: inherit;
    font-size: 10px;
    cursor: pointer;
    &:hover {
      color: var(--el-color-primary);
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-9);
    }
  }
}
.grid-submode {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3px;
  padding: 3px;
  border-radius: 5px;
  background: var(--el-fill-color-light);
  button {
    height: 24px;
    border: 0;
    border-radius: 3px;
    color: var(--el-text-color-secondary);
    background: transparent;
    font: inherit;
    font-size: 10px;
    cursor: pointer;
    &.is-active {
      color: var(--el-text-color-primary);
      background: #fff;
      box-shadow: 0 1px 3px rgba(26, 57, 86, 0.08);
    }
  }
}
.grid-fill-tip {
  margin-top: 7px;
  padding: 7px 8px;
  border: 1px solid var(--el-color-primary-light-8);
  border-radius: 4px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 9px;
  line-height: 1.5;
}
.fill-parent-action {
  width: 100%;
  height: var(--panel-control-height);
  margin-top: 7px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
  font: inherit;
  font-size: 10px;
  cursor: pointer;
  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }
}
.section-gap {
  margin-top: 10px;
}
.field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 30px;
  gap: 10px;
  margin-top: 6px;
  color: var(--el-text-color-regular);
  font-size: 11px;
  > span {
    flex: none;
  }
  .el-slider {
    flex: 1;
    min-width: 80px;
  }
  &--stack {
    align-items: flex-start;
    flex-direction: column;
    gap: 5px;
    .el-input,
    .el-select {
      width: 100%;
    }
  }
}
.layout-mode-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
  margin-top: 10px;
  button {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
    min-width: 0;
    padding: 8px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 5px;
    color: var(--el-text-color-regular);
    background: #fff;
    font: inherit;
    text-align: left;
    cursor: pointer;
    transition: border-color 0.16s, background 0.16s;
    &:hover {
      border-color: var(--el-color-primary-light-5);
    }
    &.is-active {
      border-color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
    b {
      font-size: 11px;
      font-weight: 500;
    }
    small {
      color: var(--el-text-color-placeholder);
      font-size: 9px;
    }
  }
}
.layout-preview {
  position: relative;
  display: block;
  width: 100%;
  height: 30px;
  margin-bottom: 4px;
  border-radius: 3px;
  background: var(--el-fill-color-light);
  &--absolute {
    i {
      position: absolute;
      border: 1px solid var(--el-color-primary);
      border-radius: 2px;
      background: var(--el-color-primary-light-8);
      &:first-child {
        left: 9px;
        top: 7px;
        width: 24px;
        height: 12px;
      }
      &:last-child {
        right: 8px;
        bottom: 5px;
        width: 17px;
        height: 10px;
      }
    }
  }
  &--relative {
    i {
      position: absolute;
      border: 1px solid var(--el-color-primary);
      border-radius: 2px;
      background: var(--el-color-primary-light-8);
      &:first-child {
        left: 10px;
        top: 6px;
        width: 37px;
        height: 18px;
      }
      &:last-child {
        left: 16px;
        top: 11px;
        width: 37px;
        height: 14px;
        background: color-mix(in srgb, var(--el-color-primary-light-8) 60%, #fff);
      }
    }
  }
  &--fixed {
    &::before {
      content: '';
      position: absolute;
      left: 50%;
      top: 50%;
      width: 10px;
      height: 10px;
      border: 2px solid var(--el-color-primary);
      border-radius: 50% 50% 50% 0;
      transform: translate(-50%, -60%) rotate(-45deg);
    }
    i {
      position: absolute;
      left: 26%;
      top: 6px;
      width: 48%;
      height: 18px;
      border: 1px dashed var(--el-color-primary-light-5);
      border-radius: 2px;
    }
  }
  &--flex {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 6px;
    box-sizing: border-box;
    i {
      flex: 1;
      height: 18px;
      border-radius: 2px;
      background: var(--el-color-primary-light-8);
    }
  }
  &--grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 2px;
    padding: 4px;
    box-sizing: border-box;
    i {
      border-radius: 1px;
      background: var(--el-color-primary-light-8);
    }
  }
}
.layout-mode-grid button:last-child:nth-child(odd) {
  grid-column: 1 / -1;
}
.alignment-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 92px;
  gap: 8px;
  margin-top: 9px;
}
.alignment-preview {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  min-height: 66px;
  padding-bottom: 6px;
  box-sizing: border-box;
  border: 1px dashed var(--el-border-color);
  border-radius: 5px;
  background:
    linear-gradient(var(--el-fill-color-lighter) 1px, transparent 1px),
    linear-gradient(90deg, var(--el-fill-color-lighter) 1px, transparent 1px);
  background-size: 10px 10px;
  span {
    width: 32px;
    height: 19px;
    border: 1px solid var(--el-color-primary);
    border-radius: 2px;
    background: var(--el-color-primary-light-8);
  }
  small {
    position: absolute;
    left: 6px;
    top: 5px;
    color: var(--el-text-color-placeholder);
    font-size: 9px;
  }
}
.alignment-actions,
.text-align-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 3px;
  button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: var(--panel-control-height);
    padding: 0;
    border: 1px solid transparent;
    border-radius: 3px;
    background: var(--el-fill-color-light);
    cursor: pointer;
    &:hover {
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-9);
    }
    &.is-active {
      color: var(--el-color-primary);
      border-color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
  }
}
.align-icon {
  position: relative;
  display: block;
  width: 15px;
  height: 12px;
  &::before,
  &::after {
    content: '';
    position: absolute;
    background: currentColor;
    opacity: 0.8;
  }
  &::before {
    width: 1px;
    height: 12px;
  }
  &::after {
    width: 10px;
    height: 3px;
    top: 2px;
  }
  &--left::before { left: 0; }
  &--left::after { left: 2px; }
  &--center::before { left: 50%; }
  &--center::after { left: 50%; transform: translateX(-50%); }
  &--right::before { right: 0; }
  &--right::after { right: 2px; }
  &--top::before,
  &--middle::before,
  &--bottom::before {
    width: 12px;
    height: 1px;
  }
  &--top::after,
  &--middle::after,
  &--bottom::after {
    width: 3px;
    height: 9px;
    top: 2px;
    left: 2px;
  }
  &--top::before { top: 0; left: 0; }
  &--top::after { top: 2px; left: 2px; }
  &--middle::before { top: 50%; left: 0; }
  &--middle::after { top: 50%; left: 2px; transform: translateY(-50%); }
  &--bottom::before { bottom: 0; left: 0; }
  &--bottom::after { bottom: 0; left: 2px; }
}
.fill-mode {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3px;
  padding: 3px;
  border-radius: 5px;
  background: var(--el-fill-color-light);
  button {
    height: 25px;
    border: 0;
    border-radius: 3px;
    color: var(--el-text-color-secondary);
    background: transparent;
    font: inherit;
    font-size: 10px;
    cursor: pointer;
    &.is-active {
      color: var(--el-text-color-primary);
      background: #fff;
      box-shadow: 0 1px 3px rgba(26, 57, 86, 0.08);
    }
  }
}
.fill-row,
.stroke-row,
.font-row,
.image-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  .el-input {
    flex: 1;
    min-width: 0;
  }
}
.stroke-row {
  display: grid;
  grid-template-columns: 34px minmax(52px, 0.55fr) minmax(0, 1fr);
  align-items: end;
  :deep(.el-input-number) {
    width: 100%;
  }
}
.stroke-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  color: var(--el-text-color-secondary);
  font-size: 10px;
}
.font-row {
  .el-select {
    flex: 1;
    min-width: 0;
  }
}
.spacing-title {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 6px;
  color: var(--el-text-color-regular);
  font-size: 10px;
  small {
    color: var(--el-text-color-placeholder);
  }
}
.spacing-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 4px;
  > label {
    min-width: 0;
    text-align: center;
    span {
      color: var(--el-text-color-placeholder);
      font-size: 9px;
    }
  }
  :deep(.el-input-number) {
    margin-bottom: 2px;
  }
}
.text-align-row {
  grid-template-columns: repeat(7, minmax(0, 1fr));
  margin-top: 9px;
  .mini-divider {
    width: 1px;
    justify-self: center;
    background: var(--el-border-color-lighter);
  }
}
.image-row :deep(.el-button) {
  height: var(--panel-control-height);
}
.layer-row {
  display: grid;
  grid-template-columns: auto 92px;
  align-items: center;
  justify-content: space-between;
  gap: 7px;
  margin-top: 10px;
  &__label {
    color: var(--el-text-color-secondary);
    font-size: 10px;
  }
}
.layer-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin-top: 7px;
  button {
    height: var(--panel-control-height);
    padding: 0 8px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 4px;
    color: var(--el-text-color-regular);
    background: #fff;
    font: inherit;
    font-size: 10px;
    cursor: pointer;
    transition: color 0.16s, border-color 0.16s, background-color 0.16s;
    &:hover {
      color: var(--el-color-primary);
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-9);
    }
  }
}
</style>
