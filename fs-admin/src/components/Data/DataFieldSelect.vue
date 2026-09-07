<script setup lang="ts">
/**
 * 字段选择器 - 支持 DataSchemaTable 与 DataFieldMapping 两种字段格式，统一以 name 字段为准。
 *
 * @v-model  {String|String[]}  选中的字段名称（双向绑定主值），multiple 为 false 时为字符串，为 true 时为数组
 * @prop     {Boolean}          multiple  - 是否支持多选，默认 false
 * @prop     {String}           placeholder - 占位提示，默认“选择或输入字段名称”
 * @prop     {FieldRow[] | SchemaNode[]} fields   - 字段列表，通过 v-model:fields 传入
 * @prop     {Boolean}          editable  - 是否可编辑，默认 false，通过 v-model:editable 传入
 *
 * 字段格式:
 *   DataSchemaTable 行 - { name, title, type, comment }
 *   DataFieldMapping 节点 - { path, field, name, title, type, comment, checked, children }
 *   DataFieldMapping 仅展示已勾选（checked）的叶子字段，DataSchemaTable 展示全部字段。
 *
 * @example
 * <data-field-select v-model="field" v-model:fields="fields" v-model:editable="true" />
 * <data-field-select v-model="fields" v-model:fields="allFields" v-model:editable="true" multiple />
 */
import { computed, watch } from 'vue';

const props = defineProps<{
  multiple?: boolean,
  placeholder?: string,
}>()
const model: any = defineModel()
const fields = defineModel('fields', { default: () => [] })
const editable = defineModel('editable', { type: Boolean, default: false })

const selectValue = computed({
  get() {
    return model.value
  },
  set(value: any) {
    model.value = value
  },
})

const fieldName = (item: any) => (item && typeof item === 'object') ? item.name : item
const displayValues = computed(() => {
  if (props.multiple) {
    return Array.isArray(model.value) ? model.value : []
  }
  return model.value ? [model.value] : []
})
watch(model, (val: any) => {
  if (!editable.value) return
  if (props.multiple) {
    if (!Array.isArray(val)) {
      model.value = []
      return
    }
    const names = val.map(fieldName)
    if (names.some((name, index) => name !== val[index])) {
      model.value = names
    }
  } else if (val && typeof val === 'object') {
    model.value = fieldName(val)
  }
}, { immediate: true })
const options = computed(() => {
  const result: any[] = []
  const walk = (items: any) => {
    items && items.forEach((item: any) => {
      if (item.children && item.children.length) {
        walk(item.children)
      } else if (item.checked === undefined || item.checked) {
        result.push(item)
      }
    })
  }
  walk(fields.value)
  return result
})
</script>

<template>
  <el-select
    v-if="editable"
    v-model="selectValue"
    :multiple="multiple"
    filterable
    allow-create
    :reserve-keyword="false"
    default-first-option
    :placeholder="placeholder || '选择或输入字段名称'"
  >
    <el-option v-for="item in options" :key="item.name" :label="item.name" :value="item.name" />
  </el-select>
  <el-space v-else>
    <el-tag v-for="item in displayValues" :key="fieldName(item)">{{ fieldName(item) }}</el-tag>
  </el-space>
</template>

<style lang="scss" scoped>
</style>
