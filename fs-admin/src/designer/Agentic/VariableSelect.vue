<script setup lang="ts">
/**
 * 变量选择器 - 选择画布中上游节点的输出变量或系统变量，支持按类型过滤与手工输入。
 *
 * @v-model  {String}   选中的变量引用，格式为 `节点ID.变量名` 或 `sys.变量名`
 * @prop     {*}        instance   - 画布实例（X6Container 暴露的 flow）
 * @prop     {*}        activeItem - 当前激活的节点，用于排除自身
 * @prop     {String}   types      - 允许的变量类型，多个以英文逗号分隔
 * @prop     {Boolean}  allowCreate - 是否允许输入自定义内容，默认 false
 * @prop     {String}   icon       - Element Plus 图标名称，作为下拉框的前缀图标
 */
import { computed, ref } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import { variableGroups } from './variable'

const model: any = defineModel<string>()
const {
  instance,
  activeItem = {},
  placeholder = '请选择变量',
  clearable = true,
  allowCreate = false,
  types = '',
  icon = '',
} = defineProps<{
  instance?: any,
  activeItem?: any,
  placeholder?: string,
  clearable?: boolean,
  allowCreate?: boolean,
  types?: string,
  icon?: string,
}>()

const version = ref(0)
// 展开下拉时重新收集变量，兼容画布中节点的增删
const groups = computed(() => {
  version.value
  const list = variableGroups(instance, activeItem)
  if (!types) return list
  const allowed = types.split(',')
  return list.map(group => Object.assign({}, group, {
    variables: group.variables.filter((item: any) => allowed.indexOf(item.type) >= 0),
  })).filter(group => group.variables.length > 0)
})

const handleVisible = (visible: boolean) => {
  if (visible) version.value++
}
</script>

<template>
  <el-select
    v-model="model"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="true"
    :allow-create="allowCreate"
    default-first-option
    @visible-change="handleVisible">
    <template #prefix>
      <LayoutIcon v-if="icon" :name="icon" />
    </template>
    <el-option-group :key="group.label" :label="group.label" v-for="group in groups">
      <el-option :key="item.value" :value="item.value" :label="item.label || item.name" v-for="item in group.variables">
        <span class="variable-name">{{ item.label || item.name }}</span>
        <span class="variable-alias" v-if="item.label && item.label !== item.name">{{ item.name }}</span>
        <span class="variable-type">{{ item.type }}</span>
      </el-option>
    </el-option-group>
  </el-select>
</template>

<style lang="scss" scoped>
.variable-alias {
  margin-left: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.variable-type {
  float: right;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
</style>
