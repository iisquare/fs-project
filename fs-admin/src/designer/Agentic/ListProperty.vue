<script setup lang="ts">
/**
 * 列表操作节点属性 - 用于过滤或排序数组内容。
 */
import { computed, ref } from 'vue'
import ConditionSlice from './ConditionSlice.vue'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import VariableSelect from './VariableSelect.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

const columns = computed(() => [{
  prop: 'variable', type: 'variable', label: '排序字段', placeholder: '请选择排序字段', default: '',
}, {
  prop: 'order', type: 'select', options: 'sortOrders', default: 'asc', placeholder: '排序方式',
}])
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">数据来源</el-form-item>
        <el-form-item label="输入列表">
          <VariableSelect
            v-model="model.data.input"
            :instance="$props.instance"
            :active-item="model"
            types="Array<Object>,Array<String>,Array<File>"
            placeholder="请选择列表变量" />
        </el-form-item>
        <el-form-item label="" class="title">过滤条件</el-form-item>
        <el-form-item label="">
          <ConditionSlice
            v-model="model.data.filter"
            :instance="$props.instance"
            :active-item="model" />
        </el-form-item>
        <el-form-item label="" class="title">排序规则</el-form-item>
        <el-form-item label="">
          <FieldSlice
            v-model="model.data.sorts"
            :columns="columns"
            :instance="$props.instance"
            :active-item="model"
            collapsible
            add-text="添加排序" />
        </el-form-item>
        <el-form-item label="" class="title">数量限制</el-form-item>
        <el-form-item label="限制方式">
          <el-select v-model="model.data.limit.type" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.listLimitTypes" />
          </el-select>
        </el-form-item>
        <el-form-item label="限制数量" v-if="'all' !== model.data.limit.type">
          <el-input-number v-model="model.data.limit.size" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="输出变量名">
          <el-input v-model="model.data.outputName" placeholder="如 result" />
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
