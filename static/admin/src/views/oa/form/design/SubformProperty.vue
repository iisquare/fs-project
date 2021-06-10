<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="组件属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称"><a-input v-model="value.label" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="字段"><a-input v-model="value.options.field" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="关联标识"><a-input v-model="value.options.formId" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="列表字段"><a-input v-model="value.options.column" auto-complete="on" placeholder="采用英文逗号分隔" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
    <a-tab-pane key="rule" tab="校验规则">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <subform-rule v-model="value.options" :config="config" />
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import SubformRule from './SubformRule'

export default {
  name: 'TextProperty',
  components: { SubformRule },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    defaults () {
      return this.config.widgetDefaults(this.value.type)
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        field: obj.options.field || this.defaults.field || obj.id,
        formId: obj.options.formId || this.defaults.formId,
        column: obj.options.column || this.defaults.column
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  },
  mounted () {
    this.$emit('input', this.formatted(this.value))
  }
}
</script>

<style lang="less" scoped>

</style>
