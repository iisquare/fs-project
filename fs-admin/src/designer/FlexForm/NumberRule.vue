<script setup lang="ts">
import RegularSlice from './RegularSlice.vue';


const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

</script>

<template>
  <el-form-item label="启用校验"><el-switch v-model="model.options.ruleEnabled" /></el-form-item>
  <el-form-item label="最小校验"><el-switch v-model="model.options.minEnabled" /></el-form-item>
  <el-form-item label="最小值"><el-input-number v-model="model.options.min" :controls="false" /></el-form-item>
  <el-form-item label="最小提示"><el-input v-model="model.options.minTooltip" /></el-form-item>
  <el-form-item label="最大校验"><el-switch v-model="model.options.maxEnabled" /></el-form-item>
  <el-form-item label="最大值"><el-input-number v-model="model.options.max" :controls="false" /></el-form-item>
  <el-form-item label="最大提示"><el-input v-model="model.options.maxTooltip" /></el-form-item>
  <RegularSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
</template>

<style lang="scss" scoped>
</style>
