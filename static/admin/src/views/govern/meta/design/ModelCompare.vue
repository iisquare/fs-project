<script>
export default {
  name: 'ModelCompare',
  props: {
    value: { type: Array, required: true }, // 抽取的字段属性
    form: { type: Object, required: true },
    type: { type: String, required: true }, // 对比类型
    config: { type: Object, required: true },
    menus: { type: Array, required: true } // 导航菜单
  },
  data () {
    return {}
  },
  render (h) {
    const _this = this
    const component = this.config.modelCompareComponents[this.type]?.component ?? this.config.modelCompareComponents.default.component
    return h(component, {
      props: { value: this.value, config: this.config, form: this.form, type: this.type, menus: this.menus },
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
