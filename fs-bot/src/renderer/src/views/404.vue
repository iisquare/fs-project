<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <h1>404</h1>
    <p>抱歉，您访问的页面不存在</p>
    <el-button @click="router.replace('/home')">返回首页</el-button>
  </div>
</template>

<style lang="scss" scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  gap: 16px;

  h1 {
    font-size: 72px;
    color: var(--ev-c-text-1);
  }

  p {
    color: var(--ev-c-text-2);
    margin-bottom: 16px;
  }
}
</style>
