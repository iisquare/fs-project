<script setup lang="ts">
import { ref } from 'vue';
import TextRule from './TextRule.vue';
import NodeSlice from './NodeSlice.vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="组件属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
        <el-form-item label="名称"><el-input v-model="model.label" /></el-form-item>
        <el-form-item label="字段"><el-input v-model="model.options.field" placeholder="必填" /></el-form-item>
        <el-form-item label="默认值"><el-input v-model="model.options.value" /></el-form-item>
        <el-form-item label="占位符"><el-input v-model="model.options.placeholder" /></el-form-item>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="校验规则" name="rule">
      <el-form :model="model">
        <TextRule v-model="model" :instance="instance" :config="config" :tips="tips" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
