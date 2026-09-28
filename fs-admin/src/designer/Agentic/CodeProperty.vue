<script setup lang="ts">
/**
 * 代码执行节点属性 - 执行一段 JavaScript 代码实现自定义逻辑（服务端 ScriptEngineManager，不支持其它语言）。
 * 面板顺序与写代码的顺序一致：先声明输入变量，再写代码，最后声明返回值。
 */
import { computed, ref } from 'vue'
import { RefreshLeft } from '@element-plus/icons-vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'
import config from './config'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import SectionSlice from './SectionSlice.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

const inputColumns = computed(() => [{
  prop: 'name', label: '参数名', placeholder: '函数入参名，如 arg1', default: '',
}, {
  prop: 'variable', type: 'variable', label: '变量', placeholder: '请选择变量', default: '',
}])

const outputColumns = computed(() => [{
  prop: 'name', label: '返回值名', placeholder: 'return 的键名，如 result', default: '',
}, {
  // 标题名称仅用于展示，为空时展示返回值名
  prop: 'label', label: '标题名称', placeholder: '展示名称（选填），为空时展示返回值名', default: '',
}, {
  prop: 'type', type: 'select', options: 'types', default: 'String', placeholder: '返回值类型',
}])

const handleReset = () => {
  model.value.data.code = config.codeSamples.nodejs ?? ''
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <SectionSlice title="输入变量">
          <template #title>
            输入变量
            <LayoutHelp text="输入变量就是函数的入参：代码里按这里的参数名取值，如 async function main({ arg1 })" />
          </template>
          <el-form-item label="">
            <FieldSlice
              v-model="model.data.inputs"
              :columns="inputColumns"
              :instance="$props.instance"
              :active-item="model"
              collapsible
              add-text="添加输入变量" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="代码内容（JavaScript）">
          <template #title>
            代码内容（JavaScript）
            <LayoutHelp text="由服务端 JavaScript 引擎执行（ScriptEngineManager），Node 专有 API（require / process / fs 等）不可用；返回值需为对象，键名与下方声明的返回值名一致" />
          </template>
          <template #actions>
            <!-- 提示用原生 title，不额外挂 tooltip（与插入变量/复制按钮一致） -->
            <el-button link size="small" title="重置示例代码" :icon="RefreshLeft" @click="handleReset" />
          </template>
          <el-form-item label="">
            <code-editor
              v-model="model.data.code"
              mode="javascript"
              :height="320"
              resizable
              placeholder="请输入代码" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="输出变量">
          <el-form-item label="">
            <FieldSlice v-model="model.data.outputs" :columns="outputColumns" collapsible add-text="添加输出变量" />
          </el-form-item>
        </SectionSlice>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
