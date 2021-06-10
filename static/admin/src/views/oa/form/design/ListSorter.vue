<template>
  <section class="fs-list-sorter">
    <draggable
      :value="value"
      @input="value => this.$emit('input', value)"
      group="sorter"
      handle=".fs-sorter-sort"
      chosenClass="fs-sorter-drop"
      animation="340">
      <a-row class="fs-sorter-item" type="flex" v-for="(item, index) in value" :key="index">
        <a-col flex="32px"><a-button type="link" icon="deployment-unit" class="fs-sorter-sort" /></a-col>
        <a-col flex="1">
          <a-select v-model="item.field" placeholder="请选择字段">
            <a-select-option :value="v.field" :key="v.id" v-for="v in fields">{{ v.label }}</a-select-option>
          </a-select>
        </a-col>
        <a-col flex="5px"></a-col>
        <a-col flex="1">
          <a-radio-group v-model="item.direction">
            <a-radio-button :value="v.value" :key="k" v-for="(v, k) in directions">{{ v.label }}</a-radio-button>
          </a-radio-group>
        </a-col>
        <a-col flex="32px" class="fs-sorter-delete"><a-icon @click="itemDelete(item, index)" type="minus-circle" /></a-col>
      </a-row>
    </draggable>
    <a-row class="fs-list-bottom" type="flex">
      <a-col>
        <a-space>
          <a-button @click="itemAdd" icon="plus-circle" type="link">添加排序规则</a-button>
          <a-button @click="itemClear" icon="delete" type="link">清空排序规则</a-button>
        </a-space>
      </a-col>
    </a-row>
  </section>
</template>

<script>
import draggable from 'vuedraggable'

export default {
  name: 'ListSorter',
  components: { draggable },
  props: {
    value: { type: Array, required: true },
    fields: { type: Array, required: true },
    config: { type: Object, required: true }
  },
  data () {
    return {
      directions: [{
        label: '升序', value: 'asc'
      }, {
        label: '降序', value: 'desc'
      }]
    }
  },
  methods: {
    itemData () {
      return { field: '', direction: this.directions[0].value }
    },
    itemAdd () {
      this.value.push(this.itemData())
    },
    itemDelete (item, index) {
      this.value.splice(index, 1)
    },
    itemClear () {
      this.$emit('input', [])
    }
  },
  mounted () {}
}
</script>

<style lang="less" scoped>
.fs-list-sorter {
  min-width: 350px;
  overflow: auto;
  .fs-list-bottom {
    margin-top: 15px;
    padding-top: 10px;
    border-top: solid 1px #cbcccc;
  }
  .ant-select {
    width: 160px;
  }
}
.fs-sorter-item {
  padding: 3px 0px 3px 0px;
  border:dashed 1px white;
  .fs-sorter-sort {
    color: lightslategray;
    cursor: move;
  }
  .fs-sorter-delete {
    text-align: center;
    line-height: 32px;
    vertical-align: middle;
    color: rgb(211, 69, 69);
  }
}
.fs-sorter-drop {
  border:dashed 1px lightblue;
}
</style>
