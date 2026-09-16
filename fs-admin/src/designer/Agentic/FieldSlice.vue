<script setup lang="ts">
/**
 * 字段列表编辑器 - 按列定义渲染一组可增删的字段，供输入变量、输出变量、提取参数等场景复用。
 *
 * @v-model  {Array} 字段数组
 * @prop     {Array} columns - 列定义 `{ prop, type: 'input'|'select'|'variable'|'switch'|'textarea', label, options, placeholder, default, icon }`
 *                            options 取 config 中的字典名称，如 types、codeLanguages
 *                            icon 取 Element Plus 图标名称，作为输入框/下拉框的前缀图标
 * @prop     {Boolean} collapsible - 字段项是否可展开收起，收起时仅展示标题与类型摘要
 * @prop     {String} emptyText - 字段为空时的提示文案，为空则不展示提示
 */
import { Plus } from '@element-plus/icons-vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import CollapseItem from './CollapseItem.vue'
import { useCollapse } from './collapse'
import config from './config'
import VariableSelect from './VariableSelect.vue'

const model: any = defineModel<any[]>({ required: true })
const {
  columns = [],
  addText = '添加一项',
  min = 0,
  collapsible = false,
  emptyText = '',
  instance,
  activeItem = {},
} = defineProps<{
  columns?: any[],
  addText?: string,
  min?: number,
  collapsible?: boolean,
  emptyText?: string,
  instance?: any,
  activeItem?: any,
}>()

const defaultItem = () => {
  const item: any = {}
  columns.forEach((column: any) => {
    if (column.default !== undefined) {
      item[column.prop] = column.default
    } else {
      item[column.prop] = 'switch' === column.type ? false : ''
    }
  })
  return item
}

// 兼容历史数据缺少字段数组的情况
if (!Array.isArray(model.value)) model.value = []

// 仅一条配置时默认展开，多条默认收起，兼顾面板空间与录入效率
const { isOpen, open, toggle, remove } = useCollapse(() => 1 === model.value.length)

const handleAdd = () => {
  model.value.push(defaultItem())
  open(model.value.length - 1) // 新增项默认展开，便于直接填写
}

const handleRemove = (index: number) => {
  if (model.value.length <= min) return
  model.value.splice(index, 1)
  remove(index)
}

const options = (column: any) => config[column.options] ?? []

// 收起时展示的类型摘要，取第一个下拉列对应字典的文案
const typeText = (item: any) => {
  const column: any = columns.find((column: any) => 'select' === column.type)
  const value = column ? item?.[column.prop] : ''
  if (undefined === value || null === value || '' === value) return ''
  const option: any = options(column).find((option: any) => option.value === value)
  return option ? option.label : value
}

// 收起时的摘要：类型与是否必填
const summaryTags = (item: any) => {
  return [typeText(item), true === item.required ? '必填' : ''].filter((text: string) => text)
}
</script>

<template>
  <div class="field-slice">
    <CollapseItem
      :key="index"
      v-for="(item, index) in model"
      :title="item.label || item.name || '项 ' + (index + 1)"
      :tags="summaryTags(item)"
      :collapsible="collapsible"
      :expanded="isOpen(index)"
      @toggle="toggle(index)"
      @delete="handleRemove(index)">
      <template :key="column.prop" v-for="column in columns">
        <div class="field-inline" v-if="column.type === 'switch'">
          <span>{{ column.label }}</span>
          <el-switch v-model="item[column.prop]" />
        </div>
        <VariableSelect
          v-else-if="column.type === 'variable'"
          v-model="item[column.prop]"
          :instance="instance"
          :active-item="activeItem"
          :icon="column.icon"
          allow-create
          :placeholder="column.placeholder ?? '请选择变量'" />
        <el-select
          v-else-if="column.type === 'select'"
          v-model="item[column.prop]"
          :placeholder="column.placeholder ?? '请选择'">
          <template #prefix>
            <LayoutIcon v-if="column.icon" :name="column.icon" />
          </template>
          <el-option :key="option.value" :value="option.value" :label="option.label" v-for="option in options(column)" />
        </el-select>
        <el-input
          v-else-if="column.type === 'textarea'"
          v-model="item[column.prop]"
          type="textarea"
          :rows="column.rows ?? 2"
          :placeholder="column.placeholder">
          <template #prefix>
            <LayoutIcon v-if="column.icon" :name="column.icon" />
          </template>
        </el-input>
        <el-input v-else v-model="item[column.prop]" :placeholder="column.placeholder">
          <template #prefix>
            <LayoutIcon v-if="column.icon" :name="column.icon" />
          </template>
        </el-input>
      </template>
    </CollapseItem>
    <div class="field-empty" v-if="emptyText && !model.length">{{ emptyText }}</div>
    <el-button link type="primary" :icon="Plus" @click="handleAdd">{{ addText }}</el-button>
  </div>
</template>

<style lang="scss" scoped>
.field-slice {
  width: 100%;
  .field-empty {
    margin-bottom: 6px;
    font-size: 12px;
    line-height: 1.8;
    color: var(--el-text-color-placeholder);
  }
  .el-input, .el-select {
    width: 100%;
  }
  .el-input + .el-select, .el-select + .el-input, .el-select + .el-select, .el-input + .field-inline, .el-select + .field-inline {
    margin-top: 6px;
  }
  .field-inline {
    font-size: 12px;
    color: var(--el-text-color-regular);
    @include flex-between();
  }
}
</style>
