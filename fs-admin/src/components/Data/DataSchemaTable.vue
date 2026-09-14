<script setup lang="ts">
/**
 * 数据字段表格编辑器 - 以表格形式维护数据模型字段，支持增删、排序（置顶/上移/下移/底部）。
 *
 * @v-model  {FieldRow[]}  字段行数组（双向绑定主值）
 * @prop     {String[]}     types    - 可选的字段类型列表，通过 v-model:types 传入
 * @prop     {Boolean}      editable - 是否可编辑，默认 false，通过 v-model:editable 传入
 *
 * 字段行结构 (FieldRow):
 *   { name: string, title: string, type: string, comment: string }
 *   name    - 字段名（必填）
 *   title   - 显示名称（选填，默认为字段名）
 *   type    - 数据类型（必填）
 *   comment - 注释信息（选填）
 *
 * @example
 * <data-schema-table v-model="fields" v-model:types="['String', 'Integer', 'Date']" v-model:editable="true" />
 */
import { computed, nextTick, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import type { TableInstance } from 'element-plus';
import DataUtil from '@/utils/DataUtil';
import UIUtil from '@/utils/UIUtil';

const FLAG_LABELS: any = { required: '必填', display: '画布展示', unique: '唯一', indexed: '索引' }
const model: any = defineModel()
const tableRef = ref<TableInstance>()
const types = defineModel<String[]>('types', { default: () => [] })
const editable = defineModel('editable', { type: Boolean, default: false })
const flags = defineModel<string[]>('flags', { default: () => [] })
const flagColumns = computed(() => (flags.value ?? []).map(key => ({ key, label: FLAG_LABELS[key] ?? key })))
const selection: any = ref([])
const handleAdd = () => {
  model.value.push({
    name: '',
    title: '',
    type: '',
    comment: '',
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
      <el-space>
        <button-add @click="handleAdd" />
        <button-delete :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-button-group>
        <el-button :disabled="selection.length === 0" :icon="ElementPlusIcons.Upload" @click="handleTop" />
        <el-button :disabled="selection.length === 0" :icon="ElementPlusIcons.Top" @click="handleUp" />
        <el-button :disabled="selection.length === 0" :icon="ElementPlusIcons.Bottom" @click="handleDown" />
        <el-button :disabled="selection.length === 0" :icon="ElementPlusIcons.Download" @click="handleBottom" />
      </el-button-group>
    </el-space>
    <el-table
      ref="tableRef"
      :data="model"
      :border="true"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" width="42" />
      <el-table-column label="字段" min-width="140">
        <template #default="scope">
          <el-input v-model="scope.row.name" placeholder="必填，字段名称" />
        </template>
      </el-table-column>
      <el-table-column label="名称" min-width="140">
        <template #default="scope">
          <el-input v-model="scope.row.title" :placeholder="scope.row.name || '选填，默认为字段名称'" />
        </template>
      </el-table-column>
      <el-table-column label="类型" min-width="130">
        <template #default="scope">
          <el-autocomplete v-model="scope.row.type" :fetch-suggestions="query => UIUtil.arraySuggestions(types, query)" placeholder="必填，数据类型" />
        </template>
      </el-table-column>
      <el-table-column label="注释" min-width="160">
        <template #default="scope">
          <el-input v-model="scope.row.comment" placeholder="选填，注释信息" />
        </template>
      </el-table-column>
      <el-table-column v-for="item in flagColumns" :key="item.key" :label="item.label" width="96" align="center">
        <template #default="scope">
          <el-tooltip :content="item.label" placement="top">
            <el-switch
              :model-value="!!scope.row[item.key]"
              size="small"
              @update:model-value="(value: any) => scope.row[item.key] = value"
            />
          </el-tooltip>
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
      <el-table-column prop="name" label="字段" min-width="120" />
      <el-table-column label="名称" min-width="120">
        <template #default="scope">{{ scope.row.title ? scope.row.title : scope.row.name }}</template>
      </el-table-column>
      <el-table-column prop="type" label="类型" min-width="110" />
      <el-table-column prop="comment" label="注释" min-width="140" />
      <el-table-column v-for="item in flagColumns" :key="item.key" :label="item.label" width="96" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row[item.key]" size="small" effect="plain" type="success">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </template>
</template>

<style lang="scss" scoped>
/* 表头不换行，避免"画布展示"这类较长列名折行 */
:deep(.el-table th.el-table__cell > .cell) {
  white-space: nowrap;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  width: 100%;
}
</style>
