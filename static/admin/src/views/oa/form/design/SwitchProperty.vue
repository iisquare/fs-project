<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="组件属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称"><a-input v-model="value.label" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="字段"><a-input v-model="value.options.field" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="默认值"><a-switch v-model="value.options.value" /></a-form-model-item>
        <a-form-model-item label="启用文案"><a-input v-model="value.options.txtChecked" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="关闭文案"><a-input v-model="value.options.txtUnChecked" auto-complete="on" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'SwitchProperty',
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
        field: obj.options.field || this.defaults.field || obj.id,
        value: obj.options.value || this.defaults.value,
        txtChecked: obj.options.txtChecked || this.defaults.txtChecked,
        txtUnChecked: obj.options.txtUnChecked || this.defaults.txtUnChecked
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
