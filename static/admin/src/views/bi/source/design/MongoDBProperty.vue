<template>
  <a-tabs default-active-key="basic" :animated="false" tabPosition="left">
    <a-tab-pane key="basic" tab="基础信息"><slot></slot></a-tab-pane>
    <a-tab-pane key="dag" tab="连接信息">
      <a-form-model-item></a-form-model-item>
      <a-form-model-item label="主机列表"><a-textarea v-model="value.hosts" placeholder="host1,host2,host3:27017" /></a-form-model-item>
      <a-form-model-item label="认证用户"><a-input v-model="value.username" placeholder="为空时忽略安全认证" /></a-form-model-item>
      <a-form-model-item label="认证密码"><a-input v-model="value.password" placeholder="仅在配置认证用户时有效" /></a-form-model-item>
      <a-form-model-item label="数据库"><a-input v-model="value.database" placeholder="database name to read data" /></a-form-model-item>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'MongoDBProperty',
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
        hosts: obj.hosts || this.defaults.hosts,
        database: obj.database || this.defaults.database,
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
