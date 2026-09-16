<script setup lang="ts">
/**
 * 迭代节点属性 - 对列表对象执行多次步骤直至输出所有结果，子节点拖入容器内部。
 */
import { ref } from 'vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import VariableSelect from './VariableSelect.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

/**
 * 容器内已不再自动生成入口节点，内部为空时给出提示
 */
const emptyBody = () => {
  const cell: any = props.instance?.flow?.graph?.getCellById?.(model.value?.id)
  return Boolean(cell) && !(cell.getChildren?.() ?? []).length
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="">
          <el-alert
            v-if="emptyBody()"
            type="warning"
            :closable="false"
            show-icon
            title="容器内暂无节点，把需要重复执行的节点拖入容器内部即可" />
        </el-form-item>
        <el-form-item label="" class="title">迭代配置</el-form-item>
        <el-form-item label="迭代输入">
          <VariableSelect
            v-model="model.data.input"
            :instance="$props.instance"
            :active-item="model"
            types="Array<Object>,Array<String>,Array<File>"
            placeholder="请选择列表变量" />
        </el-form-item>
        <el-form-item label="元素变量名">
          <el-input v-model="model.data.itemName" placeholder="如 item，供容器内节点引用" />
        </el-form-item>
        <el-form-item label="索引变量名">
          <el-input v-model="model.data.indexName" placeholder="如 index，从 0 开始" />
        </el-form-item>
        <el-form-item label="输出变量名">
          <el-input v-model="model.data.outputName" placeholder="如 output，收集各次结果" />
        </el-form-item>
        <el-form-item label="最大迭代次数">
          <el-input-number v-model="model.data.maxIterations" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="并行模式" class="fs-form-inline">
          <el-switch v-model="model.data.parallel" />
        </el-form-item>
        <el-form-item label="并行数量" v-if="model.data.parallel">
          <el-input-number v-model="model.data.parallelCount" :min="1" :max="10" :controls="false" />
        </el-form-item>
        <el-form-item label="错误处理">
          <el-select v-model="model.data.errorMode" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.loopErrorModes" />
          </el-select>
        </el-form-item>
        <el-form-item label="">
          <div class="iteration-tips">
            将需要重复执行的节点拖入迭代容器内部，容器内的节点可通过
            <em>{{ model.data.itemName || 'item' }}</em>
            引用当前迭代的元素
          </div>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.iteration-tips {
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
  em {
    font-style: normal;
    color: var(--el-color-primary);
  }
}
</style>
