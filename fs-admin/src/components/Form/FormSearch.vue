<script setup lang="ts">
import type { FormInstance, FormItemProp, FormValidateCallback } from 'element-plus';
import type { Arrayable } from 'element-plus/es/utils/typescript.mjs';
import { ref } from 'vue';

const formRef = ref<FormInstance>()
const model: any = defineModel()

const validate = (callback?: FormValidateCallback) => {
  return formRef.value?.validate(callback)
}

const resetFields = (props?: Arrayable<FormItemProp> | undefined) => {
  return formRef.value?.resetFields()
}

defineExpose({ validate, resetFields })
</script>

<template>
  <el-form ref="formRef" :model="model">
    <el-row :gutter="24">
      <slot></slot>
    </el-row>
  </el-form>
</template>

<style lang="scss" scoped>
.el-row {
  row-gap: 12px;
}
</style>
