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
    mv (value) {
      this.$emit('input', value ? value.format(this.format) : '')
    }
  },
  methods: {
    change () {
      this.mv = arguments[0]
      this.$emit('change', ...arguments)
    },
    ok () {
      this.mv = arguments[0]
      this.$emit('ok', ...arguments)
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
