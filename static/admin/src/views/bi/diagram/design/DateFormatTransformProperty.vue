<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="变量名称"><a-input v-model="value.options.arg" placeholder="为空时直接覆盖引用的变量" /></a-form-model-item>
        <a-form-model-item label="变量引用"><a-input v-model="value.options.reference" placeholder="数据字段的引用路径" /></a-form-model-item>
        <a-form-model-item label="日期格式"><a-input v-model="value.options.pattern" placeholder="日期目标格式" /></a-form-model-item>
        <a-form-model-item label="所在时区">
          <a-select v-model="value.options.timezone" placeholder="请选择所在时区" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in dagConfig.timezones" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="所在地区">
          <a-select v-model="value.options.locale" placeholder="请选择所在地区" :allowClear="true">
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
  name: 'DateFormatTransformProperty',
  components: {
    SliceBasic: () => import('./SliceBasic')
  },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {
      dagConfig: { timezones: [], locales: [] }
    }
  },
  computed: {
    defaults () {
      return this.config.widgetDefaults(this.value.type)
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
        arg: obj.options.arg || this.defaults.arg,
        reference: obj.options.reference || this.defaults.reference,
        pattern: obj.options.pattern || this.defaults.pattern,
        timezone: obj.options.timezone || this.defaults.timezone,
        locale: obj.options.locale || this.defaults.locale
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
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
