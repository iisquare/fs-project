<script setup lang="ts">
/**
 * 数据预览组件 - 根据字段映射结构（schema）从接口响应 JSON 中提取表格数据并展示。
 *
 * @prop     json    {Object|String} - 接口响应数据（兼容 JSON 字符串）
 * @prop     schema  {Array}         - 字段映射树（SchemaNode 列表）
 * @prop     loading {Boolean}       - 加载状态
 * @prop     empty   {String}        - 无可用数据时的提示文本
 *
 * 自适应处理：
 *  - 主数据数组：优先选择数据非空且含已勾选字段的数组节点，其次非空数组节点，最后任意数组节点，同等情况取路径最短者
 *  - 列定义：由主数组节点的叶子字段生成，存在已勾选字段时仅列勾选字段；数组元素为标量时以数组节点自身作为单列
 *  - 列名（label）：自适应使用 title || name || field || path
 *  - 单元格取值：依据字段 path 路径提取，不含任何命名耦合，值对象序列化为 JSON 字符串
 *
 * @example
 * <data-preview :json="json" :schema="fields" />
 */
import { computed } from 'vue';

const props = withDefaults(defineProps<{ json: any, schema: any, loading?: boolean, empty?: string }>(), {
  loading: false,
  empty: '暂无数据预览，请先发送请求并配置字段映射',
})

const preview = computed(() => {
  let data = props.json
  if (typeof data === 'string') {
    try {
      data = JSON.parse(data)
    } catch {
      return null
    }
  }
  if (data == null || !Array.isArray(props.schema)) return null
  const nodes: any = []
  const walk = (items: any) => {
    items && items.forEach((item: any) => {
      nodes.push(item)
      walk(item.children)
    })
  }
  walk(props.schema)
  const resolve = (obj: any, path: string) => {
    if (obj == null || !path) return undefined
    return path.split('.').reduce((value: any, key: string) => {
      return value == null ? undefined : value[key]
    }, obj)
  }
  const hasChecked = (items: any) => {
    if (!items) return false
    return items.some((item: any) => item.checked || hasChecked(item.children))
  }
  const arrays = nodes.filter((item: any) => item.type === 'array')
  let candidates = arrays.filter((item: any) => {
    const value = resolve(data, item.path)
    return Array.isArray(value) && value.length
  })
  const preferred = candidates.filter((item: any) => item.checked || hasChecked(item.children))
  if (preferred.length) candidates = preferred
  if (!candidates.length) candidates = arrays.filter((item: any) => Array.isArray(resolve(data, item.path)))
  if (!candidates.length) return null
  candidates.sort((a: any, b: any) => a.path.split('.').length - b.path.split('.').length)
  const array = candidates[0]
  const rows = resolve(data, array.path) || []
  const leaves: any = []
  ;(function collect(items: any) {
    items && items.forEach((item: any) => {
      if (item.children && item.children.length) collect(item.children)
      else leaves.push(item)
    })
  })(array.children)
  const checked = leaves.filter((item: any) => item.checked)
  const list = checked.length ? checked : leaves
  const prefix = array.path ? array.path + '.' : ''
  const columns = list.map((item: any) => ({
    id: item.path,
    label: item.title || item.name || item.field || item.path,
    extract (row: any) {
      const path = prefix ? item.path.slice(prefix.length) : item.path
      return path ? resolve(row, path) : row
    },
  }))
  if (!columns.length) {
    columns.push({
      id: array.path,
      label: array.title || array.name || array.field || array.path,
      extract: (row: any) => row,
    })
  }
  const render = (row: any, column: any) => {
    const value = column.extract(row)
    if (value == null) return ''
    if (typeof value === 'object') return JSON.stringify(value)
    return String(value)
  }
  return { array, columns, rows, render }
})
const previewCell = (row: any, column: any) => preview.value?.render(row, column)
</script>
<template>
  <template v-if="preview">
    <el-table :data="preview.rows" :border="true" table-layout="auto" v-loading="loading">
      <el-table-column v-for="column in preview.columns" :key="column.id" :column-key="column.id" :label="column.label" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ previewCell(scope.row, column) }}
        </template>
      </el-table-column>
    </el-table>
  </template>
  <el-empty v-else :description="empty" />
</template>
