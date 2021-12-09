<script>
export default {
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, default: null }
  },
  data () {
    return {}
  },
  render (h) {
    const component = this.config.widgetByType(this.value.type).element
    return h(component, {
      props: { value: this.value, config: this.config, activeItem: this.activeItem }
    })
  }
}
</script>
