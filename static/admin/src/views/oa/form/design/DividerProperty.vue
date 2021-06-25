<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="组件属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称"><a-input v-model="value.label" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="虚线"><a-switch v-model="value.options.dashed" /></a-form-model-item>
        <a-form-model-item label="类型">
          <a-radio-group v-model="value.options.type">
            <a-radio-button :value="item.value" v-for="item in types" :key="item.value">{{ item.label }}</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="对齐">
          <a-radio-group v-model="value.options.orientation">
            <a-radio-button :value="item.value" v-for="item in orientations" :key="item.value">{{ item.label }}</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'DividerProperty',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {
      types: [{ value: 'horizontal', label: '水平' }, { value: 'vertical', label: '垂直' }],
      orientations: [{ value: 'left', label: '左对齐' }, { value: 'center', label: '居中' }, { value: 'right', label: '右对齐' }]
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
        dashed: !!obj.options.dashed,
        type: obj.options.type || this.defaults.type,
        orientation: obj.options.orientation || this.defaults.orientation
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
