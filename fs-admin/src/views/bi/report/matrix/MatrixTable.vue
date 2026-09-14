<script setup lang="ts">
import { computed } from 'vue';
import TableColumn from '@/components/Table/TableColumn.vue';

defineOptions({ name: 'MatrixTable' })

const props = defineProps({
  value: { type: Object, default: null },
  loading: { type: Boolean, default: false },
})

const glue = '#'

const matrixColumn = (matrix: any, metrics: any, roads: any[]): any[] => {
  const columns: any[] = []
  ;(matrix || []).forEach((item: any) => {
    const road = roads.concat([item.label])
    const column: any = { label: item.label, align: 'center', children: [] }
    if (item.children && item.children.length > 0) {
      column.children = matrixColumn(item.children, metrics, road)
    } else {
      (metrics || []).forEach((metric: any) => {
        column.children.push({ label: metric.label, prop: road.concat([metric.key]).join(glue), align: 'center' })
      })
    }
    columns.push(column)
  })
  return columns
}

const columns = computed(() => {
  const value: any = props.value
  const result: any[] = []
  if (!value) return result
  let level = result
  ;(value.levels || []).forEach((item: any) => {
    const column: any = { label: item.label, align: 'center', children: [] }
    level.push(column)
    level = column.children
  })
  ;(value.buckets || []).forEach((item: any) => {
    level.push({ label: item.label, prop: item.key, align: 'center' })
  })
  if (!value.matrix || value.matrix.length < 1) { // 无钻取层级时以度量直接作为表头
    ;(value.metrics || []).forEach((metric: any) => {
      level.push({ label: metric.label, prop: metric.key, align: 'center' })
    })
    return result
  }
  return result.concat(matrixColumn(value.matrix, value.metrics, []))
})

// el-table-column 动态变更 label/结构时不会自动刷新表头，用列结构签名作为 key 触发重建
const columnSign = computed(() => JSON.stringify(columns.value))

const unique = (buckets: any, row: any) => {
  const result: any[] = []
  ;(buckets || []).forEach((item: any) => {
    result.push(row[item.key])
  })
  return result.join(glue)
}

const rows = computed(() => {
  const value: any = props.value
  if (!value) return []
  const result = value.x || []
  const refer: any = {}
  result.forEach((item: any) => {
    refer[unique(value.buckets, item)] = item
  })
  ;(value.y || []).forEach((item: any) => {
    ;(value.metrics || []).forEach((metric: any) => {
      const road = (item.roads || []).concat([metric.key]).join(glue)
      ;(item.metrics || []).forEach((line: any) => {
        const row = refer[unique(value.buckets, line)]
        if (row) row[road] = line[metric.key]
      })
    })
  })
  return result
})
</script>

<template>
  <div class="matrix-table">
    <el-table
      :border="true"
      :data="rows"
      v-loading="loading"
      table-layout="auto"
      height="100%"
      size="small"
    >
      <TableColumn :key="columnSign" :columns="columns" />
    </el-table>
  </div>
</template>

<style lang="scss" scoped>
.matrix-table {
  height: 100%;
}
</style>
