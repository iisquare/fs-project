<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useLoadingStore } from '@renderer/stores/loading'
import loadingAnimation from '@renderer/assets/images/app-loading.gif'
import loadingStatic from '@renderer/assets/images/app-loading-static.png'

const loading = useLoadingStore()
const root = ref<HTMLElement | null>(null)

// 遮罩出现后把焦点收上来：除了挡住鼠标，输入框与页面快捷键也一并停掉
watch(
  () => loading.visible,
  async (visible) => {
    if (!visible) return
    await nextTick()
    root.value?.focus({ preventScroll: true })
  },
  { immediate: true }
)
</script>

<template>
  <Transition name="app-loading">
    <div
      v-if="loading.visible"
      ref="root"
      class="app-loading"
      tabindex="-1"
      role="status"
      aria-live="polite"
      aria-busy="true"
      @keydown.capture.stop.prevent
    >
      <div class="app-loading__content">
        <picture class="app-loading__picture">
          <source media="(prefers-reduced-motion: reduce)" :srcset="loadingStatic" />
          <img
            class="app-loading__logo"
            :src="loadingAnimation"
            alt=""
            aria-hidden="true"
            draggable="false"
          />
        </picture>
        <span class="app-loading__text">{{ loading.text }}</span>
      </div>
    </div>
  </Transition>
</template>

<style lang="scss" scoped>
.app-loading {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: grid;
  place-items: center;
  outline: none;
  cursor: progress;
  user-select: none;
  -webkit-app-region: no-drag;
  background: rgba(255, 255, 255, 0.54);
  background: color-mix(in srgb, var(--el-bg-color-page, #fff) 54%, transparent);
  backdrop-filter: blur(6px) saturate(1.04);
  -webkit-backdrop-filter: blur(6px) saturate(1.04);
}

.app-loading__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 13px;
}

.app-loading__picture {
  display: block;
  width: 116px;
  height: 116px;
}

.app-loading__logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
  user-select: none;
  pointer-events: none;
  filter: drop-shadow(0 12px 22px rgba(17, 24, 39, 0.2));
}

.app-loading__text {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  letter-spacing: 0.02em;
  line-height: 1.4;
}

/* 出现时底层遮罩立即到位，只让 logo 与文字做轻微入场；窗口尺寸切换不能被淡入露出 */
.app-loading-enter-active {
  transition: none;
}

.app-loading-leave-active {
  transition: opacity 0.18s ease;
}

.app-loading-enter-active .app-loading__content,
.app-loading-leave-active .app-loading__content {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.app-loading-leave-to {
  opacity: 0;
}

.app-loading-enter-from .app-loading__content,
.app-loading-leave-to .app-loading__content {
  opacity: 0;
  transform: translateY(6px) scale(0.98);
}

@media (prefers-reduced-motion: reduce) {
  .app-loading-enter-active,
  .app-loading-leave-active,
  .app-loading-enter-active .app-loading__content,
  .app-loading-leave-active .app-loading__content {
    transition-duration: 0.01ms;
  }
}
</style>
