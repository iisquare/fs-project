<script setup lang="ts">
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

const appendRelation = (item: any) => {
  item.children.push(config.generateFilterRelation())
}

const appendFilter = (item: any) => {
  item.children.push(config.generateFilterOperation())
}

const remove = (index: number) => {
  model.value.splice(index, 1)
}

const clear = (item: any) => {
  item.children = []
}
</script>

<template>
  <section class="fs-list-filter-item">
    <div class="fs-list-filter-node" :key="item.id" v-for="(item, index) in model">
      <div class="fs-list-line" v-if="item.type === 'RELATION'">
        <el-checkbox v-model="item.enabled" class="fs-list-check">{{ indent }}</el-checkbox>
        <span class="fs-list-label">关系</span>
        <el-select v-model="item.value" placeholder="请选择关系" class="fs-list-input">
          <el-option :value="v.value" :key="v.value" v-for="v in config.relations" :label="v.label" />
        </el-select>
        <el-space>
          <el-button link type="primary" @click="appendRelation(item)">关系</el-button>
          <el-button link type="primary" @click="appendFilter(item)">条件</el-button>
          <el-button link type="danger" @click="remove(index)">删除</el-button>
          <el-button link @click="clear(item)">清空</el-button>
        </el-space>
      </div>
      <ListFilterItem
        v-if="item.type === 'RELATION'"
        v-model="item.children"
        :fields="fields"
        :config="config"
        :indent="indent + '-------|'" />
      <div class="fs-list-line" v-if="item.type === 'FILTER'">
        <el-checkbox v-model="item.enabled" class="fs-list-check">{{ indent }}</el-checkbox>
        <span class="fs-list-label">字段</span>
        <el-select v-model="item.field" placeholder="请选择字段" class="fs-list-input">
          <el-option :value="v.field" :key="v.id" v-for="v in fields" :label="v.label" />
        </el-select>
        <span class="fs-list-label">条件</span>
        <el-select v-model="item.operation" placeholder="请选择条件" class="fs-list-input">
          <el-option :value="v.value" :key="v.value" v-for="v in config.filters" :label="v.label" />
        </el-select>
        <span class="fs-list-label">值</span>
        <el-input v-model="item.value" class="fs-list-input" />
        <el-button link type="danger" @click="remove(index)">删除</el-button>
      </div>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.fs-list-filter-item {
  .fs-list-filter-node {
    padding: 3px 0px;
  }
  .fs-list-line {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    padding: 3px 0px;
  }
  .fs-list-check {
    margin-right: 9px;
    white-space: nowrap;
  }
  .fs-list-label {
    line-height: 32px;
    text-align: center;
    padding: 0px 12px;
    white-space: nowrap;
  }
  .fs-list-input {
    width: 160px;
    flex: none;
    margin-right: 6px;
  }
}
</style>
