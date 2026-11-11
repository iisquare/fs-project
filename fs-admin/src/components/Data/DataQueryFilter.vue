<script setup lang="ts">
/**
 * 查询条件编辑器 - 可视化构建 AND/OR 组合的过滤条件树，输出与计算引擎一致的条件结构。
 *
 * @v-model  {FilterNode[]}  过滤条件树（双向绑定主值）
 * @prop     {FieldItem[]}   fields - 可选字段列表，通过 v-model:fields 传入
 * @prop     {Boolean}       toolbar - 是否展示顶部操作栏（添加条件/添加关系/清空全部），
 *                                     默认展示，嵌套层级由父级关系行提供添加与清空操作时关闭
 *
 * 过滤条件树节点结构 (FilterNode):
 *   关系节点: { id, enabled, type: 'RELATION', value: 'AND'|'OR', children: FilterNode[] }
 *   条件节点: { id, enabled, type: 'OPERATION', value: string, left: string, right: string }
 *
 * 可选运算符: EQUAL, NOT_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN,
 *            GREATER_THAN_OR_EQUAL, IS_NULL, IS_NOT_NULL, LIKE, NOT_LIKE, IN, NOT_IN
 *
 * @example
 * <data-query-filter v-model="filter" v-model:fields="datasetFields" />
 */
import { computed } from 'vue'
import { Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import UIUtil from '@/utils/UIUtil'

defineOptions({ name: 'DataQueryFilter' })

const model = defineModel<any[]>({ default: () => [] })
const fields = defineModel<Object[]>('fields', { default: () => [] })
const { toolbar = true } = defineProps({
  toolbar: { type: Boolean, default: true },
})

// 兼容 { label, value } 与 { name, title } 两种字段结构，统一取可写入表达式的字段值
const fieldOptions = computed(() => (fields.value || []).map((item: any) => ({
  label: item.label ?? item.title ?? item.name,
  value: item.value ?? item.name,
})))
// 右侧引用字段时以反引号标记，后端据此区分列与字符常量（执行时统一转为 Trino 双引号）
const fieldExpressions = computed(() => fieldOptions.value.map((item: any) => ({
  label: item.label,
  value: (item.value.startsWith('"') || item.value.startsWith('`')) ? item.value : '`' + item.value + '`',
})))

const relations = [
  { label: '并且（AND）', value: 'AND' },
  { label: '或者（OR）', value: 'OR' },
]
const operations = [
  { label: '等于（=）', value: 'EQUAL', valuable: true },
  { label: '不等于（!=）', value: 'NOT_EQUAL', valuable: true },
  { label: '小于（<）', value: 'LESS_THAN', valuable: true },
  { label: '小于等于（<=）', value: 'LESS_THAN_OR_EQUAL', valuable: true },
  { label: '大于（>）', value: 'GREATER_THAN', valuable: true },
  { label: '大于等于（>=）', value: 'GREATER_THAN_OR_EQUAL', valuable: true },
  { label: '为空（is null）', value: 'IS_NULL', valuable: false },
  { label: '不为空（is not null）', value: 'IS_NOT_NULL', valuable: false },
  { label: '包含（like）', value: 'LIKE', valuable: true },
  { label: '不包含（not like）', value: 'NOT_LIKE', valuable: true },
  { label: '在列表中（in）', value: 'IN', valuable: true },
  { label: '不在列表中（not in）', value: 'NOT_IN', valuable: true },
]
const operationMap: any = operations.reduce((map: any, item: any) => {
  map[item.value] = item
  return map
}, {})

const generateRelation = (): any => {
  return { id: UIUtil.uuid('relation-'), enabled: true, type: 'RELATION', value: 'AND', children: [] }
}
const generateOperation = (): any => {
  return { id: UIUtil.uuid('operation-'), enabled: true, type: 'OPERATION', value: 'EQUAL', left: '', right: '' }
}
const appendRelation = (item?: any) => {
  if (item) {
    item.children.push(generateRelation())
  } else {
    model.value.push(generateRelation())
  }
}
const appendOperation = (item?: any) => {
  if (item) {
    if (!item.children) item.children = []
    item.children.push(generateOperation())
  } else {
    model.value.push(generateOperation())
  }
}
const remove = (index: number) => {
  model.value.splice(index, 1)
}
const clear = (item?: any) => {
  if (item) {
    item.children = []
  } else {
    model.value = []
  }
}
const visible = (item: any) => {
  const operation = operationMap[item.value]
  return !operation || operation.valuable
}
</script>
<template>
  <div class="query-filter">
    <el-space v-if="toolbar" class="query-filter-toolbar">
      <el-button size="small" @click="appendOperation()">添加条件</el-button>
      <el-button size="small" @click="appendRelation()">添加关系</el-button>
      <el-button size="small" type="danger" @click="clear()">清空全部</el-button>
    </el-space>
    <draggable
      v-if="model.length"
      v-model="model"
      class="query-filter-list"
      item-key="id"
      handle=".query-filter-drag"
      ghost-class="query-filter-node--ghost"
      :animation="160">
      <template #item="{ element: item, index }">
        <div class="query-filter-node" :class="{ 'is-relation': item.type === 'RELATION' }">
          <div class="query-filter-line">
            <el-icon class="query-filter-drag" title="拖动排序"><Rank /></el-icon>
            <el-checkbox v-model="item.enabled" />
            <el-select v-if="item.type === 'RELATION'" v-model="item.value" size="small" class="relation-selector">
              <el-option v-for="relation in relations" :key="relation.value" :value="relation.value" :label="relation.label" />
            </el-select>
            <template v-else>
              <el-autocomplete
                v-model="item.left"
                size="small"
                class="field-selector"
                popper-class="query-filter-field-dropdown"
                style="flex: 0 0 150px; width: 150px; max-width: 150px"
                placeholder="字段"
                clearable
                :fetch-suggestions="(query: string) => UIUtil.fetchSuggestions(fieldOptions, query, 'value')">
                <template #default="{ item: option }">{{ option.data.label || option.value }}</template>
              </el-autocomplete>
              <el-select
                v-model="item.value"
                size="small"
                popper-class="query-filter-operation-dropdown"
                class="operation-selector"
                style="flex: 0 0 160px; width: 160px">
                <el-option v-for="operation in operations" :key="operation.value" :value="operation.value" :label="operation.label" />
              </el-select>
              <el-autocomplete
                v-if="visible(item)"
                v-model="item.right"
                size="small"
                class="value-selector"
                style="flex: 1 1 220px; min-width: 180px; max-width: 280px"
                placeholder="值（引用字段使用反引号）"
                clearable
                :fetch-suggestions="(query: string) => UIUtil.fetchSuggestions(fieldExpressions, query, 'value')">
                <template #default="{ item: option }">{{ option.data.label || option.value }}</template>
              </el-autocomplete>
            </template>
            <div class="query-filter-actions">
              <template v-if="item.type === 'RELATION'">
                <el-button size="small" link @click="appendRelation(item)">关系</el-button>
                <el-button size="small" link @click="appendOperation(item)">条件</el-button>
                <el-button size="small" link @click="clear(item)">清空</el-button>
              </template>
              <el-button size="small" link type="danger" @click="remove(index)">删除</el-button>
            </div>
          </div>
          <data-query-filter v-if="item.type === 'RELATION' && item.children && item.children.length" v-model="item.children" v-model:fields="fields" :toolbar="false" class="query-filter-children" />
        </div>
      </template>
    </draggable>
    <el-empty v-if="!model.length" class="query-filter-empty" description="暂无筛选条件" :image-size="48" />
  </div>
</template>
<style lang="scss" scoped>
.query-filter {
  width: 100%;
}
.query-filter-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  margin-bottom: 10px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.query-filter-toolbar :deep(.el-button + .el-button) {
  margin-left: 0;
}
.query-filter-empty {
  display: flex;
  min-height: 64px;
  flex-direction: column;
  justify-content: center;
  padding: 0;

  :deep(.el-empty__description) {
    margin-top: 4px;

    p {
      font-size: 11px;
      line-height: 18px;
    }
  }
}
.query-filter-node {
  padding: 3px 4px;
  border-radius: 6px;
  transition: background-color 0.15s;

  &:hover {
    background: var(--el-fill-color-lighter);
  }

  &.is-relation {
    padding-top: 6px;
    padding-bottom: 6px;
    border: 1px solid var(--el-border-color-extra-light);
    background: #fafbfd;
  }
}
.query-filter-node--ghost {
  opacity: 0.45;
  background: var(--el-color-primary-light-9);
}
.query-filter-line {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 6px;

  :deep(.el-checkbox) {
    flex: none;
    margin-right: 0;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    min-height: 26px;
  }
}
.query-filter-line :deep(.el-button + .el-button) {
  margin-left: 0;
}
.query-filter-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 2px;
  margin-left: 8px;

  :deep(.el-button) {
    height: 24px;
    margin-left: 0;
    padding: 0 5px;
    font-size: 11px;
  }

  :deep(.el-button.is-link:not(.el-button--danger)) {
    color: var(--el-text-color-secondary);

    &:hover {
      color: var(--el-color-primary);
    }
  }
}
.query-filter-drag {
  flex: none;
  padding: 4px 2px;
  color: var(--el-text-color-placeholder);
  font-size: 14px;
  cursor: grab;
  transition: color 0.15s;

  &:active {
    cursor: grabbing;
  }
}
.query-filter-line:hover .query-filter-drag {
  color: var(--el-color-primary);
}
.query-filter-children {
  padding-left: 24px;
  border-left: 1px dashed var(--el-border-color-light);
  margin-top: 4px;
  margin-left: 8px;
}
.relation-selector {
  width: 124px;
}
.field-selector {
  flex: 0 0 150px;
  width: 150px;
  max-width: 150px;
}
.operation-selector {
  flex: 0 0 160px;
  width: 160px;
}
.value-selector {
  flex: 1;
  min-width: 190px;
  max-width: 280px;
}

:global(.query-filter-operation-dropdown .el-select-dropdown__item) {
  min-width: 176px;
}

:global(.query-filter-field-dropdown) {
  min-width: 240px;
}
</style>
