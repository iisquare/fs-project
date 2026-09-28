<script setup lang="ts">
/**
 * 字段列表编辑器 - 按列定义渲染一组可增删的字段，供输入变量、输出变量、提取参数等场景复用。
 *
 * @v-model  {Array} 字段数组
 * @prop     {Array} columns - 列定义 `{ prop, type: 'input'|'select'|'variable'|'switch'|'textarea'|'radio', label, options, placeholder, default, icon, when, writable }`
 *                            options 取 config 中的字典名称，如 types、assignOperations
 *                            icon 取 Element Plus 图标名称，作为输入框/下拉框的前缀图标
 *                            writable 仅对 variable 列有效：只列出可写入变量（容器内的元素/索引/循环变量）
 *                            outer 仅对 variable 列有效：只取容器外部变量（容器自身的变量不作为该列取值来源）
 *                            when(item) 返回 false 时该列不渲染（如来源为固定值时不显示引用变量）
 * @prop     {Boolean} collapsible - 字段项是否可展开收起，收起时仅展示标题与类型摘要
 * @prop     {String} emptyText - 字段为空时的提示文案，为空则不展示提示
 * @prop     {String} titleProp - 收起时的标题取哪个字段（如赋值操作的目标变量），缺省取标题名称/变量名称
 */
import { computed, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import CollapseItem from './CollapseItem.vue'
import { useCollapse } from './collapse'
import config from './config'
import { referenceOfToken, variableGroups, variableTokens } from './variable'
import VariableSelect from './VariableSelect.vue'

const model: any = defineModel<any[]>({ required: true })
const {
  columns = [],
  addText = '添加一项',
  min = 0,
  collapsible = false,
  emptyText = '',
  titleProp = '',
  instance,
  activeItem = {},
} = defineProps<{
  columns?: any[],
  addText?: string,
  min?: number,
  collapsible?: boolean,
  emptyText?: string,
  titleProp?: string,
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

/**
 * 兼容历史数据缺少字段数组的情况。
 * 用 watch 而不是 setup 里只跑一次：属性面板实例会在同类型节点之间复用，切换节点时同样要兜底。
 */
watch(model, (value: any) => {
  if (!Array.isArray(value)) model.value = []
}, { immediate: true })

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

// 按列定义上的 when 条件过滤：条件不成立（如「来源为固定值」时的引用变量）不渲染该列
const visibleColumns = (item: any) => columns.filter((column: any) => !column.when || column.when(item))

// 收起时展示的类型摘要，取第一个下拉列对应字典的文案
const typeText = (item: any) => {
  const column: any = columns.find((column: any) => 'select' === column.type)
  const value = column ? item?.[column.prop] : ''
  if (undefined === value || null === value || '' === value) return ''
  const option: any = options(column).find((option: any) => option.value === value)
  return option ? option.label : value
}

/**
 * 变量引用的展示名称（`节点标识.变量名称` → `节点名称.变量名称`）：
 * 收起标题直接显示引用时节点标识太长，先按当前画布解析一遍，解析不到（如手输的会话变量）就用原值
 */
const variableLabels = computed<Record<string, string>>(() => {
  if (!titleProp) return {}
  const tokens = variableTokens(variableGroups(instance, activeItem))
  const result: Record<string, string> = {}
  Object.keys(tokens).forEach((reference: string) => { result[reference] = tokens[reference].label })
  return result
})

// 收起时的标题：优先取 titleProp 指定的字段（如赋值操作的目标变量），其次标题名称、变量名称
const itemTitle = (item: any, index: number) => {
  const reference = String(item?.[titleProp] ?? '')
  // 取值可能是占位符（{{#节点标识.变量名#}}），按其中的变量引用查展示名称
  const key = referenceOfToken(reference) || reference
  if (key) return variableLabels.value[key] || key
  return item?.label || item?.name || '项 ' + (index + 1)
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
      :title="itemTitle(item, index)"
      :tags="summaryTags(item)"
      :collapsible="collapsible"
      :expanded="isOpen(index)"
      @toggle="toggle(index)"
      @delete="handleRemove(index)">
      <template :key="column.prop" v-for="column in visibleColumns(item)">
        <div class="field-inline" v-if="column.type === 'switch'">
          <span>{{ column.label }}</span>
          <el-switch v-model="item[column.prop]" />
        </div>
        <el-radio-group v-else-if="column.type === 'radio'" v-model="item[column.prop]">
          <el-radio-button :key="option.value" :value="option.value" v-for="option in options(column)">{{ option.label }}</el-radio-button>
        </el-radio-group>
        <VariableSelect
          v-else-if="column.type === 'variable'"
          v-model="item[column.prop]"
          :instance="instance"
          :active-item="activeItem"
          :icon="column.icon"
          :writable="column.writable"
          :outer="column.outer"
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
  .el-input + .el-select, .el-select + .el-input, .el-select + .el-select, .el-input + .field-inline, .el-select + .field-inline,
  .el-input + .el-radio-group, .el-select + .el-radio-group,
  .el-radio-group + .el-input, .el-radio-group + .el-select {
    margin-top: 6px;
  }
  /* 二选一的来源开关：整行平分，点按区域够大，也不用先展开下拉再选 */
  .el-radio-group {
    display: flex;
    width: 100%;
    :deep(.el-radio-button) {
      flex: 1;
      .el-radio-button__inner {
        width: 100%;
      }
    }
  }
  .field-inline {
    font-size: 12px;
    color: var(--el-text-color-regular);
    @include flex-between();
  }
  /* 收起时的标题可能较长（如赋值操作的目标变量），超长省略，删除按钮仍贴右 */
  :deep(.collapse-head .title) {
    min-width: 0;
    @include text-wrap();
  }
}
</style>
