<script setup lang="ts">
import ListFilterItem from './ListFilterItem.vue'

const model: any = defineModel()
const {
  fields = [] as any,
  config = {} as any,
  indent = '',
} = defineProps<{
  fields?: any,
  config?: any,
  indent?: string,
}>()

const appendRelation = () => {
  model.value.push(config.generateFilterRelation())
}

const appendFilter = () => {
  model.value.push(config.generateFilterOperation())
}

const clear = () => {
  model.value = []
}
</script>

<template>
  <section class="fs-list-filter">
    <ListFilterItem v-model="model" :fields="fields" :config="config" :indent="indent" />
    <div class="fs-list-bottom">
      <el-space>
        <el-button link type="primary" @click="appendRelation">关系</el-button>
        <el-button link type="primary" @click="appendFilter">条件</el-button>
        <el-button link type="danger" @click="clear">清空</el-button>
      </el-space>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.fs-list-filter {
  min-width: 750px;
  overflow: auto;
  .fs-list-bottom {
    margin-top: 15px;
    padding-top: 10px;
    border-top: solid 1px #cbcccc;
  }
}
</style>
