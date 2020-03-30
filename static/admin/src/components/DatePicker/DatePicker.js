import DatePicker from 'ant-design-vue/es/date-picker'
import moment from 'moment'

export default {
  name: 'SDatePicker',
  components: { DatePicker },
  props: Object.assign({}, DatePicker.props, {
    value: String
  }),
  data () {
    return {
      mv: null
    }
  },
  watch: {
    value (value) {
      this.mv = value ? moment(value, this.format) : null
    }
  },
  methods: {
    change () {
      arguments[0] = arguments[0] ? arguments[0].format(this.format) : ''
      this.$emit('change', ...arguments)
      this.$emit('input', arguments[0])
    },
    ok () {
      arguments[0] = arguments[0] ? arguments[0].format(this.format) : ''
      this.$emit('ok', ...arguments)
      this.$emit('input', arguments[0])
    }
  },
  created () {
    if (this.value) {
      this.mv = moment(this.value, this.format)
    }
  },
  render () {
    const data = { props: {}, on: {} }
    for (const key in DatePicker.props) {
      if (key === 'value') continue
      const value = this[key]
      if (typeof value === 'undefined') continue
      data.props[key] = value
    }
    data.props.value = this.mv
    for (const key of ['change', 'ok']) {
      data.on[key] = this[key]
    }
    return (<DatePicker {...data} />)
  }
}
