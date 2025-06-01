<script setup lang="ts">
/**
 * 通过表格方式维护数据模型字段
 */
import { nextTick, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import type { TableInstance } from 'element-plus';
import DataUtil from '@/utils/DataUtil';
import UIUtil from '@/utils/UIUtil';

const model: any = defineModel()
const tableRef = ref<TableInstance>()
const types = defineModel('types', { type: Array<String>, default: [] })
const editable = defineModel('editable', { type: Boolean, default: false })
const selection = ref([])
const handleAdd = () => {
  model.value.push({
    name: '',
    title: '',
    type: '',
  })
}
const handleDelete = () => {
  model.value = DataUtil.removeArrayItem(model.value, selection.value)
}
const toggleRowSelection = (rows: any, selected: boolean = true) => {
  nextTick(() => {
    rows.forEach((row: any) => {
      tableRef.value?.toggleRowSelection(row, selected)
    })
  })
}
const handleTop = () => {
  model.value = selection.value.concat(DataUtil.removeArrayItem(model.value, selection.value))
  toggleRowSelection(selection.value)
}
const handleUp = () => {
  let index = model.value.length - 1
  selection.value.forEach((row: any) => {
    index = Math.min(index, model.value.indexOf(row))
  })
  index = Math.max(0, index - 1)
  const rows = DataUtil.removeArrayItem(model.value, selection.value)
  rows.splice(index, 0, ...selection.value)
  model.value = rows
  toggleRowSelection(selection.value)
}
const handleDown = () => {
  let index = 0
  selection.value.forEach((row: any) => {
    index = Math.max(index, model.value.indexOf(row))
  })
  const rows = DataUtil.removeArrayItem(model.value, selection.value)
  index = Math.min(rows.length, index + 1)
  rows.splice(index, 0, ...selection.value)
  model.value = rows
  toggleRowSelection(selection.value)
}
const handleBottom = () => {
  model.value = DataUtil.removeArrayItem(model.value, selection.value).concat(selection.value)
  toggleRowSelection(selection.value)
}
</script>
<template>
  <template v-if="editable">
    <el-space class="toolbar">
      <button-add @click="handleAdd" />
      <button-delete :disabled="selection.length === 0" @click="handleDelete" />
      <el-button :disabled="selection.length === 0" :icon="ElementPlusIcons.Upload" @click="handleTop">顶部</el-button>
      <el-button :disabled="selection.length === 0" :icon="ElementPlusIcons.Top" @click="handleUp">上移</el-button>
      <el-button :disabled="selection.length === 0" :icon="ElementPlusIcons.Bottom" @click="handleDown">下移</el-button>
      <el-button :disabled="selection.length === 0" :icon="ElementPlusIcons.Download" @click="handleBottom">底部</el-button>
    </el-space>
    <el-table
      ref="tableRef"
      :data="model"
      :border="true"
      table-layout="auto"
      @selection-change="newSelection => selection = newSelection"
    >
      <el-table-column type="selection" />
      <el-table-column label="字段">
        <template #default="scope">
          <el-input v-model="scope.row.name" />
        </template>
      </el-table-column>
      <el-table-column label="名称">
        <template #default="scope">
          <el-input v-model="scope.row.title" :placeholder="scope.row.name" />
        </template>
      </el-table-column>
      <el-table-column label="类型">
        <template #default="scope">
          <el-autocomplete v-model="scope.row.type" :fetch-suggestions="query => UIUtil.arraySuggestions(types, query)" />
        </template>
      </el-table-column>
    </el-table>
  </template>
  <template v-else>
    <el-table
      :data="model"
      :border="true"
      table-layout="auto"
    >
      <el-table-column prop="name" label="字段" />
      <el-table-column label="名称">
        <template #default="scope">{{ scope.row.title ? scope.row.title : scope.row.name }}</template>
      </el-table-column>
      <el-table-column prop="type" label="类型" />
    </el-table>
  </template>
</template>

<style lang="scss" scoped>
.toolbar {
  margin-bottom: 15px;
}
</style>
