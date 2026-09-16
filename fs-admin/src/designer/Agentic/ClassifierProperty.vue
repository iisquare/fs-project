<script setup lang="ts">
/**
 * 问题分类器节点属性 - 定义用户问题的分类条件，由 LLM 按分类描述判定对话的进展方式。
 * 分类默认收起，仅展示分类名与描述摘要，点击标题行展开编辑。
 */
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import DesignUtil from '@/utils/DesignUtil'
import CollapseItem from './CollapseItem.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import VariableSelect from './VariableSelect.vue'
import { useCollapse } from './collapse'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

// 仅一个分类时默认展开，多个分类默认收起
const { isOpen, open, toggle, remove } = useCollapse(() => 1 === model.value.data.classes.length)

const handleAdd = () => {
  model.value.data.classes.push({
    id: DesignUtil.uuid(),
    name: `分类${model.value.data.classes.length + 1}`,
    description: '',
  })
  open(model.value.data.classes.length - 1)
}

const handleRemove = (index: number) => {
  model.value.data.classes.splice(index, 1)
  remove(index)
}

// 收起时的摘要：分类描述
const summaryTags = (item: any) => {
  const description = String(item?.description ?? '').trim()
  if (!description) return []
  return [description.length > 14 ? description.slice(0, 14) + '…' : description]
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">模型配置</el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="model.data.model" clearable placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="输入变量">
          <VariableSelect
            v-model="model.data.query"
            :instance="$props.instance"
            :active-item="model"
            allow-create
            placeholder="请选择待分类的文本" />
        </el-form-item>
        <el-form-item label="记忆" class="fs-form-inline">
          <el-switch v-model="model.data.memory.enabled" />
        </el-form-item>
        <el-form-item label="记忆窗口" v-if="model.data.memory.enabled">
          <el-input-number v-model="model.data.memory.window" :min="1" :max="50" :controls="false" />
        </el-form-item>
        <el-form-item label="分类指令">
          <el-input
            v-model="model.data.instruction"
            type="textarea"
            :rows="4"
            placeholder="补充说明分类的判断依据，如：当用户询问价格时归入咨询分类" />
        </el-form-item>
        <el-form-item label="" class="title">分类条件</el-form-item>
        <el-form-item label="">
          <div class="class-slice">
            <CollapseItem
              :key="item.id"
              v-for="(item, index) in model.data.classes"
              :title="item.name || '分类 ' + (index + 1)"
              :tags="summaryTags(item)"
              :expanded="isOpen(index)"
              @toggle="toggle(index)"
              @delete="handleRemove(index)">
              <el-input v-model="item.name" placeholder="分类名称，作为分支输出" />
              <el-input v-model="item.description" type="textarea" :rows="2" placeholder="分类描述，用于指导模型判定" />
            </CollapseItem>
            <el-button link type="primary" :icon="Plus" @click="handleAdd">添加分类</el-button>
          </div>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.class-slice {
  width: 100%;
  .collapse-body {
    .el-input {
      width: 100%;
      & + .el-input {
        margin-top: 6px;
      }
    }
  }
}
</style>
