<script setup lang="ts">
/**
 * 列表操作节点属性 - 用于过滤或排序数组内容。
 * 过滤条件与排序字段都针对「输入列表里的每一项」：填的是列表项里的字段名（支持 a.b），不是画布变量；
 * 字符串等标量数组没有字段，字段名留空即按元素本身过滤/排序。
 */
import { computed, ref } from 'vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'
import ConditionSlice from './ConditionSlice.vue'
import FieldSlice from './FieldSlice.vue'
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
  prop: 'variable', label: '排序字段', placeholder: '字段名，如 score，支持 a.b；字符串数组留空即元素本身', default: '',
}, {
  prop: 'order', type: 'select', options: 'sortOrders', default: 'asc', placeholder: '排序方式',
}])
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <SectionSlice title="数据来源">
          <el-form-item label="输入列表">
            <VariableSelect
              v-model="model.data.input"
              :instance="$props.instance"
              :active-item="model"
              types="Array<Object>,Array<String>,Array<File>"
              placeholder="请选择列表变量" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="过滤条件">
          <template #title>
            过滤条件
            <LayoutHelp text="条件作用于输入列表的每一项：字段名填列表项里的键，如 name、user.age；字符串数组字段名留空，直接比较元素本身" />
          </template>
          <el-form-item label="">
            <ConditionSlice
              v-model="model.data.filter"
              :instance="$props.instance"
              :active-item="model"
              field />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="排序规则">
          <template #title>
            排序规则
            <LayoutHelp text="按输入列表里每一项的字段排序，字段名支持 a.b 形式，如 score、user.age；字符串数组字段名留空，按元素本身排序" />
          </template>
          <el-form-item label="">
            <FieldSlice
              v-model="model.data.sorts"
              :columns="columns"
              :instance="$props.instance"
              :active-item="model"
              collapsible
              add-text="添加排序" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="数量限制">
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
        </SectionSlice>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
