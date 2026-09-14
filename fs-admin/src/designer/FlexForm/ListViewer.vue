<script setup lang="ts">
import ListViewerItem from './ListViewerItem.vue'

const model: any = defineModel()
const {
  fields = [] as any,
  config = {} as any,
  indent = '',
} = defineProps<{
  fields?: any,
  config?: any,
  indent?: string,
  group?: string,
}>()

const selectAll = (items: any) => {
  items.forEach((item: any) => {
    item.enabled = true
    if (item.children) selectAll(item.children)
  })
}

const reverseAll = (items: any) => {
  items.forEach((item: any) => {
    item.enabled = !item.enabled
    if (item.children) reverseAll(item.children)
  })
}
</script>

<template>
  <section class="fs-list-viewer">
    <div class="fs-list-viewer-container">
      <ListViewerItem
        v-model="model"
        :fields="fields"
        :config="config"
        group="viewer"
        :indent="indent" />
    </div>
    <div class="fs-list-bottom">
      <el-space>
        <el-button link type="primary" @click="selectAll(model)">全选</el-button>
        <el-button link type="primary" @click="reverseAll(model)">反选</el-button>
      </el-space>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.fs-list-viewer {
  min-width: 350px;
  overflow: auto;
  .fs-list-bottom {
    margin-top: 15px;
    padding-top: 10px;
    border-top: solid 1px #cbcccc;
  }
  .fs-list-viewer-container {
    height: 300px;
    overflow-x: auto;
  }
}
</style>
