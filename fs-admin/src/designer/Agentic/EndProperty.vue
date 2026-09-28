<script setup lang="ts">
/**
 * 结束节点属性 - 回复内容与输出变量在对话流与工作流下均可配置：
 * 回复内容用于返回给用户的文本，输出变量用于返回结构化结果。
 */
import { computed, ref } from 'vue'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'
import VariableField from './VariableField.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

const mode = computed(() => props.config?.mode || model.value.data.mode || 'workflow')
const modeText = computed(() => 'chat' === mode.value ? '对话流' : '工作流')

const columns = computed(() => [{
  prop: 'name', label: '输出名', icon: 'PriceTag', placeholder: '输出变量名，如 result', default: '',
}, {
  // 标题名称仅用于展示，为空时展示输出名
  prop: 'label', label: '标题名称', icon: 'Memo', placeholder: '展示名称（选填），为空时展示输出名', default: '',
}, {
  prop: 'type', type: 'select', options: 'types', icon: 'Grid', default: 'String', placeholder: '输出类型',
}, {
  prop: 'variable', type: 'variable', label: '输出内容', icon: 'Aim', placeholder: '请选择变量',
}])
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <VariableField
          v-model="model.data.template"
          title="回复内容"
          :instance="$props.instance"
          :active-item="model"
          :height="180"
          placeholder="请输入回复内容，可插入上游变量" />
        <SectionSlice title="输出变量">
          <el-form-item label="">
            <FieldSlice
              v-model="model.data.outputs"
              :columns="columns"
              :instance="$props.instance"
              :active-item="model"
              collapsible
              empty-text="输出变量可为空，需要返回结构化结果时再添加"
              add-text="添加输出变量" />
          </el-form-item>
        </SectionSlice>
        <el-form-item label="">
          <tip-text>
            当前应用类型为{{ modeText }}：回复内容作为返回给用户的文本，输出变量作为返回的结构化结果，
            两者均可配置、也均可为空
          </tip-text>
        </el-form-item>
        <!-- 只读清单会带上「回复内容」，与上面的可编辑输出变量同名，这里换个标题避免两处重名 -->
        <OutputSlice :data="model.data" title="下游可引用变量" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>
