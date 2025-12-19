<script setup lang="ts">
import { ref } from 'vue';
import NodeSlice from './NodeSlice.vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const directions = [
  { value: 'horizontal', label: '水平' },
  { value: 'vertical', label: '垂直' },
]
const positions = [
  { value: 'left', label: '居左' },
  { value: 'center', label: '居中' },
  { value: 'right', label: '居右' },
]
const borderStyles = [
  { value: 'none', label: '无' },
  { value: 'hidden', label: '隐藏' },
  { value: 'dotted', label: '圆点' },
  { value: 'dashed', label: '虚线' },
  { value: 'solid', label: '实线' },
  { value: 'double', label: '双实线' },
  { value: 'groove', label: '雕刻' },
  { value: 'ridge', label: '浮雕' },
  { value: 'inset', label: '陷入' },
  { value: 'outset', label: '突出' },
]
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="组件属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
        <el-form-item label="名称"><el-input v-model="model.label" /></el-form-item>
        <el-form-item label="方向">
          <el-radio-group v-model="model.options.direction">
            <el-radio-button :label="item.label" :value="item.value" v-for="item in directions" :key="item.value" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="位置">
          <el-radio-group v-model="model.options.position">
            <el-radio-button :label="item.label" :value="item.value" v-for="item in positions" :key="item.value" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="边框">
          <el-select v-model="model.options.border" placeholder="请选择">
            <el-option v-for="item in borderStyles" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
