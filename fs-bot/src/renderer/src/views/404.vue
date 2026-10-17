<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <div class="error-art" aria-hidden="true">
      <span>404</span>
    </div>
    <span class="error-eyebrow">页面未找到</span>
    <h1>这里暂时没有内容</h1>
    <p>链接可能已经失效，或页面地址发生了变化。</p>
    <el-button type="primary" @click="router.replace('/home')">返回首页</el-button>
  </div>
</template>

<style lang="scss" scoped>
.error-page {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  padding: 40px 24px;
  overflow: hidden;
  text-align: center;
  background:
    radial-gradient(
      circle at 50% 38%,
      color-mix(in srgb, var(--el-color-primary) 9%, transparent),
      transparent 34%
    ),
    var(--el-bg-color-page);

  .error-eyebrow {
    margin-top: 26px;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.12em;
    color: var(--el-color-primary);
  }

  h1 {
    margin-top: 6px;
    font-size: 26px;
    line-height: 1.35;
    font-weight: 650;
    color: var(--ev-c-text-1);
  }

  p {
    margin: 8px 0 24px;
    font-size: 14px;
    color: var(--ev-c-text-2);
  }
}

.error-art {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 168px;
  height: 168px;
  color: var(--el-color-primary);
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--el-color-primary) 12%, var(--el-bg-color)),
    var(--el-bg-color)
  );
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, var(--el-border-color-lighter));
  border-radius: 50%;
  box-shadow:
    0 24px 60px color-mix(in srgb, var(--el-color-primary) 12%, transparent),
    inset 0 1px 0 rgb(255 255 255 / 50%);

  &::before,
  &::after {
    position: absolute;
    content: '';
    border-radius: 50%;
    border: 1px solid color-mix(in srgb, var(--el-color-primary) 10%, transparent);
  }

  &::before {
    inset: -18px;
  }

  &::after {
    inset: -36px;
  }

  span {
    font-size: 48px;
    font-weight: 700;
    letter-spacing: -0.05em;
  }
}
</style>
