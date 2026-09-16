<script setup lang="ts">
/**
 * 变量赋值节点属性 - 向会话变量等可写入变量进行赋值。
 */
import { computed, ref } from 'vue'
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

const columns = computed(() => [{
  prop: 'target', label: '目标变量', placeholder: '会话变量名，如 conversation.result', default: '',
}, {
  prop: 'operation', type: 'select', options: 'assignOperations', default: 'set', placeholder: '赋值方式',
}, {
  prop: 'source', type: 'select', options: 'assignSources', default: 'variable', placeholder: '赋值来源',
}, {
  prop: 'variable', type: 'variable', label: '引用变量', placeholder: '来源于变量时填写', default: '',
}, {
  prop: 'value', label: '固定值', placeholder: '来源于固定值时填写', default: '',
}])
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">赋值操作</el-form-item>
        <el-form-item label="">
          <FieldSlice
            v-model="model.data.assignments"
            :columns="columns"
            :instance="$props.instance"
            :active-item="model"
            collapsible
            add-text="添加赋值操作" />
        </el-form-item>
        <el-form-item label="">
          <div class="assigner-tips">目标变量需为可写入变量，如会话变量 conversation.xxx，赋值结果可在后续节点中引用</div>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.assigner-tips {
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
}
</style>
