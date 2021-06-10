<template>
  <section class="fs-list-filter">
    <list-filter-item :value="value" @input="val => $emit('input', val)" :fields="fields" :config="config" :indent="indent" />
    <a-row class="fs-list-bottom" type="flex">
      <a-col>
        <a-space>
          <a-button type="link" @click="() => appendRelation(value)">关系</a-button>
          <a-button type="link" @click="() => appendFilter(value)">条件</a-button>
          <a-button type="link" @click="() => clear(value)">清空</a-button>
        </a-space>
      </a-col>
    </a-row>
  </section>
</template>

<script>
import ListFilterItem from './ListFilterItem'

export default {
  name: 'ListFilter',
  components: { ListFilterItem },
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
    appendRelation (item) {
      this.value.push(this.config.generateFilterRelation())
    },
    appendFilter (item) {
      this.value.push(this.config.generateFilterOperation())
    },
    clear (item) {
      this.$emit('input', [])
    }
  },
  mounted () {}
}
</script>

<style lang="less" scoped>
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
