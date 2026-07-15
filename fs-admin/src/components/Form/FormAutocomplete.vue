<script setup lang="ts">
/**
 * 自动完成输入框 - 通过回调函数远程搜索并显示建议列表，选中后绑定 label。
 *
 * @v-model  {String}         选中项的 label 值（双向绑定主值）
 * @prop     {Function}       callback     - 远程搜索回调（必填），签名为 (params) => Promise<{ data: { rows: Row[] } }>
 * @prop     {Boolean}        clearable    - 是否可清空，默认 false
 * @prop     {String}         placeholder  - 占位文本
 * @prop     {String}         fieldKey     - 用作唯一标识的字段名，默认 'id'
 * @prop     {String}         fieldValue   - 用作值的字段名，默认 'id'
 * @prop     {String}         fieldLabel   - 用作标签的字段名，默认 'name'
 * @prop     {*}              exceptIds    - 排除的记录 ID
 * @prop     {Number}         pageSize     - 分页大小，默认 15
 * @prop     {Function}       parameter    - 扩展查询参数函数，签名为 (query: string) => Object
 *
 * 行数据结构 (Row):
 *   { id: any, name: string, ... } — 需包含 fieldKey、fieldValue、fieldLabel 对应的字段
 *
 * @example
 * <form-autocomplete v-model="userName" :callback="UserApi.search" field-label="displayName" placeholder="搜索用户" />
 */
import { ref } from 'vue';

const {
  clearable = false,
  placeholder = '',
  fieldKey = 'id',
  fieldValue = 'id',
  fieldLabel = 'name',
  exceptIds = '',
  pageSize = 15,
  callback,
  parameter = undefined,
} = defineProps({
  multiple: { type: Boolean, required: false },
  clearable: { type: Boolean, required: false },
  placeholder: { type: String, required: false },
  fieldKey: { type: String, required: false },
  fieldValue: { type: String, required: false },
  fieldLabel: { type: String, required: false },
  exceptIds: { required: false },
  pageSize: { type: Number, required: false },
  callback: Function,
  parameter: { type: Function, required: false },
})

const model: any = defineModel()
const loading = ref(false)

const handleCallback = async (params: any) => {
  if (!callback) return []
  loading.value = true
  return await callback(params).then((result: any) => {
    return result.data.rows.map((item: any) => {
      return { key: item[fieldKey], value: item[fieldValue], label: item[fieldLabel], }
    })
  }).finally(() => {
    loading.value = false
  })
}

const handleParameter = (params: any, query: string) => {
  return Object.assign({}, { pageSize, exceptIds }, params, parameter && parameter(query))
}

const handleSuggestion = (query: string, cb: (arg: any) => void) => {
  handleCallback(handleParameter({ [fieldLabel]: query }, query)).then(result => {
    cb(result)
  })
}

</script>

<template>
  <el-autocomplete
    v-model="model"
    :clearable="clearable"
    :placeholder="placeholder"
    value-key="label"
    :fetch-suggestions="handleSuggestion" />
</template>

<style lang="scss" scoped>
</style>
