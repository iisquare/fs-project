<template>
  <section class="fs-list-viewer">
    <div class="fs-list-viewer-container">
      <list-viewer-item
        :value="value"
        @input="val => $emit('input', val)"
        :fields="fields"
        :config="config"
        group="viewer"
        :indent="indent" />
    </div>
    <a-row class="fs-list-bottom" type="flex">
      <a-col>
        <a-space>
          <a-button type="link" @click="selectAll(value)">全选</a-button>
          <a-button type="link" @click="reverseAll(value)">反选</a-button>
        </a-space>
      </a-col>
    </a-row>
  </section>
</template>

<script>
import ListViewerItem from './ListViewerItem'

export default {
  name: 'ListViewer',
  components: { ListViewerItem },
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
    selectAll (value) {
      value.forEach(item => {
        item.enabled = true
        if (item.children) this.selectAll(item.children)
      })
    },
    reverseAll (value) {
      value.forEach(item => {
        item.enabled = !item.enabled
        if (item.children) this.reverseAll(item.children)
      })
    }
  },
  mounted () {}
}
</script>

<style lang="less" scoped>
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
