<script setup lang="ts">
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

</script>

<template>
  <el-form-item label="启用校验"><el-switch v-model="model.options.ruleEnabled" /></el-form-item>
  <el-form-item label="最少应填"><el-input-number v-model="model.options.minLength" :controls="false" :min="0" /></el-form-item>
  <el-form-item label="最少提示"><el-input v-model="model.options.minTooltip" /></el-form-item>
  <el-form-item label="最多可填"><el-input-number v-model="model.options.maxLength" :controls="false" :min="0" /></el-form-item>
  <el-form-item label="最多提示"><el-input v-model="model.options.maxTooltip" /></el-form-item>
</template>

<style lang="scss" scoped>
</style>
