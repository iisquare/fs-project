<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="节点属性">
      <a-form-model :model="form" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="类型">{{ element.type }}</a-form-model-item>
        <a-form-model-item label="标识"><a-input v-model="form.id" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="名称"><a-input v-model="form.name" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="描述"><a-textarea v-model="form.documentation" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'NodeProperty',
  props: {
    value: { type: Object, required: true },
    bpmn: { type: Object, required: true },
    element: { type: Object, required: true }
  },
  data () {
    return {
      form: {}
    }
  },
  watch: {
    element: {
      handler (obj) {
        this.form = this.formatted(obj)
      },
      immediate: true
    },
    form: {
      handler (obj) {
        this.updateProperties(obj)
      },
      deep: true
    }
  },
  methods: {
    updateProperties (obj) {
      if (!obj.id) return false
      const result = {
        id: obj.id,
        name: obj.name,
        documentation: this.bpmn.createDocumentation(obj.documentation)
      }
      this.bpmn.modeling.updateProperties(this.element, result)
      return true
    },
    formatted (element) {
      const obj = element.businessObject
      const result = {
        id: obj.id,
        name: obj.name || '',
        documentation: this.bpmn.parseDocumentation(element)
      }
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
