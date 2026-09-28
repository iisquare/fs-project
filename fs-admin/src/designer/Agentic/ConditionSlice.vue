<script setup lang="ts">
/**
 * 条件编辑 - 维护一组比较条件，支持全部/任一逻辑，供条件分支、循环终止、列表过滤复用。
 * 每个条件默认收起，仅展示变量与运算符摘要，点击标题行展开编辑。
 *
 * @v-model  {Object} 条件对象 `{ logic: 'and'|'or', conditions: [{ variable, operator, source, value }] }`；
 *                     source 为比较值来源（fixed 固定值 / variable 引用变量），value 存取值本身
 * @prop     {Boolean} field - 条件的「变量」是列表项字段名（列表过滤用），默认 false 时用画布变量选择器
 */
import { Plus } from '@element-plus/icons-vue'
import { watch } from 'vue'
import CollapseItem from './CollapseItem.vue'
import { useCollapse } from './collapse'
import config from './config'
import VariableSelect from './VariableSelect.vue'
import { referenceOfToken } from './variable'

/** 比较值来源：固定值 / 引用变量，取值同存 value 字段（引用变量存占位符，运行时按变量解析） */
const valueSources = [
  { label: '固定值', value: 'fixed' },
  { label: '引用变量', value: 'variable' },
]

/** 比较值来源 - 历史数据没有该字段时按取值推断：整串是变量引用即为引用变量 */
const sourceOf = (condition: any) => 'variable' === condition?.source || 'fixed' === condition?.source
  ? condition.source
  : (referenceOfToken(condition?.value) ? 'variable' : 'fixed')

const model: any = defineModel<any>({ required: true })
const props = defineProps<{
  instance?: any,
  activeItem?: any,
  emptyText?: string,
  field?: boolean,
}>()

/**
 * 兼容历史数据缺少条件字段的情况。
 * 用 watch 而不是 setup 里只跑一次：属性面板实例会在同类型节点之间复用，切换节点时同样要兜底。
 */
watch(model, (value: any) => {
  if (!value) return
  if (!Array.isArray(value.conditions)) value.conditions = []
  if (!value.logic) value.logic = 'and'
  // 补齐比较值来源：缺省按当前取值推断，历史数据同样能选中对应的那一档
  value.conditions.forEach((condition: any) => {
    condition.source = sourceOf(condition)
  })
}, { immediate: true })

// 仅一条条件时默认展开，多条默认收起
const { isOpen, open, toggle, remove } = useCollapse(() => 1 === model.value.conditions.length)

const handleAdd = () => {
  model.value.conditions.push({ variable: '', operator: 'eq', value: '' })
  open(model.value.conditions.length - 1)
}

const handleRemove = (index: number) => {
  model.value.conditions.splice(index, 1)
  remove(index)
}

const needValue = (operator: string) => config.noValueOperators.indexOf(operator) < 0

// 收起时的摘要：变量名与运算符
const summaryTags = (condition: any) => {
  // 变量的取值是占位符（{{#节点标识.变量名#}}）或手工输入的会话变量，摘要只展示变量名
  const variable = referenceOfToken(condition?.variable) || String(condition?.variable ?? '')
  const operator: any = config.operators.find((item: any) => item.value === condition?.operator)
  return [
    variable ? variable.split('.').pop() : '未选变量',
    operator ? operator.label : condition?.operator,
  ].filter((text: any) => text)
}
</script>

<template>
  <div class="condition-slice">
    <div class="logic" v-if="model.conditions.length > 1">
      <span>满足</span>
      <el-select v-model="model.logic">
        <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in config.logicOperators" />
      </el-select>
      <span>条件</span>
    </div>
    <CollapseItem
      :key="index"
      v-for="(condition, index) in model.conditions"
      :title="'条件 ' + (index + 1)"
      :tags="summaryTags(condition)"
      :expanded="isOpen(index)"
      @toggle="toggle(index)"
      @delete="handleRemove(index)">
      <!-- 列表过滤：条件针对列表里的每一项，这里填的是列表项里的字段名，不是画布变量 -->
      <el-input
        v-if="props.field"
        v-model="condition.variable"
        placeholder="字段名，如 name，支持 a.b；字符串数组留空即元素本身" />
      <VariableSelect
        v-else
        v-model="condition.variable"
        :instance="instance"
        :active-item="activeItem"
        allow-create
        placeholder="请选择变量" />
      <el-select v-model="condition.operator" placeholder="请选择运算符">
        <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in config.operators" />
      </el-select>
      <template v-if="needValue(condition.operator)">
        <!-- 比较值来源与赋值节点同一套二选一：选定后只摆出对应的取值控件 -->
        <el-radio-group v-model="condition.source">
          <el-radio-button :key="item.value" :value="item.value" v-for="item in valueSources">{{ item.label }}</el-radio-button>
        </el-radio-group>
        <el-input
          v-if="'variable' !== condition.source"
          v-model="condition.value"
          placeholder="请输入固定值，如 10" />
        <VariableSelect
          v-else
          v-model="condition.value"
          :instance="instance"
          :active-item="activeItem"
          allow-create
          placeholder="请选择变量" />
      </template>
    </CollapseItem>
    <el-button link type="primary" :icon="Plus" @click="handleAdd">添加条件</el-button>
  </div>
</template>

<style lang="scss" scoped>
.condition-slice {
  width: 100%;
  .logic {
    margin-bottom: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    @include flex-start();
    gap: 6px;
    .el-select {
      width: 80px;
    }
  }
  .collapse-item {
    .el-select, .el-input {
      width: 100%;
    }
    /* 条件内的控件（字段名 / 运算符 / 取值来源 / 取值）逐行排列：相邻行距统一，
       避免按控件组合逐个补规则时漏配（如「字段名 → 运算符」「运算符 → 取值来源」） */
    :deep(.collapse-body > * + *) {
      margin-top: 6px;
    }
    /* 二选一的取值来源：整行平分，与字段编辑器同一套观感 */
    .el-radio-group {
      display: flex;
      width: 100%;
      :deep(.el-radio-button) {
        flex: 1;
        .el-radio-button__inner {
          width: 100%;
        }
      }
    }
  }
}
</style>
