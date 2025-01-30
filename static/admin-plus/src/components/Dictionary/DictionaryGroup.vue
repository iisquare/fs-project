<script setup lang="ts">
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
