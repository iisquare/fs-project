<script setup lang="ts">
import { computed, ref } from 'vue';
import FlexFormItem from './FlexFormItem.vue';
import type { FormInstance } from 'element-plus';

const model: any = defineModel()
const {
  config = {} as any,
  frame = {} as any,
  authority = {} as any,
} = defineProps<{
  config: { type: null, required: false },
  frame: { type: null, required: true },
  authority: any,
}>()

const formLayout = computed(() => {
  return config.exhibition.formLayout(frame.content)
})

const rules = computed(() => {
  return config.validator.generate(frame.content.widgets, authority)
})

const formRef: any = ref<FormInstance>()

const validate = (callback: Function) => {
  formRef.value?.validate((valid: boolean) => callback(valid))
}

defineExpose({ validate })
</script>

<template>
  <el-form ref="formRef" :model="model" :rules="rules" v-bind="formLayout">
    <FlexFormItem v-model="model" :config="config" :widgets="frame.content.widgets" :authority="authority" />
    <el-empty description="无可用组件" v-if="frame.content.widgets.length === 0" />
  </el-form>
</template>

<style lang="scss" scoped>
@import url('./design.scss');
</style>
