<script setup lang="ts">
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
  fieldKey: { type: String, required: false }, // 唯一标识
  fieldValue: { type: String, required: false }, // 字段值
  fieldLabel: { type: String, required: false }, // 字段标签
  exceptIds: { required: false }, // 排除记录
  pageSize: { type: Number, required: false }, // 分页大小
  callback: Function,
  parameter: { type: Function, required: false }, // 拓展查询参数
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
