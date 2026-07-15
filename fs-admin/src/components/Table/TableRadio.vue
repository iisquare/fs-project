<script setup lang="ts">
/**
 * 表格单选列 - 在 el-table 中添加一列 radio 单选按钮，通过函数从行数据中提取选中值。
 *
 * @v-model  {*}        当前选中的值（双向绑定主值）
 * @prop     {Function}  value - 从行数据中提取值的函数（必填），签名为 (scope: any) => any
 *
 * @example
 * <el-table :data="list">
 *   <table-radio v-model="selectedId" :value="(scope) => scope.row.id" />
 *   <el-table-column prop="name" label="名称" />
 * </el-table>
 */
const model: any = defineModel()
const {
  value = (scope: any) => {},
} = defineProps({
  value: { type: Function, required: true },
})
</script>

<template>
  <el-table-column label="">
    <template #default="scope">
      <div class="flex-center">
        <el-radio v-model="model" :value="value(scope)"></el-radio>
      </div>
    </template>
  </el-table-column>
</template>

<style lang="scss" scoped>
:deep(.el-radio__label) {
  padding-left: 0;
}
</style>
