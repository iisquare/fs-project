<script setup lang="ts">
import PropertySection from '@/components/Layout/PropertySection.vue';

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

</script>

<template>
  <PropertySection title="校验规则" :reset-key="model.id">
    <el-form-item label="启用校验"><el-switch v-model="model.options.ruleEnabled" /></el-form-item>
    <el-form-item label="必填字段"><el-switch v-model="model.options.required" /></el-form-item>
    <el-form-item label="必填提示"><el-input v-model="model.options.requiredTooltip" /></el-form-item>
  </PropertySection>
</template>

<style lang="scss" scoped>
</style>
