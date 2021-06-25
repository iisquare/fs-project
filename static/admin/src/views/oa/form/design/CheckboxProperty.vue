<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="组件属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称"><a-input v-model="value.label" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="字段"><a-input v-model="value.options.field" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="默认值"><a-input v-model="value.options.value" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="排列方式">
          <a-radio-group v-model="value.options.display">
            <a-radio-button :value="item.value" v-for="item in displays" :key="item.value">{{ item.label }}</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
      </a-form-model>
      <selector v-model="value.options" :activeItem="activeItem" />
    </a-tab-pane>
    <a-tab-pane key="rule" tab="校验规则">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <checkbox-rule v-model="value.options" :config="config" :activeItem="activeItem" />
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'CheckboxProperty',
  components: { Selector: () => import('./Selector'), CheckboxRule: () => import('./CheckboxRule') },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {
      displays: [{ value: 'inline', label: '行内' }, { value: 'block', label: '块级' }]
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
        field: obj.options.field || this.defaults.field || obj.id,
        value: obj.options.value || this.defaults.value,
        items: obj.options.items || this.defaults.items,
        display: obj.options.display || this.defaults.display
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
