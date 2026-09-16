<script setup lang="ts">
/**
 * 代码执行节点属性 - 执行一段 Python 或 NodeJS 代码实现自定义逻辑。
 */
import { computed, ref, watch } from 'vue'
import 'codemirror/mode/python/python'
import config from './config'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

const mode = computed(() => 'python3' === model.value.data.language ? 'python' : 'javascript')

const inputColumns = computed(() => [{
  prop: 'name', label: '参数名', placeholder: '函数入参名，如 arg1', default: '',
}, {
  prop: 'variable', type: 'variable', label: '变量', placeholder: '请选择变量', default: '',
}])

const outputColumns = computed(() => [{
  prop: 'name', label: '返回值名', placeholder: 'return 的键名，如 result', default: '',
}, {
  prop: 'type', type: 'select', options: 'types', default: 'String', placeholder: '返回值类型',
}])

watch(() => model.value.data.language, (language: string) => {
  const samples = Object.values(config.codeSamples)
  if (!model.value.data.code || samples.indexOf(model.value.data.code) >= 0) {
    model.value.data.code = config.codeSamples[language] ?? ''
  }
})

const handleReset = () => {
  model.value.data.code = config.codeSamples[model.value.data.language] ?? ''
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">代码配置</el-form-item>
        <el-form-item label="执行语言">
          <el-select v-model="model.data.language" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.codeLanguages" />
          </el-select>
        </el-form-item>
        <el-form-item label="">
          <div class="code-header">
            <span>代码内容</span>
            <el-button link type="primary" @click="handleReset">重置示例代码</el-button>
          </div>
          <code-editor
            v-model="model.data.code"
            :key="model.data.language"
            :mode="mode"
            :height="320"
            :fold-gutter="true"
            placeholder="请输入代码" />
        </el-form-item>
        <el-form-item label="">
          <div class="code-tips">入参为函数入参，返回值会作为节点的输出变量，返回值需为对象且键名与下方声明一致</div>
        </el-form-item>
        <el-form-item label="" class="title">输入变量</el-form-item>
        <el-form-item label="">
          <FieldSlice
            v-model="model.data.inputs"
            :columns="inputColumns"
            :instance="$props.instance"
            :active-item="model"
            collapsible
            add-text="添加输入变量" />
        </el-form-item>
        <el-form-item label="" class="title">输出变量</el-form-item>
        <el-form-item label="">
          <FieldSlice v-model="model.data.outputs" :columns="outputColumns" collapsible add-text="添加输出变量" />
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.code-header {
  width: 100%;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  @include flex-between();
}
.code-tips {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
}
</style>
