<script setup lang="ts">
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
  fieldKey: { type: String, required: false }, // 唯一标识
  fieldValue: { type: String, required: false }, // 字段值
  fieldLabel: { type: String, required: false }, // 字段标签
  exceptIds: { required: false }, // 排除记录
  pageSize: { type: Number, required: false }, // 分页大小
  callback: Function,
  parameter: { type: Function, required: false }, // 拓展查询参数
})

const model: any = defineModel()
const data = defineModel('data', { type: Array<Object>, default: [] }) // 最后一次检索结果
const selected = defineModel('selected', { type: [Object, Array<Object>], default: null }) // 选中项对应的结果条目
const emit = defineEmits(['change']) // 参数：选中值，选中值对应的结果条目，最后一次检索结果数据
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
