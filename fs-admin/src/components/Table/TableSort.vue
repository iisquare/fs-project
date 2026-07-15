<script setup lang="ts">
/**
 * 表格排序设置 - 下拉面板组件，可视化配置多列排序规则（升序/降序），输出排序字符串。
 *
 * @v-model  {String}          排序字符串（双向绑定主值），格式如 "field1.asc,field2.desc"
 * @prop     {ColumnConfig[]}  columns  - 表格列配置数组（必填），需包含 prop 和 label
 * @prop     {String}          sortable - 可排序字段列表，格式同 v-model，用于初始化可选字段
 *
 * @emits {Function} change - 确认排序变更时触发
 *
 * @example
 * <table-sort v-model="sortString" :columns="columns" :sortable="'createdAt.desc'" @change="fetchData" />
 */
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { onMounted, ref, computed } from 'vue'
import type { DropdownInstance, TreeInstance } from 'element-plus'
import TreeUtil from '@/utils/TreeUtil'
import DataUtil from '@/utils/DataUtil'

const model = defineModel<string>({ default: '' })

const props = defineProps({
  columns: { type: Array<any>, required: true },
  sortable: { type: String, default: '' },
})

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

const sortableFields = computed(() => parseSortEntries(props.sortable))

const tree = ref<TreeInstance>()
const treeData = ref<any[]>([])
const treeCheckedKeys = ref<string[]>([])
const treeCache = ref<any[]>([])
const orderCache = ref<Record<string, string>>({})

const handleAllowDrop = (draggingNode: any, dropNode: any, type: any) => {
  return type !== 'inner'
}

onMounted(() => {
  const currentSort = Object.fromEntries(parseSortEntries(model.value).map(e => [e.field, e.order]))
  treeCache.value = sortableFields.value.map(({ field, order: defaultOrder }) => {
    const column = props.columns.find((col: any) => col.prop === field)
    const checked = field in currentSort
    const order = currentSort[field] || defaultOrder
    return {
      id: field,
      label: column?.label || field,
      checked,
      order,
    }
  })
  treeData.value = JSON.parse(JSON.stringify(treeCache.value))
  treeCheckedKeys.value = TreeUtil.ids(treeData.value, (item: any) => !item.checked)
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
</script>

<template>
  <el-dropdown ref="dropdown" placement="bottom-end" trigger="click" popper-class="fs-table-dropdown">
    <el-button
      :icon="ElementPlusIcons.Sort"
      circle
      :type="isActive ? 'primary' : undefined"
      title="排序"
    />
    <template #dropdown>
      <el-space class="header flex-end" spacer="/">
        <el-button link @click="tree?.setCheckedKeys(treeCheckedKeys = TreeUtil.ids(treeData))">全选</el-button>
        <el-button link @click="tree?.setCheckedKeys(treeCheckedKeys = DataUtil.removeArrayItem(TreeUtil.ids(treeData), treeCheckedKeys))">反选</el-button>
      </el-space>
      <div class="tree-body">
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
      </div>
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
.header {
  border-bottom: solid 1px var(--fs-layout-border-color);
}
.footer {
  border-top: solid 1px var(--fs-layout-border-color);
}
.tree-body {
  max-height: 320px;
  overflow-y: auto;
  padding: 4px 0;
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
