<script setup lang="ts">
/**
 * 删除状态筛选 - 三个单选按钮组，用于筛选"全部"/"已删除"/"未删除"记录。
 *
 * @v-model  {String}   筛选值：''=全部，'only'=仅已删除，'without'=仅未删除
 *
 * @example
 * const filters = ref(RouteUtil.query2filter(route, { deleted: 'without' }))
 * <form-deleted v-model="filters.deleted" @change="handleRefresh(true, false)" />
 */
const model: any = defineModel()

</script>

<template>
  <el-radio-group v-model="model">
    <el-radio-button label="全部" value="" />
    <el-radio-button label="已删除" value="only" />
    <el-radio-button label="未删除" value="without" />
  </el-radio-group>
</template>

<style lang="scss" scoped>
</style>
