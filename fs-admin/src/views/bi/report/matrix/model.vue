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
import MatrixTable from '@/views/bi/components/MatrixTable.vue';
import DataQueryFilter from '@/components/Data/DataQueryFilter.vue';
import MatrixApi from '@/api/bi/MatrixApi';
import DatasetApi from '@/api/bi/DatasetApi';
import UIUtil from '@/utils/UIUtil';
import MenuUtil from '@/utils/MenuUtil';

const route = useRoute()
const loading = ref(false)
const columnSearch = ref('')
const columnDragged = ref<any>(null)
const datasetColumns = ref<any[]>([])
const tips = ref('拖拽左侧字段至编辑区，点击字段可编辑属性，右键查看更多操作')
const form: any = ref({ name: '', datasetId: undefined })
const filter: any = ref([])
const aggregation: any = ref({
  levels: [],
  buckets: [],
  metrics: [],
})
const itemEditing: any = ref({})
const aggResult: any = ref(null)

// vuedraggable 需要稳定的唯一键，无法直接修改业务数据，因此借助 WeakMap 生成
const keyMap = new WeakMap<object, string>()
let sequence = 0
const itemKey = (item: any) => {
  if (!keyMap.has(item)) keyMap.set(item, `aggregate-${++sequence}`)
  return keyMap.get(item) as string
}

// 通过稳定 id 关联数组项，避免 vuedraggable 插槽传递的 element 与编辑目标不是同一引用
const ensureItemIds = (list: any[]) => {
  (list || []).forEach((item: any) => {
    if (!item.id) item.id = UIUtil.uuid('agg-')
  })
}
const findItem = (type: string, id: any) => {
  return (aggregation.value[type] || []).find((item: any) => item.id === id)
}
const itemTitle = (type: string, item: any) => {
  const found = item && item.id ? findItem(type, item.id) : null
  return (found || item)?.title
}
const selectItem = (type: string, id: any) => {
  itemEditing.value = (type && id ? findItem(type, id) : null) || {}
}

const lines = [
  { type: 'levels', label: '层级（Level）', icon: 'Share' },
  { type: 'buckets', label: '维度（Bucket）', icon: 'Grid' },
  { type: 'metrics', label: '度量（Metric）', icon: 'TrendCharts' },
]
const metrics = [
  { label: '求和（SUM）', value: 'SUM' },
  { label: '计数（COUNT）', value: 'COUNT' },
  { label: '去重计数（COUNT_DISTINCT）', value: 'COUNT_DISTINCT' },
  { label: '最大（MAX）', value: 'MAX' },
  { label: '最小（MIN）', value: 'MIN' },
  { label: '平均（AVG）', value: 'AVG' },
]
const directions = [
  { label: '正序', value: 'asc' },
  { label: '倒序', value: 'desc' },
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
const simpleFields = computed(() => {
  const result: any[] = []
  datasetColumns.value.forEach((item: any) => {
    result.push({ label: item.title || item.name, value: item.name })
  })
  return result
})
const rows = computed(() => {
  const content = columnSearch.value.toUpperCase()
  return datasetColumns.value.filter((item: any) => {
    return (item.name || '').toUpperCase().indexOf(content) >= 0 || (item.title || '').toUpperCase().indexOf(content) >= 0
  })
})
const hints = computed(() => {
  const result: any[] = []
  datasetColumns.value.forEach((item: any) => {
    result.push({ text: '`' + item.name + '`', displayText: item.name })
  })
  return result
})

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

const aggDragOver = (event: any) => {
  if (columnDragged.value) event.preventDefault()
}
const aggDrop = (type: string, event: any) => {
  if (!columnDragged.value) return false
  event.preventDefault()
  const column = columnDragged.value
  const item: any = { id: UIUtil.uuid('agg-'), enabled: true, type, name: column.name, title: column.title || column.name, expression: '' }
  switch (type) {
    case 'levels':
    case 'buckets':
      item.sort = 'asc'
      break
    case 'metrics':
      item.aggregation = 'COUNT'
      break
  }
  aggregation.value[type].push(item)
  selectItem(type, item.id)
  tips.value = `已添加${lines.find((line: any) => line.type === type)?.label || '字段'}：${item.title}`
}
const handleContextMenu = (type: string, event: any, item: any, index: number) => {
  MenuUtil.context(event, [
    { key: 'edit', icon: 'Edit', title: '编辑字段' },
    { key: 'delete', icon: 'Delete', title: '移除字段' },
    { key: 'deleteOther', icon: 'Delete', title: '移除其他字段' },
    { key: 'deleteLeft', icon: 'Delete', title: '移除左侧字段' },
    { key: 'deleteRight', icon: 'Delete', title: '移除右侧字段' },
    { key: 'deleteAll', icon: 'Delete', title: '移除全部字段' },
  ], (menu: any) => {
    itemEditing.value = {}
    const list = aggregation.value[type]
    switch (menu.key) {
      case 'edit':
        selectItem(type, item.id)
        break
      case 'delete':
        list.splice(index, 1)
        break
      case 'deleteOther':
        list.splice(0, list.length, item)
        break
      case 'deleteLeft':
        list.splice(0, index)
        break
      case 'deleteRight':
        list.splice(index + 1, list.length - index + 1)
        break
      case 'deleteAll':
        list.splice(0, list.length)
        break
      default:
        return false
    }
  })
}
const collect = () => {
  return {
    filter: filter.value,
    aggregation: aggregation.value,
  }
}
// 表头直接采用设计器配置的展示名称，避免后端回显缺失时只显示字段名或丢失表头
const applyDisplayLabels = (data: any) => {
  if (!data) return data
  const enabledOf = (list: any) => (list || []).filter((item: any) => item.enabled)
  const labelOf = (item: any, fallback: any) => item?.title || item?.name || fallback
  data.levels = enabledOf(aggregation.value.levels).map((item: any, index: number) => ({
    label: labelOf(item, data.levels?.[index]?.label),
  }))
  enabledOf(aggregation.value.buckets).forEach((item: any, index: number) => {
    if (data.buckets?.[index]) data.buckets[index].label = labelOf(item, data.buckets[index].label)
  })
  enabledOf(aggregation.value.metrics).forEach((item: any, index: number) => {
    if (data.metrics?.[index]) data.metrics[index].label = labelOf(item, data.metrics[index].label)
  })
  return data
}
const load = () => {
  if (!route.query.id) return false
  loading.value = true
  tips.value = '正在载入矩阵信息...'
  MatrixApi.info(route.query.id).then((result: any) => {
    if (result.code !== 0) return false
    const data = result.data || {}
    if (!data.datasetId || data.datasetId < 1) data.datasetId = undefined
    Object.assign(form.value, data)
    try {
      if (data.content) {
        const content = JSON.parse(data.content)
        if (content.filter) filter.value = content.filter
        const agg = content.aggregation || {}
        if (agg.levels) aggregation.value.levels = agg.levels
        if (agg.buckets) aggregation.value.buckets = agg.buckets
        if (agg.metrics) aggregation.value.metrics = agg.metrics
        ;['levels', 'buckets', 'metrics'].forEach((key) => ensureItemIds(aggregation.value[key]))
      }
    } catch (e: any) {
      ElMessage.error('数据解析异常：' + e.message)
    }
    tips.value = `已载入矩阵：${data.name || data.id}`
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
  loading.value = true
  tips.value = '正在更新预览...'
  MatrixApi.search({ datasetId: form.value.datasetId, preview: collect() }).then((result: any) => {
    if (result.code === 0) {
      aggResult.value = applyDisplayLabels(result.data)
      tips.value = '预览已更新'
    }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
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
    datasetId: form.value.datasetId,
    content: JSON.stringify(collect()),
  }
  MatrixApi.save(data, { success: true }).then((result: any) => {
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
  <LayoutDesigner splitter :left-size="320" :right-size="360" :left-min="240" :right-min="300">
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
            <div
              class="field-item"
              v-for="record in rows"
              :key="record.name"
              :title="record.name"
              draggable="true"
              @dragstart="columnDragged = record"
              @dragend="columnDragged = null">
              <el-icon class="field-item__icon"><component :is="icon(fieldIcon(record.type))" /></el-icon>
              <span class="field-item__name">{{ record.name }}</span>
              <span class="field-item__title">{{ record.title || record.name }}</span>
            </div>
            <el-empty v-if="!rows.length" :description="columnSearch ? '无匹配字段' : '暂无字段'" :image-size="48" />
          </div>
        </el-scrollbar>
      </div>
    </template>
    <template #top>
      <el-space class="designer-toolbar">
        <LayoutBack to="/bi/olap/matrix" title="返回数据矩阵" />
        <el-divider direction="vertical" />
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
      <div class="designer-canvas matrix-canvas">
        <div class="matrix-lines">
          <div
            class="matrix-line"
            v-for="line in lines"
            :key="line.type"
            @dragover="aggDragOver($event)"
            @drop="aggDrop(line.type, $event)">
            <div class="matrix-line__title">
              <el-icon><component :is="icon(line.icon)" /></el-icon>
              <span>{{ line.label }}</span>
            </div>
            <draggable
              v-model="aggregation[line.type]"
              class="matrix-line__list"
              :item-key="itemKey"
              :group="line.type"
              ghost-class="matrix-item--ghost"
              :animation="340">
              <template #item="{ element: item, index }">
                <div
                  class="matrix-item"
                  :class="{ 'is-active': itemEditing.id && itemEditing.id === item.id }"
                  :title="itemTitle(line.type, item)"
                  @mousedown.left="selectItem(line.type, item.id)"
                  @click="selectItem(line.type, item.id)"
                  @contextmenu="(event: any) => handleContextMenu(line.type, event, item, index)">
                  <el-checkbox v-model="item.enabled" @click.stop />
                  <span class="matrix-item__text">{{ itemTitle(line.type, item) }}</span>
                </div>
              </template>
            </draggable>
            <div v-if="!aggregation[line.type].length" class="matrix-line__empty">拖拽左侧字段到此处</div>
          </div>
        </div>
        <div class="matrix-result">
          <MatrixTable :value="aggResult" :loading="loading" />
        </div>
      </div>
    </template>
    <template #right>
      <el-scrollbar class="designer-property">
        <layout-heading title="字段属性" />
        <el-form v-if="itemEditing.type" label-width="80px" label-position="left" size="small">
          <el-form-item label="启用状态">
            <el-checkbox v-model="itemEditing.enabled">{{ itemEditing.type }}</el-checkbox>
          </el-form-item>
          <el-form-item label="展示名称">
            <el-input v-model="itemEditing.title" placeholder="请输入展示名称" />
          </el-form-item>
          <el-form-item label="聚合字段">
            <el-autocomplete
              v-model="itemEditing.name"
              clearable
              style="width: 100%"
              placeholder="请输入聚合字段"
              :fetch-suggestions="(query: string) => UIUtil.fetchSuggestions(simpleFields, query, 'value')" />
          </el-form-item>
          <el-form-item label="排序方式" v-if="itemEditing.type !== 'metrics'">
            <el-select v-model="itemEditing.sort">
              <el-option v-for="item in directions" :key="item.value" :value="item.value" :label="item.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="聚合方式" v-if="itemEditing.type === 'metrics'">
            <el-select v-model="itemEditing.aggregation">
              <el-option v-for="item in metrics" :key="item.value" :value="item.value" :label="item.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="定制运算" v-if="itemEditing.type === 'levels'">
            <code-editor v-model="itemEditing.expression" mode="sql" :hints="hints" :height="120" :line-numbers="false" placeholder="支持 SQL 表达式，字段引用使用反引号，如 `字段`" />
          </el-form-item>
        </el-form>
        <el-empty v-else description="拖拽字段至编辑区，点击字段进行编辑" :image-size="56" />
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
.designer-property :deep(.fs-heading) {
  margin-bottom: 8px;
}
.designer-aside__section :deep(.fs-heading__title),
.designer-property :deep(.fs-heading__title) {
  font-size: 14px;
}
.designer-aside__section :deep(.fs-heading__title)::before,
.designer-property :deep(.fs-heading__title)::before {
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
    cursor: move;
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
.designer-canvas {
  height: 100%;
  padding: 12px;
  box-sizing: border-box;
  overflow: hidden;
}
.matrix-canvas {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.matrix-lines {
  flex: none;
  padding: 10px;
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
}
.matrix-line {
  display: flex;
  align-items: stretch;
  min-height: 36px;
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
  &:not(:last-child) {
    margin-bottom: 4px;
  }
  &__title {
    flex: none;
    align-self: stretch;
    display: flex;
    align-items: center;
    gap: 4px;
    width: 120px;
    padding: 0 10px;
    box-sizing: border-box;
    font-size: 12px;
    line-height: 1.3;
    color: var(--el-text-color-primary);
    border-right: 1px solid var(--el-border-color-light);
  }
  &__list {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    padding: 6px 10px;
    box-sizing: border-box;
    align-items: center;
  }
  &__empty {
    flex: 1;
    align-self: center;
    padding: 5px 10px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}
.matrix-item {
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 4px 10px;
  font-size: 12px;
  color: #fff;
  cursor: move;
  background: #4996b2;
  border-radius: 10px;
  transition: background-color 0.2s;
  // 重置 el-checkbox 默认的 32px 高度与 30px 右外边距，避免撑大胶囊
  :deep(.el-checkbox) {
    height: auto;
    margin-right: 0;
  }
  &.is-active {
    // 仅改变背景色，不涉及描边/外框，选中前后尺寸与未选中项完全一致
    background: var(--el-color-primary);
  }
  &__text {
    display: inline-block;
    max-width: 96px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &--ghost {
    opacity: 0.6;
    border: 1px dashed var(--el-color-primary);
  }
}
.matrix-result {
  flex: 1;
  min-height: 0;
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  overflow: hidden;
}
.designer-property {
  height: 100%;
  :deep(.el-scrollbar__view) {
    min-height: 100%;
    padding: 12px;
    box-sizing: border-box;
  }
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>
