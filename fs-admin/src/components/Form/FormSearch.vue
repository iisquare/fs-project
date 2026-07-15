<script setup lang="ts">
/**
 * 搜索表单容器 - 基于 el-form + el-row 的表单布局组件，内置表单校验和重置方法。
 *
 * @v-model  {Object}               表单数据对象（双向绑定主值）
 *
 * @expose   {Function} validate     - 表单校验，同 el-form.validate
 * @expose   {Function} resetFields  - 重置表单，同 el-form.resetFields
 *
 * @example
 * <form-search ref="formRef" v-model="searchParams">
 *   <form-search-item label="名称" prop="name">
 *     <el-input v-model="searchParams.name" />
 *   </form-search-item>
 *   <form-search-item label="状态" prop="status">
 *     <dictionary-select v-model="searchParams.status" dictionary="STATUS" />
 *   </form-search-item>
 * </form-search>
 */
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
