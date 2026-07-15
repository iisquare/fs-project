<script setup lang="ts">
/**
 * 字典分组下拉框 - 基于字典数据源的 select 组件，自动渲染分组选项（支持 el-option-group）。
 *
 * @v-model  {*}        选中值（双向绑定主值），多选时为数组
 * @prop     {String}    dictionary    - 字典标识（必填）
 * @prop     {Boolean}   multiple      - 是否多选，默认 false
 * @prop     {Boolean}   clearable     - 是否可清空，默认 false
 * @prop     {Boolean}   filterable    - 是否可搜索，默认 false
 * @prop     {Boolean}   allowCreate   - 是否允许创建新条目，默认 false
 * @prop     {String}    placeholder   - 占位文本，默认"请选择"
 *
 * @example
 * <dictionary-group v-model="value" dictionary="PRODUCT_CATEGORY" />
 */
import DictionaryApi from '@/api/member/DictionaryApi';
import { onMounted, ref } from 'vue';

const {
  multiple = false,
  clearable = false,
  filterable = false,
  allowCreate = false,
  dictionary = '',
  placeholder = '请选择',
} = defineProps({
  multiple: { type: Boolean, required: false },
  clearable: { type: Boolean, required: false },
  filterable: { type: Boolean, required: false },
  allowCreate: { type: Boolean, required: false },
  dictionary: { type: String, required: true },
  placeholder: { type: String, required: false },
})

const model: any = defineModel()
const loading = ref(false)
const options: any = ref([])

const reload = () => {
  loading.value = true
  const params = {
    dictionary
  }
  DictionaryApi.options(params).then((result: any) => {
    options.value = result.data
    loading.value = false
  }).catch(() => {})
}

onMounted(async () => {
  reload()
})
</script>

<template>
  <el-select
    v-model="model"
    :multiple="multiple"
    :clearable="clearable"
    :filterable="filterable"
    :allow-create="allowCreate"
    :placeholder="placeholder"
    :loading="loading"
  >
    <DictionaryGroupItem :options="options" />
  </el-select>
</template>

<style lang="scss" scoped>
</style>
