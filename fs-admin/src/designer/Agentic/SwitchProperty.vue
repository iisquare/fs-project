<script setup lang="ts">
/**
 * 条件分支节点属性 - switch-case-default 形式，每个 case 与 default 各有一个连线锚点。
 * 分支默认收起，仅展示分支名与条件数量，点击标题行展开编辑。
 */
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import DesignUtil from '@/utils/DesignUtil'
import CollapseItem from './CollapseItem.vue'
import ConditionSlice from './ConditionSlice.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import { useCollapse } from './collapse'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

// 仅一个分支时默认展开，多个分支默认收起
const { isOpen, open, toggle, remove } = useCollapse(() => 1 === model.value.data.cases.length)

const handleAdd = () => {
  const index = model.value.data.cases.length + 1
  model.value.data.cases.push({
    id: DesignUtil.uuid(),
    name: `条件${index}`,
    logic: 'and',
    conditions: [{ variable: '', operator: 'eq', value: '' }],
  })
  open(model.value.data.cases.length - 1)
}

const handleRemove = (index: number) => {
  model.value.data.cases.splice(index, 1)
  remove(index)
}

// 收起时的摘要：条件数量
const summaryTags = (item: any) => {
  const conditions = item?.conditions ?? []
  return [conditions.length ? conditions.length + ' 个条件' : '未配置条件']
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">分支配置</el-form-item>
        <el-form-item label="">
          <div class="case-slice">
            <CollapseItem
              :key="item.id"
              v-for="(item, index) in model.data.cases"
              :title="item.name || '分支 ' + (index + 1)"
              :tags="summaryTags(item)"
              :expanded="isOpen(index)"
              @toggle="toggle(index)"
              @delete="handleRemove(index)">
              <el-input v-model="item.name" placeholder="分支名称，同时作为连线名称" />
              <ConditionSlice v-model="model.data.cases[index]" :instance="$props.instance" :active-item="model" />
            </CollapseItem>
            <el-button link type="primary" :icon="Plus" @click="handleAdd">添加分支</el-button>
          </div>
        </el-form-item>
        <el-form-item label="默认分支名称">
          <el-input v-model="model.data.defaultName" placeholder="以上分支均未命中时走该分支" />
        </el-form-item>
        <el-form-item label="">
          <div class="case-tips">
            节点右侧每个分支各有一个锚点，可分别连线到不同节点；移除分支时其连线一并移除
          </div>
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.case-slice {
  width: 100%;
  .collapse-body > .el-input {
    width: 100%;
    margin-bottom: 6px;
  }
}
.case-tips {
  font-size: 12px;
  line-height: 1.8;
  color: var(--el-text-color-placeholder);
}
</style>
