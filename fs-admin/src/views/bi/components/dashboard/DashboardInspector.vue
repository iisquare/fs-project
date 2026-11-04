<script setup lang="ts">
import { computed, ref, watch, type PropType } from 'vue'
import { Delete, Plus, Rank } from '@element-plus/icons-vue'
import DashboardTemplateApi from '@/api/bi/DashboardTemplateApi'
import DatasetApi from '@/api/bi/DatasetApi'
import DataQueryFilter from '@/components/Data/DataQueryFilter.vue'
import DashboardPropertySection from './DashboardPropertySection.vue'
import DashboardStylePanel from './DashboardStylePanel.vue'

const props = defineProps({
  widget: { type: Object as PropType<any>, default: null },
  model: { type: Object, required: true },
  meta: { type: Object, required: true },
  statuses: { type: Object, default: () => ({}) },
  isNew: { type: Boolean, default: false },
})
const emit = defineEmits(['refresh', 'remove', 'clone', 'update:meta'])

const activeTab = ref('style')
const columns = ref<any[]>([])
const columnsLoading = ref(false)
const filterVisible = ref(false)
let columnsSerial = 0

const dataWidgets = ['Metric', 'Table', 'Line', 'Bar', 'Area', 'Pie', 'Funnel', 'Radar', 'Gauge']
const isDataWidget = computed(() => dataWidgets.includes(props.widget?.type))
const isRoot = computed(() => !props.widget)
const updateMeta = (field: string, value: any) => {
  emit('update:meta', { ...props.meta, [field]: value })
}
const templateCallback = (params: any) => DashboardTemplateApi.list(params).then((result: any) => {
  if (Array.isArray(result?.data?.rows)) {
    result.data.rows = result.data.rows.map((row: any) => ({
      ...row,
      disabled: Number(row.status) !== 1,
    }))
  }
  return result
})
const fields = computed(() => columns.value.map((item: any) => ({
  label: item.title || item.name,
  value: item.name,
})))
const metrics = computed({
  get: () => props.widget?.data?.axis?.metrics || [],
  set: (value) => {
    if (props.widget?.data?.axis) props.widget.data.axis.metrics = value
  },
})
const buckets = computed({
  get: () => props.widget?.data?.axis?.buckets || [],
  set: (value) => {
    if (props.widget?.data?.axis) props.widget.data.axis.buckets = value
  },
})
const metricTypes = [
  { label: '求和', value: 'SUM' },
  { label: '计数', value: 'COUNT' },
  { label: '去重计数', value: 'COUNT_DISTINCT' },
  { label: '最大值', value: 'MAX' },
  { label: '最小值', value: 'MIN' },
  { label: '平均值', value: 'AVG' },
]
const typeLabels: Record<string, string> = {
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
  Border: '科技边框',
  DecorationHeader: '科技标题',
  DecorationScan: '扫描光效',
  DecorationGrid: '网格背景',
  DecorationDivider: '分割装饰',
}

const loadColumns = () => {
  const datasetId = props.widget?.data?.datasetId
  const widgetId = props.widget?.id
  if (!datasetId) {
    columnsSerial++
    columns.value = []
    return
  }
  const serial = ++columnsSerial
  columnsLoading.value = true
  DatasetApi.columns(datasetId).then((result: any) => {
    if (serial !== columnsSerial || props.widget?.id !== widgetId || props.widget?.data?.datasetId !== datasetId) return
    columns.value = result.code === 0 ? (result.data?.columns || []) : []
    if (columns.value.length && props.widget?.data?.axis) {
      const axis = props.widget.data.axis
      if (!axis.buckets?.length) {
        const first = columns.value[0]
        axis.buckets = [{
          aggregation: 'TERM',
          field: first.name,
          label: first.title || first.name,
          interval: '',
          filters: [],
        }]
      }
      if (!axis.metrics?.length) {
        axis.metrics = [{
          aggregation: 'COUNT',
          field: '',
          label: '记录数',
          filter: [],
        }]
      }
    }
    emit('refresh')
  }).catch(() => {
    if (serial === columnsSerial) columns.value = []
  }).finally(() => {
    if (serial === columnsSerial) columnsLoading.value = false
  })
}

watch(
  () => [props.widget?.id, props.widget?.data?.datasetId],
  ([widgetId, datasetId], previous = [] as any[]) => {
    const [previousWidgetId, previousDatasetId] = previous
    if (!widgetId) {
      activeTab.value = props.isNew ? 'data' : 'style'
      return
    }
    if (widgetId !== previousWidgetId) {
      activeTab.value = isDataWidget.value ? 'data' : 'style'
      loadColumns()
      return
    }
    if (datasetId !== previousDatasetId && props.widget?.data?.axis) {
      props.widget.data.axis.metrics = []
      props.widget.data.axis.buckets = []
      loadColumns()
    }
  },
  { immediate: true },
)

const refresh = () => emit('refresh')
const createBucket = () => {
  if (!props.widget?.data?.axis || !columns.value.length) return
  const first = columns.value[buckets.value.length % columns.value.length]
  buckets.value = [...buckets.value, {
    aggregation: 'TERM',
    field: first.name,
    label: first.title || first.name,
    interval: '',
    filters: [],
  }]
  refresh()
}
const removeBucket = (index: number) => {
  buckets.value = buckets.value.filter((_: any, current: number) => current !== index)
  refresh()
}
const fieldChanged = (bucket: any) => {
  const field = columns.value.find((item: any) => item.name === bucket.field)
  if (field) bucket.label = field.title || field.name
  refresh()
}
const addMetric = () => {
  const first = columns.value[0]
  metrics.value = [
    ...metrics.value,
    {
      aggregation: 'SUM',
      field: first?.name || '',
      label: `指标 ${metrics.value.length + 1}`,
      filter: [],
    },
  ]
  refresh()
}
const removeMetric = (index: number) => {
  metrics.value = metrics.value.filter((_: any, current: number) => current !== index)
  refresh()
}
</script>

<template>
  <div class="inspector">
    <header class="inspector__head">
      <div class="inspector__identity">
        <span class="inspector__mark">{{ isRoot ? '画' : typeLabels[widget.type]?.slice(0, 1) || '组' }}</span>
        <div>
          <strong>{{ isRoot ? '画布设置' : widget.name }}</strong>
          <small>{{ isRoot ? '大屏画布' : typeLabels[widget.type] || '组件' }}</small>
        </div>
      </div>
      <el-space v-if="!isRoot" :size="1">
        <el-button text size="small" :icon="Rank" title="复制组件" @click="emit('clone')" />
        <el-button text size="small" type="danger" :icon="Delete" title="删除组件" @click="emit('remove')" />
      </el-space>
    </header>

    <el-tabs v-model="activeTab" class="inspector__tabs">
      <el-tab-pane label="样式" name="style">
        <DashboardStylePanel :widget="widget" :model="model" />
      </el-tab-pane>
      <el-tab-pane label="数据" name="data">
        <section v-if="isRoot" class="screen-info">
          <div class="screen-info__heading">
            <strong>大屏信息</strong>
            <span>随顶部“保存”统一提交</span>
          </div>
          <div class="screen-info__grid">
            <label class="screen-info__field screen-info__field--full">
              <span>大屏名称 <i>*</i></span>
              <el-input
                :model-value="meta.name"
                maxlength="255"
                placeholder="请输入大屏名称"
                @update:model-value="updateMeta('name', $event)" />
            </label>
            <label class="screen-info__field">
              <span>状态</span>
              <el-select
                :model-value="meta.status"
                @update:model-value="updateMeta('status', $event)">
                <el-option
                  v-for="(label, value) in statuses"
                  :key="value"
                  :label="label"
                  :value="String(value)" />
              </el-select>
            </label>
            <label class="screen-info__field">
              <span>排序</span>
              <el-input-number
                :model-value="meta.sort"
                :controls="false"
                @update:model-value="updateMeta('sort', $event ?? 0)" />
            </label>
            <label class="screen-info__field screen-info__field--full">
              <span>引用模板</span>
              <form-select
                :model-value="meta.templateId"
                :callback="templateCallback"
                :page-size="20"
                :label-formatter="(row: any) => `${row.name}${Number(row.status) === 1 ? '' : '（已停用）'}`"
                clearable
                placeholder="不使用模板"
                @update:model-value="updateMeta('templateId', $event || undefined)" />
              <small v-if="isNew">创建时继承模板布局、通用样式和个性化组件样式</small>
              <small v-else>切换模板只更新后续新增组件的默认样式，现有组件保持不变</small>
            </label>
            <label class="screen-info__field screen-info__field--full">
              <span>描述</span>
              <el-input
                :model-value="meta.description"
                type="textarea"
                :rows="3"
                placeholder="请输入大屏说明"
                @update:model-value="updateMeta('description', $event)" />
            </label>
          </div>
        </section>

        <template v-else-if="isDataWidget">
          <DashboardPropertySection title="数据来源" description="数据集">
            <form-select
              v-model="widget.data.datasetId"
              :callback="DatasetApi.list"
              placeholder="检索并选择数据集"
              clearable />
            <div v-if="!widget.data.datasetId" class="inspector-empty inspector-empty--small">
              <span>选择数据集后可自由组合维度、指标与过滤条件。</span>
            </div>
          </DashboardPropertySection>

          <DashboardPropertySection
            v-if="widget.data.datasetId"
            title="维度"
            :description="`${buckets.length} 个 · 按顺序组合`">
            <template #extra>
              <el-button text size="small" type="primary" :icon="Plus" @click="createBucket">添加</el-button>
            </template>
            <div v-for="(bucket, index) in buckets" :key="index" class="config-card">
              <div class="config-card__head">
                <span><i>{{ index + 1 }}</i> 维度</span>
                <el-button text size="small" type="danger" :icon="Delete" @click="removeBucket(index)" />
              </div>
              <div class="field-grid">
                <label class="field-cell">
                  <span>字段</span>
                  <el-select v-model="bucket.field" filterable :loading="columnsLoading" @change="fieldChanged(bucket)">
                    <el-option v-for="field in fields" :key="field.value" :label="field.label" :value="field.value" />
                  </el-select>
                </label>
                <label class="field-cell">
                  <span>名称</span>
                  <el-input v-model="bucket.label" placeholder="维度名称" @change="refresh" />
                </label>
              </div>
              <div class="field-grid section-gap">
                <label class="field-cell">
                  <span>分组方式</span>
                  <el-select v-model="bucket.aggregation" @change="refresh">
                    <el-option label="字段分组" value="TERM" />
                    <el-option label="数值分段" value="HISTOGRAM" />
                    <el-option label="日期分段" value="DATE_HISTOGRAM" />
                  </el-select>
                </label>
                <label v-if="bucket.aggregation === 'HISTOGRAM'" class="field-cell">
                  <span>分段间隔</span>
                  <el-input v-model="bucket.interval" placeholder="例如 1000" @change="refresh" />
                </label>
                <label v-if="bucket.aggregation === 'DATE_HISTOGRAM'" class="field-cell">
                  <span>日期间隔</span>
                  <el-select v-model="bucket.interval" @change="refresh">
                    <el-option v-for="item in ['HOUR', 'DAY', 'MONTH', 'QUARTER', 'YEAR']" :key="item" :label="item" :value="item" />
                  </el-select>
                </label>
              </div>
            </div>
            <el-empty v-if="!buckets.length" description="暂无维度" :image-size="42" />
          </DashboardPropertySection>

          <DashboardPropertySection
            v-if="widget.data.datasetId"
            title="指标"
            :description="`${metrics.length} 项`">
            <template #extra>
              <el-button text size="small" type="primary" :icon="Plus" @click="addMetric">添加</el-button>
            </template>
            <div v-for="(metric, index) in metrics" :key="index" class="config-card">
              <div class="config-card__head">
                <span><i>{{ index + 1 }}</i> 指标</span>
                <el-button text size="small" type="danger" :icon="Delete" @click="removeMetric(index)" />
              </div>
              <div class="field-grid">
                <label class="field-cell">
                  <span>显示名称</span>
                  <el-input v-model="metric.label" placeholder="例如：销售额" @change="refresh" />
                </label>
                <label class="field-cell">
                  <span>聚合方式</span>
                  <el-select v-model="metric.aggregation" @change="refresh">
                    <el-option v-for="item in metricTypes" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </label>
              </div>
              <label v-if="metric.aggregation !== 'COUNT'" class="field-cell section-gap">
                <span>字段</span>
                <el-select v-model="metric.field" filterable @change="refresh">
                  <el-option v-for="field in fields" :key="field.value" :label="field.label" :value="field.value" />
                </el-select>
              </label>
            </div>
            <el-empty v-if="!metrics.length" description="暂无指标" :image-size="42" />
          </DashboardPropertySection>

          <DashboardPropertySection
            v-if="widget.data.datasetId"
            title="数据过滤"
            description="并且 / 或者">
            <template #extra>
              <el-button text size="small" type="primary" @click="filterVisible = true">配置</el-button>
            </template>
            <span class="filter-summary">{{ (widget.data.filter || []).length ? `已配置 ${widget.data.filter.length} 组条件` : '未设置过滤条件' }}</span>
          </DashboardPropertySection>
        </template>

        <template v-else>
          <DashboardPropertySection :title="widget.type === 'DecorationHeader' ? '标题内容' : '组件内容'">
            <el-input
              v-if="widget.type === 'Text' || widget.type === 'DecorationHeader'"
              v-model="widget.content"
              :type="widget.type === 'Text' ? 'textarea' : 'text'"
              :rows="6"
              placeholder="请输入内容"
              @change="refresh" />
            <div v-else class="inspector-empty inspector-empty--small">
              <span>该装饰组件无需绑定数据，可在样式页签调整主题与动效。</span>
            </div>
          </DashboardPropertySection>
        </template>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="filterVisible" title="数据过滤" width="760px" append-to-body destroy-on-close>
      <DataQueryFilter v-if="widget" v-model="widget.data.filter" :fields="fields" />
      <template #footer>
        <el-button @click="filterVisible = false">取消</el-button>
        <el-button type="primary" @click="filterVisible = false; refresh()">应用过滤</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.inspector {
  min-height: 100%;
  background: #fff;
  &__head {
    position: sticky;
    top: 0;
    z-index: 9;
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 48px;
    padding: 0 12px;
    box-sizing: border-box;
    background: rgba(255, 255, 255, 0.96);
    border-bottom: 1px solid var(--el-border-color-lighter);
    backdrop-filter: blur(8px);
  }
  &__identity {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    strong,
    small {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    strong {
      max-width: 190px;
      color: var(--el-text-color-primary);
      font-size: 12px;
      font-weight: 600;
    }
    small {
      margin-top: 1px;
      color: var(--el-text-color-placeholder);
      font-size: 9px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
  }
  &__mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 28px;
    height: 28px;
    border-radius: 6px;
    color: #fff;
    background: linear-gradient(145deg, #1a71ed, #14bdd4);
    font-size: 9px;
    font-weight: 700;
    letter-spacing: -0.3px;
  }
  &__tabs {
    :deep(.el-tabs__header) {
      position: sticky;
      top: 48px;
      z-index: 8;
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
    :deep(.el-tabs__content) {
      overflow: visible;
    }
  }
}
.screen-info {
  padding: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-extra-light);
  &__heading {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 10px;
    strong {
      color: var(--el-text-color-primary);
      font-size: 13px;
    }
    span {
      color: var(--el-text-color-placeholder);
      font-size: 10px;
    }
  }
  &__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 9px;
  }
  &__field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
    > span {
      color: var(--el-text-color-secondary);
      font-size: 10px;
      i {
        color: var(--el-color-danger);
        font-style: normal;
      }
    }
    > small {
      color: var(--el-text-color-placeholder);
      font-size: 9px;
      line-height: 1.5;
    }
    .el-select,
    .el-input-number {
      width: 100%;
    }
    &--full {
      grid-column: 1 / -1;
    }
  }
}
.inspector-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  margin: 12px;
  padding: 24px 16px;
  border-radius: 6px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
  text-align: center;
  .el-icon {
    margin-bottom: 4px;
    color: var(--el-color-primary);
    font-size: 22px;
  }
  strong {
    font-size: 12px;
  }
  span {
    color: var(--el-text-color-placeholder);
    font-size: 10px;
    line-height: 1.6;
  }
  &--small {
    align-items: flex-start;
    min-height: 0;
    margin: 8px 0 0;
    padding: 10px;
    text-align: left;
  }
}
.config-card {
  margin-bottom: 8px;
  padding: 9px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 5px;
  background: #fbfcfd;
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 7px;
    color: var(--el-text-color-regular);
    font-size: 10px;
    font-weight: 500;
    > span {
      display: flex;
      align-items: center;
      gap: 5px;
    }
    i {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 15px;
      height: 15px;
      border-radius: 3px;
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
      font-size: 9px;
      font-style: normal;
    }
  }
}
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}
.field-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  color: var(--el-text-color-secondary);
  font-size: 10px;
  .el-input,
  .el-select {
    width: 100%;
  }
}
.section-gap {
  margin-top: 7px;
}
.filter-summary {
  display: block;
  padding: 8px 9px;
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 10px;
}
</style>
