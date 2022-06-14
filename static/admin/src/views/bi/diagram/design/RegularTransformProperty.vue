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
        <a-form-model-item label="变量名称"><a-input v-model="value.data.options.arg" placeholder="为空时直接覆盖引用的变量" /></a-form-model-item>
        <a-form-model-item label="变量引用"><a-input v-model="value.data.options.reference" placeholder="数据字段的引用路径" /></a-form-model-item>
        <a-form-model-item label="正则表达式"><a-textarea v-model="value.data.options.pattern" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'RegularTransformProperty',
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
        arg: obj.data.options.arg || this.defaults.arg,
        reference: obj.data.options.reference || this.defaults.reference,
        pattern: obj.data.options.pattern || this.defaults.pattern
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
