<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <div class="fs-property-title">流程配置</div>
        <a-form-model-item label="并发度"><a-input-number v-model="diagram.options.concurrent" :min="0" /></a-form-model-item>
        <a-form-model-item label="并发策略">
          <a-select v-model="diagram.options.concurrency" placeholder="请选择并发策略">
            <a-select-option :value="item.value" v-for="item in config.concurrences" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="失败策略">
          <a-select v-model="diagram.options.failure" placeholder="请选择失败策略">
            <a-select-option :value="item.value" v-for="item in config.failures" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
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
        grid: obj.options?.grid ?? this.defaults.grid,
        concurrent: obj.options?.concurrent ?? this.defaults.concurrent,
        concurrency: obj.options?.concurrency ?? this.defaults.concurrency,
        failure: obj.options?.failure ?? this.defaults.failure
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
