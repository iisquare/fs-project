<script>
export default {
  name: 'SourceArg',
  props: {
    value: { type: Object, required: true },
    form: { type: Object, required: true },
    config: { type: Object, required: true },
    menus: { type: Array, required: true } // 导航菜单
  },
  data () {
    return {}
  },
  render (h) {
    const _this = this
    const component = this.config.sourceArgComponents[this.form.type]?.component ?? this.config.sourceArgComponents.default.component
    return h(component, {
      props: { value: this.value, config: this.config, form: this.form, menus: this.menus },
      on: {
        input (val) {
          _this.$emit('input', val)
        },
        'update:menus' (val) {
          _this.$emit('update:menus', val)
        }
      }
    })
  }
}
</script>
