<script setup lang="ts">
import CaptchApi from '@/api/member/CaptchApi';
import { computed, onMounted, ref } from 'vue';
import LayoutIcon from '../Layout/LayoutIcon.vue';

const model: any = defineModel()
const captcha: any = ref({
  base64: '',
  retry: 0, // 重试次数
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
