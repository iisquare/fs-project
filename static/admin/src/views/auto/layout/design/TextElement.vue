<template>
  <section>{{ value.content }}</section>
</template>

<script>
export default {
  name: 'TextElement',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, default: null }
  },
  data () {
    return {}
  },
  methods: {
  }
}
</script>

<style lang="less" scoped>

</style>
