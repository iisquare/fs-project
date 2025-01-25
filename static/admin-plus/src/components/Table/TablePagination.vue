<script setup lang="ts">
import RouteUtil from '@/utils/RouteUtil';
import { ref } from 'vue';

const model: any = defineModel()
const emit = defineEmits(['change'])

const lastPageSize = ref(model.value.pageSize)
const handleChange = (currentPage: number, pageSize: number) => {
  if (lastPageSize.value != pageSize) {
    lastPageSize.value = pageSize
    model.value[RouteUtil.paginationPageKey] = 1
  }
  emit('change', { currentPage, pageSize })
}
</script>

<template>
  <el-pagination
    v-model:current-page="model.currentPage"
    v-model:page-size="model.pageSize"
    @change="handleChange"
    v-bind="model" />
</template>

<style lang="scss" scoped>
</style>
