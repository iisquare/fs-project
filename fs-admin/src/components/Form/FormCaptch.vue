<script setup lang="ts">
/**
 * 图形验证码组件 - 显示验证码图片，点击可刷新，自动绑定生成的 UUID。
 *
 * @v-model  {String}   验证码 UUID（双向绑定主值），提交时需将 uuid 和用户输入的 code 一起发送
 *
 * @expose {Function} reload - 刷新验证码
 *
 * @example
 * <form-captch ref="captchaRef" v-model="captchaUuid" />
 *
 * <!-- 提交时 -->
 * <!-- { uuid: captchaUuid, code: userInput } -->
 */
import CaptchApi from '@/api/member/CaptchApi';
import { computed, onMounted, ref } from 'vue';
import LayoutIcon from '../Layout/LayoutIcon.vue';

const model: any = defineModel()
const captcha: any = ref({
  base64: '',
  retry: 0,
})
const loading = ref(false)
const base64 = computed(() => {
  return `url('${captcha.value.base64}')`
})

const reload = () => {
  loading.value = true
  const param = {
    uuid: captcha.value.uuid,
    retry: captcha.value.retry + 1,
    time: Date.now(),
  }
  CaptchApi.generate(param).then((result: any) => {
    captcha.value = result.data
    model.value = captcha.value.uuid
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  reload()
})

defineExpose({ reload })
</script>

<template>
  <div class="fs-captcha" @click="reload">
    <LayoutIcon name="Loading" class="fs-spin" v-if="loading" />
  </div>
</template>

<style lang="scss" scoped>
.fs-captcha {
  width: 100px;
  cursor: pointer;
  height: var(--el-input-height);
  line-height: var(--el-input-height);
  background-size: cover;
  background-image: v-bind(base64);
  background-color: var(--el-input-bg-color, var(--el-fill-color-blank));
}
</style>
