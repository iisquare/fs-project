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
        <div class="fs-property-title">接口配置</div>
        <a-form-model-item label="接口地址"><a-input v-model="value.data.options.url" placeholder="HTTP/HTTPS链接地址" /></a-form-model-item>
        <a-form-model-item label="请求方式">
          <a-radio-group v-model="value.data.options.method">
            <a-radio-button :value="item.value" v-for="item in methods" :key="item.value">{{ item.label }}</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="校验字段"><a-input v-model="value.data.options.checkField" placeholder="接口状态字段，为空时不校验" /></a-form-model-item>
        <a-form-model-item label="校验值"><a-input v-model="value.data.options.checkValue" placeholder="请求成功的返回值" /></a-form-model-item>
        <a-form-model-item label="数据字段"><a-input v-model="value.data.options.dataField" placeholder="数据项所在路径" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'APIConfigProperty',
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
      methods: [{ value: 'GET', label: 'GET' }]
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
        url: obj.data.options.url || this.defaults.url,
        method: obj.data.options.method || this.defaults.method,
        checkField: obj.data.options.checkField || this.defaults.checkField,
        checkValue: obj.data.options.checkValue || this.defaults.checkValue,
        dataField: obj.data.options.dataField || this.defaults.dataField
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
