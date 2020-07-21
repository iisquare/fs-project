<template>
  <div id="main">{{ readyText }}</div>
</template>

<script>
import DataUtil from '@/utils/data'

export default {
  computed: {
    ready () {
      return this.$store.state.user.ready
    },
    readyText () {
      return this.$store.state.user.readyText
    }
  },
  methods: {
    completed () {
      let url = this.$router.currentRoute.query.redirect
      if (DataUtil.empty(url)) url = '/'
      this.$router.push(url).catch(err => err)
    }
  },
  watch: {
    ready (newValue, oldValue) {
      if (newValue) this.completed()
    }
  },
  mounted () {
    if (this.ready) this.completed()
  }
}
</script>

<style lang="less" scoped>
#main {
  margin: 0px;
  padding: 0px;
  background: #f0f2f5 url(~@/assets/background.svg) no-repeat 50%;
  width: 100%;
  height: 100%;
  position: absolute;
  overflow: hidden;
  display:flex;
  justify-content:center;
  align-items:center;
}
</style>
