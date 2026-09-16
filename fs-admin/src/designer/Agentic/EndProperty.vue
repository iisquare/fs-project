<script setup lang="ts">
/**
 * 结束节点属性 - 回复内容与输出变量在对话流与工作流下均可配置：
 * 回复内容用于返回给用户的文本，输出变量用于返回结构化结果。
 */
import { computed, ref } from 'vue'
import { DocumentCopy, Plus } from '@element-plus/icons-vue'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import VariableEditor from './VariableEditor.vue'
import VariablePicker from './VariablePicker.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

const mode = computed(() => props.config?.mode || model.value.data.mode || 'workflow')
const modeText = computed(() => 'chat' === mode.value ? '对话流' : '工作流')

// 回复内容的插入变量与复制按钮放在标题右端，编辑器实例由 VariableEditor 暴露
const editorRef = ref()
const pickerRef = ref()
const insertVisible = ref(false)
const handleInsert = (value: string) => {
  insertVisible.value = false
  editorRef.value?.insert(value)
}
const handleCopy = () => {
  editorRef.value?.copy()
}

const columns = computed(() => [{
  prop: 'name', label: '输出名', icon: 'PriceTag', placeholder: '输出变量名，如 result', default: '',
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
        <el-form-item label="" class="title">
          <span>回复内容</span>
          <el-space :size="4">
            <el-popover
              v-model:visible="insertVisible"
              :width="280"
              trigger="click"
              placement="bottom-end"
              @show="pickerRef?.focus()">
              <template #reference>
                <!-- mousedown 阻止默认行为，避免点击按钮时编辑器失焦丢失光标；点击仍冒泡给弹出框触发 -->
                <span class="insert-trigger" @mousedown.prevent>
                  <el-button link type="primary" size="small" :icon="Plus">插入变量</el-button>
                </span>
              </template>
              <VariablePicker
                ref="pickerRef"
                :instance="$props.instance"
                :active-item="model"
                @select="handleInsert" />
              <div class="insert-tips">
                占位符展示为「节点名称.变量中文名称」，实际值为
                <em v-pre>{{#节点标识.变量英文名称#}}</em>
                ；也可直接输入
                <em>/</em>
                唤起变量提示
              </div>
            </el-popover>
            <el-button link size="small" :icon="DocumentCopy" @click="handleCopy">复制</el-button>
          </el-space>
        </el-form-item>
        <el-form-item label="">
          <VariableEditor
            ref="editorRef"
            v-model="model.data.template"
            :instance="$props.instance"
            :active-item="model"
            :height="180"
            placeholder="请输入回复内容，可插入上游变量" />
        </el-form-item>
        <el-form-item label="" class="title">输出变量</el-form-item>
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
        <el-form-item label="">
          <div class="end-tips">
            当前应用类型为{{ modeText }}：回复内容作为返回给用户的文本，输出变量作为返回的结构化结果，
            两者均可配置、也均可为空
          </div>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.insert-trigger {
  display: inline-flex;
}
.insert-tips {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-placeholder);
  em {
    font-style: normal;
    color: var(--el-color-primary);
  }
}
.end-tips {
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
}
</style>
