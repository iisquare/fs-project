<script setup lang="ts">
/**
 * 字段映射编辑器 - 以树形表格勾选响应结构字段并配置映射信息，支持全选/全不选/反选，
 * 勾选与映射结果直接维护在响应结构树（schema）上，不单独输出字段列表。
 *
 * @v-model  {SchemaNode[]}  schema - 响应结构树（双向绑定主值）
 * @prop     {String[]}      types  - 可选的字段类型列表，通过 v-model:types 传入
 * @prop     {Boolean}       editable - 是否可编辑，默认 false，通过 v-model:editable 传入
 *
 * SchemaNode 结构:
 *   { path: string, field: string, name: string, title: string, type: string, comment: string, checked: boolean, children: SchemaNode[] }
 *   path     - 字段路径，数据或对象的下级字段之间采用点分割
 *   field    - 响应字段名
 *   name     - 映射字段名（选填，默认为响应字段名）
 *   title    - 显示名称（选填，默认为字段名）
 *   type     - 数据类型
 *   comment  - 注释信息（选填）
 *   checked  - 是否勾选
 *   children - 子节点
 *
 * @example
 * <data-field-mapping v-model="fields" v-model:types="['String', 'Integer', 'Boolean', 'Date']" />
 */
import { computed, ref } from 'vue';
import TreeUtil from '@/utils/TreeUtil';
import UIUtil from '@/utils/UIUtil';

const schema: any = defineModel()
const types = defineModel<String[]>('types', { default: () => [] })
const editable = defineModel('editable', { type: Boolean, default: false })
const view = ref<'all' | 'checked'>('checked')
const checkedCount = computed(() => TreeUtil.ids(schema.value, (item: any) => !item.checked, 'path').length)
const visibleSchema = computed(() => {
  if (editable.value || view.value !== 'checked') return schema.value
  const filter = (items: any): any => {
    const result: any = []
    items && items.forEach((item: any) => {
      const children = filter(item.children)
      if (item.checked || children.length) {
        result.push({ ...item, children })
      }
    })
    return result
  }
  return filter(schema.value)
})
const handleAll = (checked: boolean) => {
  ;(function walk(items: any) {
    items && items.forEach((item: any) => {
      item.checked = checked
      walk(item.children)
    })
  })(schema.value)
  handleFix()
}
const handleInvert = () => {
  ;(function walk(items: any) {
    items && items.forEach((item: any) => {
      item.checked = !item.checked
      walk(item.children)
    })
  })(schema.value)
  handleFix()
}
const handleFix = () => {
  ;(function walk(items: any) {
    items && items.forEach((item: any) => {
      walk(item.children)
      if (item.children && item.children.length) {
        item.checked = item.children.every((child: any) => child.checked)
      }
    })
  })(schema.value)
}
const handleCheck = (item: any) => {
  ;(function walk(items: any, checked: boolean) {
    items && items.forEach((child: any) => {
      child.checked = checked
      walk(child.children, checked)
    })
  })(item.children, item.checked)
  handleFix()
}
const halfChecked = (item: any) => {
  const children = item.children || []
  if (!children.length) return false
  const checked = children.filter((child: any) => child.checked).length
  return checked > 0 && checked < children.length
}
</script>
<template>
  <el-empty v-if="!schema.length" description="请先发送请求获取响应结构" />
  <template v-else>
    <div class="flex-between" style="margin-bottom: 15px">
      <el-space v-if="editable">
        <el-button size="small" @click="handleAll(true)">全选</el-button>
        <el-button size="small" @click="handleAll(false)">全不选</el-button>
        <el-button size="small" @click="handleInvert">反选</el-button>
      </el-space>
      <el-radio-group v-else v-model="view" size="small">
        <el-radio-button value="all">全部字段</el-radio-button>
        <el-radio-button value="checked">已选字段</el-radio-button>
      </el-radio-group>
      <span class="field-count">已选 <b>{{ checkedCount }}</b> 个字段</span>
    </div>
    <el-table :data="visibleSchema" row-key="path" :border="true" table-layout="auto" default-expand-all>
      <el-table-column label="字段" align="left" width="300" class-name="field-column">
        <template #default="scope">
          <el-checkbox
            v-if="editable"
            v-model="scope.row.checked"
            :indeterminate="halfChecked(scope.row)"
            @change="handleCheck(scope.row)"
            :label="scope.row.field"
            class="field-checkbox"
          />
          <el-checkbox v-else :model-value="scope.row.checked" disabled :label="scope.row.field" class="field-checkbox" />
        </template>
      </el-table-column>
      <el-table-column label="映射" min-width="200">
        <template #default="scope">
          <el-input v-if="editable" v-model="scope.row.name" placeholder="必填，字段名称" />
          <span v-else>{{ scope.row.name || scope.row.field }}</span>
        </template>
      </el-table-column>
      <el-table-column label="名称" min-width="180">
        <template #default="scope">
          <el-input v-if="editable" v-model="scope.row.title" :placeholder="scope.row.name || '选填，默认为字段名称'" />
          <span v-else>{{ scope.row.title || scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" min-width="150">
        <template #default="scope">
          <el-autocomplete v-if="editable" v-model="scope.row.type" :fetch-suggestions="query => UIUtil.arraySuggestions(types, query)" placeholder="必填，数据类型" />
          <span v-else>{{ scope.row.type }}</span>
        </template>
      </el-table-column>
      <el-table-column label="注释" min-width="200">
        <template #default="scope">
          <el-input v-if="editable" v-model="scope.row.comment" placeholder="选填，注释信息" />
          <span v-else>{{ scope.row.comment }}</span>
        </template>
      </el-table-column>
    </el-table>
  </template>
</template>

<style lang="scss" scoped>
.field-column {
  :deep(.cell) {
    white-space: nowrap;
  }
}
.field-checkbox {
  display: inline-flex;
  max-width: calc(100% - 24px);
  vertical-align: middle;
  :deep(.el-checkbox__label) {
    flex: 1;
    min-width: 0;
    line-height: 20px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.field-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  b {
    font-size: 16px;
    color: var(--el-color-primary);
    font-weight: 600;
    margin: 0 2px;
  }
}
</style>
