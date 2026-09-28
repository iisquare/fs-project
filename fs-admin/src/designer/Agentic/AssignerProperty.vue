<script setup lang="ts">
/**
 * 变量赋值节点属性 - 向会话变量等可写入变量进行赋值。
 * 每条赋值按「目标变量 → 赋值方式 → 取值」三步录入：取值来源用二选一开关，
 * 只展示对应的那个框；清除操作没有取值，相关字段直接不展示。
 */
import { computed, ref } from 'vue'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

const columns = computed(() => [{
  // 目标变量按统一规范写引用（{{#容器标识.变量名#}} / {{#conversation.变量名#}}，运行时按引用解析）：
  // 容器（循环/迭代）内的变量只列可写入的那部分，会话变量按 conversation.xxx 手动输入
  prop: 'target', type: 'variable', label: '目标变量', icon: 'Aim', default: '', writable: true,
  placeholder: '请选择容器内变量，或输入会话变量 conversation.xxx',
}, {
  prop: 'operation', type: 'select', options: 'assignOperations', default: 'set', placeholder: '赋值方式',
}, {
  // 取值来源用二选一开关，避免同时摆出「引用变量」与「固定值」两个框
  prop: 'source', type: 'radio', options: 'assignSources', default: 'variable',
  when: (item: any) => 'clear' !== item.operation,
}, {
  prop: 'variable', type: 'variable', label: '引用变量', icon: 'Aim', default: '',
  placeholder: '请选择变量',
  when: (item: any) => 'clear' !== item.operation && 'variable' === item.source,
}, {
  prop: 'value', label: '固定值', placeholder: '请输入固定值', default: '',
  when: (item: any) => 'clear' !== item.operation && 'variable' !== item.source,
}])
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <SectionSlice title="赋值操作">
          <el-form-item label="">
            <FieldSlice
              v-model="model.data.assignments"
              :columns="columns"
              :instance="$props.instance"
              :active-item="model"
              title-prop="target"
              collapsible
              add-text="添加赋值操作" />
          </el-form-item>
          <el-form-item label="">
            <tip-text text="目标变量需为可写入变量：容器（循环/迭代）内的变量可直接选择，会话变量按 conversation.xxx 输入；赋值结果可在后续节点中引用" />
          </el-form-item>
        </SectionSlice>
        <OutputSlice :data="model.data" :instance="$props.instance" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>
