<template>
  <section>
    <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-model-item label="启用校验"><a-switch v-model="value.ruleEnabled" /></a-form-model-item>
      <a-form-model-item label="必填字段"><a-switch v-model="value.required" /></a-form-model-item>
      <a-form-model-item label="必填提示"><a-input v-model="value.requiredTooltip" auto-complete="on" /></a-form-model-item>
      <a-form-model-item label="最小长度"><a-input-number v-model="value.minLength" :min="0" /></a-form-model-item>
      <a-form-model-item label="最小提示"><a-input v-model="value.minTooltip" auto-complete="on" /></a-form-model-item>
      <a-form-model-item label="最大长度"><a-input-number v-model="value.maxLength" :min="0" /></a-form-model-item>
      <a-form-model-item label="最大提示"><a-input v-model="value.maxTooltip" auto-complete="on" /></a-form-model-item>
    </a-form-model>
    <regular v-model="value.regulars" :config="config" v-if="value.regulars" />
  </section>
</template>

<script>
import Regular from './Regular'

export default {
  name: 'TextRule',
  components: { Regular },
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
        requiredTooltip: obj.requiredTooltip || '',
        minLength: Number.isInteger(obj.minLength) ? obj.minLength : 0,
        minTooltip: obj.minTooltip || '',
        maxLength: Number.isInteger(obj.maxLength) ? obj.maxLength : 0,
        maxTooltip: obj.maxTooltip || '',
        regulars: obj.regulars || []
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
