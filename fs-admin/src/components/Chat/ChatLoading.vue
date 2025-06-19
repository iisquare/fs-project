<script setup lang="ts">
</script>

<template>
  <div class="loading-dots">
    <div class="loading-dot"></div>
    <div class="loading-dot"></div>
    <div class="loading-dot"></div>
  </div>
</template>

<style lang="scss" scoped>
.loading-dots {
  display: flex; /* 使用 Flexbox 排列圆点 */
  align-items: center;
  gap: 4px; /* 圆点之间的间距 */
}

.loading-dot {
  width: 6px; /* 圆点宽度 */
  height: 6px; /* 圆点高度 */
  background-color: #3b82f6; /* 蓝色 */
  border-radius: 50%; /* 使其成为圆形 */
  animation: fade-pulse 1.2s infinite ease-in-out; /* 应用新的渐变脉冲动画 */
}

/* 为每个圆点设置不同的动画延迟，以实现错开的渐变效果 */
.loading-dot:nth-child(1) {
  animation-delay: 0s;
}

.loading-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dot:nth-child(3) {
  animation-delay: 0.4s;
}

/* 定义渐变脉冲动画 */
@keyframes fade-pulse {
  0%, 100% {
    opacity: 0.2; /* 初始和结束时透明度较低 */
  }
  60% {
    opacity: 1; /* 动画中间透明度为1（完全可见） */
  }
}
</style>
