<script setup lang="ts">
/**
 * 模型配置 - 模型名称（可输入 + 远程联想）、温度、思考模式、思考强度、多模态输入参数，
 * 大语言模型 / 问题分类器 / 参数提取器共用；节点专有参数（如 AGENT 策略）通过 #extra 插槽插在中间。
 *
 * 布局：每个参数一行——左侧参数名（后三项各带启用开关），右侧取值控件，
 * 开关关闭（默认）时只显示参数名与开关，取值留在数据里不丢失。
 *
 * @v-model {Object} data   - 节点 data 对象，直接读写其中的模型参数字段
 * @prop {*} config     - 全局配置字典（thinkModes、thinkEfforts）
 * @prop {*} instance   - 画布实例（多模态参数的变量选择需要）
 * @prop {*} activeItem - 当前激活节点（多模态参数的变量选择需要）
 * @prop {String} systemPlaceholder - 系统提示词的占位提示（各节点可自定义）
 * @slot extra - 节点专有参数，插在思考参数与多模态参数之间
 */
import { computed } from 'vue'
import ModelApi from '@/api/lm/ModelApi'
import MultimodalField from './MultimodalField.vue'
import ParamRow from './ParamRow.vue'
import SectionSlice from './SectionSlice.vue'
import VariableField from './VariableField.vue'

const data: any = defineModel<any>({ required: true })
const {
  config,
  instance,
  activeItem = {},
  systemPlaceholder = '留空为不增加系统提示词，可插入上游变量',
} = defineProps<{
  config?: any,
  instance?: any,
  activeItem?: any,
  systemPlaceholder?: string,
}>()

/**
 * 思考强度是否可配
 */
const effortEnabled = computed(() => false !== data.value?.thinkEffortEnabled)
</script>

<template>
  <SectionSlice title="模型配置">
    <!-- 模型名称：可直接输入，也从模型库联想（绑定模型名称） -->
    <ParamRow label="模型名称" wide>
      <form-autocomplete v-model="data.model" :callback="ModelApi.list" clearable placeholder="请输入或选择模型名称" />
    </ParamRow>
    <ParamRow label="温度" switchable v-model:enabled="data.temperatureEnabled">
      <el-input-number
        v-if="data.temperatureEnabled"
        v-model="data.temperature"
        :precision="2"
        :step="0.1"
        :min="0"
        :max="2"
        :controls="false" />
    </ParamRow>
    <!-- 思考模式/强度用下拉：选项只有三个，按钮组恢复到默认尺寸后在窄面板里太占宽 -->
    <ParamRow label="思考模式" switchable v-model:enabled="data.thinkModeEnabled">
      <el-select-v2
        v-if="data.thinkModeEnabled"
        v-model="data.thinkMode"
        :options="config?.thinkModes ?? []"
        placeholder="请选择" />
    </ParamRow>
    <ParamRow label="思考强度" switchable v-model:enabled="data.thinkEffortEnabled">
      <el-select-v2
        v-if="effortEnabled"
        v-model="data.thinkEffort"
        :options="config?.thinkEfforts ?? []"
        placeholder="请选择" />
    </ParamRow>
    <!-- 节点专有参数（如调度策略） -->
    <slot name="extra"></slot>
    <!-- 系统提示词：默认收起，展开后可用变量编辑器编写 -->
    <VariableField
      v-model="data.systemPrompt"
      title="系统提示词"
      collapsible
      :instance="instance"
      :active-item="activeItem"
      :height="140"
      :placeholder="systemPlaceholder" />
    <!-- 多模态输入参数随模型配置一起 -->
    <MultimodalField
      v-model="data.multimodal"
      v-model:enabled="data.multimodalEnabled"
      :instance="instance"
      :active-item="activeItem" />
  </SectionSlice>
</template>

<style lang="scss" scoped>
</style>
