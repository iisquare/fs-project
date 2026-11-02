<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { formatNumber } from './utils'

const props = defineProps({
  type: { type: String, required: true },
  axis: { type: Object, default: null },
  style: { type: Object, required: true },
})

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
let observer: ResizeObserver | null = null
const colors = ['#12d6df', '#2b8cff', '#73e6b4', '#ffbd4a', '#ff6f91', '#9b8cff', '#4dd0e1']
const axisSplitNumber = 5

const niceAxisMax = (value: number) => {
  const safeValue = Math.max(1, value)
  const roughInterval = safeValue / axisSplitNumber
  const exponent = Math.floor(Math.log10(roughInterval))
  const magnitude = Math.pow(10, exponent)
  const fraction = roughInterval / magnitude
  const niceFraction = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 3 ? 3 : fraction <= 5 ? 5 : 10
  return niceFraction * magnitude * axisSplitNumber
}

const textColor = () => props.style?.color || '#e9f7ff'
const subtleText = () => '#8eb8d9'

const categoryData = () => props.axis?.x?.data || []
const metrics = () => props.axis?.y || []

const baseOption = () => ({
  color: colors,
  animationDuration: 520,
  textStyle: { color: textColor(), fontFamily: 'inherit', fontSize: 13 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(3, 18, 40, .94)',
    borderColor: 'rgba(38, 184, 245, .55)',
    textStyle: { color: '#e9f7ff', fontSize: 12 },
  },
  legend: {
    show: props.style?.showLegend !== false,
    top: 2,
    right: 8,
    type: 'scroll',
    textStyle: { color: subtleText(), fontSize: 11 },
    itemWidth: 9,
    itemHeight: 7,
  },
})

const cartesianOption = () => {
  const isBar = props.type === 'Bar'
  const isArea = props.type === 'Area'
  return {
    ...baseOption(),
    grid: { left: 44, right: 18, top: 42, bottom: 34, containLabel: true },
    xAxis: {
      type: 'category',
      data: categoryData(),
      boundaryGap: isBar,
      axisLine: { lineStyle: { color: 'rgba(112, 178, 219, .32)' } },
      axisTick: { show: false },
      axisLabel: { color: subtleText(), fontSize: 11, hideOverlap: true },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(112, 178, 219, .13)', type: 'dashed' } },
      axisLabel: { color: subtleText(), fontSize: 11 },
    },
    series: metrics().map((metric: any, index: number) => ({
      name: metric.label || `指标 ${index + 1}`,
      type: isBar ? 'bar' : 'line',
      data: metric.data || [],
      smooth: isBar ? false : props.style?.smooth !== false,
      showSymbol: !isBar,
      symbolSize: 6,
      barMaxWidth: 28,
      areaStyle: isArea ? {
        opacity: 0.22,
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: colors[index % colors.length] },
          { offset: 1, color: 'rgba(12, 90, 150, 0)' },
        ]),
      } : undefined,
      lineStyle: { width: 2 },
      label: { show: props.style?.showLabel === true, color: textColor(), fontSize: 11 },
    })),
  }
}

const pieOption = () => {
  const metric = metrics()[0] || { label: '指标', data: [] }
  return {
    ...baseOption(),
    tooltip: { ...baseOption().tooltip, trigger: 'item' },
    legend: {
      ...baseOption().legend,
      orient: 'vertical',
      top: 'middle',
      right: 8,
    },
    series: [{
      name: metric.label,
      type: 'pie',
      radius: ['38%', '68%'],
      center: ['42%', '53%'],
      data: categoryData().map((name: any, index: number) => ({
        name: name ?? '空值',
        value: Number(metric.data?.[index]) || 0,
      })),
      label: {
        show: props.style?.showLabel === true,
        color: textColor(),
        formatter: '{b}\n{d}%',
        fontSize: 11,
      },
      itemStyle: { borderColor: '#06152c', borderWidth: 2 },
      emphasis: { scaleSize: 6 },
    }],
  }
}

const funnelOption = () => {
  const metric = metrics()[0] || { label: '指标', data: [] }
  return {
    ...baseOption(),
    tooltip: { ...baseOption().tooltip, trigger: 'item' },
    legend: { ...baseOption().legend, orient: 'vertical', right: 8, top: 'middle' },
    series: [{
      name: metric.label,
      type: 'funnel',
      left: '5%',
      right: '26%',
      top: 34,
      bottom: 14,
      minSize: '20%',
      sort: 'descending',
      gap: 4,
      label: {
        show: true,
        color: textColor(),
        position: 'inside',
        fontSize: 11,
      },
      itemStyle: { borderColor: '#081b35', borderWidth: 1 },
      data: categoryData().map((name: any, index: number) => ({
        name: name ?? '空值',
        value: Number(metric.data?.[index]) || 0,
      })),
    }],
  }
}

const radarOption = () => {
  const categories = categoryData()
  const rawMax = Math.max(1, ...metrics().flatMap((metric: any) =>
    (metric.data || []).map((value: any) => Number(value) || 0)))
  const max = niceAxisMax(rawMax)
  return {
    ...baseOption(),
    tooltip: { ...baseOption().tooltip, trigger: 'item' },
    radar: {
      center: ['50%', '55%'],
      radius: '63%',
      splitNumber: axisSplitNumber,
      indicator: categories.map((name: any) => ({ name: name ?? '空值', max })),
      axisName: { color: subtleText(), fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(112, 178, 219, .2)' } },
      splitArea: { areaStyle: { color: ['rgba(12, 60, 102, .1)', 'rgba(12, 60, 102, .2)'] } },
      axisLine: { lineStyle: { color: 'rgba(112, 178, 219, .2)' } },
    },
    series: [{
      type: 'radar',
      data: metrics().map((metric: any, index: number) => ({
        value: metric.data || [],
        name: metric.label || `指标 ${index + 1}`,
        areaStyle: { opacity: 0.12 },
      })),
    }],
  }
}

const gaugeOption = () => {
  const metric = metrics()[0] || { label: '指标', data: [] }
  const values = (metric.data || []).map((value: any) => Number(value) || 0)
  const value = values.length ? values.reduce((sum: number, current: number) => sum + current, 0) : 0
  const max = niceAxisMax(Math.max(value, 100))
  return {
    ...baseOption(),
    tooltip: { show: false },
    legend: { show: false },
    series: [{
      type: 'gauge',
      startAngle: 210,
      endAngle: -30,
      min: 0,
      max,
      splitNumber: axisSplitNumber,
      radius: '82%',
      center: ['50%', '57%'],
      progress: {
        show: true,
        width: 12,
        itemStyle: { color: colors[0] },
      },
      axisLine: { lineStyle: { width: 12, color: [[1, 'rgba(74, 132, 175, .22)']] } },
      splitLine: { show: false },
      axisTick: { show: false },
      axisLabel: { show: false },
      pointer: { width: 4, length: '56%', itemStyle: { color: colors[0] } },
      anchor: { show: true, size: 10, itemStyle: { color: colors[0] } },
      title: { color: subtleText(), fontSize: 12, offsetCenter: [0, '64%'] },
      detail: {
        valueAnimation: true,
        color: textColor(),
        fontSize: 24,
        fontWeight: 600,
        offsetCenter: [0, '34%'],
        formatter: (value: number) => formatNumber(value),
      },
      data: [{ value, name: metric.label || '指标' }],
    }],
  }
}

const render = async () => {
  await nextTick()
  if (!chartRef.value || !props.axis) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(chartRef.value)
  }
  let option: any
  switch (props.type) {
    case 'Pie':
      option = pieOption()
      break
    case 'Funnel':
      option = funnelOption()
      break
    case 'Radar':
      option = radarOption()
      break
    case 'Gauge':
      option = gaugeOption()
      break
    default:
      option = cartesianOption()
  }
  chart.setOption(option, true)
  chart.resize()
}

watch(() => [props.axis, props.type, props.style], render, { deep: true })

onMounted(render)
onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="chartRef" class="dashboard-chart"></div>
</template>

<style scoped>
.dashboard-chart {
  width: 100%;
  height: 100%;
  min-height: 80px;
}
</style>
