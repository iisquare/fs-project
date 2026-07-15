<script setup lang="ts">
/**
 * 表格分页组件 - 基于 el-pagination 封装，v-bind 传递分页参数，页大小改变时自动重置到第1页。
 *
 * @v-model  {PaginationState}  分页状态对象（双向绑定主值）
 *
 * @emits {Function} change - 分页变动时触发，参数：({ currentPage: number, pageSize: number })
 *
 * 分页状态结构 (PaginationState):
 *   { currentPage: number, pageSize: number, total?: number, ...el-pagination 所有 props }
 *
 * @example
 * <table-pagination v-model="pagination" @change="fetchData" />
 */
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
