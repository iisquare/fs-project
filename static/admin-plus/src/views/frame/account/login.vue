<script setup lang="ts">
import RbacApi from '@/api/admin/RbacApi';
import { useUserStore } from '@/stores/user';
import ApiUtil from '@/utils/ApiUtil';
import DataUtil from '@/utils/DataUtil';
import DateUtil from '@/utils/DateUtil';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { ElNotification, type FormInstance } from 'element-plus';
import { onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const form = ref({
  serial: '',
  password: '',
  remember: false,
})

const rules = {
  serial: [{ required: true, message: '请输入帐号名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入帐号密码', trigger: 'blur' }]
}

const user = useUserStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()
const handleSubmit = () => {
  formRef.value && formRef.value.validate(valid => {
    if (!valid || loading.value) return
    loading.value = true
    RbacApi.login(form.value).then(result => {
      if (ApiUtil.succeed(result)) {
        user.reset(ApiUtil.data(result))
        redirect()
      } else {
        loading.value = false
      }
    })
  })
}

const redirect = () => {
  let url = route.query.redirect
  if (DataUtil.empty(url)) url = '/'
  router.push(url as string)
  setTimeout(() => {
    ElNotification({
      title: '欢迎',
      message: `${DateUtil.timeFix()}，欢迎回来`,
      type: 'success',
    })
  }, 1000)
}

const keyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleSubmit()
  }
}

onMounted(() => {
  window.addEventListener('keydown', keyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', keyDown, false)
})
</script>

<template>
  <div class="container">
    <div class="header">
      <img src="@/assets/logo.svg" class="logo" alt="logo">
      <span class="title">FS Project</span>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form">
      <el-form-item prop="serial">
        <el-input v-model="form.serial" :prefix-icon="ElementPlusIcons.User" placeholder="账号" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" :prefix-icon="ElementPlusIcons.Lock" placeholder="密码" type="password" show-password />
      </el-form-item>
      <el-form-item class="login-ctl">
        <el-checkbox v-model="form.remember">自动登录</el-checkbox>
        <el-tooltip content="请联系管理员" effect="dark" placement="top">
          <el-button link type="primary">忘记密码</el-button>
        </el-tooltip>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit" class="login-button">登录</el-button>
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
