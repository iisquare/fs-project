<script setup lang="ts">
/**
 * 记忆配置 - 记忆范围与窗口大小，需要模型的节点共用。
 * 「是否启用记忆」开关直接放在分组标题栏里（与多模态一致）；
 * 范围与窗口始终展示（关闭记忆时也能先设好值），避免分组展开后内部为空。
 * 「工具链」默认关闭，开启后完整对话里历史轮次的工具调用会一并加入上下文。
 *
 * @v-model {Object} data - 节点 data 对象，读写其中的 memory 字段
 */
import { computed } from 'vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'
import SectionSlice from './SectionSlice.vue'

const data: any = defineModel<any>({ required: true })
/** 记忆范围：旧数据没有该字段时按完整对话展示（与后端默认一致） */
const scope = computed<string>(() => data.value?.memory?.scope || 'conversation')
const handleScope = (value: string) => {
  // 兜底：没有任何记忆配置的历史数据（如早期参数提取器）先补一份默认配置
  if (!data.value.memory) data.value.memory = { enabled: true, window: 10, toolchain: false }
  data.value.memory.scope = value
}
</script>

<template>
  <SectionSlice title="记忆">
    <template #actions>
      <el-switch v-model="data.memory.enabled" title="是否启用记忆" />
    </template>
    <el-form-item label="记忆范围" class="fs-form-inline">
      <template #label>
        <span>记忆范围</span>
        <LayoutHelp text="完整对话：带上一轮的用户提问与助手回复（多轮对话需要）；仅用户提问：只带上一轮的用户提问，适合分类器、参数提取器、输出图表这类只关心问题的节点" />
      </template>
      <el-select :model-value="scope" class="memory-scope" @change="handleScope">
        <el-option label="完整对话" value="conversation" />
        <el-option label="仅用户提问" value="user" />
      </el-select>
    </el-form-item>
    <el-form-item label="记忆窗口大小">
      <el-input-number v-model="data.memory.window" :min="1" :max="50" :controls="false" />
    </el-form-item>
    <el-form-item label="工具链" class="fs-form-inline">
      <template #label>
        <span>工具链</span>
        <LayoutHelp text="仅「完整对话」有效：开启后历史轮次里的工具调用（参数与返回结果）会一并加入上下文，模型可复用此前的工具执行过程" />
      </template>
      <el-switch v-model="data.memory.toolchain" title="历史对话轮次中的工具调用是否加入上下文" />
    </el-form-item>
  </SectionSlice>
</template>

<style lang="scss" scoped>
/**
 * 记忆范围下拉框：行内布局的取值区按内容收缩，下拉框默认的 100% 宽在这种收缩容器里
 * 会被压到只剩后缀箭头（选中项文字看不见），因此显式给宽度；
 * 与下方「记忆窗口大小」的数值框同宽，两条取值右边缘对齐。
 */
.memory-scope {
  width: 150px;
}
</style>
