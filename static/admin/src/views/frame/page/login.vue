<template>
  <div class="main">
    <a-form
      id="formLogin"
      class="user-layout-login"
      ref="formLogin"
      :form="form"
      @submit="handleSubmit"
    >
      <a-alert v-if="messageLoginError" type="error" showIcon style="margin-bottom: 24px;" :message="messageLoginError" />
      <a-form-item>
        <a-input
          size="large"
          type="text"
          placeholder="帐号"
          v-decorator="[
            'serial',
            {rules: [{ required: true, message: '请输入帐号名称' }], validateTrigger: 'blur'}
          ]"
        >
          <a-icon slot="prefix" type="user" :style="{ color: 'rgba(0,0,0,.25)' }"/>
        </a-input>
      </a-form-item>

      <a-form-item>
        <a-input
          size="large"
          type="password"
          autocomplete="false"
          placeholder="密码"
          v-decorator="[
            'password',
            {rules: [{ required: true, message: '请输入帐号密码' }], validateTrigger: 'blur'}
          ]"
        >
          <a-icon slot="prefix" type="lock" :style="{ color: 'rgba(0,0,0,.25)' }"/>
        </a-input>
      </a-form-item>

      <a-form-item>
        <a-checkbox v-decorator="['remember', { valuePropName: 'checked' }]">自动登录</a-checkbox>
        <a-tooltip title="请联系管理员" class="forge-password" style="float: right;">
          <a-button type="link">忘记密码</a-button>
        </a-tooltip>
      </a-form-item>

      <a-form-item style="margin-top:24px">
        <a-button
          size="large"
          type="primary"
          htmlType="submit"
          class="login-button"
          :loading="loading"
          :disabled="loading"
        >确定</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
import { timeFix } from '@/utils/util'
import DataUtil from '@/utils/data'
import userService from '@/service/member/user'

export default {
  data () {
    return {
      loading: false,
      messageLoginError: null,
      form: this.$form.createForm(this)
    }
  },
  methods: {
    handleSubmit (e) {
      e.preventDefault()
      this.loading = true
      this.form.validateFields({ force: true }, (err, values) => {
        if (!err) {
          userService.login(values).then(result => {
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
