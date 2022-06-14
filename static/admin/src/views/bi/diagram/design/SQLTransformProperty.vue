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
        <div class="fs-property-title">SQL查询</div>
        <code-editor ref="sql" v-model="value.data.options.sql" mode="sql" :height="230" />
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'SQLTransformProperty',
  components: {
    SliceBasic: () => import('./SliceBasic'),
    CodeEditor: () => import('@/components/Editor/CodeEditor')
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
    return {}
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
        this.$refs.sql && this.$refs.sql.setContent(this.value.data.options.sql)
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        sql: obj.data.options.sql || this.defaults.sql
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
