<template>
  <span>startup page {{ready}}</span>
</template>

<script>
import DataUtil from '@/utils/data'

export default {
  computed: {
    ready () {
      return this.$store.state.user.ready
    }
  },
  methods: {
    completed () {
      let url = this.$router.currentRoute.query.redirect
      if (DataUtil.empty(url)) url = '/'
      this.$router.push(url)
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

<style>

</style>
