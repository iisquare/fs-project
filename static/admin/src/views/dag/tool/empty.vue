<template>
  <section></section>
</template>

<script>
export default {
  name: 'DagToolEmpty',
  props: {
    value: {
      type: Object,
      required: true
    }
  },
  data () {
    return {}
  },
  methods: {},
  mounted () {
    this.$emit('input', {})
  }
}
</script>
