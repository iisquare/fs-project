<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="流程属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="主键">{{ value.id }}</a-form-model-item>
        <a-form-model-item label="名称">{{ value.name }}</a-form-model-item>
        <a-form-model-item label="标识" :key="index" v-for="(element, index) in rootElements">
          <a-input auto-complete="on" v-model="element.id" />
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'CanvasProperty',
  props: {
    value: { type: Object, required: true },
    bpmn: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    rootElements () {
      return this.bpmn.modeler._definitions ? this.bpmn.modeler._definitions.rootElements : []
    }
  }
}
</script>

<style lang="less" scoped>

</style>
