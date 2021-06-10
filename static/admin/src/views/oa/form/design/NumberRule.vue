<template>
  <section>
    <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-model-item label="启用校验"><a-switch v-model="value.ruleEnabled" /></a-form-model-item>
      <a-form-model-item label="最小校验"><a-switch v-model="value.minEnabled" /></a-form-model-item>
      <a-form-model-item label="最小值"><a-input-number v-model="value.min" :min="0" /></a-form-model-item>
      <a-form-model-item label="最小提示"><a-input v-model="value.minTooltip" auto-complete="on" /></a-form-model-item>
      <a-form-model-item label="最大校验"><a-switch v-model="value.maxEnabled" /></a-form-model-item>
      <a-form-model-item label="最大值"><a-input-number v-model="value.max" :min="0" /></a-form-model-item>
      <a-form-model-item label="最大提示"><a-input v-model="value.maxTooltip" auto-complete="on" /></a-form-model-item>
    </a-form-model>
    <regular v-model="value.regulars" :config="config" v-if="value.regulars" />
  </section>
</template>

<script>
import Regular from './Regular'

export default {
  name: 'NumberRule',
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
        minEnabled: !!obj.minEnabled,
        min: Number.isInteger(obj.min) ? obj.min : 0,
        minTooltip: obj.minTooltip || '',
        maxEnabled: !!obj.maxEnabled,
        max: Number.isInteger(obj.max) ? obj.max : 0,
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
