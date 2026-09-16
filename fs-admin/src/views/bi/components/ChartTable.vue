<script setup lang="ts">
import { computed } from 'vue';
import { CaretTop, CaretBottom } from '@element-plus/icons-vue';

defineOptions({ name: 'ChartTable' })

const levels: any = defineModel({ default: () => [] })
const props = defineProps({
  config: { type: Object, default: () => ({}) },
  axis: { type: Object, default: null },
  options: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['drill'])

const hasDrill = computed(() => (props.axis?.xSize || 0) > 1)
const rows = computed(() => {
  const axis: any = props.axis
  if (!axis) return []
  const result: any[] = []
  ;(axis.x?.data || []).forEach((value: any, rowIndex: number) => {
    const item: any = { x: value }
    ;(axis.y || []).forEach((y: any, columnIndex: number) => {
      item['y' + columnIndex] = y.data[rowIndex]
    })
    result.push(item)
  })
  return result
})

const sortMethod = (prop: string, a: any, b: any) => {
  return (Number(a[prop]) || 0) - (Number(b[prop]) || 0)
}
const drillUp = (record: any) => {
  levels.value.pop()
  emit('drill', record)
}
const drillDown = (record: any, index: number) => {
  levels.value.push({ index, x: record.x })
  emit('drill', record)
}
</script>
<template>
  <div class="chart-table">
    <el-table :border="false" :data="rows" table-layout="auto" height="100%" size="small">
      <el-table-column v-if="hasDrill" :key="'drill-' + (axis?.xSize || 0)" label="钻取" width="80" align="center">
        <template #default="scope">
          <el-icon v-if="levels.length > 0" class="fs-drill-icon" @click="drillUp(scope.row)"><CaretTop /></el-icon>
          <el-icon v-if="levels.length < (axis?.xSize || 0) - 1" class="fs-drill-icon" @click="drillDown(scope.row, scope.$index)"><CaretBottom /></el-icon>
        </template>
      </el-table-column>
      <el-table-column :key="'x-' + (axis?.x?.label || '')" :label="axis?.x?.label || 'X-坐标'" prop="x" align="center" />
      <el-table-column
        v-for="(y, columnIndex) in (axis?.y || [])"
        :key="'y' + columnIndex + '-' + (y.label || '')"
        :label="y.label || ('Y-坐标-' + columnIndex)"
        :prop="'y' + columnIndex"
        align="center"
        sortable
        :sort-method="(a: any, b: any) => sortMethod('y' + columnIndex, a, b)" />
    </el-table>
  </div>
</template>
<style lang="scss" scoped>
.chart-table {
  height: 100%;
}
.fs-drill-icon {
  cursor: pointer;
  margin: 0 5px;
}
</style>
