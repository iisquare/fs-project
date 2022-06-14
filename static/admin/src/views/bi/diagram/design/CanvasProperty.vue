<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="diagram" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <div class="fs-property-title">基础信息</div>
        <a-form-model-item label="主键">{{ diagram.id }}</a-form-model-item>
        <a-form-model-item label="名称">{{ diagram.name }}</a-form-model-item>
        <div class="fs-property-title">画布设置</div>
        <a-form-model-item label="启用网格"><a-switch v-model="diagram.options.grid" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'CanvasProperty',
  props: {
    value: { type: Object, default: null },
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
      return this.config.canvas.options()
    }
  },
  watch: {
    'diagram.options.grid' () {
      this.flow.toggleGrid(this.diagram.options.grid)
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        grid: obj.options?.grid ?? this.default.grid
      }
      return Object.assign({}, obj, { options })
    }
  },
  mounted () {
    this.$emit('update.diagram', this.formatted(this.diagram))
  }
}
</script>

<style lang="less" scoped>

</style>
