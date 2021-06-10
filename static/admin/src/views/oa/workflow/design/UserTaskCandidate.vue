<template>
  <section>TODO:List</section>
</template>

<script>
export default {
  name: 'UserTaskCandidate',
  props: {
    value: { type: String, required: true },
    bpmn: { type: Object, required: true },
    element: { type: Object, required: true },
    workflow: { type: Object, required: true }
  },
  data () {
    return {
    }
  },
  methods: {
  }
}
</script>

<style lang="less" scoped>

</style>
