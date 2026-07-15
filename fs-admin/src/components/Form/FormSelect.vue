<script setup lang="ts">
/**
 * 远程搜索下拉选择器 - 通过回调函数远程搜索并展示下拉选项，支持单选/多选，自动回显已选值。
 *
 * @v-model  {*}                选中值（双向绑定主值），多选时为数组
 * @prop     {Function}         callback     - 远程搜索回调（必填），签名为 (params) => Promise<{ data: { rows: Row[] } }>
 * @prop     {Row[]}            data         - 最后一次检索结果，通过 v-model:data 传入
 * @prop     {Row|Row[]}        selected     - 选中项对应的完整行数据，通过 v-model:selected 传入
 * @prop     {Boolean}          multiple     - 是否多选，默认 false
 * @prop     {Boolean}          clearable    - 是否可清空，默认 false
 * @prop     {String}           placeholder  - 占位文本，默认"输入关键词进行查找"
 * @prop     {String}           fieldKey     - 用作唯一标识的字段名，默认 'id'
 * @prop     {String}           fieldValue   - 用作值的字段名，默认 'id'
 * @prop     {String}           fieldLabel   - 用作标签的字段名，默认 'name'
 * @prop     {*}                exceptIds    - 排除的记录 ID
 * @prop     {Number}           pageSize     - 分页大小，默认 15
 * @prop     {Function}         parameter    - 扩展查询参数函数，签名为 (query: string) => Object
 *
 * @emits    {Function} change - 选中变化，参数：(value, selected, data)
 *                                 value    - 选中值
 *                                 selected - 选中值对应的完整行数据
 *                                 data     - 最后一次检索结果数组
 *
 * 行数据结构 (Row):
 *   { id: any, name: string, ... } — 需包含 fieldKey、fieldValue、fieldLabel 对应的字段
 *
 * @example
 * <form-select
 *   v-model="userId"
 *   v-model:selected="selectedUser"
 *   :callback="UserApi.search"
 *   placeholder="搜索用户"
 * />
 */
import DataUtil from '@/utils/DataUtil';
import { ref, watch } from 'vue';

const {
  multiple = false,
  clearable = false,
  placeholder = '输入关键词进行查找',
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
const data = defineModel<Object[]>('data', { default: () => [] })
const selected = defineModel('selected', { type: [Object, Array<Object>], default: null })
const emit = defineEmits(['change'])
const options: any = ref([])
const loading = ref(false)

const handleCallback = async (params: any) => {
  if (!callback) return
  loading.value = true
  options.value = await callback(params).then((result: any) => {
    data.value = result.data.rows
    if (multiple) {
      // 过滤后最后一次检索结果可能不包含已选中内容
      const map = DataUtil.array2map(result.data.rows.concat(selected.value || []), fieldValue)
      selected.value = model.value?.map((v: any) => map[v])
    } else {
      const map = DataUtil.array2map(result.data.rows, fieldValue)
      selected.value = map[model.value]
    }
    return result.data.rows.map((item: any) => {
      return { key: item[fieldKey], value: item[fieldValue], label: item[fieldLabel], }
    })
  }).catch(() => []).finally(() => {
    loading.value = false
  })
}

const handleParameter = (params: any, query: string) => {
  return Object.assign({}, { pageSize, exceptIds }, params, parameter && parameter(query))
}

watch(model, (value, oldValue) => {
  if (value === oldValue || DataUtil.empty(value)) return
  const size = Math.max(pageSize, DataUtil.isArray(model.value) ? model.value.length : 1)
  handleCallback(handleParameter({ [fieldValue]: model.value, pageSize: size }, ''))
}, { immediate: true })

const remoteMethod = (query: string) => {
  handleCallback(handleParameter({ [fieldLabel]: query }, query))
}

const handleChange = (value: any) => {
  if (DataUtil.isArray(value)) {
    // 过滤后最后一次检索结果可能不包含已选中内容
    const map = DataUtil.array2map(data.value.concat(selected.value || []), fieldValue)
    selected.value = value.map((v: any) => map[v])
  } else {
    const map = DataUtil.array2map(data.value, fieldValue)
    selected.value = map[value]
  }
  emit('change', value, selected.value, data.value)
}
</script>

<template>
  <el-select
    v-model="model"
    :multiple="multiple"
    filterable
    remote
    :clearable="clearable"
    :placeholder="placeholder"
    remote-show-suffix
    :remote-method="remoteMethod"
    :loading="loading"
    @change="handleChange"
  >
    <el-option v-for="item in options" :key="item.key" :label="item.label" :value="item.value" />
  </el-select>
</template>

<style lang="scss" scoped>
</style>
