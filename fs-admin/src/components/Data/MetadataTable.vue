<script setup lang="ts">
/**
 * 元数据编辑器 - 以表格形式维护元数据。
 *
 * @v-model  {Record<string, string>}  元数据对象（双向绑定主值）
 * @prop     {Boolean}                 editable - 是否可编辑，默认 false，通过 v-model:editable 传入
 *
 * @example
 * <metadata-table v-model="{&quot;作者&quot;: &quot;张三&quot;, &quot;版本&quot;: &quot;1.0&quot;}" :editable="true" />
 */
import { nextTick, ref, watch } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import type { TableInstance } from 'element-plus';
import DataUtil from '@/utils/DataUtil';

const model = defineModel<Record<string, string>>({ default: () => ({}) })
const tableRef = ref<TableInstance>()
const editable = defineModel('editable', { type: Boolean, default: false })
const selection: any = ref([])

interface FieldRow {
  key: string
  value: string
}

const rows = ref<FieldRow[]>([])
let syncingFromModel = false
let syncingToModel = false

const initRows = () => {
  rows.value = Object.entries(model.value ?? {}).map(([key, value]) => ({ key, value }))
}
initRows()

watch(() => model.value, () => {
  if (syncingToModel) return
  syncingFromModel = true
  initRows()
  nextTick(() => { syncingFromModel = false })
})

const syncToModel = () => {
  if (syncingFromModel) return
  syncingToModel = true
  const obj: Record<string, string> = {}
  rows.value.forEach(({ key, value }) => {
    obj[key] = value
  })
  model.value = obj
  nextTick(() => { syncingToModel = false })
}

watch(rows, () => syncToModel(), { deep: true })

const handleAdd = () => {
  rows.value.push({
    key: '',
    value: '',
  })
}
const handleDelete = () => {
  rows.value = DataUtil.removeArrayItem(rows.value, selection.value)
}
const toggleRowSelection = (items: any, selected: boolean = true) => {
  nextTick(() => {
    items.forEach((row: any) => {
      tableRef.value?.toggleRowSelection(row, selected)
    })
  })
}
const handleTop = () => {
  rows.value = selection.value.concat(DataUtil.removeArrayItem(rows.value, selection.value))
  toggleRowSelection(selection.value)
}
const handleUp = () => {
  let index = rows.value.length - 1
  selection.value.forEach((row: any) => {
    index = Math.min(index, rows.value.indexOf(row))
  })
  index = Math.max(0, index - 1)
  const arr = DataUtil.removeArrayItem(rows.value, selection.value)
  arr.splice(index, 0, ...selection.value)
  rows.value = arr
  toggleRowSelection(selection.value)
}
const handleDown = () => {
  let index = 0
  selection.value.forEach((row: any) => {
    index = Math.max(index, rows.value.indexOf(row))
  })
  const arr = DataUtil.removeArrayItem(rows.value, selection.value)
  index = Math.min(arr.length, index + 1)
  arr.splice(index, 0, ...selection.value)
  rows.value = arr
  toggleRowSelection(selection.value)
}
const handleBottom = () => {
  rows.value = DataUtil.removeArrayItem(rows.value, selection.value).concat(selection.value)
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
      :data="rows"
      :border="true"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <el-table-column label="字段名称">
        <template #default="scope">
          <el-input v-model="scope.row.key" placeholder="必填，字段名称" />
        </template>
      </el-table-column>
      <el-table-column label="字段值">
        <template #default="scope">
          <el-input v-model="scope.row.value" placeholder="必填，字段值" />
        </template>
      </el-table-column>
    </el-table>
  </template>
  <template v-else>
    <el-table
      :data="rows"
      :border="true"
      table-layout="auto"
    >
      <el-table-column prop="key" label="字段名称" />
      <el-table-column prop="value" label="字段值" />
    </el-table>
  </template>
</template>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  width: 100%;
}
</style>
