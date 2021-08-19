<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-form-model
      ref="form"
      :model="form"
      :rules="rules"
      @submit="submit"
      :labelCol="{lg: {span: 7}, sm: {span: 7}}"
      :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
      <a-collapse v-model="activeKey">
        <a-collapse-panel key="site" header="站点信息">
          <a-form-model-item label="站点名称"><a-input v-model="form.siteName" /></a-form-model-item>
        </a-collapse-panel>
        <a-collapse-panel key="home" header="站点首页">
          <a-form-model-item label="首页标题"><a-input v-model="form.homeTitle" /></a-form-model-item>
          <a-form-model-item label="关键词"><a-input v-model="form.keywords" /></a-form-model-item>
          <a-form-model-item label="站点描述"><a-textarea v-model="form.description" /></a-form-model-item>
        </a-collapse-panel>
        <a-collapse-panel key="global" header="全局配置">
          <a-form-model-item label="禁用评论"><a-checkbox v-model="form.commentDisabled">关闭整站的评论功能</a-checkbox></a-form-model-item>
        </a-collapse-panel>
      </a-collapse>
      <a-form-model-item :wrapperCol="{ span: 24 }" style="text-align: center;">
        <a-button type="primary" html-type="submit" :loading="loading">提交</a-button>
        <a-button @click.native="$router.go(-1)" :style="{ marginLeft: '8px' }">返回</a-button>
      </a-form-model-item>
    </a-form-model>
  </a-card>
</template>

<script>
import settingService from '@/service/cms/setting'
export default {
  data () {
    return {
      activeKey: ['site', 'home', 'global'],
      names: ['siteName', 'keywords', 'description', 'homeTitle', 'commentDisabled'],
      loading: false,
      form: {},
      rules: {}
    }
  },
  methods: {
    load () {
      this.loading = true
      settingService.load({ names: this.names }).then(result => {
        if (result.code !== 0) return
        this.form = Object.assign(result.data, { commentDisabled: result.data.commentDisabled === 'true' })
        this.loading = false
      })
    },
    submit (e) {
      e.preventDefault()
      this.$refs.form.validate(valid => {
        if (!valid || this.loading) return false
        this.loading = true
        const data = Object.assign({}, this.form, { commentDisabled: this.form.commentDisabled ? 'true' : 'false' })
        settingService.change({ data }, { success: true }).then(result => {
          this.loading = false
        })
      })
    }
  },
  mounted () {
    this.load()
  }
}
</script>
