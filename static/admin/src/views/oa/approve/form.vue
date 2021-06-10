<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <fs-form v-model="form" ref="form" :frame="workflow.formInfo" :config="config" v-if="workflow.formInfo"/>
    <a-empty v-else />
    <div class="fs-footer-action">
      <a-space>
        <a-button type="primary" :loading="loading" @click="submit(false)">提交</a-button>
        <a-button type="danger" :loading="loading" @click="submit(true)">强制提交</a-button>
        <a-button :loading="loading" @click="cancel">返回</a-button>
      </a-space>
    </div>
  </a-card>
</template>

<script>
import config from '../form/design/config'
import FsForm from './design/FsForm'
import FsView from './design/FsView'
import approveService from '@/service/oa/approve'

export default {
  components: { FsForm, FsView },
  data () {
    return {
      config,
      loading: false,
      form: {},
      workflow: {
        id: 0,
        name: ''
      }
    }
  },
  methods: {
    load () {
      this.loading = true
      approveService.form({ workflowId: this.$route.query.id, withForm: true }).then(async result => {
        this.loading = false
        if (result.code === 0) {
          this.workflow = result.data
        }
      })
    },
    submit (modeForce = false) {
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.$message.warning('数据校验不通过')
          if (!modeForce) return
        }
        if (this.loading) return false
        const data = { workflowId: this.workflow.id, form: this.form }
        this.loading = true
        approveService.submit(data).then(result => {
          this.loading = false
          if (result.code === 0) {
            this.$router.go(-1) // TODO:调转至由我发起的数据列表
          }
        })
      })
    },
    cancel () {
      this.$router.go(-1)
    }
  },
  mounted () {
    this.load()
  }
}
</script>

<style lang="less" scoped>
.fs-footer-action {
  padding-top: 15px;
  text-align: center;
}
</style>
