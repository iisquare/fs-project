<script setup lang="ts">
/**
 * 表格排序设置 - 下拉面板组件，可视化配置多列排序规则（升序/降序），输出排序字符串。
 *
 * @v-model  {String}          排序字符串（双向绑定主值），格式如 "field1.asc,field2.desc"
 * @prop     {ColumnConfig[]}  columns  - 表格列配置数组（必填），需包含 prop 和 label
 * @prop     {String}          sortable - 可排序字段列表，格式同 v-model，用于初始化可选字段
 * @prop     {Boolean}         loading  - 表格加载中状态，加载中时禁用排序设置
 *
 * @emits {Function} change - 确认排序变更时触发

 * @example
 * <table-sort v-model="sortString" :columns="columns" :sortable="'createdAt.desc'" @change="fetchData" />
 * <table-sort v-model="sortString" :columns="[...columns, { prop: 'beginTime', label: '处理开始时间' }]" :sortable="config.sorts" @change="fetchData" />
 * const tableSortColumns = computed(() => [
 *   ...columns.value,
 *   { prop: 'beginTime', label: '处理开始时间' },
 *   { prop: 'coastTotal', label: '整体耗时(ms)' },
 * ])
 * <TableSort v-model="filters.sort" :columns="tableSortColumns" :sortable="config.sorts" @change="handleRefresh(true, true)" />
 * 
 */
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { onMounted, ref, computed, watch } from 'vue'
import type { DropdownInstance, TreeInstance } from 'element-plus'
import type { PropType } from 'vue'
import TreeUtil from '@/utils/TreeUtil'
import DataUtil from '@/utils/DataUtil'

const model = defineModel<string>({ default: '' })
const loading = defineModel('loading', { type: Boolean, default: false })

const props = defineProps({
  columns: { type: Array<any>, required: true },
  sortable: { type: [String, Object], default: '' },
  label: { type: String, default: '' },
  tool: { type: Boolean, default: false },
  size: { type: String as PropType<'' | 'default' | 'large' | 'small'>, default: '' },
})

/** 常用排序字段名称定义，可通过 columns 传入同名 prop 覆盖 */
const BUILTIN_COLUMNS: Array<{ prop: string, label: string }> = [
  { prop: 'id', label: 'ID' },
  { prop: 'status', label: '状态' },
  { prop: 'sort', label: '排序' },
  { prop: 'state', label: '状态' },
  { prop: 'createdTime', label: '创建时间' },
  { prop: 'updatedTime', label: '修改时间' },
  { prop: 'deletedTime', label: '删除时间' },
]

const emit = defineEmits(['change'])

const dropdown = ref<DropdownInstance>()

const parseSortEntries = (value: string) => {
  if (!value) return []
  return value.split(',').map(s => {
    const trimmed = s.trim()
    if (!trimmed) return null
    const lastDot = trimmed.lastIndexOf('.')
    if (lastDot >= 0) {
      const field = trimmed.substring(0, lastDot)
      const order = trimmed.substring(lastDot + 1)
      if (field && (order === 'asc' || order === 'desc')) {
        return { field, order: order as 'asc' | 'desc' }
      }
    }
    return { field: trimmed, order: 'asc' as const }
  }).filter(Boolean) as { field: string; order: 'asc' | 'desc' }[]
}

const sortableFields = computed(() => {
  const sortable = props.sortable as any
  if (sortable && typeof sortable === 'object') {
    return Object.entries(sortable).map(([field, order]) => ({
      field,
      order: order === 'desc' ? ('desc' as const) : ('asc' as const),
    }))
  }
  return parseSortEntries(sortable)
})

const tree = ref<TreeInstance>()
const treeData = ref<any[]>([])
const treeCheckedKeys = ref<string[]>([])
const treeCache = ref<any[]>([])
const orderCache = ref<Record<string, string>>({})

const columnLabels = computed(() => {
  const labels = new Map<string, string>()
  BUILTIN_COLUMNS.forEach(column => labels.set(column.prop, column.label))
  props.columns.forEach((column: any) => {
    if (column?.prop) labels.set(column.prop, column.label)
  })
  return labels
})

const handleAllowDrop = (draggingNode: any, dropNode: any, type: any) => {
  return type !== 'inner'
}

const initialize = () => {
  const currentSort = Object.fromEntries(parseSortEntries(model.value).map(e => [e.field, e.order]))
  treeCache.value = sortableFields.value.map(({ field, order: defaultOrder }) => {
    const checked = field in currentSort
    const order = currentSort[field] || defaultOrder
    return {
      id: field,
      label: columnLabels.value.get(field) || field,
      checked,
      order,
    }
  })
  treeData.value = JSON.parse(JSON.stringify(treeCache.value))
  treeCheckedKeys.value = TreeUtil.ids(treeData.value, (item: any) => !item.checked)
}

onMounted(() => {
  initialize()
})

watch(sortableFields, () => {
  initialize()
})

watch(columnLabels, () => {
  initialize()
})

const handleSubmit = () => {
  orderCache.value = {}
  const walk = (data: any[]) => {
    data.forEach((item: any) => {
      orderCache.value[item.id] = item.order
      if (item.children) walk(item.children)
    })
  }
  walk(treeData.value)
  const parts: string[] = []
  TreeUtil.ids(treeData.value).forEach((id: string) => {
    if (treeCheckedKeys.value.includes(id)) {
      parts.push(`${id}.${orderCache.value[id] || 'asc'}`)
    }
  })
  model.value = parts.join(',')
  dropdown.value?.handleClose()
  emit('change')
}

const handleReset = () => {
  treeData.value = JSON.parse(JSON.stringify(treeCache.value))
  treeCheckedKeys.value = TreeUtil.ids(treeData.value, (item: any) => !item.checked)
}

const handleCancel = () => {
  dropdown.value?.handleClose()
}

const handleCheck = (node: any, data: any) => {
  treeCheckedKeys.value = data.checkedKeys
}

const isActive = computed(() => !!model.value)
const activeCount = computed(() => parseSortEntries(model.value).length)
</script>

<template>
  <el-dropdown
    ref="dropdown"
    :placement="props.tool ? 'bottom-start' : 'bottom-end'"
    trigger="click"
    popper-class="fs-table-dropdown">
    <el-button
      :icon="ElementPlusIcons.Sort"
      :circle="!props.label"
      :type="isActive && !props.tool ? 'primary' : undefined"
      :size="props.size || undefined"
      :class="[{ 'is-active': isActive, 'table-sort-tool': props.tool }]"
      title="排序"
      :loading="loading"
    >
      <template v-if="props.label">
        <span class="table-sort-tool__content">
          <span class="table-sort-tool__label">{{ props.label }}</span>
          <span v-if="activeCount" class="table-sort-tool__count">{{ activeCount }}</span>
        </span>
      </template>
    </el-button>
    <template #dropdown>
      <el-space class="header flex-end" spacer="/">
        <el-button link @click="tree?.setCheckedKeys(treeCheckedKeys = TreeUtil.ids(treeData))">全选</el-button>
        <el-button link @click="tree?.setCheckedKeys(treeCheckedKeys = DataUtil.removeArrayItem(TreeUtil.ids(treeData), treeCheckedKeys))">反选</el-button>
      </el-space>
      <el-scrollbar class="tree-body" max-height="320px">
        <el-tree
          ref="tree"
          :data="treeData"
          node-key="id"
          draggable
          default-expand-all
          show-checkbox
          :check-strictly="true"
          :expand-on-click-node="false"
          :default-checked-keys="treeCheckedKeys"
          @check="handleCheck"
          :allow-drop="handleAllowDrop">
          <template #default="{ data }">
            <div class="tree-item">
              <span class="tree-label" :title="data.label">{{ data.label }}</span>
              <el-radio-group v-model="data.order" size="small">
                <el-radio-button value="asc" @click.stop>升序</el-radio-button>
                <el-radio-button value="desc" @click.stop>降序</el-radio-button>
              </el-radio-group>
            </div>
          </template>
        </el-tree>
      </el-scrollbar>
      <el-space class="footer flex-center" size="large">
        <el-button type="primary" size="small" @click="handleSubmit">确认</el-button>
        <el-button type="warning" size="small" @click="handleReset">重置</el-button>
        <el-button size="small" @click="handleCancel">取消</el-button>
      </el-space>
    </template>
  </el-dropdown>
</template>

<style lang="scss" scoped>
.header, .footer {
  width: 100%;
  height: 35px;
  padding: 0 12px;
  box-sizing: border-box;
}
.table-sort-tool {
  display: inline-flex;
  height: var(--query-control-height, 30px);
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font-weight: 400;
  transition: color 0.15s, border-color 0.15s, background-color 0.15s, box-shadow 0.15s;

  .table-sort-tool__content {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    margin-left: 0;
  }

  &.is-active {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
    box-shadow: 0 0 0 2px var(--el-color-primary-light-9);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
  }

  .table-sort-tool__label {
    flex: none;
    margin-left: 0;
    white-space: nowrap;
  }

  .table-sort-tool__count {
    display: inline-flex;
    flex: 0 0 auto;
    min-width: 17px;
    height: 17px;
    align-items: center;
    justify-content: center;
    margin-left: 0;
    padding: 0 4px;
    border-radius: 999px;
    color: #fff;
    background: var(--el-color-primary);
    font-size: 9px;
    font-style: normal;
    line-height: 1;
    white-space: nowrap;
  }
}
.header {
  border-bottom: solid 1px var(--fs-layout-border-color);
}
.footer {
  border-top: solid 1px var(--fs-layout-border-color);
}
.tree-body {
  max-height: 320px;
  padding: 4px 0;
  box-sizing: border-box;
}
.tree-body :deep(.el-scrollbar__view) {
  padding: 0 2px;
}
.tree-body :deep(.el-tree) {
  background: transparent;
}
.tree-item {
  flex: 1;
  min-width: 0;
  padding-right: 14px;
  @include flex-between();
  gap: 8px;
}
.tree-item :deep(.el-radio-group) {
  flex-shrink: 0;
}
:deep(.el-tree-node__content) {
  height: 30px;
  overflow: hidden;
}
.tree-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
