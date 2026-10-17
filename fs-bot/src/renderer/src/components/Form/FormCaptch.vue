<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import LayoutIcon from '@renderer/components/Layout/LayoutIcon.vue'
import CaptchApi from '@renderer/api/CaptchApi'
import { networkErrorMessage } from '@renderer/utils/ErrorUtil'

interface CaptchaResult {
  uuid: string
  base64: string
  retry: number
}

const model: any = defineModel()
const emit = defineEmits<{
  success: []
  error: [message: string]
}>()

const captcha = ref<CaptchaResult>({
  uuid: '',
  base64: '',
  retry: 0
})
const loading = ref(false)
const loadError = ref('')
const base64 = computed(() => (captcha.value.base64 ? `url("${captcha.value.base64}")` : 'none'))

async function reload(): Promise<void> {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  // 没有拿到新图之前先清掉旧 uuid，避免提交一张与当前画面不对应的验证码
  model.value = ''

  const param = {
    uuid: captcha.value.uuid,
    retry: captcha.value.retry + 1,
    time: Date.now()
  }
  captcha.value = { ...captcha.value, retry: param.retry }

  try {
    const result: any = await CaptchApi.generate(param, { error: false, warning: false })
    const data = result?.data ?? {}
    if (!data.base64 || !data.uuid) throw new Error('验证码返回内容不完整')
    captcha.value = {
      uuid: String(data.uuid),
      base64: String(data.base64),
      retry: Number(data.retry ?? param.retry)
    }
    model.value = captcha.value.uuid
    emit('success')
  } catch (error) {
    captcha.value = { ...captcha.value, uuid: '', base64: '' }
    loadError.value = networkErrorMessage(error, '验证码加载失败')
    emit('error', loadError.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void reload()
})

defineExpose({ reload })
</script>

<template>
  <button
    type="button"
    class="fs-captcha"
    :class="{ 'is-loading': loading, 'is-error': !!loadError }"
    :title="loadError || '点击刷新验证码'"
    :aria-label="loadError || '刷新验证码'"
    @click="reload"
  >
    <span v-if="loading" class="fs-captcha__state">
      <LayoutIcon name="plus.loading" class="fs-spin" />
      <span>加载中</span>
    </span>
    <span v-else-if="loadError" class="fs-captcha__state fs-captcha__state--error">
      <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
        <circle cx="8" cy="8" r="6.2" fill="none" stroke="currentColor" stroke-width="1.3" />
        <path
          d="M8 4.8v4.1"
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
        />
        <circle cx="8" cy="11.4" r="0.8" fill="currentColor" />
      </svg>
      <span>加载失败·重试</span>
    </span>
  </button>
</template>

<style lang="scss" scoped>
.fs-captcha {
  width: 100px;
  height: var(--el-input-height);
  padding: 0;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  pointer-events: auto;
  appearance: none;
  background-color: color-mix(in srgb, var(--el-bg-color) 78%, transparent);
  background-image: v-bind(base64);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  border: 0;
  border-radius: var(--el-border-radius-base);
  transition:
    color 0.2s,
    background-color 0.2s;

  &:hover {
    color: var(--el-color-primary);
  }

  &.is-loading {
    cursor: wait;
  }

  &.is-error {
    color: var(--el-color-danger);
    background-color: color-mix(in srgb, var(--el-color-danger) 7%, var(--el-bg-color));
    box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--el-color-danger) 24%, transparent);
  }
}

.fs-spin {
  animation: fs-captcha-spin 0.9s linear infinite;
}

.fs-captcha__state {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 100%;
  padding: 0 6px;
  font-size: 11px;
  line-height: 1.2;
  white-space: nowrap;
}

.fs-captcha__state--error {
  flex-direction: column;
  gap: 1px;
  padding: 0 3px;
  font-size: 10px;
}

@keyframes fs-captcha-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
