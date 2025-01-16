<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { onMounted, ref, type PropType } from 'vue';
import type { DropdownInstance, TreeInstance, TableInstance } from 'element-plus'
import UIUtil from '@/utils/UIUtil';
import DataUtil from '@/utils/DataUtil';

const model = defineModel()
const table = defineModel('table', { type: Object as PropType<TableInstance>, required: false })

const dropdown = ref<DropdownInstance>()
const handleSubmit = () => {
  model.value = (function transform (data: any) {
    const result: any = []
    data && data.map((item: any) => {
      const column = columnCache[item.id]
      column.fixed = item.fixed
      column.hide = !treeCheckedKeys.value.includes(item.id)
      const children = transform(item.children)
      if (children && item.children.length > 0) column.children = children
      result.push(column)
    })
    return result
  })(treeData.value)
  dropdown.value?.handleClose()
}
const handleReset = () => {
  treeData.value = JSON.parse(JSON.stringify(treeCache.value))
  treeCheckedKeys.value = UIUtil.treeIds(treeData.value, (item: any) => !item.checked)
}
const handleCancel = () => {
  dropdown.value?.handleClose()
}
const handleFixed = () => {
  (function cancel(data) {
    data && data.map((item: any) => {
      item.fixed = false
      cancel(item.children)
    })
  })(treeData.value)
}

const tree = ref<TreeInstance>()
const treeData = ref([])
const treeCheckedKeys: any = ref([])
const columnCache: any = ref({})
const treeCache = ref([])
const handleAllowDrop = (draggingNode: any, dropNode: any, type: any) => {
  return type !== 'inner' && draggingNode.parent === dropNode.parent
}

onMounted(() => {
  treeCache.value = (function cache (columns: any, parentId: string = '') {
    const result: any = []
    columns && columns.map((column: any, index: any) => {
      const item: any = {}
      item.id = parentId ? parentId + '-' + index : index
      item.label = column.label
      item.fixed = column.fixed
      item.checked = !column.hide
      if (item.fixed instanceof Boolean && item.fixed) {
        item.fixed = 'left'
      }
      item.parentId = parentId
      const children = cache(column.children, item.id)
      if (children) item.children = children
      result.push(item)
      columnCache[item.id] = column
    })
    return result
  })(model.value)
  handleReset()
})
</script>

<template>
  <el-dropdown ref="dropdown" placement="bottom-end" trigger="click" popper-class="fs-table-dropdown">
    <el-button :icon="ElementPlusIcons.Operation" circle title="设置表头列" />
    <template #dropdown>
      <el-space class="header flex-end" spacer="/">
        <el-button link @click="tree?.setCheckedKeys(treeCheckedKeys = UIUtil.treeIds(treeData))">全选</el-button>
        <el-button link @click="tree?.setCheckedKeys(treeCheckedKeys = DataUtil.removeArrayItem(UIUtil.treeIds(treeData), treeCheckedKeys))">反选</el-button>
        <el-button link @click="handleFixed">取消固定</el-button>
      </el-space>
      <el-tree
        ref="tree"
        :data="treeData"
        node-key="id"
        draggable
        default-expand-all
        show-checkbox
        :check-strictly="true"
        :expand-on-click-node="false"
        :default-checked-keys="treeCheckedKeys"
        @check="(node, data) => treeCheckedKeys = data.checkedKeys"
        :allow-drop="handleAllowDrop">
        <template #default="{ node, data }">
          <div class="tree-item">
            <span>{{ node.label }}</span>
            <el-space>
              <LayoutIcon name="layout.fixedLeft" :class="[data.fixed === 'left' && 'checked']" @click.stop="data.fixed = 'left'" />
              <LayoutIcon name="layout.fixedRight"  :class="[data.fixed === 'right' && 'checked']" @click.stop="data.fixed = 'right'" />
            </el-space>
          </div>
        </template>
      </el-tree>
      <el-space class="footer flex-center" size="large">
        <el-button type="primary" size="small" @click="handleSubmit">确认</el-button>
        <el-button type="warning" size="small" @click="handleReset">重置</el-button>
        <el-button size="small" @click="handleCancel">取消</el-button>
      </el-space>
    </template>
  </el-dropdown>
</template>

<style lang="scss" scoped>
.header, .footer {
  width: 100%;
  height: 35px;
  padding: 0 15px;
  box-sizing: border-box;
}
.header {
  border-bottom: solid 1px var(--fs-layout-border-color);
}
.footer {
  border-top: solid 1px var(--fs-layout-border-color);
}
.tree-item {
  width: 100%;
  padding-right: 15px;
  @include flex-between();
  .checked {
    color: var(--el-color-primary);
  }
}
</style>
