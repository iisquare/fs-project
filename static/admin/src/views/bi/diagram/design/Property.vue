<script>
export default {
  props: {
    value: { type: Object, default: null },
    flow: { type: Object, required: true },
    config: { type: Object, required: true },
    diagram: { type: Object, required: true },
    activeItem: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {}
  },
  render (h) {
    const _this = this
    let component = null
    if (!this.activeItem) {
      component = this.config.canvas.property
    } else if (['flow-edge', 'edge'].indexOf(this.activeItem.shape) !== -1) {
      component = this.config.edge.property
    } else {
      component = this.config.widgetByType(this.value.data.type).property
    }
    return h(component, {
      props: { value: this.value, flow: this.flow, config: this.config, diagram: this.diagram, activeItem: this.activeItem, tips: this.tips },
      on: {
        input (val) {
          _this.$emit('input', val)
        },
        'update:diagram' (val) {
          _this.$emit('update:diagram', val)
        },
        'update:tips' (val) {
          _this.$emit('update:tips', val)
        },
        'update:activeItem' (val) {
          _this.$emit('update:activeItem', val)
        }
      }
    })
  }
}
</script>
