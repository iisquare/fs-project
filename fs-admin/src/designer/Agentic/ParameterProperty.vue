<script setup lang="ts">
/**
 * 参数提取器节点属性 - 利用 LLM 从自然语言中推理提取结构化参数。
 */
import { computed, ref } from 'vue'
import FieldSlice from './FieldSlice.vue'
import MemoryField from './MemoryField.vue'
import ModelParamsField from './ModelParamsField.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'
import VariableSelect from './VariableSelect.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

const columns = computed(() => [{
  prop: 'name', label: '参数名', icon: 'PriceTag', placeholder: '参数名，如 language', default: '',
}, {
  // 标题名称与开始节点的自定义参数同一口径：只用于展示，为空时展示参数名
  prop: 'label', label: '标题名称', icon: 'Memo', placeholder: '展示名称（选填），为空时展示参数名', default: '',
}, {
  prop: 'type', type: 'select', options: 'types', icon: 'Grid', default: 'String', placeholder: '参数类型',
}, {
  prop: 'description', label: '参数说明', icon: 'InfoFilled', placeholder: '参数含义，指导模型提取', default: '',
}, {
  prop: 'required', type: 'switch', label: '是否必填', default: false,
}])
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <ModelParamsField
          v-model="model.data"
          :config="$props.config"
          :instance="$props.instance"
          :active-item="model"
          system-placeholder="补充说明参数的提取规则，可插入上游变量" />
        <SectionSlice title="输入">
          <el-form-item label="输入变量">
            <VariableSelect
              v-model="model.data.query"
              :instance="$props.instance"
              :active-item="model"
              allow-create
              placeholder="请选择待提取的文本" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="提取参数">
          <el-form-item label="">
            <FieldSlice v-model="model.data.parameters" :columns="columns" collapsible add-text="添加参数" />
          </el-form-item>
        </SectionSlice>
        <MemoryField v-model="model.data" />
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
