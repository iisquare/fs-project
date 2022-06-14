<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic
          :value="value"
          @input="value => $emit('input', value)"
          :flow="flow"
          :config="config"
          :diagram="diagram"
          :activeItem="activeItem"
          @update:activeItem="val => $emit('update:activeItem', val)"
          :tips="tips"
          @update:tips="val => $emit('update:tips', val)" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="变量名称"><a-input v-model="value.data.options.arg" placeholder="为空时不创建新变量，直接展开" /></a-form-model-item>
        <a-form-model-item label="起始数值"><a-input-number v-model="value.data.options.start" /></a-form-model-item>
        <a-form-model-item label="步长数值"><a-input-number v-model="value.data.options.step" /></a-form-model-item>
        <a-form-model-item label="终止数值"><a-input-number v-model="value.data.options.end" /></a-form-model-item>
        <a-form-model-item label="除数因子"><a-input-number v-model="value.data.options.divisor" /></a-form-model-item>
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
      return this.config.widgetDefaults(this.value.data.type)
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
        arg: obj.data.options.variable || this.defaults.variable,
        start: Number.isNaN(obj.data.options.start) ? this.defaults.start : obj.data.options.start,
        step: Number.isNaN(obj.data.options.step) ? this.defaults.step : obj.data.options.step,
        end: Number.isNaN(obj.data.options.end) ? this.defaults.end : obj.data.options.end,
        divisor: Number.isNaN(obj.data.options.divisor) ? this.defaults.divisor : obj.data.options.divisor
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
