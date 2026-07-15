<script setup lang="ts">
/**
 * 返回按钮 - 点击优先调用 router.back()，若上一页为启动页或无历史记录则跳转到指定路由。
 *
 * @prop {String|Object}  to    - 无历史记录时的回退路由，支持字符串路径或 { name, path, query, params }
 * @prop {String}         title - 按钮提示文本，默认"返回"
 *
 * @example
 * <!-- 默认回退到上一页 -->
 * <layout-back />
 *
 * <!-- 无历史时跳转到首页 -->
 * <layout-back to="/home" title="返回首页" />
 */
import { useRouter } from 'vue-router'
import LayoutIcon from './LayoutIcon.vue'

const props = defineProps<{
  to?: string | { name?: string; path?: string; query?: any; params?: any }
  title?: string
}>()

const router = useRouter()

const skipBackPaths: string[] = ['/startup']

const isSkipBack = (backPath: string) => {
  return skipBackPaths.some(path => backPath === path || backPath.startsWith(path + '?'))
}

const handleClick = () => {
  const history = router.options.history as any
  const backPath = history?.state?.back
  if (backPath && !isSkipBack(backPath)) {
    router.back()
    return
  }
  if (props.to) {
    router.replace(props.to as any).catch(() => {})
  }
}
</script>

<template>
  <LayoutIcon name="Back" class="fs-pointer" :title="title || '返回'" @click="handleClick" />
</template>

<style lang="scss" scoped>
</style>
