<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic
          :value="value"
          @input="value => $emit('input', value)"
          :flow="flow"
          :config="config"
          :diagram="diagram"
          :activeItem="activeItem"
          @update:activeItem="val => $emit('update:activeItem', val)"
          :tips="tips"
          @update:tips="val => $emit('update:tips', val)" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="变量名称"><a-input v-model="value.data.options.arg" placeholder="为空时直接覆盖引用的变量" /></a-form-model-item>
        <a-form-model-item label="变量引用"><a-input v-model="value.data.options.reference" placeholder="配置参数的引用路径" /></a-form-model-item>
        <a-form-model-item label="后置偏移">
          <a-space><a-input-number v-model="value.data.options.offset" placeholder="微调" /><span>ms</span></a-space>
        </a-form-model-item>
        <a-form-model-item label="调整数值"><a-input-number v-model="value.data.options.value" placeholder="doValue" /></a-form-model-item>
        <a-form-model-item label="调整方式">
          <a-select v-model="value.data.options.method" placeholder="请选择调整方式" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in methods" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="调整字段">
          <a-select v-model="value.data.options.field" placeholder="请选择日期字段" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in fields" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="所在时区">
          <a-select v-model="value.data.options.timezone" placeholder="请选择所在时区" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in config.timezones" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="所在地区">
          <a-select v-model="value.data.options.locale" placeholder="请选择所在地区" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in config.locales" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'CalendarOffsetConfigProperty',
  components: {
    SliceBasic: () => import('./SliceBasic')
  },
  props: {
    value: { type: Object, required: true },
    flow: { type: Object, required: true },
    config: { type: Object, required: true },
    diagram: { type: Object, required: true },
    activeItem: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {
      methods: [{
        'label': 'METHOD.NOTHING', 'value': ''
      }, {
        'label': 'METHOD.SET', 'value': 'set'
      }, {
        'label': 'METHOD.ADD', 'value': 'add'
    }],
      fields: [{
        'label': 'Calendar.NOTHING', 'value': '0'
      }, {
        'label': 'Calendar.YEAR', 'value': '1'
      }, {
        'label': 'Calendar.MONTH', 'value': '2'
      }, {
        'label': 'Calendar.WEEK_OF_YEAR', 'value': '3'
      }, {
        'label': 'Calendar.WEEK_OF_MONTH', 'value': '4'
      }, {
        'label': 'Calendar.DAY_OF_MONTH', 'value': '5'
      }, {
        'label': 'Calendar.DAY_OF_YEAR', 'value': '6'
      }, {
        'label': 'Calendar.DAY_OF_WEEK', 'value': '7'
      }, {
        'label': 'Calendar.DAY_OF_WEEK_IN_MONTH', 'value': '8'
      }, {
        'label': 'Calendar.HOUR', 'value': '10'
      }, {
        'label': 'Calendar.HOUR_OF_DAY', 'value': '11'
      }, {
        'label': 'Calendar.MINUTE', 'value': '12'
      }, {
        'label': 'Calendar.SECOND', 'value': '13'
      }, {
        'label': 'Calendar.MILLISECOND', 'value': '14'
      }]
    }
  },
  computed: {
    defaults () {
      return this.config.widgetDefaults(this.value.data.type)
    }
  },
  watch: {
    'activeItem.id': {
      handler () {
        this.$emit('input', this.formatted(this.value))
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        arg: obj.data.options.arg || this.defaults.arg,
        reference: obj.data.options.reference || this.defaults.reference,
        pattern: obj.data.options.pattern || this.defaults.pattern,
        timezone: obj.data.options.timezone || this.defaults.timezone,
        locale: obj.data.options.locale || this.defaults.locale
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
