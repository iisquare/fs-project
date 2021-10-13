<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="变量名称"><a-input v-model="value.options.arg" placeholder="为空时不创建新变量，直接展开" /></a-form-model-item>
        <a-form-model-item label="起始数值"><a-input-number v-model="value.options.start" /></a-form-model-item>
        <a-form-model-item label="步长数值"><a-input-number v-model="value.options.step" /></a-form-model-item>
        <a-form-model-item label="终止数值"><a-input-number v-model="value.options.end" /></a-form-model-item>
        <a-form-model-item label="除数因子"><a-input-number v-model="value.options.divisor" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'NumberGenerateConfigProperty',
  components: {
    SliceBasic: () => import('./SliceBasic')
  },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
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
    formatted (obj) {
      const options = {
        arg: obj.options.variable || this.defaults.variable,
        start: Number.isNaN(obj.options.start) ? this.defaults.start : this.defaults.start,
        step: Number.isNaN(obj.options.step) ? this.defaults.step : this.defaults.step,
        end: Number.isNaN(obj.options.end) ? this.defaults.end : this.defaults.end,
        divisor: Number.isNaN(obj.options.divisor) ? this.defaults.divisor : this.defaults.divisor
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
