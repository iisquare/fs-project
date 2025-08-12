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
  <el-form-item label="必填字段"><el-switch v-model="model.options.required" /></el-form-item>
  <el-form-item label="必填提示"><el-input v-model="model.options.requiredTooltip" /></el-form-item>
  <el-form-item label="最小长度"><el-input-number v-model="model.options.minLength" :controls="false" :min="0" /></el-form-item>
  <el-form-item label="最小提示"><el-input v-model="model.options.minTooltip" /></el-form-item>
  <el-form-item label="最大长度"><el-input-number v-model="model.options.maxLength" :controls="false" /></el-form-item>
  <el-form-item label="最大提示"><el-input v-model="model.options.maxTooltip" /></el-form-item>
  <RegularSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
</template>

<style lang="scss" scoped>
</style>
