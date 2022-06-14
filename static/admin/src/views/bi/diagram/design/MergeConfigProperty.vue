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
        <a-form-model-item label="变量名称"><a-input v-model="value.data.options.arg" placeholder="为空时不创建新变量，直接展开" /></a-form-model-item>
        <a-form-model-item label="合并方式">
          <a-radio-group v-model="value.data.options.mergeType">
            <a-radio-button :value="item.value" v-for="item in mergeTypes" :key="item.value">{{ item.label }}</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="输出节点"><a-input v-model="value.data.options.echoPrefix" placeholder="为空时直接输出结果" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'MergeConfigProperty',
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
      mergeTypes: [{ value: 'map', label: '对象合并' }, { value: 'array', label: '数组拼接' }]
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
        mergeType: obj.data.options.mergeType || this.defaults.mergeType,
        echoPrefix: obj.data.options.echoPrefix ?? this.defaults.echoPrefix
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
