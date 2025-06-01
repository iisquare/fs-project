<script setup lang="ts">
/**
 * 根据数据模型，选择字段名称
 */
import { watch, ref, nextTick } from 'vue';
import type { TableInstance } from 'element-plus';
import DataUtil from '@/utils/DataUtil';

const model: any = defineModel()
const tableRef = ref<TableInstance>()
const fields = defineModel('fields', { type: Array<Object>, default: [] })
const editable = defineModel('editable', { type: Boolean, default: false })
const selection = ref([])

watch(selection, (val) => {
  model.value = DataUtil.values(val, 'name')
})

watch(fields, (rows) => {
  const selected = model.value
  nextTick(() => {
    rows.forEach((item: any) => {
      tableRef.value?.toggleRowSelection(item, selected.includes(item.name))
    })
  })
})

</script>
<template>
<el-table
  ref="tableRef"
  :data="fields"
  :border="true"
  table-layout="auto"
  @selection-change="newSelection => selection = newSelection"
>
  <el-table-column type="selection" :selectable="() => editable" />
  <el-table-column prop="name" label="字段" />
  <el-table-column label="名称">
    <template #default="scope">{{ scope.row.title ? scope.row.title : scope.row.name }}</template>
  </el-table-column>
  <el-table-column prop="type" label="类型" />
</el-table>
</template>

<style lang="scss" scoped>
</style>
