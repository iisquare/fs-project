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
        <a-form-model-item label="打印配置"><a-checkbox v-model="value.data.options.echoConfig">打印配置参数</a-checkbox></a-form-model-item>
        <a-form-model-item label="输出模式">
          <a-select v-model="value.data.options.mode" placeholder="请选择输出模式" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in modes" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'ConsoleSinkProperty',
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
      modes: [
        { label: '自适应打印输出模式', value: '' },
        { label: '输出为JSON字符串', value: 'json' },
        { label: '输出为键值对字符串', value: 'map' },
        { label: '输出为逗号分隔文本', value: 'line' }
      ]
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
        echoConfig: !!obj.data.options.echoConfig,
        mode: obj.data.options.mode || this.defaults.mode
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
