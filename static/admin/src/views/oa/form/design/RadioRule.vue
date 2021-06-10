<template>
  <section>
    <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-model-item label="启用校验"><a-switch v-model="value.ruleEnabled" /></a-form-model-item>
      <a-form-model-item label="必填字段"><a-switch v-model="value.required" /></a-form-model-item>
      <a-form-model-item label="必填提示"><a-input v-model="value.requiredTooltip" auto-complete="on" /></a-form-model-item>
    </a-form-model>
  </section>
</template>

<script>
export default {
  name: 'RadioRule',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true }
  },
  data () {
    return {}
  },
  methods: {
    formatted (obj) {
      const rules = {
        ruleEnabled: !!obj.ruleEnabled,
        required: !!obj.required,
        requiredTooltip: obj.requiredTooltip || ''
      }
      const result = Object.assign({}, obj, rules)
      return result
    }
  },
  mounted () {
    this.$emit('input', this.formatted(this.value))
  }
}
</script>

<style lang="less" scoped>
</style>
