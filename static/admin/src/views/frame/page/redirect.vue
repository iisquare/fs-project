<template>
  <a-skeleton />
</template>
<script>
import RouteUtil from '@/utils/route'

export default {
  data () {
      return {}
  },
  methods: {
  },
  mounted () {
    this.$$router.replace({
      path: this.$route.query.path,
      query: RouteUtil.decode(this.$route.query.query)
    })
  }
}
</script>

<style lang="less" scoped>
</style>
