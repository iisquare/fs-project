<script setup lang="ts">
import DictionaryApi from '@/api/member/DictionaryApi';
import { onMounted, ref } from 'vue';
import type { CascaderProps } from 'element-plus'

const {
  multiple = false,
  clearable = false,
  filterable = false,
  dictionary = '',
  placeholder = '请选择',
  showAllLevels = true,
  checkStrictly = false,
  lazy = false,
} = defineProps({
  multiple: { type: Boolean, required: false },
  clearable: { type: Boolean, required: false },
  filterable: { type: Boolean, required: false },
  dictionary: { type: String, required: true },
  placeholder: { type: String, required: false },
  showAllLevels: { type: Boolean, required: false },
  checkStrictly: { type: Boolean, required: false },
  lazy: { type: Boolean, required: false },
})

const model: any = defineModel()
const level = defineModel('level', { type: Number, default: 0 })
const loading = ref(false)
const options: any = ref([])
const props: CascaderProps = {
  multiple,
  checkStrictly,
  lazy,
  lazyLoad(node, resolve) {
    const params = {
      dictionary,
      parentId: node.data?.id ?? 0,
    }
    DictionaryApi.options(params).then((result: any) => {
      resolve(result.data.map((item: any) => {
        item.leaf = item.leaf || (level.value > 0 && node.level >= level.value - 1)
        return item
      }))
    }).catch(() => {})
  },
}

const reload = () => {
  const params = {
    dictionary
  }
  if (lazy) { // 仅加载已选项
    // 组件自身会自动加载相应层级，无需手动处理或后端兼容
  } else { // 加载全部字典项
    loading.value = true
    DictionaryApi.options(params).then((result: any) => {
      options.value = result.data
      loading.value = false
    }).catch(() => {})
  }
}

onMounted(async () => {
  reload()
})
</script>

<template>
  <el-cascader
    v-model="model"
    :options="options"
    :props="props"
    :clearable="clearable"
    :filterable="filterable"
    :placeholder="placeholder"
    :show-all-levels="showAllLevels"
  />
</template>

<style lang="scss" scoped>
</style>
