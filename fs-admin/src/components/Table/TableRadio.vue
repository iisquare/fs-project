<script setup lang="ts">
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
