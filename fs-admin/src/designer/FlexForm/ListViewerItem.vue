<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue'
import draggable from 'vuedraggable'

const model: any = defineModel()
const {
  fields = [] as any,
  config = {} as any,
  indent = '',
  group = 'viewer',
} = defineProps<{
  fields?: any,
  config?: any,
  indent?: string,
  group?: string,
}>()

// vuedraggable 需要稳定的唯一键，无法直接修改业务数据，因此借助 WeakMap 生成
const keyMap = new WeakMap<object, string>()
let sequence = 0
const itemKey = (item: any) => {
  if (!keyMap.has(item)) keyMap.set(item, `viewer-${++sequence}`)
  return keyMap.get(item) as string
}
</script>

<template>
  <section class="fs-list-viewer-item">
    <draggable
      v-model="model"
      :item-key="itemKey"
      :group="group"
      handle=".fs-viewer-sort"
      :animation="340">
      <template #item="{ element, index }">
        <div class="fs-list-viewer-node">
          <div class="fs-list-viewer-line">
            <el-button link class="fs-viewer-sort" :icon="ElementPlusIcons.Rank" />
            <el-checkbox v-model="element.enabled">{{ indent }}{{ element.label }}</el-checkbox>
          </div>
          <ListViewerItem
            v-if="element.children"
            v-model="element.children"
            :fields="fields"
            :config="config"
            :group="`viewer-${index}-${element.field}`"
            indent="" />
        </div>
      </template>
    </draggable>
  </section>
</template>

<style lang="scss" scoped>
.fs-list-viewer-item {
  .fs-list-viewer-line {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    padding: 3px 0px;
  }
  .fs-viewer-sort {
    color: lightslategray;
    cursor: move;
  }
}
</style>
