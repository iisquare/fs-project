<script setup lang="ts">
/**
 * 变量聚合器节点属性 - 把多路分支的变量按分组聚合，分组名称即输出变量名（标题名称仅用于展示），供下游统一引用。
 * 分组先定输出类型，组内只能选同类型的变量，保证聚合结果类型一致。
 */
import { ref, watch } from 'vue'
import { Close, Delete, Plus } from '@element-plus/icons-vue'
import DesignUtil from '@/utils/DesignUtil'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'
import VariableSelect from './VariableSelect.vue'
import config from './config'
import { referenceOfToken, variableGroups } from './variable'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

/**
 * 兼容历史数据缺少分组的情况。
 * 用 watch 而不是 setup 里只跑一次：属性面板实例会在同类节点之间复用，切换节点时同样要兜底。
 */
watch(model, (value: any) => {
  if (!value?.data) return
  if (!Array.isArray(value.data.groups)) value.data.groups = []
  // 分组上后加的字段：缺输出类型按文本处理，变量清单兜底为空数组
  value.data.groups.forEach((group: any) => {
    if (!group) return
    if (undefined === group.outputType) group.outputType = 'String'
    if (undefined === group.label) group.label = ''
    if (!Array.isArray(group.variables)) group.variables = []
  })
}, { immediate: true })

// 分组默认两个空变量位：聚合的典型场景是多路分支汇总
const defaultVariables = () => [{ variable: '' }, { variable: '' }]

const typeOptions = () => props.config?.types ?? config.types ?? []

// 分组输出类型的文案：卡片收起时作为摘要标签展示，变量选择器的提示语也用它
const typeLabel = (value: string) => {
  const option: any = typeOptions().find((item: any) => item.value === value)
  return option ? option.label : (value || '')
}

// 变量选择器的提示语带上分组类型，免去单独一行说明「变量需与分组类型一致」
const variablePlaceholder = (group: any) => {
  const label = typeLabel(group?.outputType)
  return label ? `请选择「${label}」类型的变量` : '请选择变量'
}

const handleAddGroup = () => {
  model.value.data.groups.push({
    id: DesignUtil.uuid(),
    name: 'output',
    // 标题名称仅用于展示，为空时展示输出变量名
    label: '聚合结果',
    outputType: 'String',
    variables: defaultVariables(),
  })
}

const handleRemoveGroup = (index: number) => {
  model.value.data.groups.splice(index, 1)
}

const handleAddVariable = (group: any) => {
  if (!Array.isArray(group.variables)) group.variables = []
  group.variables.push({ variable: '' })
}

const handleRemoveVariable = (group: any, index: number) => {
  group.variables.splice(index, 1)
}

/**
 * 组内变量需与分组类型一致：切换输出类型后，只清掉与新模式不再匹配的取值，
 * 同类型的变量保留，避免一次切换把整份清单清空
 */
const handleTypeChange = (group: any) => {
  const references: Record<string, string> = {}
  variableGroups(props.instance).forEach((item: any) => {
    item.variables.forEach((variable: any) => { references[variable.value] = variable.type })
  })
  ;(group.variables ?? []).forEach((item: any) => {
    // 取值是占位符（{{#节点标识.变量名#}}），类型比对按其中的变量引用
    const reference = referenceOfToken(item.variable) || String(item.variable ?? '')
    if (reference && references[reference] !== group.outputType) item.variable = ''
  })
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <SectionSlice title="聚合分组">
          <div class="group-slice">
            <div
              :key="item.id"
              v-for="(item, index) in model.data.groups as any[]"
              class="group">
              <!-- 标题行：输出变量名 + 输出类型 + 删除分组（整组删除用垃圾桶，与移除变量的叉号区分） -->
              <div class="group-head">
                <el-input v-model="item.name" placeholder="输出变量名">
                  <template #prefix><LayoutIcon name="PriceTag" /></template>
                </el-input>
                <el-select v-model="item.outputType" placeholder="类型" @change="handleTypeChange(item)">
                  <template #prefix><LayoutIcon name="Grid" /></template>
                  <el-option :key="option.value" :value="option.value" :label="option.label" v-for="option in typeOptions()" />
                </el-select>
                <el-icon class="delete" title="删除该分组" @click="handleRemoveGroup(index)"><Delete /></el-icon>
              </div>
              <!-- 标题名称独立一行：右侧让出与移除变量图标等宽的位置，与下面的变量选择器右边界对齐 -->
              <div class="group-title">
                <el-input v-model="item.label" placeholder="标题名称（选填），为空时展示输出变量名">
                  <template #prefix><LayoutIcon name="Memo" /></template>
                </el-input>
              </div>
              <div class="variable-row" :key="vi" v-for="(variable, vi) in (item.variables ?? []) as any[]">
                <VariableSelect
                  v-model="variable.variable"
                  :instance="$props.instance"
                  :active-item="model"
                  :types="item.outputType"
                  icon="Aim"
                  :placeholder="variablePlaceholder(item)" />
                <!-- 与「删除分组」区分开：整组删除用垃圾桶，移除一行变量用叉号 -->
                <el-icon class="delete" title="移除该变量" @click="handleRemoveVariable(item, vi)"><Close /></el-icon>
              </div>
              <el-button link type="primary" :icon="Plus" @click="handleAddVariable(item)">添加变量</el-button>
            </div>
            <el-button class="group-add" link type="primary" :icon="Plus" @click="handleAddGroup">添加分组</el-button>
            <tip-text text="每个分组按变量清单顺序聚合成一个变量，变量名取分组名称（标题名称仅用于展示），各路分支只需各自连线到本节点" />
          </div>
        </SectionSlice>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
/**
 * 分组卡片：第一行是「输出变量名 + 输出类型 + 删除分组」，下面依次是标题名称与这个分组的变量清单，
 * 用缩进与卡片底色表达从属关系；变量选择器只列与分组类型一致的变量
 */
.group-slice {
  width: 100%;
  .group {
    padding: 8px 10px;
    border-radius: 4px;
    background: var(--fs-panel-surface);
    & + .group {
      margin-top: 8px;
    }
    .group-head {
      @include flex-start();
      /* 面板变窄时名称与类型整块换行：不给面板撑出横向滚动条，类型也不会被压缩截断 */
      flex-wrap: wrap;
      gap: 6px;
      .el-input {
        flex: 1 1 90px;
        min-width: 0;
      }
      .el-select {
        /* 宽度按最长类型名（「对象数组」4 个字）加图标与箭头留足；放不下时换行而不是压缩它 */
        flex: 0 1 120px;
        min-width: 120px;
      }
      .delete {
        margin-left: auto;
      }
    }
    .group-title {
      @include flex-start();
      gap: 6px;
      margin-top: 6px;
      .el-input {
        flex: 1;
        min-width: 0;
      }
      /* 与移除变量的图标等宽，标题名称的右边界因此和下面的变量选择器对齐 */
      &::after {
        content: '';
        flex: none;
        width: 16px;
      }
    }
    .variable-row {
      @include flex-start();
      gap: 6px;
      margin-top: 6px;
      .el-select {
        flex: 1;
        min-width: 0;
      }
    }
    .delete {
      flex: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 16px;
      color: var(--el-text-color-placeholder);
      cursor: pointer;
      &:hover {
        color: var(--el-color-error);
      }
    }
    .el-button {
      margin-top: 6px;
    }
  }
  .group-add {
    margin-top: 8px;
  }
  .tip-text {
    margin-top: 6px;
  }
}
</style>
