<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { FullScreen, ScaleToOriginal } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import DashboardApi from '@/api/bi/DashboardApi'
import DashboardRenderer from '@/views/bi/components/dashboard/DashboardRenderer.vue'
import { defaultDashboard } from '@/views/bi/components/dashboard/model'

const route = useRoute()
const loading = ref(true)
const viewerRef = ref<HTMLElement>()
const model = ref(defaultDashboard())
const displaySize = ref({ width: 0, height: 0 })
const scaleX = ref(1)
const scaleY = ref(1)
const fullscreen = ref(false)
const controlsVisible = ref(true)
let observer: ResizeObserver | null = null
let controlsTimer: ReturnType<typeof setTimeout> | null = null

const fit = () => {
  if (!viewerRef.value) return
  const width = viewerRef.value.clientWidth
  const height = viewerRef.value.clientHeight
  const displayWidth = model.value.canvas.displayWidthUnit === '%'
    ? width * model.value.canvas.displayWidth / 100
    : model.value.canvas.displayWidth
  const displayHeight = model.value.canvas.displayHeightUnit === '%'
    ? height * model.value.canvas.displayHeight / 100
    : model.value.canvas.displayHeight
  displaySize.value = { width: displayWidth, height: displayHeight }
  scaleX.value = displayWidth / Math.max(1, model.value.canvas.width)
  scaleY.value = displayHeight / Math.max(1, model.value.canvas.height)
}

const load = () => {
  if (!route.query.id) {
    loading.value = false
    return
  }
  DashboardApi.info(route.query.id).then((result: any) => {
    if (result.code !== 0) return
    try {
      if (result.data?.content) {
        const parsed = JSON.parse(result.data.content)
        model.value = Object.assign(defaultDashboard(), parsed)
        model.value.canvas = Object.assign(defaultDashboard().canvas, parsed.canvas || {})
      }
    } catch {
      model.value = defaultDashboard()
    }
  }).catch(() => {}).finally(async () => {
    loading.value = false
    await nextTick()
    fit()
  })
}

const scheduleControlsHide = () => {
  if (controlsTimer) clearTimeout(controlsTimer)
  if (!fullscreen.value) return
  controlsTimer = setTimeout(() => {
    controlsVisible.value = false
  }, 2200)
}
const revealControls = () => {
  controlsVisible.value = true
  scheduleControlsHide()
}
const toggleFullscreen = async () => {
  try {
    if (!document.fullscreenElement) {
      if (!viewerRef.value?.requestFullscreen) {
        ElMessage.warning('当前浏览器不支持全屏模式')
        return
      }
      await viewerRef.value.requestFullscreen()
    } else {
      await document.exitFullscreen()
    }
  } catch {
    ElMessage.warning('无法切换全屏模式')
  }
}
const onFullscreenChange = async () => {
  fullscreen.value = Boolean(document.fullscreenElement)
  controlsVisible.value = true
  if (controlsTimer) clearTimeout(controlsTimer)
  await nextTick()
  fit()
  scheduleControlsHide()
}
const scaledStyle = computed(() => ({
  width: `${displaySize.value.width}px`,
  height: `${displaySize.value.height}px`,
}))
const viewerStyle = computed(() => ({
  background: model.value.canvas.background || undefined,
  backgroundImage: model.value.canvas.backgroundImage
    ? `url("${model.value.canvas.backgroundImage}")`
    : undefined,
  backgroundSize: model.value.canvas.backgroundImage ? 'cover' : undefined,
  backgroundPosition: model.value.canvas.backgroundImage ? 'center' : undefined,
}))

onMounted(() => {
  load()
  document.addEventListener('fullscreenchange', onFullscreenChange)
  observer = new ResizeObserver(fit)
  if (viewerRef.value) observer.observe(viewerRef.value)
})
onBeforeUnmount(() => {
  observer?.disconnect()
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  if (controlsTimer) clearTimeout(controlsTimer)
})
</script>

<template>
  <div
    ref="viewerRef"
    class="screen-view"
    :style="viewerStyle"
    v-loading="loading"
    @pointermove="revealControls"
    @pointerdown="revealControls">
    <div
      class="screen-view__controls"
      :class="{ 'is-hidden': fullscreen && !controlsVisible }"
      @mouseenter="revealControls">
      <el-button
        class="screen-view__fullscreen"
        :icon="fullscreen ? ScaleToOriginal : FullScreen"
        :aria-label="fullscreen ? '退出全屏' : '进入全屏'"
        @click="toggleFullscreen">
        {{ fullscreen ? '退出全屏' : '进入全屏' }}
      </el-button>
    </div>
    <div class="screen-view__center" :style="scaledStyle">
      <DashboardRenderer :model="model" :scale-x="scaleX" :scale-y="scaleY" :scale="Math.min(scaleX, scaleY)" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.screen-view {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  &__center {
    position: relative;
    flex: none;
  }
  &__controls {
    position: fixed;
    top: 18px;
    right: 18px;
    z-index: 20;
    display: flex;
    align-items: center;
    gap: 7px;
    padding: 5px;
    border: 1px solid rgba(181, 213, 230, 0.24);
    border-radius: 9px;
    background: rgba(7, 23, 38, 0.68);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
    backdrop-filter: blur(10px);
    transition: opacity 0.22s ease, transform 0.22s ease;
    &.is-hidden {
      opacity: 0;
      transform: translateY(-8px);
      pointer-events: none;
    }
  }
  &__fullscreen {
    height: 32px;
    padding: 0 12px;
    color: #dff8ff;
    border-color: rgba(111, 203, 247, 0.28);
    background: rgba(27, 86, 127, 0.72);
    font-size: 12px;
    &:hover,
    &:focus {
      color: #fff;
      border-color: rgba(125, 218, 255, 0.62);
      background: rgba(33, 111, 160, 0.84);
    }
  }
}
</style>
