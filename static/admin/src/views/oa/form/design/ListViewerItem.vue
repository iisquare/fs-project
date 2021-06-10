<template>
  <section :class="['fs-list-viewer-item']">
    <draggable
      :value="value"
      @input="value => this.$emit('input', value)"
      :group="group"
      handle=".fs-viewer-sort"
      @choose="() => this.dragging = true"
      @unchoose="() => this.dragging = false"
      animation="340">
      <div :class="['fs-list-viewer-node']" :key="item.field" v-for="(item, index) in value">
        <a-row type="flex">
          <a-col flex="32px"><a-button type="link" icon="deployment-unit" class="fs-viewer-sort" /></a-col>
          <a-col class="fs-list-check"><a-checkbox v-model="item.enabled">{{ indent }}{{ item.label }}</a-checkbox></a-col>
        </a-row>
        <list-viewer-item
          v-if="item.children"
          v-model="item.children"
          :fields="fields"
          :config="config"
          :group="'viewer-' + index + '-' + item.field"
          indent="" />
      </div>
    </draggable>
  </section>
</template>

<script>
import draggable from 'vuedraggable'

export default {
  name: 'ListViewerItem',
  components: { draggable },
  props: {
    value: { type: Array, required: true },
    fields: { type: Array, required: true },
    config: { type: Object, required: true },
    indent: { type: String, default: '' },
    group: { type: String, required: true }
  },
  data () {
    return {
      dragging: false
    }
  },
  methods: {},
  mounted () {}
}
</script>

<style lang="less" scoped>
.fs-list-viewer-item {
  .fs-list-viewer-node {
    .fs-viewer-sort {
      color: lightslategray;
      cursor: move;
    }
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
