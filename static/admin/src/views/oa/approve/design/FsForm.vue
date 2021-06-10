<template>
  <section class="fs-form">
    <a-form-model
      :ref="refForm"
      :model="value"
      :rules="rules"
      v-bind="formLayout">
      <fs-form-item v-model="value" :config="config" :widgets="frame.widgets" />
    </a-form-model>
  </section>
</template>

<script>
import FsFormItem from './FsFormItem'

export default {
  name: 'FsForm',
  components: { FsFormItem },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    frame: { type: Object, required: true }
  },
  data () {
    return {
      refForm: this.config.uuidForm()
    }
  },
  computed: {
    formLayout () { return this.config.exhibition.formLayout(this.frame) },
    rules () { return this.config.validator.generate(this.frame.widgets) }
  },
  methods: {
    validate (callback) {
      this.$refs[this.refForm].validate(valid => callback(valid))
    },
    formatted (obj) {
      const result = this.config.validator.format(this.frame.widgets, obj)
      return Object.assign({}, obj, result)
    }
  },
  mounted () {
    this.$emit('input', this.formatted(this.value))
  }
}
</script>

<style lang="less">
@import './design.less';
</style>
