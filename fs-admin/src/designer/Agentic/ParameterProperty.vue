<script setup lang="ts">
/**
 * 参数提取器节点属性 - 利用 LLM 从自然语言中推理提取结构化参数。
 */
import { computed, ref } from 'vue'
import FieldSlice from './FieldSlice.vue'
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

const columns = computed(() => [{
  prop: 'name', label: '参数名', placeholder: '参数名，如 language', default: '',
}, {
  prop: 'type', type: 'select', options: 'types', default: 'String', placeholder: '参数类型',
}, {
  prop: 'description', label: '参数说明', placeholder: '参数含义，指导模型提取', default: '',
}, {
  prop: 'required', type: 'switch', label: '是否必填', default: false,
}])
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
        <el-form-item label="输入变量">
          <VariableSelect
            v-model="model.data.query"
            :instance="$props.instance"
            :active-item="model"
            allow-create
            placeholder="请选择待提取的文本" />
        </el-form-item>
        <el-form-item label="记忆" class="fs-form-inline">
          <el-switch v-model="model.data.memory.enabled" />
        </el-form-item>
        <el-form-item label="记忆窗口" v-if="model.data.memory.enabled">
          <el-input-number v-model="model.data.memory.window" :min="1" :max="50" :controls="false" />
        </el-form-item>
        <el-form-item label="提取指令">
          <el-input v-model="model.data.instruction" type="textarea" :rows="4" placeholder="补充说明参数的提取规则" />
        </el-form-item>
        <el-form-item label="" class="title">提取参数</el-form-item>
        <el-form-item label="">
          <FieldSlice v-model="model.data.parameters" :columns="columns" collapsible add-text="添加参数" />
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
