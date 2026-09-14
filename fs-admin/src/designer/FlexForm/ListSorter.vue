<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue'
import draggable from 'vuedraggable'

const model: any = defineModel()
const {
  fields = [] as any,
  config = {} as any,
} = defineProps<{
  fields?: any,
  config?: any,
}>()

const directions = [{
  label: '升序', value: 'asc'
}, {
  label: '降序', value: 'desc'
}]

// vuedraggable 需要稳定的唯一键，无法直接修改业务数据，因此借助 WeakMap 生成
const keyMap = new WeakMap<object, string>()
let sequence = 0
const itemKey = (item: any) => {
  if (!keyMap.has(item)) keyMap.set(item, `sorter-${++sequence}`)
  return keyMap.get(item) as string
}

const itemAdd = () => {
  model.value.push({ field: '', direction: directions[0].value })
}

const itemDelete = (index: number) => {
  model.value.splice(index, 1)
}

const itemClear = () => {
  model.value = []
}
</script>

<template>
  <section class="fs-list-sorter">
    <draggable
      v-model="model"
      :item-key="itemKey"
      group="sorter"
      handle=".fs-sorter-sort"
      ghost-class="fs-sorter-ghost"
      :animation="340">
      <template #item="{ element, index }">
        <div class="fs-sorter-item">
          <el-button link class="fs-sorter-sort" :icon="ElementPlusIcons.Rank" />
          <el-select v-model="element.field" placeholder="请选择字段" class="fs-sorter-input">
            <el-option :value="v.field" :key="v.id" v-for="v in fields" :label="v.label" />
          </el-select>
          <el-radio-group v-model="element.direction">
            <el-radio-button :value="v.value" :key="k" v-for="(v, k) in directions">{{ v.label }}</el-radio-button>
          </el-radio-group>
          <el-button link type="danger" :icon="ElementPlusIcons.Remove" @click="itemDelete(index)" />
        </div>
      </template>
    </draggable>
    <div class="fs-list-bottom">
      <el-space>
        <el-button link type="primary" :icon="ElementPlusIcons.Plus" @click="itemAdd">添加排序规则</el-button>
        <el-button link type="danger" :icon="ElementPlusIcons.Delete" @click="itemClear">清空排序规则</el-button>
      </el-space>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.fs-list-sorter {
  min-width: 360px;
  overflow: auto;
  .fs-list-bottom {
    margin-top: 15px;
    padding-top: 10px;
    border-top: solid 1px #cbcccc;
  }
  .fs-sorter-input {
    width: 160px;
    margin: 0px 6px;
  }
}
.fs-sorter-item {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  padding: 3px 0px;
  border: dashed 1px white;
  .fs-sorter-sort {
    color: lightslategray;
    cursor: move;
  }
  .fs-sorter-ghost {
    border: dashed 1px lightblue;
  }
}
.fs-sorter-ghost {
  border: dashed 1px lightblue;
  opacity: 0.6;
}
</style>
