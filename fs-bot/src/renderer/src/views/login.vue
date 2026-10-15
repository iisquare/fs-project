<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessageBox } from 'element-plus'
import UserApi from '@renderer/api/UserApi'
import FormCaptch from '@renderer/components/Form/FormCaptch.vue'
import LayoutIcon from '@renderer/components/Layout/LayoutIcon.vue'
import TitleBar from '@renderer/components/Layout/TitleBar.vue'
import { useUserStore } from '@renderer/stores/user'
import { useRouter, useRoute } from 'vue-router'
import ApiUtil from '@renderer/utils/ApiUtil'
import Config from '@renderer/core/Config'

const user = useUserStore()
const captchRef: any = ref(null)
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()
const serialInputRef = ref<any>(null)

const form: any = ref({})

const rules = {
  serial: [{ required: true, message: '请输入帐号名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入帐号密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  agree: [{ required: true, message: '请仔细阅读并同意后继续', trigger: 'change' }]
}

const historyList = ref<string[]>([])

async function loadHistory() {
  historyList.value = (await Config.listLoginHistory()) || []
  if (historyList.value.length && !form.value.serial) {
    form.value.serial = historyList.value[0]
  }
}

async function removeFromHistory(serial: string) {
  try {
    await ElMessageBox.confirm(`确定删除记录「${serial}」？`, '确认删除', { type: 'warning' })
  } catch {
    return
  }
  await Config.removeLoginHistory(serial)
  historyList.value = historyList.value.filter((s) => s !== serial)
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid || loading.value) return
    loading.value = true
    try {
      const result = await UserApi.login(form.value)
      // 登录历史写入失败不影响登录流程
      Config.saveLoginHistory(form.value.serial).catch(() => {})
      await user.reset(ApiUtil.data(result))
      redirect()
    } catch {
      loading.value = false
      captchRef.value?.reload()
    }
  })
}

const redirect = () => {
  nextTick(() => {
    window.ipc.setNormalSize()
  })
  let url = route.query.redirect as string
  if (!url) url = '/home'
  router.push(url)
}

onMounted(() => {
  window.ipc.setLoginSize()
  loadHistory()
})

watch(
  () => user.ready,
  (newVal) => {
    if (newVal && user.isLogined()) {
      redirect()
    }
  },
  { immediate: true }
)

// 后台管理地址，页面路由为 hash 模式（参考 fs-admin：/#/signup、/#/forgot）
const websiteUrl = import.meta.env.VITE_APP_WEBSITE_URL

const openSignup = () => {
  window.open(websiteUrl + '/#/signup', '_blank')
}

const openForgot = () => {
  window.open(websiteUrl + '/#/forgot', '_blank')
}
</script>

<template>
  <div class="login-page">
    <TitleBar class="login-page__titlebar" :show-maximize="false" />
    <div class="login-container">
      <div class="login-header">
      <span class="login-title">用户登录</span>
    </div>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      size="large"
      class="login-form"
      @keyup.enter="handleSubmit"
    >
      <el-form-item prop="serial">
        <el-autocomplete
          ref="serialInputRef"
          v-model="form.serial"
          :fetch-suggestions="(_query: string, cb: (list: { value: string }[]) => void) => cb(historyList.map((s) => ({ value: s })))"
          placeholder="账号"
          @focus="loadHistory"
        >
          <template #default="{ item }">
            <div class="history-item">
              <span>{{ item.value }}</span>
              <el-button
                text
                size="small"
                type="danger"
                @click.stop="removeFromHistory(item.value)"
              >
                删除
              </el-button>
            </div>
          </template>
          <template #prefix>
            <LayoutIcon name="plus.user" />
          </template>
          <template #suffix>
            <span class="dropdown-arrow" @click="serialInputRef?.focus()">&#x25BC;</span>
          </template>
        </el-autocomplete>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          placeholder="密码"
          type="password"
          show-password
        >
          <template #prefix>
            <LayoutIcon name="plus.lock" />
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="captcha">
        <el-input v-model="form.captcha" placeholder="验证码">
          <template #prefix>
            <LayoutIcon name="layout.captcha" />
          </template>
          <template #suffix>
            <FormCaptch ref="captchRef" v-model="form.uuid" />
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="agree">
        <el-checkbox v-model="form.agree">已阅读并同意《用户协议》和《隐私条款》</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :loading="loading || !user.ready"
          class="login-button"
          @click="handleSubmit"
        >
          {{ user.ready ? '登录' : '正在初始化...' }}
        </el-button>
      </el-form-item>
      <el-form-item class="login-ctl">
        <a @click.prevent="openSignup">注册账号</a>
        <a @click.prevent="openForgot">忘记密码</a>
      </el-form-item>
    </el-form>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  position: relative;
}

.login-page__titlebar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding-top: 80px;
  padding-bottom: 80px;
}

.login-header {
  display: flex;
  align-items: center;
  gap: 15px;
  height: 44px;
  line-height: 44px;
  margin-bottom: 40px;

  .login-title {
    font-size: 33px;
    color: var(--ev-c-text-1);
    font-weight: 600;
  }
}

.login-form {
  min-width: 260px;
  width: 368px;

  .login-ctl :deep(> div) {
    display: flex;
    justify-content: space-between;
    width: 100%;

    a {
      color: var(--el-color-primary);
      cursor: pointer;
    }
  }

  a {
    color: var(--el-color-primary);
  }
}

.login-button {
  width: 100%;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.dropdown-arrow {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.2s;

  &:hover {
    color: var(--el-color-primary);
  }
}
</style>
