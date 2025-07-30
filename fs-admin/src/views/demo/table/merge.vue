<script setup lang="ts">
import { ref, onMounted } from 'vue';
import TableUtil from '@/utils/TableUtil';
import TableRadio from '@/components/Table/TableRadio.vue';

const rows = ref([
  /**
   *    c0      c1      c2
   * r0 a1      b1      c1
   * r1 a1      b1      c2
   * r2 a1      b2      c3
   * r3 a2      b3
   * r4 a3
   */
  { id: 'a1', parentId: '', name: 'L0-a1' },
  { id: 'a2', parentId: '', name: 'L0-a2' },
  { id: 'a3', parentId: '', name: 'L0-a3' },
  { id: 'b1', parentId: 'a1', name: 'L1-b1' },
  { id: 'b2', parentId: 'a1', name: 'L1-b2' },
  { id: 'b3', parentId: 'a2', name: 'L1-b3' },
  { id: 'c1', parentId: 'b1', name: 'L2-c1' },
  { id: 'c2', parentId: 'b1', name: 'L2-c2' },
  { id: 'c3', parentId: 'b2', name: 'L2-c3' }
])
const table: any = ref({})
const selection = ref()

onMounted(() => {
  table.value = TableUtil.matrix(TableUtil.tree(rows.value, ''), 3)
  TableUtil.pretty(table.value)
})
</script>

<template>
  <el-card :bordered="false">
    <el-alert>Select: {{ selection }}</el-alert>
    <el-table
      :data="table.leaves"
      :row-key="record => record.id"
      :border="true"
      :span-method="(data: any) => TableUtil.span(table, data.rowIndex, data.columnIndex, 2)"
    >
      <TableRadio v-model="selection" :value="(scope: any) => scope.row.id" width="50px" />
      <el-table-column label="LEAF_ID" prop="id" />
      <el-table-column label="LEVEL_0" :formatter="(row: any, column: any, cellValue: any, index: number) => TableUtil.cellValue(table, index, 0)" />
      <el-table-column label="LEVEL_1" :formatter="(row: any, column: any, cellValue: any, index: number) => TableUtil.cellValue(table, index, 1)" />
      <el-table-column label="LEVEL_2" :formatter="(row: any, column: any, cellValue: any, index: number) => TableUtil.cellValue(table, index, 2)" />
      <el-table-column label="LEAF_NAME" prop="name" />
    </el-table>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
