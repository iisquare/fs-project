<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { FullScreen } from '@element-plus/icons-vue'
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
let observer: ResizeObserver | null = null

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

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}

const scaledStyle = computed(() => ({
  width: `${displaySize.value.width}px`,
  height: `${displaySize.value.height}px`,
}))

onMounted(() => {
  load()
  observer = new ResizeObserver(fit)
  if (viewerRef.value) observer.observe(viewerRef.value)
})
onBeforeUnmount(() => observer?.disconnect())
</script>

<template>
  <div ref="viewerRef" class="screen-view" v-loading="loading">
    <el-button class="screen-view__fullscreen" circle :icon="FullScreen" title="全屏展示" @click="toggleFullscreen" />
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
  background: #020b1c;
  &__center {
    position: relative;
    flex: none;
  }
  &__fullscreen {
    position: fixed;
    right: 18px;
    top: 18px;
    z-index: 20;
    color: #dff8ff;
    background: rgba(12, 54, 90, 0.72);
    border-color: rgba(74, 189, 242, 0.45);
  }
}
</style>
