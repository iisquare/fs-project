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
 * @prop     {String}         fieldLabel   - 标签来源字段，默认 'name'，同时作为远程搜索的关键词字段
 * @prop     {String|Array}   labelFields  - 标签来源字段（多个），默认空；支持逗号分割（如 'name,alias'）或字段数组（如 ['name', 'alias']）
 * @prop     {Function}       labelFormatter - 标签内容格式化函数，签名为 (row, index) => String|Array
 * @prop     {*}              exceptIds    - 排除的记录 ID
 * @prop     {Number}         pageSize     - 分页大小，默认 15
 * @prop     {Function}       parameter    - 扩展查询参数函数，签名为 (query: string) => Object
 *
 * 标签来源优先级（互斥）：labelFormatter > labelFields > fieldLabel
 *   - 设置 labelFormatter 时，仅以其返回值作为标签，返回数组时逐个作为标签值
 *   - 未设置 labelFormatter 且 labelFields 非空时，仅取 labelFields 对应字段的值
 *   - 否则取 fieldLabel 对应字段的值
 * 无论以哪种方式取得标签值，均会过滤空值并按标签值去重；远程搜索关键词始终取 fieldLabel
 *
 * 行数据结构 (Row):
 *   { id: any, name: string, ... } — 需包含 fieldKey、fieldValue 及标签来源字段
 *
 * @example
 * <form-autocomplete v-model="userName" :callback="UserApi.search" field-label="name" placeholder="搜索用户" />
 * <form-autocomplete v-model="modelName" :callback="ModelApi.list" label-fields="name,alias" placeholder="请输入模型名称" />
 * <form-autocomplete v-model="modelName" :callback="ModelApi.list" :label-fields="['name', 'alias']" placeholder="请输入模型名称" />
 * <form-autocomplete v-model="modelName" :callback="ModelApi.list" :labelFormatter="(row, index) => row.alias ? row.name + '(' + row.alias + ')' : row.name" />
 * <form-autocomplete v-model="modelName" :callback="ModelApi.list" :labelFormatter="(row, index) => [row.name, row.alias]" />
 */
import DataUtil from '@/utils/DataUtil';
import { computed, ref } from 'vue';

const {
  clearable = false,
  placeholder = '',
  fieldKey = 'id',
  fieldValue = 'id',
  fieldLabel = 'name',
  labelFields = '',
  labelFormatter = undefined,
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
  labelFields: { type: [String, Array], required: false },
  labelFormatter: { type: Function, required: false },
  exceptIds: { required: false },
  pageSize: { type: Number, required: false },
  callback: Function,
  parameter: { type: Function, required: false },
})

const model: any = defineModel()
const loading = ref(false)

// 备选标签字段：支持逗号分割的多个字段或字段数组，忽略空值
const labelFieldList = computed(() => {
  const fields: any[] = Array.isArray(labelFields) ? labelFields : String(labelFields).split(',')
  return fields.map((field: any) => String(field).trim()).filter((field: string) => !DataUtil.empty(field))
})

// 逐级读取字段值，兼容 a.b 形式的嵌套字段，中间节点缺失时返回 undefined
const pick = (item: any, field: string): any => {
  let value = item
  for (const key of field.split('.')) {
    if (value === null || value === undefined) return undefined
    value = value[key]
  }
  return value
}

// 标签内容：优先级 labelFormatter > labelFields > fieldLabel，三者互斥，空值忽略
const labels = (item: any, index: number): any[] => {
  const result = labelFormatter ? labelFormatter(item, index)
    : labelFieldList.value.length > 0 ? labelFieldList.value.map((field: string) => pick(item, field))
      : pick(item, fieldLabel)
  return (Array.isArray(result) ? result : [result]).filter((value: any) => !DataUtil.empty(value))
}

const handleCallback = async (params: any) => {
  if (!callback) return []
  loading.value = true
  return await callback(params).then((result: any) => {
    const rows: any[] = result.data.rows || []
    const options: any[] = []
    const known = new Set<string>()
    rows.forEach((item: any, index: number) => {
      labels(item, index).forEach((label: any) => {
        if (DataUtil.empty(label) || known.has(String(label))) return
        known.add(String(label))
        options.push({ key: item[fieldKey], value: item[fieldValue], label })
      })
    })
    return options
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
