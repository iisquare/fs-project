<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="内容"><a-textarea v-model="value.content" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
    <a-tab-pane key="layout" tab="布局">
      <slice-element-property :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'TextProperty',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, default: null }
  },
  components: {
    SliceElementProperty: () => import('./SliceElementProperty')
  },
  data () {
    return {}
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
        content: typeof obj.content === 'undefined' ? this.defaults.content : obj.content
      }
      const style = {
      }
      const link = {
      }
      const marquee = {
      }
      const result = Object.assign({}, obj, options, { style, link, marquee })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
