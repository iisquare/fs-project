<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <div class="fs-property-title">
          <span>基础信息</span>
          <a-space class="fs-property-action">
            <a-popconfirm title="确认删除该元素?" placement="bottomRight" @confirm="remove">
              <a-icon type="delete" class="fs-action-delete" />
            </a-popconfirm>
          </a-space>
        </div>
        <a-form-model-item label="来源">{{ value.source.cell }}</a-form-model-item>
        <a-form-model-item label="目标">{{ value.target.cell }}</a-form-model-item>
        <div class="fs-property-title">连接信息</div>
        <a-form-model-item label="名称"><a-input v-model="value.data.title" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="备注"><a-textarea v-model="value.data.description" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'EdgeProperty',
  props: {
    value: { type: Object, required: true },
    flow: { type: Object, required: true },
    config: { type: Object, required: true },
    diagram: { type: Object, required: true },
    activeItem: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {}
  },
  computed: {
    defaults () {
      return this.config.edge.options()
    }
  },
  watch: {
    value: {
      handler () {
        const edge = this.flow.graph.getCellById(this.value.id)
        edge.setData(this.value.data)
        edge.setLabels(this.value.data.title)
      },
      deep: true
    }
  },
  methods: {
    remove () {
      this.flow.graph.removeCell(this.value.id)
      this.$emit('update:activeItem', null)
    },
    formatted (obj) {
      const options = {}
      return Object.assign({}, obj, { options })
    }
  },
  mounted () {
    this.$emit('input', this.formatted(this.value))
  }
}
</script>

<style lang="less" scoped>

</style>
