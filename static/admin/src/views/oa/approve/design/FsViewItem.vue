<template>
  <section class="fs-widget-container">
    <div :class="['fs-form-item']" v-for="item in widgets" :key="item.id">
      <a-form-model-item :label="item.label" v-if="item.type === 'txt'">{{ item.options.txt }}</a-form-model-item>
      <a-form-model-item :label="item.label" v-else-if="item.type === 'html'"><span v-html="item.options.html"></span></a-form-model-item>
      <a-divider :dashed="item.options.dashed" :type="item.options.type" :orientation="item.options.orientation" v-else-if="item.type === 'divider'">{{ item.label }}</a-divider>
      <a-row v-else-if="item.type === 'grid'">
        <a-col :span="c.span" :key="c.id" v-for="c in item.options.items">
          <fs-view-item v-model="value" :config="config" :widgets="c.widgets" />
        </a-col>
        <a-col span="24" v-if="item.options.items.length === 0">
          <a-alert message="注意：当前栅格中未设置任何列" banner />
        </a-col>
      </a-row>
      <a-form-model-item :label="item.label" v-else-if="item.type === 'subform'">
        <fs-subform v-model="value[item.options.field]" :config="config" :subform="item" :editable="false" v-if="value[item.options.field]" />
      </a-form-model-item>
      <a-form-model-item :label="item.label" v-else>{{ value[item.options.field] }}</a-form-model-item>
    </div>
  </section>
</template>

<script>
export default {
  name: 'FsViewItem',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    widgets: { type: Array, required: true }
  },
  data () {
    return {
    }
  },
  methods: {
  },
  beforeCreate () {
    // 处理组件循环引用
    this.$options.components.FsSubform = require('./FsSubform').default
  }
}
</script>

<style lang="less" scoped>
</style>
