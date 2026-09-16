<script setup lang="ts">
/**
 * 大语言模型节点属性 - 调用大语言模型回答问题或对自然语言进行处理。
 */
import { ref } from 'vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import PromptEditor from './PromptEditor.vue'
import VariableSelect from './VariableSelect.vue'

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
        <el-form-item label="" class="title">模型配置</el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="model.data.model" clearable placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="生成多样性">
          <el-input-number v-model="model.data.temperature" :precision="2" :step="0.1" :min="0" :max="2" :controls="false" />
        </el-form-item>
        <el-form-item label="最大令牌数">
          <el-input-number v-model="model.data.maxTokens" :min="0" :controls="false" placeholder="0 表示系统默认" />
        </el-form-item>
        <el-form-item label="Top P">
          <el-input-number v-model="model.data.topP" :precision="2" :step="0.1" :min="0" :max="1" :controls="false" />
        </el-form-item>
        <el-form-item label="" class="title">提示词</el-form-item>
        <el-form-item label="系统提示词">
          <el-input v-model="model.data.systemPrompt" type="textarea" :rows="3" placeholder="留空为不增加系统提示词" />
        </el-form-item>
        <el-form-item label="用户提示词">
          <PromptEditor
            v-model="model.data.prompt"
            :instance="$props.instance"
            :active-item="model"
            :rows="6"
            placeholder="请输入提示词，可插入上游变量" />
        </el-form-item>
        <el-form-item label="" class="title">上下文与记忆</el-form-item>
        <el-form-item label="引用上下文" class="fs-form-inline">
          <el-switch v-model="model.data.context.enabled" />
        </el-form-item>
        <el-form-item label="上下文变量" v-if="model.data.context.enabled">
          <VariableSelect
            v-model="model.data.context.variable"
            :instance="$props.instance"
            :active-item="model"
            types="String,Array<Object>" />
        </el-form-item>
        <el-form-item label="记忆" class="fs-form-inline">
          <el-switch v-model="model.data.memory.enabled" />
        </el-form-item>
        <el-form-item label="记忆窗口" v-if="model.data.memory.enabled">
          <el-input-number v-model="model.data.memory.window" :min="1" :max="50" :controls="false" />
        </el-form-item>
        <el-form-item label="视觉" class="fs-form-inline">
          <el-switch v-model="model.data.vision.enabled" />
        </el-form-item>
        <el-form-item label="图片变量" v-if="model.data.vision.enabled">
          <VariableSelect
            v-model="model.data.vision.variable"
            :instance="$props.instance"
            :active-item="model"
            types="File,Array<File>,String" />
        </el-form-item>
        <el-form-item label="" class="title">结构化输出</el-form-item>
        <el-form-item label="启用结构化输出" class="fs-form-inline">
          <el-switch v-model="model.data.structured.enabled" />
        </el-form-item>
        <el-form-item label="输出结构" v-if="model.data.structured.enabled">
          <el-input v-model="model.data.structured.schema" type="textarea" :rows="5" placeholder="请输入 JSON Schema" />
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
