<script>
export default {
  props: {
    value: { type: Object, required: true },
    bpmn: { type: Object, required: true },
    element: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {}
  },
  render (h) {
    const _this = this
    const config = this.bpmn.config
    const component = this.element ? config.elements[this.element.type].property : config.canvas.property
    return h(component, {
      props: { value: this.value, bpmn: this.bpmn, element: this.element },
      on: {
        input (val) {
          _this.$emit('input', val)
        }
      }
    })
  }
}
</script>
