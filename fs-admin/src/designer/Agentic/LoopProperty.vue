<script setup lang="ts">
/**
 * 循环节点属性 - 循环执行一段逻辑直到满足结束条件或到达循环次数上限。
 */
import { computed, ref } from 'vue'
import ConditionSlice from './ConditionSlice.vue'
import FieldSlice from './FieldSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'

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
  prop: 'type', type: 'select', options: 'types', default: 'Integer', placeholder: '变量类型',
}, {
  prop: 'value', label: '初始值', placeholder: '循环变量的初始值', default: '0',
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
        <el-form-item label="" class="title">循环变量</el-form-item>
        <el-form-item label="">
          <FieldSlice v-model="model.data.variables" :columns="columns" collapsible add-text="添加循环变量" />
        </el-form-item>
        <el-form-item label="" class="title">终止条件</el-form-item>
        <el-form-item label="">
          <ConditionSlice
            v-model="model.data.condition"
            :instance="$props.instance"
            :active-item="model" />
        </el-form-item>
        <el-form-item label="最大循环次数">
          <el-input-number v-model="model.data.maxIterations" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="输出变量名">
          <el-input v-model="model.data.outputName" placeholder="如 output，收集各次结果" />
        </el-form-item>
        <el-form-item label="">
          <div class="loop-tips">
            将需要重复执行的节点拖入循环容器内部，满足终止条件或达到最大次数后结束循环；
            容器内的节点可通过
            <em>{{ variableNames }}</em>
            引用循环变量的当前取值
          </div>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.loop-tips {
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
  em {
    font-style: normal;
    color: var(--el-color-primary);
  }
}
</style>
