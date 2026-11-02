<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import DashboardChart from './DashboardChart.vue'
import DashboardDecoration from './DashboardDecoration.vue'
import { formatNumber } from './utils'
import DashboardApi from '@/api/bi/DashboardApi'
import ApiUtil from '@/utils/ApiUtil'

const props = defineProps({
  widget: { type: Object, required: true },
  refreshToken: { type: Number, default: 0 },
})

const loading = ref(false)
const error = ref('')
const axis = ref<any>(null)
let requestSerial = 0
let timer: any = null

const isDataWidget = computed(() => ['Metric', 'Table', 'Line', 'Bar', 'Area', 'Pie', 'Funnel', 'Radar', 'Gauge'].includes(props.widget.type))

const metricRows = computed(() => {
  if (!axis.value) return []
  if (Array.isArray(axis.value.rows)) return axis.value.rows
  const categories = axis.value.x?.data || []
  return categories.map((name: any, index: number) => {
    const row: any = { x: name ?? '空值' }
    ;(axis.value.y || []).forEach((metric: any, metricIndex: number) => {
      row[`metric${metricIndex}`] = metric.data?.[index]
    })
    return row
  })
})

const metrics = computed(() => axis.value?.y || [])
const dimensions = computed(() => axis.value?.dimensions || [])

const load = () => {
  if (!isDataWidget.value) return
  if (!props.widget.data?.datasetId) {
    axis.value = null
    error.value = '请在右侧“数据”页签选择数据集'
    return
  }
  const serial = ++requestSerial
  loading.value = true
  error.value = ''
  DashboardApi.search({
    datasetId: props.widget.data.datasetId,
    preview: {
      mode: 'aggregate',
      filter: props.widget.data.filter || [],
      axis: props.widget.data.axis || { metrics: [], buckets: [] },
    },
  }).then((result: any) => {
    if (serial !== requestSerial) return
    if (ApiUtil.succeed(result)) {
      axis.value = result.data
    } else {
      error.value = ApiUtil.message(result) || '数据加载失败'
      axis.value = null
    }
  }).catch(() => {
    if (serial === requestSerial) error.value = '数据加载失败'
  }).finally(() => {
    if (serial === requestSerial) loading.value = false
  })
}

const scheduleLoad = () => {
  clearTimeout(timer)
  timer = setTimeout(load, 280)
}

watch(() => [props.widget.data, props.refreshToken], scheduleLoad, { deep: true })
onMounted(load)
onBeforeUnmount(() => {
  requestSerial++
  clearTimeout(timer)
})
</script>

<template>
  <div class="dashboard-content">
    <div v-if="widget.type === 'Text'" class="dashboard-text">
      {{ widget.content || '文本组件' }}
    </div>

    <DashboardDecoration
      v-else-if="['Border', 'DecorationHeader', 'DecorationScan', 'DecorationGrid', 'DecorationDivider'].includes(widget.type)"
      :widget="widget" />

    <div v-else-if="widget.type === 'Container'" class="layout-container">
      <slot></slot>
    </div>

    <div v-else-if="loading" class="dashboard-state">
      <el-skeleton :rows="3" animated />
    </div>
    <div v-else-if="error" class="dashboard-state dashboard-state--empty">
      <el-icon><Warning /></el-icon>
      <span>{{ error }}</span>
    </div>
    <div v-else-if="widget.type === 'Metric'" class="metric-panel">
      <template v-if="metrics.length">
        <div v-for="(metric, index) in metrics.slice(0, 4)" :key="index" class="metric-item">
          <div class="metric-item__label">{{ metric.label || `指标 ${index + 1}` }}</div>
          <div class="metric-item__value">{{ formatNumber((metric.data || []).reduce((sum: number, value: any) => sum + (Number(value) || 0), 0)) }}</div>
        </div>
      </template>
      <el-empty v-else description="暂无指标" :image-size="42" />
    </div>
    <el-table
      v-else-if="widget.type === 'Table'"
      :data="metricRows"
      height="100%"
      size="small"
      table-layout="auto"
      class="dashboard-table">
      <template v-if="dimensions.length">
        <el-table-column
          v-for="(dimension, index) in dimensions"
          :key="dimension.key || index"
          :label="dimension.label || `维度 ${index + 1}`"
          :prop="`__d${index}`"
          min-width="100">
          <template #default="{ row }">{{ row[`__d${index}`] ?? '空值' }}</template>
        </el-table-column>
      </template>
      <el-table-column v-else :label="axis?.x?.label || '维度'" prop="x" min-width="100" />
      <el-table-column
        v-for="(metric, index) in metrics"
        :key="index"
        :label="metric.label || `指标 ${index + 1}`"
        :prop="dimensions.length ? `__m${index}` : `metric${index}`"
        min-width="100">
        <template #default="{ row }">{{ formatNumber(row[dimensions.length ? `__m${index}` : `metric${index}`]) }}</template>
      </el-table-column>
    </el-table>
    <DashboardChart
      v-else
      :type="widget.type"
      :axis="axis"
      :style="widget.style" />
  </div>
</template>

<style lang="scss" scoped>
.dashboard-content {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.dashboard-text {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: v-bind('props.widget.style.verticalAlign === "middle" ? "center" : props.widget.style.verticalAlign === "bottom" ? "flex-end" : "flex-start"');
  justify-content: center;
  white-space: pre-wrap;
  line-height: v-bind('props.widget.style.lineHeight');
  letter-spacing: v-bind('`${props.widget.style.letterSpacing}px`');
  font-size: v-bind('`${props.widget.style.fontSize}px`');
  font-weight: v-bind('props.widget.style.fontWeight');
  text-align: v-bind('props.widget.style.align');
  overflow: auto;
}
.layout-container {
  width: 100%;
  height: 100%;
  position: relative;
}
.dashboard-state {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  box-sizing: border-box;
  overflow: hidden;
}
.dashboard-state--empty {
  flex-direction: column;
  gap: 8px;
  color: #7faac9;
  font-size: 12px;
  text-align: center;
}
.metric-panel {
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  align-items: center;
  gap: 12px;
  overflow: auto;
}
.metric-item {
  min-width: 0;
  text-align: center;
  &__label {
    margin-bottom: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #8eb8d9;
    font-size: 13px;
  }
  &__value {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: clamp(20px, 2.2vw, 34px);
    font-weight: 600;
    line-height: 1.15;
    color: #69e7ff;
    text-shadow: 0 0 16px rgba(31, 202, 255, 0.45);
  }
}
.dashboard-table {
  flex: 1;
  --el-table-bg-color: transparent !important;
  --el-table-tr-bg-color: transparent !important;
  --el-table-header-bg-color: rgba(13, 60, 94, 0.96) !important;
  --el-table-expanded-cell-bg-color: transparent !important;
  --el-table-row-hover-bg-color: rgba(31, 179, 235, 0.12);
  --el-table-border-color: rgba(101, 174, 219, 0.16);
  --el-table-text-color: #d9efff;
  --el-table-header-text-color: #b9ebff;
  background: transparent;
  :deep(.el-table__header-wrapper) {
    background: linear-gradient(180deg, rgba(22, 92, 137, 0.68), rgba(9, 45, 76, 0.52));
    box-shadow: inset 0 1px 0 rgba(117, 222, 255, 0.12);
  }
  :deep(.el-table__header th.el-table__cell) {
    height: 38px;
    padding: 0;
    border-right-color: rgba(120, 196, 235, 0.12);
    border-bottom-color: rgba(76, 195, 255, 0.34);
    background: linear-gradient(180deg, rgba(22, 92, 137, 0.96), rgba(9, 45, 76, 0.94)) !important;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.35px;
  }
  :deep(.el-table__header th.el-table__cell .cell) {
    padding: 0 14px;
    line-height: 38px;
    white-space: nowrap;
  }
  :deep(.el-table__body td.el-table__cell) {
    height: 34px;
    padding: 0;
    border-bottom-color: rgba(101, 174, 219, 0.11);
  }
  :deep(.el-table__body td.el-table__cell .cell) {
    padding: 0 14px;
    line-height: 34px;
  }
  :deep(.el-table__row:nth-child(even) > td.el-table__cell) {
    background: rgba(32, 112, 158, 0.07);
  }
  :deep(.el-table__row:hover > td.el-table__cell) {
    background: rgba(31, 179, 235, 0.13);
  }
  :deep(.el-table__body-wrapper),
  :deep(.el-table__empty-block),
  :deep(.el-table__empty-text) {
    background: transparent !important;
  }
  :deep(.el-table__fixed-right-patch) {
    background: #0d3a5c !important;
  }
  :deep(.el-table__header th.el-table__cell:first-child) {
    box-shadow: inset 2px 0 0 rgba(76, 195, 255, 0.38);
  }
  :deep(.el-table__inner-wrapper::before) {
    display: none;
  }
}
</style>
