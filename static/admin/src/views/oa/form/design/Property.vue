<script>
export default {
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {}
  },
  render (h) {
    const _this = this
    const component = this.activeItem ? this.config.widgetByType(this.value.type).property : this.config.form.property
    return h(component, {
      props: { value: this.value, config: this.config },
      on: {
        input (val) {
          _this.$emit('input', val)
        }
      }
    })
  }
}
</script>
