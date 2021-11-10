<script>
export default {
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    form: { type: Object, required: true }
  },
  data () {
    return {}
  },
  render (h) {
    const _this = this
    const widget = this.config.widgetByType(this.form.type)
    if (!widget) return null
    const component = widget.property
    return h(component, {
      props: { value: this.value, config: this.config, form: this.form },
      on: {
        input (val) {
          _this.$emit('input', val)
        }
      }
    }, this.$slots.default)
  }
}
</script>
