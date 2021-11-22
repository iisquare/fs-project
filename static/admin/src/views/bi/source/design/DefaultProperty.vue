<template>
  <a-tabs default-active-key="basic" :animated="false" tabPosition="left">
    <a-tab-pane key="basic" tab="基础信息"><slot></slot></a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'DefaultProperty',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    form: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    defaults () {
      return this.config.widgetDefaults(this.form.type)
    }
  },
  methods: {
    formatted (obj) {
      const options = {}
      const result = Object.assign({}, obj, options)
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
