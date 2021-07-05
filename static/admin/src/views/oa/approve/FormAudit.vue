<template>
  <section class="fs-form">
    <a-form-model v-bind="formLayout">
      <a-form-model-item label="备注选项">
        <a-checkbox v-model="value.local">仅局部节点可见</a-checkbox>
      </a-form-model-item>
      <a-form-model-item label="备注内容">
        <a-textarea v-model="value.message" />
      </a-form-model-item>
    </a-form-model>
  </section>
</template>

<script>
export default {
  name: 'ApproveFormAudit',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    frame: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    formLayout () { return this.config.exhibition.formLayout(this.frame) }
  },
  methods: {}
}
</script>

<style lang="less">
</style>
