<script setup lang="ts">
import { ref } from 'vue';
import NodeSlice from './NodeSlice.vue';
import SelectorSlice from './SelectorSlice.vue';
import SelectRule from './SelectRule.vue';

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
        <el-form-item label="默认值">
          <el-select v-model="model.options.value" :multiple="model.options.multiple" clearable filterable allow-create :reserve-keyword="false">
            <el-option v-for="(item, index) in model.options.items" :key="index" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否多选"><el-switch v-model="model.options.multiple" /></el-form-item>
        <el-form-item label="可否清空"><el-switch v-model="model.options.clearable" /></el-form-item>
        <el-form-item label="可否筛选"><el-switch v-model="model.options.filterable" /></el-form-item>
        <el-form-item label="允许创建"><el-switch v-model="model.options.allowCreate" /></el-form-item>
        <el-form-item label="保留搜索"><el-switch v-model="model.options.reserveKeyword" /></el-form-item>
        <SelectorSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="校验规则" name="rule">
      <el-form :model="model">
        <SelectRule v-model="model" :instance="instance" :config="config" :tips="tips" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
