<script setup lang="ts">
/**
 * 循环节点属性 - 循环执行一段逻辑直到满足结束条件或到达循环次数上限。
 * 循环节点没有输出变量：循环变量的初始值可为固定值或引用变量，容器内的节点可重写其取值。
 */
import { computed, ref } from 'vue'
import ConditionSlice from './ConditionSlice.vue'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

const variableNames = computed(() => {
  const names = (model.value?.data?.variables ?? []).map((item: any) => item?.name).filter(Boolean)
  return names.length ? names.join('、') : '循环变量'
})

/**
 * 容器内已不再自动生成入口节点，内部为空时给出提示
 */
const emptyBody = () => {
  const cell: any = props.instance?.flow?.graph?.getCellById?.(model.value?.id)
  return Boolean(cell) && !(cell.getChildren?.() ?? []).length
}

const columns = computed(() => [{
  prop: 'name', label: '变量名', placeholder: '循环变量名，如 index', default: '',
}, {
  // 标题名称仅用于展示，为空时展示变量名
  prop: 'label', label: '标题名称', placeholder: '展示名称（选填），为空时展示变量名', default: '',
}, {
  prop: 'type', type: 'select', options: 'types', default: 'Integer', placeholder: '变量类型',
}, {
  // 初始值来源用二选一开关，选哪种只展示对应的取值控件
  prop: 'source', type: 'radio', options: 'variableSources', default: 'constant',
}, {
  // 初始值取自容器外部：循环自身的变量尚未初始化，不参与选择（见 FieldSlice/VariableSelect 的 outer）
  prop: 'variable', type: 'variable', label: '引用变量', placeholder: '请选择外部变量作为初始值', default: '', outer: true,
  when: (item: any) => 'variable' === item.source,
}, {
  prop: 'value', label: '固定值', placeholder: '初始值，如 0', default: '',
  when: (item: any) => 'variable' !== item.source,
}])
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
        <SectionSlice title="循环变量">
          <el-form-item label="">
            <FieldSlice
              v-model="model.data.variables"
              :columns="columns"
              :instance="$props.instance"
              :active-item="model"
              collapsible
              add-text="添加循环变量" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="终止条件">
          <el-form-item label="">
            <ConditionSlice
              v-model="model.data.condition"
              :instance="$props.instance"
              :active-item="model" />
          </el-form-item>
          <el-form-item label="最大循环次数">
            <el-input-number v-model="model.data.maxIterations" :min="1" :controls="false" />
          </el-form-item>
          <el-form-item label="">
            <tip-text>
              将需要重复执行的节点拖入循环容器内部，满足终止条件或达到最大次数后结束循环；
              循环变量的初始值可填固定值，也可引用容器外部的变量；
              容器内的节点可通过
              <em>{{ variableNames }}</em>
              引用循环变量的当前取值，并用变量赋值节点覆盖其值
            </tip-text>
          </el-form-item>
        </SectionSlice>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>
