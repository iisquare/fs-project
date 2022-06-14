<template>
  <section class="fs-flow-node">
    <a-icon class="fs-ui-icon" :component="icons[item.icon]" v-if="item.icon" />
    <div class="fs-ui-title">{{ item.title }}</div>
  </section>
</template>

<script>
import icons from '@/assets/icons'

export default {
  name: 'FlowNode',
  inject: ['getGraph', 'getNode'],
  data () {
    return {
      icons,
      item: {}
    }
  },
  methods: {
  },
  mounted () {
    const _this = this
    const node = this.getNode()
    this.$set(this, 'item', node.getData())
    node.on('change:data', ({ current }) => {
      this.$set(_this, 'item', node.getData())
    })
  }
}
</script>

<style lang="less" scoped>
.fs-flow-node {
  width: 100%;
  height: 100%;
  line-height: 100%;
  background: #fff;
  border: 1px solid #d9d9d9;
  padding: 3px 10px;
  border-radius: 10px;
  .fs-ui-icon {
    font-size: 14px;
    vertical-align: middle;
  }
  .fs-ui-title {
    float: right;
    width: calc(100% - 20px);
    height: 100%;
    line-height: 100%;
    padding-left: 10px;
    text-align: center;
    font-size: 14px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    border-left: 1px dashed #d9d9d9;
  }
}
.fs-flow-node::before, .fs-ui-title::before {
  display: inline-block;
  content: "";
  height: 100%;
  vertical-align: middle;
}
</style>
