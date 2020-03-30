<template>
  <div class="main">
    <a-form-model
      class="user-layout-login"
      ref="form"
      :model="form"
      :rules="rules"
      @submit="handleSubmit"
    >
      <a-alert v-if="messageLoginError" type="error" showIcon style="margin-bottom: 24px;" :message="messageLoginError" />
      <a-form-model-item prop="serial">
        <a-input
          size="large"
          type="text"
          placeholder="帐号"
          v-model="form.serial"
        >
          <a-icon slot="prefix" type="user" :style="{ color: 'rgba(0,0,0,.25)' }"/>
        </a-input>
      </a-form-model-item>

      <a-form-model-item prop="password">
        <a-input
          size="large"
          type="password"
          autocomplete="false"
          placeholder="密码"
          v-model="form.password"
        >
          <a-icon slot="prefix" type="lock" :style="{ color: 'rgba(0,0,0,.25)' }"/>
        </a-input>
      </a-form-model-item>

      <a-form-model-item>
        <a-checkbox v-model="form.remember">自动登录</a-checkbox>
        <a-tooltip title="请联系管理员" class="forge-password" style="float: right;">
          <a-button type="link">忘记密码</a-button>
        </a-tooltip>
      </a-form-model-item>

      <a-form-model-item style="margin-top:24px">
        <a-button
          size="large"
          type="primary"
          htmlType="submit"
          class="login-button"
          :loading="loading"
          :disabled="loading"
        >确定</a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>

<script>
import { timeFix } from '@/utils/util'
import DataUtil from '@/utils/data'
import rbacService from '@/service/admin/rbac'

export default {
  data () {
    return {
      loading: false,
      messageLoginError: null,
      form: {},
      rules: {
        serial: [{ required: true, message: '请输入帐号名称', trigger: 'blur' }],
        password: [{ required: true, message: '请输入帐号密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    handleSubmit (e) {
      e.preventDefault()
      this.loading = true
      this.$refs.form.validate(valid => {
        if (valid) {
          rbacService.login(this.form).then(result => {
            if (result.code === 0) {
              this.$store.commit('user/data', result.data)
              let url = this.$router.currentRoute.query.redirect
              if (DataUtil.empty(url)) url = '/'
              this.$router.push(url)
              setTimeout(() => {
                this.$notification.success({
                  message: '欢迎',
                  description: `${timeFix()}，欢迎回来`
                })
              }, 1000)
              this.messageLoginError = null
            } else {
              this.messageLoginError = result.message
            }
            this.loading = false
          })
        } else {
          setTimeout(() => {
            this.loading = false
          }, 600)
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.user-layout-login {
  label {
    font-size: 14px;
  }

  .getCaptcha {
    display: block;
    width: 100%;
    height: 40px;
  }

  .forge-password {
    font-size: 14px;
  }

  button.login-button {
    padding: 0 15px;
    font-size: 16px;
    height: 40px;
    width: 100%;
  }

  .user-login-other {
    text-align: left;
    margin-top: 24px;
    line-height: 22px;

    .item-icon {
      font-size: 24px;
      color: rgba(0, 0, 0, 0.2);
      margin-left: 16px;
      vertical-align: middle;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }

    .register {
      float: right;
    }
  }
}
</style>
