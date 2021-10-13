<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">接口配置</div>
        <a-form-model-item label="接口地址"><a-input v-model="value.options.url" placeholder="HTTP/HTTPS链接地址" /></a-form-model-item>
        <a-form-model-item label="请求方式">
          <a-radio-group v-model="value.options.method">
            <a-radio-button :value="item.value" v-for="item in methods" :key="item.value">{{ item.label }}</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="校验字段"><a-input v-model="value.options.checkField" placeholder="接口状态字段，为空时不校验" /></a-form-model-item>
        <a-form-model-item label="校验值"><a-input v-model="value.options.checkValue" placeholder="请求成功的返回值" /></a-form-model-item>
        <a-form-model-item label="数据字段"><a-input v-model="value.options.dataField" placeholder="数据项所在路径" /></a-form-model-item>
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
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {
      methods: [{ value: 'GET', label: 'GET' }]
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
        url: obj.options.url || this.defaults.url,
        method: obj.options.method || this.defaults.method,
        checkField: obj.options.checkField || this.defaults.checkField,
        checkValue: obj.options.checkValue || this.defaults.checkValue,
        dataField: obj.options.dataField || this.defaults.dataField
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
