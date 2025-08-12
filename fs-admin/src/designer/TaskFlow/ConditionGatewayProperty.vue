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
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
        <el-form-item label="" class="title">参数配置</el-form-item>
        <el-form-item label="分支条件"><el-input type="textarea" v-model="model.data.condition" /></el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
