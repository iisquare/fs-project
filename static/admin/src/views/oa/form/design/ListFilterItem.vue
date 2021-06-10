<template>
  <section class="fs-list-filter-item">
    <div class="fs-list-filter-node" :key="item.id" v-for="(item, index) in value">
      <a-row type="flex" v-if="item.type === 'RELATION'">
        <a-col class="fs-list-check"><a-checkbox v-model="item.enabled">{{ indent }}</a-checkbox></a-col>
        <a-col class="fs-list-label">关系</a-col>
        <a-col>
          <a-select v-model="item.value" placeholder="请选择关系">
            <a-select-option :value="v.value" :key="v.value" v-for="v in config.relations">{{ v.label }}</a-select-option>
          </a-select>
        </a-col>
        <a-col>
          <a-button-group>
            <a-button type="link" @click="() => appendRelation(index, item)">关系</a-button>
            <a-button type="link" @click="() => appendFilter(index, item)">条件</a-button>
            <a-button type="link" @click="() => remove(index, item)">删除</a-button>
            <a-button type="link" @click="() => clear(index, item)">清空</a-button>
          </a-button-group>
        </a-col>
      </a-row>
      <list-filter-item
        v-if="item.type === 'RELATION'"
        v-model="item.children"
        :fields="fields"
        :config="config"
        :indent="indent + '-------|'" />
      <a-row type="flex" v-if="item.type === 'FILTER'">
        <a-col class="fs-list-check"><a-checkbox v-model="item.enabled">{{ indent }}</a-checkbox></a-col>
        <a-col class="fs-list-label">字段</a-col>
        <a-col>
          <a-select v-model="item.field" placeholder="请选择字段">
            <a-select-option :value="v.field" :key="v.id" v-for="v in fields">{{ v.label }}</a-select-option>
          </a-select>
        </a-col>
        <a-col class="fs-list-label">条件</a-col>
        <a-col>
          <a-select v-model="item.operation" placeholder="请选择条件">
            <a-select-option :value="v.value" :key="v.value" v-for="v in config.filters">{{ v.label }}</a-select-option>
          </a-select>
        </a-col>
        <a-col class="fs-list-label">值</a-col>
        <a-col>
          <a-input v-model="item.value" />
        </a-col>
        <a-col>
          <a-button-group>
            <a-button type="link" @click="() => remove(index, item)">删除</a-button>
          </a-button-group>
        </a-col>
      </a-row>
    </div>
  </section>
</template>

<script>
export default {
  name: 'ListFilterItem',
  props: {
    value: { type: Array, required: true },
    fields: { type: Array, required: true },
    config: { type: Object, required: true },
    indent: { type: String, default: '' }
  },
  data () {
    return {}
  },
  methods: {
    appendRelation (index, item) {
      item.children.push(this.config.generateFilterRelation())
    },
    appendFilter (index, item) {
      item.children.push(this.config.generateFilterOperation())
    },
    remove (index, item) {
      this.value.splice(index, 1)
    },
    clear (index, item) {
      this.$set(item, 'children', [])
    }
  },
  mounted () {}
}
</script>

<style lang="less" scoped>
.fs-list-filter-item {
  .fs-list-filter-node {
    padding: 3px 0px;
  }
  .fs-list-check {
    margin-right: 3px;
    line-height: 32px;
  }
  .fs-list-label {
    line-height: 32px;
    text-align: center;
    padding: 0px 12px;
  }
  .ant-select {
    width: 160px;
  }
}
</style>
