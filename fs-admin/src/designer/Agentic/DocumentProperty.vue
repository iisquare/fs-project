<script setup lang="ts">
/**
 * 文档提取器节点属性 - 用于将用户上传的文档解析为 LLM 便于理解的文本内容。
 */
import { ref } from 'vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
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
        <el-form-item label="" class="title">解析配置</el-form-item>
        <el-form-item label="文档变量">
          <VariableSelect
            v-model="model.data.input"
            :instance="$props.instance"
            :active-item="model"
            types="File,Array<File>"
            placeholder="请选择文档变量" />
        </el-form-item>
        <el-form-item label="保留图片" class="fs-form-inline">
          <el-switch v-model="model.data.keepImages" />
        </el-form-item>
        <el-form-item label="输出变量名">
          <el-input v-model="model.data.outputName" placeholder="如 text" />
        </el-form-item>
        <el-form-item label="">
          <div class="document-tips">支持 PDF、Word、Excel、PPT 等格式，多文件时按顺序解析并拼接</div>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.document-tips {
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
}
</style>
