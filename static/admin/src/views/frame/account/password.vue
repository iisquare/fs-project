<template>
  <a-form :form="form" @submit="handleSubmit">
    <a-form-item label="账号" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">{{ userInfo.serial }}</a-form-item>
    <a-form-item label="名称" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">{{ userInfo.name }}</a-form-item>
    <a-form-item label="原密码" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
      <a-input-password
        placeholder="请输入当前登录密码"
        v-decorator="['passwordOld', { rules: [{ required: true, message: '请输入当前登录密码' }] }]"
      />
    </a-form-item>
    <a-form-item label="新密码" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
      <a-input-password
        placeholder="请输入新的登录密码"
        v-decorator="['password', { rules: [{ required: true, message: '请输入新的登录密码' }] }]"
      />
    </a-form-item>
    <a-form-item label="确认密码" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
      <a-input-password
        placeholder="请确认新的登录密码"
        v-decorator="['passwordNew', { rules: [{ required: true, message: '请确认新的登录密码' }] }]"
      />
    </a-form-item>
    <a-form-item :wrapper-col="{ span: 12, offset: 5 }">
      <a-button type="primary" html-type="submit" :loading="formLoading">提交</a-button>
      <a-button @click.native="$router.go(-1)" :style="{ marginLeft: '8px' }">返回</a-button>
    </a-form-item>
  </a-form>
</template>

<script>
import userService from '@/service/member/user'
export default {
  data () {
    return {
      formLoading: false,
      form: this.$form.createForm(this)
    }
  },
  computed: {
    userInfo () {
      return this.$store.state.user.data.info
    }
  },
  methods: {
    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields({ force: true }, (err, values) => {
        if (err || this.formLoading) return false
        this.formLoading = true
        userService.password(values).then(response => {
          if (response.code === 0) {
            this.$router.go(0)
          }
          this.formLoading = false
        })
      })
    }
  }
}
</script>
