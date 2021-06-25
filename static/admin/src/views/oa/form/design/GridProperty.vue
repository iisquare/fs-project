<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="组件属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="栅格总数">{{ sum }} / {{ total }}</a-form-model-item>
      </a-form-model>
      <a-divider>栅格配置</a-divider>
      <section>
        <draggable v-model="value.options.items" group="grid" handle=".fs-grid-sort" chosenClass="fs-grid-chosen" animation="340">
          <a-row class="fs-grid-item" type="flex" v-for="(item, index) in value.options.items" :key="index">
            <a-col flex="32px" class="fs-grid-sort"><a-button type="link" icon="deployment-unit" /></a-col>
            <a-col flex="1" class="fs-grid-number"><a-input-number v-model="item.span" :min="0" :max="total" /></a-col>
            <a-col flex="1" class="fs-grid-index">{{ index }}</a-col>
            <a-col flex="32px" class="fs-grid-delete"><a-icon @click="removeItem(item, index)" type="minus-circle" /></a-col>
          </a-row>
        </draggable>
        <a-row class="fs-grid-btn">
          <a-col :span="24"><a-button @click="addItem" icon="plus-circle" type="link">添加列</a-button></a-col>
        </a-row>
      </section>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import draggable from 'vuedraggable'

export default {
  name: 'GridProperty',
  components: { draggable },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {
      total: 24
    }
  },
  computed: {
    sum () {
      return this.value.options.items.reduce((accumulator, currentValue, index, array) => {
        return accumulator + currentValue.span
      }, 0)
    },
    defaults () {
      return this.config.widgetDefaults(this.value.type)
    }
  },
  watch: {
    'activeItem.id': {
      handler () {
        this.$emit('input', this.formatted(this.value))
      },
      immediate: true
    }
  },
  methods: {
    addItem () {
      this.value.options.items.push(this.config.generateGridItem())
    },
    removeItem (item, index) {
      this.value.options.items.splice(index, 1)
    },
    formatted (obj) {
      const options = {
        items: obj.options.items || []
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>
.fs-grid-item {
  padding: 3px 0px 3px 0px;
  border:dashed 1px white;
  .fs-grid-number {
    padding-left: 15px;
  }
  .fs-grid-index {
    width: 100%;
    height: 32px;
    line-height: 32px;
    text-align: center;
    vertical-align: middle;
    padding: 0px 10px;
  }
  .fs-grid-sort button {
    color: lightslategray;
    cursor: move;
  }
  .fs-grid-delete {
    text-align: center;
    line-height: 32px;
    vertical-align: middle;
    color: rgb(211, 69, 69);
  }
}
.fs-grid-chosen {
  border:dashed 1px lightblue;
}
.fs-grid-btn {
  padding: 3px 0px 3px 0px;
}
</style>
