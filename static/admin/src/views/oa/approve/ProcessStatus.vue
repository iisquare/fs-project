<template>
  <section>
    <template v-if="historic.endTime">
      <a-tag color="purple" v-if="historic.deleteReason === null">已结束</a-tag>
      <a-popover title="撤销原因" v-else>
        <template slot="content">{{ historic.deleteReason }}</template>
        <a-tag color="red">已撤销</a-tag>
      </a-popover>
    </template>
    <template v-else-if="runtime.id">
      <a-tag color="orange" v-if="runtime.isSuspended">已挂起</a-tag>
      <a-tag color="green" v-else>运行中</a-tag>
    </template>
  </section>
</template>

<script>
export default {
  name: 'ApproveProcessStatus',
  props: {
    runtime: { type: Object, required: true },
    historic: { type: Object, required: true }
  },
  data () {
    return {}
  }
}
</script>

<style lang="less" scoped>

</style>
