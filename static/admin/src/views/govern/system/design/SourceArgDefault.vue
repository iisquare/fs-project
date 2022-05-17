<template>
  <a-empty class="fs-ui-empty" />
</template>

<script>
export default {
  name: 'SourceArgDefault',
  props: {
    value: { type: Object, required: true },
    form: { type: Object, required: true },
    config: { type: Object, required: true },
    menus: { type: Array, required: true }
  },
  data () {
    return {}
  },
  methods: {
    format (obj) {
      return {}
    }
  },
  mounted () {
    this.$emit('input', this.format(this.value))
  }
}
</script>

<style lang="less" scoped>
.fs-ui-empty {
    margin-top: 150px;
}
</style>
