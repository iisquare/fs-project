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
        <a-form-model-item label="变量名称"><a-input v-model="value.data.options.arg" placeholder="date" /></a-form-model-item>
        <a-form-model-item label="固定日期"><a-input v-model="value.data.options.datetime" placeholder="默认为服务执行时间" /></a-form-model-item>
        <a-form-model-item label="日期格式"><a-input v-model="value.data.options.pattern" placeholder="仅在设定固定日期时有效" /></a-form-model-item>
        <a-form-model-item label="所在时区">
          <a-select v-model="value.data.options.timezone" placeholder="请选择所在时区" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in dagConfig.timezones" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="所在地区">
          <a-select v-model="value.data.options.locale" placeholder="请选择所在地区" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in dagConfig.locales" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import UIUtil from '@/utils/ui'
import dagService from '@/service/bi/dag'

export default {
  name: 'DateGenerateConfigProperty',
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
      dagConfig: { timezones: [], locales: [] }
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
        datetime: obj.data.options.datetime || this.defaults.datetime,
        pattern: obj.data.options.pattern || this.defaults.pattern,
        timezone: obj.data.options.timezone || this.defaults.timezone,
        locale: obj.data.options.locale || this.defaults.locale
      }
      return this.config.mergeOptions(obj, options)
    },
    loadDAGConfig () {
      UIUtil.cache(null, () => dagService.config(), 0).then(result => {
        if (result.code === 0) {
          this.dagConfig = result.data
        }
      })
    }
  },
  mounted () {
    this.loadDAGConfig()
  }
}
</script>

<style lang="less" scoped>

</style>
