<template>
  <a-form-model ref="form" :model="form" :rules="rules" @submit="handleSubmit">
    <a-form-model-item label="账号" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">{{ userInfo.serial }}</a-form-model-item>
    <a-form-model-item label="名称" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">{{ userInfo.name }}</a-form-model-item>
    <a-form-model-item label="原密码" prop="passwordOld" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
      <a-input-password
        placeholder="请输入当前登录密码"
        v-model="form.passwordOld"
      />
    </a-form-model-item>
    <a-form-model-item label="新密码" prop="password" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
      <a-input-password
        placeholder="请输入新的登录密码"
        v-model="form.password"
      />
    </a-form-model-item>
    <a-form-model-item label="确认密码" prop="passwordNew" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
      <a-input-password
        placeholder="请确认新的登录密码"
        v-model="form.passwordNew"
      />
    </a-form-model-item>
    <a-form-model-item :wrapper-col="{ span: 12, offset: 5 }">
      <a-button type="primary" html-type="submit" :loading="formLoading">提交</a-button>
      <a-button @click.native="$router.go(-1)" :style="{ marginLeft: '8px' }">返回</a-button>
    </a-form-model-item>
  </a-form-model>
</template>

<script>
import userService from '@/service/member/user'
export default {
  data () {
    return {
      formLoading: false,
      form: {},
      rules: {
        passwordOld: [{ required: true, message: '请输入当前登录密码' }],
        password: [{ required: true, message: '请输入新的登录密码' }],
        passwordNew: [{ required: true, message: '请确认新的登录密码' }]
      }
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
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        userService.password(this.form).then(response => {
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
