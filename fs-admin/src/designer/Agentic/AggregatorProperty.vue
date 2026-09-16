<script setup lang="ts">
/**
 * 变量聚合器节点属性 - 将多路分支的变量聚合为一个变量，以实现下游节点统一配置。
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
  prop: 'variable', type: 'variable', label: '变量', placeholder: '请选择待聚合的变量', default: '',
}])
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">聚合配置</el-form-item>
        <el-form-item label="聚合策略">
          <el-select v-model="model.data.strategy" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.aggregateStrategies" />
          </el-select>
        </el-form-item>
        <el-form-item label="">
          <div class="aggregator-tips">按分支顺序聚合变量，多路分支只需各自连线到本节点即可</div>
        </el-form-item>
        <el-form-item label="" class="title">变量列表</el-form-item>
        <el-form-item label="">
          <FieldSlice
            v-model="model.data.variables"
            :columns="columns"
            :instance="$props.instance"
            :active-item="model"
            collapsible
            add-text="添加变量" />
        </el-form-item>
        <el-form-item label="" class="title">输出配置</el-form-item>
        <el-form-item label="输出变量名">
          <el-input v-model="model.data.outputName" placeholder="如 output" />
        </el-form-item>
        <el-form-item label="输出类型">
          <el-select v-model="model.data.outputType" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.types" />
          </el-select>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.aggregator-tips {
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
}
</style>
