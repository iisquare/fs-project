<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserApi from '@renderer/api/UserApi'
import FormCaptch from '@renderer/components/Form/FormCaptch.vue'
import LayoutIcon from '@renderer/components/Layout/LayoutIcon.vue'
import TitleBar from '@renderer/components/Layout/TitleBar.vue'
import BrandLogo from '@renderer/components/Brand/BrandLogo.vue'
import { useUserStore } from '@renderer/stores/user'
import { useLoadingStore } from '@renderer/stores/loading'
import { useRouter, useRoute } from 'vue-router'
import ApiUtil from '@renderer/utils/ApiUtil'
import Config from '@renderer/core/Config'
import { page } from '@renderer/router/config'
import { errorMessage, networkErrorMessage } from '@renderer/utils/ErrorUtil'

const user = useUserStore()
const appLoading = useLoadingStore()
const captchRef: any = ref(null)
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()
const serialInputRef = ref<any>(null)
const captchaError = ref('')
const submitError = ref('')

const form: any = ref({})

const rules = {
  serial: [{ required: true, message: '请输入帐号名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入帐号密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  agree: [{ required: true, message: '请仔细阅读并同意后继续', trigger: 'change' }]
}

const historyList = ref<string[]>([])

async function loadHistory() {
  try {
    historyList.value = (await Config.listLoginHistory()) || []
    if (historyList.value.length && !form.value.serial) {
      form.value.serial = historyList.value[0]
    }
  } catch (error) {
    ElMessage.error(errorMessage(error, '登录记录加载失败'))
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
  submitError.value = ''
  formRef.value?.validate(async (valid) => {
    if (!valid || loading.value) return
    loading.value = true
    try {
      const result = await UserApi.login(form.value, { error: false, warning: false })
      // 登录历史写入失败不影响登录流程
      Config.saveLoginHistory(form.value.serial).catch(() => {})
      await user.reset(ApiUtil.data(result))
      redirect()
    } catch (error) {
      loading.value = false
      submitError.value = networkErrorMessage(error, '登录失败')
      captchRef.value?.reload()
    }
  })
}

function handleCaptchaError(message: string): void {
  captchaError.value = message
}

function handleCaptchaSuccess(): void {
  captchaError.value = ''
}

/** watch 与登录成功回调都可能触发跳转：同一时间只走一次窗口切换 */
let redirecting = false
/** 切主界面时先隐藏登录页本体：遮罩是半透明的，不能让表单从下面透出来 */
const leaving = ref(false)

function destination(): string {
  const target = route.query.redirect
  return typeof target === 'string' && target ? target : page.home
}

const redirect = async (): Promise<void> => {
  if (redirecting) return
  redirecting = true
  const release = appLoading.show('正在进入主界面...')
  leaving.value = true
  const url = destination()
  try {
    // 先把遮罩画出来，再改窗口尺寸并直接进主界面；路由资源在遮罩下加载
    await nextTick()
    const resized = window.ipc
      .setNormalSize()
      .catch((error) => console.error('[Login] Failed to resize window:', error))
    await router.push(url)
    await resized
    // 两帧之后再显示：让主界面按 900×670 完成布局与首次绘制
    await new Promise<void>((done) => {
      requestAnimationFrame(() => requestAnimationFrame(() => done()))
    })
  } catch (error) {
    console.error('[Login] Failed to enter main window:', error)
    leaving.value = false
    await window.ipc
      .setLoginSize()
      .catch((resizeError) => console.error('[Login] Failed to restore login size:', resizeError))
    loading.value = false
  } finally {
    release()
    redirecting = false
  }
}

onMounted(() => {
  void window.ipc
    .setLoginSize()
    .catch((error) => console.error('[Login] Failed to set login size:', error))
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
  <div class="login-page" :class="{ 'login-page--leaving': leaving }">
    <TitleBar class="login-page__titlebar" :show-maximize="false" />
    <div class="login-container">
      <div class="login-stage">
        <div class="login-brand">
          <BrandLogo :size="38" class="brand-mark" />
          <div class="brand-copy">
            <span class="brand-name">fs-bot</span>
            <span class="brand-tagline">智能体工作台</span>
          </div>
        </div>
        <main class="login-main">
          <div class="login-header">
            <span class="login-title">欢迎回来</span>
            <p class="login-subtitle">登录后继续使用你的项目与对话</p>
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
                :fetch-suggestions="
                  (_query: string, cb: (list: { value: string }[]) => void) =>
                    cb(historyList.map((s) => ({ value: s })))
                "
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
              <el-input v-model="form.password" placeholder="密码" type="password" show-password>
                <template #prefix>
                  <LayoutIcon name="plus.lock" />
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="captcha" class="login-captcha">
              <el-input v-model="form.captcha" placeholder="验证码">
                <template #prefix>
                  <LayoutIcon name="layout.captcha" />
                </template>
                <template #suffix>
                  <FormCaptch
                    ref="captchRef"
                    v-model="form.uuid"
                    @success="handleCaptchaSuccess"
                    @error="handleCaptchaError"
                  />
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="agree" class="login-agree">
              <el-checkbox v-model="form.agree">已阅读并同意《用户协议》和《隐私条款》</el-checkbox>
            </el-form-item>
            <el-form-item class="login-submit">
              <el-button
                type="primary"
                :loading="loading || !user.ready"
                class="login-button"
                @click="handleSubmit"
              >
                {{ user.ready ? '登录' : '正在初始化...' }}
              </el-button>
            </el-form-item>
            <div
              class="login-feedback"
              :class="{ 'is-error': !!submitError, 'is-warning': !submitError && !!captchaError }"
              role="status"
              aria-live="polite"
            >
              <template v-if="submitError">
                <span class="login-feedback__mark" aria-hidden="true">!</span>
                <span class="login-feedback__text" :title="submitError">{{ submitError }}</span>
              </template>
              <template v-else-if="captchaError">
                <span class="login-feedback__mark" aria-hidden="true">!</span>
                <span class="login-feedback__text" :title="captchaError">{{ captchaError }}</span>
              </template>
            </div>
            <el-form-item class="login-ctl">
              <a @click.prevent="openSignup">注册账号</a>
              <a @click.prevent="openForgot">忘记密码</a>
            </el-form-item>
          </el-form>
        </main>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  position: relative;
  background:
    radial-gradient(
      circle at 18% 8%,
      color-mix(in srgb, var(--el-color-primary) 12%, transparent),
      transparent 33%
    ),
    radial-gradient(
      circle at 88% 92%,
      color-mix(in srgb, var(--el-color-primary) 9%, transparent),
      transparent 36%
    ),
    var(--el-bg-color-page);

  &::before {
    position: absolute;
    top: -110px;
    right: -100px;
    width: 280px;
    height: 280px;
    content: '';
    border: 1px solid color-mix(in srgb, var(--el-color-primary) 10%, transparent);
    border-radius: 50%;
    box-shadow:
      0 0 0 48px color-mix(in srgb, var(--el-color-primary) 3%, transparent),
      0 0 0 96px color-mix(in srgb, var(--el-color-primary) 2%, transparent);
  }
}

.login-page--leaving {
  opacity: 0;
}

.login-page__titlebar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

.login-container {
  height: 100%;
  padding: calc(var(--title-bar-height) + 18px) 24px 18px;
  overflow-y: auto;
}

.login-stage {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: min(100%, 520px);
  min-height: 100%;
  margin: 0 auto;
}

.login-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  min-height: 0;
  padding: 10px 0;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;

  .brand-mark {
    flex-shrink: 0;
  }

  .brand-copy {
    display: flex;
    flex-direction: column;
    gap: 1px;
  }

  .brand-name {
    font-size: 14px;
    font-weight: 600;
    line-height: 1.3;
    color: var(--ev-c-text-1);
  }

  .brand-tagline {
    font-size: 11px;
    line-height: 1.4;
    color: var(--ev-c-text-3);
  }
}

.login-header {
  margin-bottom: 18px;

  .login-title {
    font-size: 28px;
    line-height: 1.3;
    color: var(--ev-c-text-1);
    font-weight: 650;
    letter-spacing: -0.02em;
  }

  .login-subtitle {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.55;
    color: var(--ev-c-text-3);
  }
}

.login-form {
  width: 100%;

  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  :deep(.el-input__wrapper) {
    min-height: 44px;
    background: color-mix(in srgb, var(--el-bg-color) 76%, transparent);
    border-radius: 9px;
    box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-border-color) 76%, transparent) inset;
    backdrop-filter: blur(10px);
    transition:
      background-color 0.2s,
      box-shadow 0.2s;
  }

  :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px var(--el-border-color-hover) inset;
  }

  :deep(.el-input__wrapper.is-focus) {
    background: var(--el-bg-color);
    box-shadow: 0 0 0 1px var(--el-color-primary) inset;
  }

  .login-ctl :deep(> div) {
    display: flex;
    justify-content: space-between;
    width: 100%;

    a {
      color: var(--el-color-primary);
      cursor: pointer;
      font-size: 13px;
    }
  }

  a {
    color: var(--el-color-primary);
  }
}

.login-button {
  width: 100%;
  height: 44px;
  font-weight: 500;
}

.login-submit {
  margin-bottom: 4px !important;
}

.login-captcha {
  margin-bottom: 20px !important;
}

.login-agree {
  margin-bottom: 22px !important;
}

.login-feedback {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: none;
  height: 18px;
  margin-bottom: 4px;
  overflow: hidden;
  font-size: 11px;
  line-height: 18px;
  color: transparent;

  &.is-error {
    color: var(--el-color-danger);
  }

  &.is-warning {
    color: var(--el-color-warning);
  }
}

.login-feedback__mark {
  flex: none;
  font-weight: 700;
  line-height: 1;
}

.login-feedback__text {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-ctl {
  padding-top: 8px;
  margin-top: 0;
  margin-bottom: 0 !important;
  border-top: 1px solid color-mix(in srgb, var(--el-border-color) 58%, transparent);
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
