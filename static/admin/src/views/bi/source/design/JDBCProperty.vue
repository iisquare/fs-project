<template>
  <a-tabs default-active-key="basic" :animated="false" tabPosition="left">
    <a-tab-pane key="basic" tab="基础信息"><slot></slot></a-tab-pane>
    <a-tab-pane key="server" tab="连接信息">
      <a-form-model-item></a-form-model-item>
      <a-form-model-item label="链接"><a-textarea v-model="value.url" placeholder="url" rows="5" /></a-form-model-item>
      <a-form-model-item label="用户"><a-input v-model="value.username" placeholder="username" /></a-form-model-item>
      <a-form-model-item label="密码"><a-input v-model="value.password" placeholder="password" /></a-form-model-item>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'JDBCProperty',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    form: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    defaults () {
      return this.config.widgetDefaults(this.form.type)
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        url: obj.url || this.defaults.url,
        username: obj.username || this.defaults.username,
        password: obj.password || this.defaults.password
      }
      const result = Object.assign({}, obj, options)
      return result
    }
  },
  mounted () {
    this.$emit('input', this.formatted(this.value))
  }
}
</script>

<style lang="less" scoped>

</style>
