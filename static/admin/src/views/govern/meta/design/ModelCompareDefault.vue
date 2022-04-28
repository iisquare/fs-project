<template>
  <a-empty class="fs-ui-empty" />
</template>

<script>
export default {
  name: 'ModelCompareDefault',
  props: {
    value: { type: Array, required: true },
    form: { type: Object, required: true },
    type: { type: String, required: true },
    config: { type: Object, required: true },
    menus: { type: Array, required: true }
  },
  data () {
    return {}
  },
  methods: {
  },
  mounted () {
    this.$emit('input', [])
  }
}
</script>

<style lang="less" scoped>
.fs-ui-empty {
    margin-top: 150px;
}
</style>
