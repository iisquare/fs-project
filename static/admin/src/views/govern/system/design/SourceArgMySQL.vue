<template>
  <div class="fs-layout-box">
    <a-form-model ref="jdbc" :model="value" :rules="rules">
      <a-form-model-item label="链接" prop="url"><a-textarea v-model="value.url" placeholder="url" /></a-form-model-item>
      <a-form-model-item label="用户"><a-input v-model="value.username" placeholder="username" /></a-form-model-item>
      <a-form-model-item label="密码"><a-input v-model="value.password" placeholder="password" /></a-form-model-item>
    </a-form-model>
  </div>
</template>

<script>

export default {
  name: 'SourceArgMySQL',
  props: {
    value: { type: Object, required: true },
    form: { type: Object, required: true },
    config: { type: Object, required: true },
    menus: { type: Array, required: true }
  },
  data () {
    return {
      loading: false,
      rules: {
        url: [{ required: true, message: '请输入连接字符串', trigger: 'blur' }]
      }
    }
  },
  methods: {
    format (obj) {
      return {
        url: obj.url || '',
        username: obj.username || '',
        password: obj.password || ''
      }
    }
  },
  mounted () {
    this.$emit('input', this.format(this.value))
  }
}
</script>

<style lang="less" scoped>
</style>
