<script setup lang="ts">
import { ref } from 'vue';
import NodeSlice from './NodeSlice.vue';
import SelectorSlice from './SelectorSlice.vue';
import CheckboxRule from './CheckboxRule.vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const displays = [{ value: 'inline', label: '行内' }, { value: 'block', label: '块级' }]
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="组件属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
        <el-form-item label="名称"><el-input v-model="model.label" /></el-form-item>
        <el-form-item label="字段"><el-input v-model="model.options.field" placeholder="必填" /></el-form-item>
        <el-form-item label="默认值"><el-switch v-model="model.options.value" /></el-form-item>
        <el-form-item label="打开文案"><el-input v-model="model.options.active" placeholder="状态为 on 时的文字描述" /></el-form-item>
        <el-form-item label="关闭文案"><el-input v-model="model.options.inactive" placeholder="状态为 off 时的文字描述" /></el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
