<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute } from 'vue-router';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import draggable from 'vuedraggable';
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue';
import LayoutBack from '@/components/Layout/LayoutBack.vue';
import LayoutHeading from '@/components/Layout/LayoutHeading.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import Chart from './design/Chart.vue';
import config from './design/config';
import DataQueryFilter from '@/components/Data/DataQueryFilter.vue';
import VisualizeApi from '@/api/bi/VisualizeApi';
import DatasetApi from '@/api/bi/DatasetApi';
import UIUtil from '@/utils/UIUtil';

const route = useRoute()
const loading = ref(false)
const chartRef = ref()
const datasetColumns = ref<any[]>([])
const columnSearch = ref('')
const tips = ref('选择数据集，配置维度与度量后点击「更新」预览效果')
const form: any = ref({ name: '', type: 'Table', datasetId: undefined })
const filter: any = ref([])
const axis: any = ref({
  metrics: [],
  buckets: [],
})
// 编辑面板折叠状态：新增指标/维度时自动展开对应面板
const activeMetrics = ref<number[]>([])
const activeBuckets = ref<number[]>([])

// vuedraggable 需要稳定的唯一键，无法直接修改业务数据，因此借助 WeakMap 生成
const keyMap = new WeakMap<object, string>()
let sequence = 0
const itemKey = (item: any) => {
  if (!keyMap.has(item)) keyMap.set(item, `visualize-${++sequence}`)
  return keyMap.get(item) as string
}

const metrics = [
  { label: '求和（SUM）', value: 'SUM' },
  { label: '计数（COUNT）', value: 'COUNT' },
  { label: '去重计数（COUNT_DISTINCT）', value: 'COUNT_DISTINCT' },
  { label: '最大（MAX）', value: 'MAX' },
  { label: '最小（MIN）', value: 'MIN' },
  { label: '平均（AVG）', value: 'AVG' },
]
const buckets = [
  { label: '字段（TERM）', value: 'TERM' },
  { label: '过滤（FILTER）', value: 'FILTER' },
  { label: '分段（HISTOGRAM）', value: 'HISTOGRAM' },
  { label: '日期分段（DATE_HISTOGRAM）', value: 'DATE_HISTOGRAM' },
]
const intervals = [
  { label: '毫秒（MILLISECOND）', value: 'MILLISECOND' },
  { label: '秒（SECOND）', value: 'SECOND' },
  { label: '分钟（MINUTE）', value: 'MINUTE' },
  { label: '小时（HOUR）', value: 'HOUR' },
  { label: '日（DAY）', value: 'DAY' },
  { label: '月（MONTH）', value: 'MONTH' },
  { label: '季度（QUARTER）', value: 'QUARTER' },
  { label: '年（YEAR）', value: 'YEAR' },
]

const icon = (name: string) => (ElementPlusIcons as any)[name]
const fieldIcon = (type?: string) => {
  if (!type) return 'Document'
  if (['integer', 'long', 'float', 'double'].includes(type)) return 'Odometer'
  if (['date', 'time', 'datetime'].includes(type)) return 'Calendar'
  return 'Document'
}
const fields = computed(() => {
  const result: any[] = []
  datasetColumns.value.forEach((item: any) => {
    result.push({ label: item.title || item.name, value: item.name })
  })
  return result
})
const filteredColumns = computed(() => {
  const keyword = columnSearch.value.trim().toLowerCase()
  if (!keyword) return datasetColumns.value
  return datasetColumns.value.filter((item: any) =>
    (item.name || '').toLowerCase().includes(keyword) || (item.title || '').toLowerCase().includes(keyword))
})
const typeLabel = computed(() => config.widgetByType(form.value.type)?.label || form.value.type)

const loadColumns = () => {
  if (!form.value.datasetId || form.value.datasetId < 1) {
    datasetColumns.value = []
    return true
  }
  if (loading.value) return false
  loading.value = true
  DatasetApi.columns(form.value.datasetId).then((result: any) => {
    datasetColumns.value = result.code === 0 ? (result.data.columns || []) : []
  }).catch(() => {
    datasetColumns.value = []
  }).finally(() => {
    loading.value = false
  })
}

watch(() => form.value.datasetId, () => {
  loadColumns()
})

const addMetric = () => {
  axis.value.metrics.push({ aggregation: 'COUNT', field: '', label: '', filter: [] })
  activeMetrics.value = [axis.value.metrics.length - 1]
}
const deleteMetric = (index: any) => {
  axis.value.metrics.splice(index, 1)
}
const addBucket = () => {
  axis.value.buckets.push({ aggregation: 'TERM', field: '', label: '', interval: '', filters: [] })
  activeBuckets.value = [axis.value.buckets.length - 1]
}
const deleteBucket = (index: any) => {
  axis.value.buckets.splice(index, 1)
}
const deleteBucketFilter = (bucket: any, index: any) => {
  bucket.filters.splice(index, 1)
}
const addBucketFilter = (bucket: any) => {
  bucket.filters.push({ label: '', filter: [] })
}
const collect = () => {
  return {
    filter: filter.value,
    axis: axis.value,
  }
}
const load = () => {
  if (!route.query.id) return false
  loading.value = true
  tips.value = '正在载入报表信息...'
  VisualizeApi.info(route.query.id).then((result: any) => {
    if (result.code !== 0) return false
    const data = result.data || {}
    if (!data.datasetId || data.datasetId < 1) data.datasetId = undefined
    if (!data.type) data.type = form.value.type
    Object.assign(form.value, data)
    try {
      if (data.content) {
        const content = JSON.parse(data.content)
        if (content.filter) filter.value = content.filter
        if (content.axis) Object.assign(axis.value, content.axis)
      }
    } catch (e: any) {
      ElMessage.error('数据解析异常：' + e.message)
    }
    tips.value = `已载入报表：${data.name || data.id}`
  }).catch(() => {}).finally(() => {
    loading.value = false
    loadColumns()
  })
}
const search = () => {
  if (loading.value) return false
  if (!form.value.datasetId || form.value.datasetId < 1) {
    ElMessage.warning('请先选择数据集')
    return false
  }
  tips.value = '正在更新预览...'
  const task = chartRef.value?.preview(form.value.datasetId, collect())
  if (task && typeof task.then === 'function') {
    task.finally(() => {
      tips.value = '预览已更新'
    })
  }
  return task
}
const submit = () => {
  if (loading.value) return false
  if (!form.value.datasetId || form.value.datasetId < 1) {
    ElMessage.warning('请先选择数据集')
    return false
  }
  loading.value = true
  const data = {
    id: form.value.id,
    type: form.value.type,
    datasetId: form.value.datasetId,
    content: JSON.stringify(collect()),
  }
  VisualizeApi.save(data, { success: true }).then((result: any) => {
    if (result.code === 0) {
      form.value = Object.assign({}, form.value, result.data)
      tips.value = '保存成功'
    }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  load()
})
</script>

<template>
  <LayoutDesigner splitter :left-size="300" :right-size="380" :left-min="220" :right-min="320">
    <template #left>
      <div class="designer-aside">
        <div class="designer-aside__head">
          <form-select v-model="form.datasetId" :callback="DatasetApi.list" placeholder="检索选择数据集" clearable />
        </div>
        <div class="designer-aside__section">
          <layout-heading title="数据字段" :description="datasetColumns.length + ' 个字段'" />
          <el-input
            v-model="columnSearch"
            size="small"
            placeholder="搜索字段"
            clearable
            :prefix-icon="icon('Search')" />
        </div>
        <el-scrollbar class="designer-aside__list">
          <div class="field-list">
            <div class="field-item" v-for="record in filteredColumns" :key="record.name" :title="record.name">
              <el-icon class="field-item__icon"><component :is="icon(fieldIcon(record.type))" /></el-icon>
              <span class="field-item__name">{{ record.name }}</span>
              <span class="field-item__title">{{ record.title || record.name }}</span>
            </div>
            <el-empty v-if="!filteredColumns.length" :description="columnSearch ? '无匹配字段' : '暂无字段'" :image-size="48" />
          </div>
        </el-scrollbar>
      </div>
    </template>
    <template #top>
      <el-space class="designer-toolbar">
        <LayoutBack to="/bi/olap/visualize" title="返回数据报表" />
        <el-divider direction="vertical" />
        <el-popover placement="bottom-start" trigger="click" :width="300" popper-class="designer-popover">
          <template #reference>
            <el-button text size="small" :icon="icon('Grid')">{{ typeLabel }}</el-button>
          </template>
          <div class="type-grid">
            <div
              class="type-item"
              :class="{ 'is-active': form.type === widget.type }"
              v-for="widget in config.widgets"
              :key="widget.type"
              @click="form.type = widget.type">
              <el-icon><component :is="icon(widget.icon)" /></el-icon>
              <span>{{ widget.label }}</span>
            </div>
          </div>
        </el-popover>
        <el-popover placement="bottom-start" trigger="click" :width="680" popper-class="designer-popover">
          <template #reference>
            <el-button text size="small" :icon="icon('Filter')">筛选</el-button>
          </template>
          <DataQueryFilter v-model="filter" :fields="fields" />
        </el-popover>
        <el-button text size="small" type="primary" :icon="icon('Search')" :loading="loading" @click="search">更新</el-button>
      </el-space>
      <el-space class="designer-menus">
        <el-button v-if="route.query.id" text size="small" :icon="icon('Refresh')" :loading="loading" @click="load">重新载入</el-button>
        <el-button text size="small" type="primary" :icon="icon('Check')" :loading="loading" @click="submit">保存</el-button>
      </el-space>
    </template>
    <template #default>
      <div class="designer-canvas">
        <Chart ref="chartRef" :value="form.id" :config="config" :type="form.type" />
      </div>
    </template>
    <template #right>
      <el-scrollbar class="designer-property">
        <div class="property-section">
          <layout-heading title="Y-度量" description="纵坐标展示的指标">
            <template #extra>
              <el-button text size="small" type="primary" :icon="icon('Plus')" @click="addMetric">指标</el-button>
            </template>
          </layout-heading>
          <el-collapse v-model="activeMetrics" expand-icon-position="left" class="property-collapse">
            <el-collapse-item :name="index" v-for="(metric, index) in axis.metrics" :key="index">
              <template #title>
                <span class="property-collapse__title">{{ metric.label || '未命名指标' }}</span>
                <el-icon class="property-collapse__delete" title="删除指标" @click.stop="deleteMetric(index)"><component :is="icon('Delete')" /></el-icon>
              </template>
              <el-form label-width="80px" label-position="left" size="small">
                <el-form-item label="标签名称">
                  <el-input v-model="metric.label" placeholder="请输入标签名称" />
                </el-form-item>
                <el-form-item label="聚合方式">
                  <el-select v-model="metric.aggregation">
                    <el-option v-for="item in metrics" :key="item.value" :value="item.value" :label="item.label" />
                  </el-select>
                </el-form-item>
                <el-form-item label="聚合字段" v-if="metric.aggregation !== 'COUNT'">
                  <el-autocomplete
                    v-model="metric.field"
                    clearable
                    style="width: 100%"
                    placeholder="请输入聚合字段"
                    :fetch-suggestions="(query: string) => UIUtil.fetchSuggestions(fields, query, 'value')" />
                </el-form-item>
                <el-form-item label="过滤条件">
                  <el-popover placement="bottom-end" trigger="click" :width="680" popper-class="designer-popover">
                    <template #reference>
                      <el-button size="small" :icon="icon('Filter')">筛选</el-button>
                    </template>
                    <DataQueryFilter v-model="metric.filter" :fields="fields" />
                  </el-popover>
                </el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
          <el-empty v-if="!axis.metrics.length" description="暂无度量，点击右上角「指标」添加" :image-size="48" />
        </div>
        <div class="property-section">
          <layout-heading title="X-维度" description="横坐标展示的维度">
            <template #extra>
              <el-button text size="small" type="primary" :icon="icon('Plus')" @click="addBucket">钻取</el-button>
            </template>
          </layout-heading>
          <el-collapse v-model="activeBuckets" expand-icon-position="left" class="property-collapse">
            <el-collapse-item :name="index" v-for="(bucket, index) in axis.buckets" :key="index">
              <template #title>
                <span class="property-collapse__title">{{ bucket.label || '未命名维度' }}</span>
                <el-icon class="property-collapse__delete" title="删除维度" @click.stop="deleteBucket(index)"><component :is="icon('Delete')" /></el-icon>
              </template>
              <el-form label-width="80px" label-position="left" size="small">
                <el-form-item label="层级名称">
                  <el-input v-model="bucket.label" placeholder="请输入层级名称" />
                </el-form-item>
                <el-form-item label="聚合方式">
                  <el-select v-model="bucket.aggregation">
                    <el-option v-for="item in buckets" :key="item.value" :value="item.value" :label="item.label" />
                  </el-select>
                </el-form-item>
                <el-form-item label="聚合字段" v-if="bucket.aggregation !== 'FILTER'">
                  <el-autocomplete
                    v-model="bucket.field"
                    clearable
                    style="width: 100%"
                    placeholder="请输入聚合字段"
                    :fetch-suggestions="(query: string) => UIUtil.fetchSuggestions(fields, query, 'value')" />
                </el-form-item>
                <el-form-item label="分段间隔" v-if="bucket.aggregation === 'HISTOGRAM'">
                  <el-input v-model="bucket.interval" placeholder="请输入分段间隔" />
                </el-form-item>
                <el-form-item label="分段间隔" v-if="bucket.aggregation === 'DATE_HISTOGRAM'">
                  <el-select v-model="bucket.interval">
                    <el-option v-for="item in intervals" :key="item.value" :value="item.value" :label="item.label" />
                  </el-select>
                </el-form-item>
                <template v-if="bucket.aggregation === 'FILTER'">
                  <draggable
                    v-model="bucket.filters"
                    class="bucket-list"
                    group="bucket"
                    handle=".bucket-item__handle"
                    ghost-class="bucket-item--ghost"
                    :item-key="itemKey"
                    :animation="340">
                    <template #item="{ element: item, index: idx }">
                      <div class="bucket-item">
                        <el-icon class="bucket-item__handle" title="拖拽排序"><component :is="icon('Rank')" /></el-icon>
                        <el-input v-model="item.label" placeholder="标签名称" size="small" />
                        <el-popover placement="bottom-end" trigger="click" :width="680" popper-class="designer-popover">
                          <template #reference>
                            <el-button size="small" :icon="icon('Filter')">筛选</el-button>
                          </template>
                          <DataQueryFilter v-model="item.filter" :fields="fields" />
                        </el-popover>
                        <el-icon class="bucket-item__delete" title="删除条件" @click="deleteBucketFilter(bucket, idx)"><component :is="icon('Delete')" /></el-icon>
                      </div>
                    </template>
                  </draggable>
                  <el-button text size="small" type="primary" :icon="icon('Plus')" @click="addBucketFilter(bucket)">添加过滤条件</el-button>
                </template>
              </el-form>
            </el-collapse-item>
          </el-collapse>
          <el-empty v-if="!axis.buckets.length" description="暂无维度，点击右上角「钻取」添加" :image-size="48" />
        </div>
      </el-scrollbar>
    </template>
    <template #footer>
      <el-space>
        <LayoutIcon name="Opportunity" color="#409eff" />
        <span>{{ tips }}</span>
      </el-space>
    </template>
  </LayoutDesigner>
</template>

<style lang="scss" scoped>
.designer-aside {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--fs-layout-header-height) - 35px);
  overflow: hidden;
  background: #fff;
  &__head {
    flex: none;
    padding: 12px;
    box-sizing: border-box;
    border-bottom: 1px solid var(--el-border-color-light);
    :deep(.el-select) {
      width: 100%;
    }
    :deep(.el-select__wrapper) {
      min-height: 28px;
      padding: 2px 10px;
      font-size: 13px;
      line-height: 22px;
    }
  }
  &__section {
    flex: none;
    padding: 10px 12px 6px;
    box-sizing: border-box;
  }
  &__list {
    flex: 1;
    min-height: 0;
    :deep(.el-scrollbar__view) {
      padding: 4px 6px 12px;
      box-sizing: border-box;
    }
  }
}
.designer-aside__section :deep(.fs-heading),
.property-section :deep(.fs-heading) {
  margin-bottom: 8px;
}
.designer-aside__section :deep(.fs-heading__title),
.property-section :deep(.fs-heading__title) {
  font-size: 14px;
}
.designer-aside__section :deep(.fs-heading__title)::before,
.property-section :deep(.fs-heading__title)::before {
  height: 14px;
}
.field-list {
  .field-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 5px 6px;
    border-radius: 4px;
    font-size: 13px;
    line-height: 20px;
    transition: background-color 0.2s;
    &:hover {
      background: var(--el-fill-color-light);
    }
    &__icon {
      flex: none;
      font-size: 14px;
      color: var(--el-color-primary);
    }
    &__name {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: var(--el-text-color-primary);
    }
    &__title {
      flex: none;
      margin-left: auto;
      max-width: 45%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }
}
.designer-toolbar,
.designer-menus {
  :deep(.el-divider--vertical) {
    margin: 0 4px;
  }
}
.type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  .type-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 8px 4px;
    font-size: 12px;
    color: var(--el-text-color-regular);
    cursor: pointer;
    border: 1px solid transparent;
    border-radius: 6px;
    transition: all 0.2s;
    .el-icon {
      font-size: 18px;
    }
    &:hover {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
    &.is-active {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
      border-color: var(--el-color-primary);
    }
  }
}
.designer-canvas {
  height: 100%;
  padding: 12px;
  box-sizing: border-box;
  overflow: hidden;
}
.designer-property {
  height: 100%;
  :deep(.el-scrollbar__view) {
    min-height: 100%;
    padding: 12px;
    box-sizing: border-box;
  }
}
.property-section {
  margin-bottom: 16px;
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
.property-collapse {
  --el-collapse-header-height: 34px;
  border-top: none;
  :deep(.el-collapse-item__header) {
    padding: 0 12px;
    font-size: 13px;
    background: var(--el-fill-color-light);
    border-radius: 4px;
    border-bottom: none;
  }
  :deep(.el-collapse-item__title) {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }
  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
  :deep(.el-collapse-item__content) {
    padding: 10px 12px 0;
  }
  :deep(.el-collapse-item) {
    margin-bottom: 6px;
  }
}
.property-collapse__title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.property-collapse__delete {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  &:hover {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }
}
.bucket-list {
  margin-bottom: 6px;
}
.bucket-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 0;
  :deep(.el-input) {
    flex: 1;
    min-width: 0;
  }
  :deep(.el-button) {
    flex: none;
  }
  &__handle {
    flex: none;
    display: inline-flex;
    align-items: center;
    color: var(--el-text-color-secondary);
    cursor: move;
  }
  &__delete {
    flex: none;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: 4px;
    color: var(--el-color-danger);
    cursor: pointer;
    &:hover {
      background: var(--el-color-danger-light-9);
    }
  }
  &--ghost {
    opacity: 0.5;
    border: 1px dashed var(--el-color-primary);
    border-radius: 4px;
  }
}
</style>
