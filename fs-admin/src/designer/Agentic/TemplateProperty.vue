<script setup lang="ts">
/**
 * 模板转换节点属性 - 使用 Jinja 模板语法将数据转换为字符串。
 */
import { ref } from 'vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import PromptEditor from './PromptEditor.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">模板配置</el-form-item>
        <el-form-item label="模板内容">
          <PromptEditor
            v-model="model.data.template"
            :instance="$props.instance"
            :active-item="model"
            :rows="8"
            placeholder="请输入 Jinja 模板，如 {{ name }} 或 {% for item in items %}" />
        </el-form-item>
        <el-form-item label="" class="title">输出配置</el-form-item>
        <el-form-item label="输出变量名">
          <el-input v-model="model.data.outputName" placeholder="如 output" />
        </el-form-item>
        <el-form-item label="输出类型">
          <el-select v-model="model.data.outputType" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.types" />
          </el-select>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
