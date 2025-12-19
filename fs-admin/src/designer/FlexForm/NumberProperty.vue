<script setup lang="ts">
import { ref } from 'vue';
import NodeSlice from './NodeSlice.vue';
import NumberRule from './NumberRule.vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const controlsPositions = [{ value: '', label: '两侧' }, { value: 'right', label: '右侧' }]
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="组件属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
        <el-form-item label="名称"><el-input v-model="model.label" /></el-form-item>
        <el-form-item label="字段"><el-input v-model="model.options.field" placeholder="必填" /></el-form-item>
        <el-form-item label="默认值"><el-input-number v-model="model.options.value" :controls="false" /></el-form-item>
        <el-form-item label="占位符"><el-input v-model="model.options.placeholder" /></el-form-item>
        <el-form-item label="控制按钮"><el-switch v-model="model.options.controls" /></el-form-item>
        <el-form-item label="按钮位置">
          <el-radio-group v-model="model.options.controlsPosition">
            <el-radio-button :label="item.label" :value="item.value" v-for="item in controlsPositions" :key="item.value" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="前缀"><el-input v-model="model.options.prefix" /></el-form-item>
        <el-form-item label="后缀"><el-input v-model="model.options.suffix" /></el-form-item>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="校验规则" name="rule">
      <el-form :model="model">
        <NumberRule v-model="model" :instance="instance" :config="config" :tips="tips" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
