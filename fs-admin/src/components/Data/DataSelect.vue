<script setup lang="ts">
/**
 * 字段选择器 - 以表格多选的方式从字段列表中选取字段，返回选中的字段名数组。
 *
 * @v-model  {String[]}   选中的字段名称数组（双向绑定主值）
 * @prop     {FieldItem[]} fields   - 可选字段列表，通过 v-model:fields 传入
 * @prop     {Boolean}     editable - 是否可编辑，默认 false，通过 v-model:editable 传入
 *
 * 字段项结构 (FieldItem):
 *   { name: string, title?: string, type: string }
 *
 * @example
 * <data-select v-model="selectedFields" v-model:fields="allFields" v-model:editable="true" />
 */
import { watch, ref, nextTick } from 'vue';
import type { TableInstance } from 'element-plus';
import DataUtil from '@/utils/DataUtil';

const model: any = defineModel()
const tableRef = ref<TableInstance>()
const fields = defineModel<Object[]>('fields', { default: () => [] })
const editable = defineModel('editable', { type: Boolean, default: false })
const selection: any = ref([])

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
  @selection-change="(s: any) => selection = s"
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
