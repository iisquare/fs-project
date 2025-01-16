<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'
import DataUtil from '@/utils/DataUtil'

const loading = ref(true)
const user = useUserStore()
const route = useRoute()
const router = useRouter()

user.reload().then(() => {
  loading.value = false
})

const completed = () => {
  let url = route.query.redirect
  if (DataUtil.empty(url)) url = '/'
  router.push({
    path: (url as string)
  }).catch(err => err)
}

watch(() => user.ready, (value) => {
  value && completed()
})

const tip = computed(() => {
  if (loading.value) return '正在获取配置...'
  if (!user.ready) return '配置失败，请稍后再试'
  return user.info.id > 0 ? '配置成功，前往工作页面...' : '用户未登录，前往授权页面...'
})
</script>

<template>
  <div class="animbox">
    <div></div>
    <div></div>
    <div></div>
    <div></div>
    <div></div>
  </div>
  <div class="animtip">{{ tip }}</div>
</template>

<style lang="scss" scoped>
</style>
