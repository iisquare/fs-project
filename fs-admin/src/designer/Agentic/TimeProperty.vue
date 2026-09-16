<script setup lang="ts">
/**
 * 时间节点属性 - 时间戳转换、获取当前时间、时区转换、星期几计算等。
 */
import { computed, ref } from 'vue'
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

const operation = computed(() => model.value.data.operation)
// 系统当前时间可直接取值，无需输入
const noInputOperations = ['current', 'now2timestamp']
// 时间内容既可直接填写，也可引用上游变量
const needInput = computed(() => noInputOperations.indexOf(operation.value) < 0)
const needFormat = computed(() => ['timestamp2time', 'format', 'timezone', 'add', 'weekday'].indexOf(operation.value) >= 0)
const needTimezone = computed(() => ['current', 'now2timestamp', 'timestamp2time', 'time2timestamp', 'format', 'add', 'weekday'].indexOf(operation.value) >= 0)
const needAmount = computed(() => 'add' === operation.value)
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">时间配置</el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="model.data.operation" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.timeOperations" />
          </el-select>
        </el-form-item>
        <el-form-item label="时区" v-if="needTimezone">
          <el-select v-model="model.data.timezone" filterable allow-create default-first-option placeholder="请选择时区">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.timezones" />
          </el-select>
        </el-form-item>
        <template v-if="needInput">
          <el-form-item label="时间变量">
            <VariableSelect
              v-model="model.data.variable"
              :instance="$props.instance"
              :active-item="model"
              allow-create
              placeholder="请选择时间变量" />
          </el-form-item>
          <el-form-item label="结束时间变量" v-if="'diff' === operation">
            <VariableSelect
              v-model="model.data.variable2"
              :instance="$props.instance"
              :active-item="model"
              allow-create
              placeholder="请选择对比的时间变量" />
          </el-form-item>
          <el-form-item label="固定时间值">
            <el-input v-model="model.data.datetime" placeholder="时间变量为空时使用，如 2024-01-01 00:00:00" />
          </el-form-item>
        </template>
        <el-form-item label="输出格式" v-if="needFormat">
          <el-input v-model="model.data.format" placeholder="如 YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="目标时区" v-if="'timezone' === operation">
          <el-select v-model="model.data.targetTimezone" filterable allow-create default-first-option placeholder="请选择目标时区">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.timezones" />
          </el-select>
        </el-form-item>
        <template v-if="needAmount">
          <el-form-item label="加减数量">
            <el-input-number v-model="model.data.amount" :controls="false" />
          </el-form-item>
          <el-form-item label="加减单位">
            <el-select v-model="model.data.unit" placeholder="请选择">
              <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.timeUnits" />
            </el-select>
          </el-form-item>
        </template>
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
</style>
