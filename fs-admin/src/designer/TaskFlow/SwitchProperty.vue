<script setup lang="ts">
/**
 * 条件分支节点属性 - switch/case-default 形式，每个分支与默认分支各有一个连线锚点。
 * 与 /agent/agentic/model 的条件分支同一形态；条件用表达式书写（与网关节点同一套 SpEL 口径），
 * 运行时按顺序求值、命中第一个为真的分支，全部不命中时走默认分支。
 * 分支默认收起，仅展示分支名与条件摘要，点击标题行展开编辑。
 */
import { ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import DesignUtil from '@/utils/DesignUtil'
import { casePortId } from '@/designer/X6/switch'
import CollapseItem from './CollapseItem.vue'
import NodeSlice from './NodeSlice.vue'
import { useCollapse } from './collapse'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

/**
 * 兼容历史数据：分支列表与默认分支名缺失时补齐。
 * 用 watch 而不是 setup 里只跑一次：属性面板实例会在同类型节点之间复用，切换节点时同样要兜底。
 */
watch(model, (value: any) => {
  if (!value || !value.data) return
  if (!Array.isArray(value.data.cases)) value.data.cases = []
  if (undefined === value.data.defaultName) value.data.defaultName = '否则（默认）'
}, { immediate: true })

// 仅一个分支时默认展开，多个分支默认收起
const { isOpen, open, toggle, remove } = useCollapse(() => 1 === (model.value?.data?.cases ?? []).length)

/** 移除分支锚点上的连线：分支删除后不留下指向已删分支的悬空连线 */
const removeEdges = (item: any) => {
  const graph: any = props.instance?.flow?.graph
  const cell: any = graph?.getCellById(model.value?.id)
  if (!cell) return
  const portId = casePortId(item)
  const edges: any[] = graph.getOutgoingEdges(cell) ?? []
  edges.filter((edge: any) => portId === edge.getSourcePortId()).forEach((edge: any) => graph.removeCell(edge.id))
}

const handleAdd = () => {
  const index = model.value.data.cases.length + 1
  model.value.data.cases.push({
    id: DesignUtil.uuid(),
    name: `条件${index}`,
    condition: '',
  })
  open(model.value.data.cases.length - 1)
}

const handleRemove = (index: number) => {
  removeEdges(model.value.data.cases[index])
  model.value.data.cases.splice(index, 1)
  remove(index)
}

// 收起时的摘要：条件表达式（过长时截断，条件分支节点的分支行共用同一份摘要口径）
const summaryTags = (item: any) => {
  const condition = String(item?.condition ?? '').trim()
  if (!condition) return ['未配置条件']
  return [condition.length > 24 ? condition.slice(0, 24) + '…' : condition]
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="props.instance" :config="props.config" :tips="tips" />
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
              <el-input
                type="textarea"
                :rows="2"
                v-model="item.condition"
                placeholder="分支条件，如 state['api1'] == 'SUCCEED'；留空表示该分支不参与命中" />
            </CollapseItem>
            <el-button link type="primary" :icon="Plus" @click="handleAdd">添加分支</el-button>
          </div>
        </el-form-item>
        <el-form-item label="默认分支名称">
          <el-input v-model="model.data.defaultName" placeholder="以上分支均未命中时走该分支" />
        </el-form-item>
        <el-form-item label="">
          <tip-text>
            节点右侧每个分支各有一个锚点，可分别连线到不同节点；调度时未命中分支上的节点标记为跳过执行，移除分支时其连线一并移除
          </tip-text>
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.case-slice {
  width: 100%;
  /* 分支内的控件（分支名 / 条件表达式）逐行排列，相邻行距统一 */
  :deep(.collapse-body > * + *) {
    margin-top: 6px;
  }
  :deep(.collapse-body .el-input) {
    width: 100%;
  }
}
</style>
