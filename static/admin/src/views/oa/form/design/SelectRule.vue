<template>
  <section>
    <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-model-item label="启用校验"><a-switch v-model="value.ruleEnabled" /></a-form-model-item>
      <a-form-model-item label="最少应选"><a-input-number v-model="value.minLength" :min="0" /></a-form-model-item>
      <a-form-model-item label="最少提示"><a-input v-model="value.minTooltip" auto-complete="on" /></a-form-model-item>
      <a-form-model-item label="最多可选"><a-input-number v-model="value.maxLength" :min="0" /></a-form-model-item>
      <a-form-model-item label="最多提示"><a-input v-model="value.maxTooltip" auto-complete="on" /></a-form-model-item>
    </a-form-model>
    <regular v-model="value.regulars" :config="config" :activeItem="activeItem" v-if="value.regulars" />
  </section>
</template>

<script>
import Regular from './Regular'

export default {
  name: 'SelectRule',
  components: { Regular },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {}
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
      const rules = {
        ruleEnabled: !!obj.ruleEnabled,
        minLength: Number.isInteger(obj.minLength) ? obj.minLength : 0,
        minTooltip: obj.minTooltip || '',
        maxLength: Number.isInteger(obj.maxLength) ? obj.maxLength : 0,
        maxTooltip: obj.maxTooltip || '',
        regulars: obj.regulars || []
      }
      const result = Object.assign({}, obj, rules)
      return result
    }
  }
}
</script>

<style lang="less" scoped>
</style>
