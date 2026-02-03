<script setup lang="ts">
import RbacApi from '@/api/admin/RbacApi';
import FormCaptch from '@/components/Form/FormCaptch.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { type FormInstance } from 'element-plus';
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const captchRef: any = ref(null)

const form: any = ref({})

const rules = {
  serial: [{ required: true, message: '请输入帐号名称', trigger: 'blur' }],
  name: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入帐号密码', trigger: 'blur' }],
  confirm: [{ required: true, message: '请确认帐号密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱地址', trigger: 'blur' }],
}

const time = ref(0)
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()
const handleTime = (tick: number) => {
  time.value = Math.max(0, tick)
  if (time.value > 0) {
    setTimeout(() => {
      handleTime(time.value - 1)
    }, 1000)
  }
}
const handleEmail = () => {
  formRef.value && formRef.value.validate(valid => {
    if (!valid || loading.value || time.value > 0) return
    loading.value = true
    const param = Object.assign({}, form.value, { action: 'email' })
    RbacApi.signup(param, { success: true }).then(result => {
      form.value.verify = ''
      handleTime(120)
    }).catch(() => {
      captchRef.value?.reload()
    }).finally(() => {
      loading.value = false
    })
  })
}
const handleSubmit = () => {
  formRef.value && formRef.value.validate(valid => {
    if (!valid || loading.value) return
    loading.value = true
    RbacApi.signup(form.value, { success: true }).then(result => {
      router.push({
        path: '/login',
        query: route.query,
      })
    }).catch(() => {
      loading.value = false
    })
  })
}

</script>

<template>
  <div class="container">
    <div class="header">
      <img src="@/assets/logo.svg" class="logo" alt="logo">
      <span class="title">用户注册</span>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form">
      <el-form-item prop="serial">
        <el-input v-model="form.serial" :prefix-icon="ElementPlusIcons.User" placeholder="账号" />
      </el-form-item>
      <el-form-item prop="name">
        <el-input v-model="form.name" :prefix-icon="ElementPlusIcons.Postcard" placeholder="昵称" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" :prefix-icon="ElementPlusIcons.Lock" placeholder="密码" type="password" show-password />
      </el-form-item>
      <el-form-item prop="confirm">
        <el-input v-model="form.confirm" :prefix-icon="ElementPlusIcons.Lock" placeholder="确认密码" type="password" show-password />
      </el-form-item>
      <el-form-item prop="captcha">
        <el-input v-model="form.captcha" placeholder="图形验证码">
          <template #prefix><layout-icon name="layout.captcha" /></template>
          <template #suffix><form-captch ref="captchRef" v-model="form.uuid" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="email">
        <el-input v-model="form.email" :prefix-icon="ElementPlusIcons.Message" placeholder="邮箱地址" />
      </el-form-item>
      <el-form-item prop="verify">
        <el-input v-model="form.verify" placeholder="邮箱验证码">
          <template #prefix><layout-icon name="form.number" /></template>
          <template #suffix><el-button link :loading="loading" :disabled="time > 0" @click="handleEmail">{{ time > 0 ? `${time} 秒后可重发` : '获取邮箱验证码' }}</el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit" class="login-button" :disabled="!form.verify">注册</el-button>
      </el-form-item>
      <el-form-item class="login-ctl">
        <router-link :to="{ path: '/login', query: route.query }">返回登录</router-link>
        <router-link :to="{ path: '/forgot', query: route.query }">找回密码</router-link>
      </el-form-item>
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
.container {
  width: 100%;
  min-height: 100%;
  background: #f0f2f5 url('/images/background.svg') no-repeat 50%;
  background-size: 100%;
  padding: 110px 0 144px;
  box-sizing: border-box;
  position: relative;
  text-align: center;
  .header {
    height: 44px;
    line-height: 44px;
    margin-bottom: 40px;
    .logo {
      height: 44px;
      vertical-align: top;
      margin-right: 16px;
      border-style: none;
    }
    .title {
      font-size: 33px;
      color: rgba(0, 0, 0, .85);
      font-family: Avenir, 'Helvetica Neue', Arial, Helvetica, sans-serif;
      font-weight: 600;
      position: relative;
      top: 2px;
    }
  }
}
.login-form {
  min-width: 260px;
  width: 368px;
  margin: 0 auto;
  .login-ctl {
    :deep(> div) {
      @include flex-between();
      a {
        color: var(--el-color-primary);
      }
    }
  }
}
.login-button {
  padding: 0 15px;
  font-size: 16px;
  height: 40px;
  width: 100%;
}
</style>
