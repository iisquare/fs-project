<script>
export default {
  props: {
    value: { type: Object, default: null },
    config: { type: Object, required: true }
  },
  data () {
    return {}
  },
  render (h) {
    const _this = this
    const component = this.config.ruleByType(this.value.type)?.property ?? null
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
