<script setup lang="ts">
/**
 * 返回按钮 - 点击优先调用 router.back()，若上一页为启动页或无历史记录则跳转到指定路由。
 *
 * 展示形式由 variant 选择，也可用默认插槽完全自定义内容：
 * - icon   仅图标（默认），用于工具栏
 * - text   无边框文字按钮：图标 + 文字
 * - button 描边按钮：图标 + 文字
 *
 * @prop {String|Object} to      - 无历史记录时的回退路由，支持字符串路径或 { name, path, query, params }
 * @prop {String}        title   - 按钮提示文本，默认"返回"，仅图标形式用作 tooltip
 * @prop {String}        label   - 文字按钮/描边按钮显示的文字，缺省时用插槽内容
 * @prop {String}        variant - 展示形式：icon(默认) | text | button
 * @prop {String}        icon    - 图标名称，默认"Back"，传空字符串则只显示文字
 *
 * 默认插槽 - 自定义按钮内容，设置后忽略 label
 *
 * @example
 * <!-- 仅图标 -->
 * <layout-back />
 *
 * <!-- 文字按钮：图标 + 返回 -->
 * <layout-back variant="text" label="返回" />
 *
 * <!-- 描边按钮 + 自定义内容 -->
 * <layout-back variant="button" to="/home">返回首页</layout-back>
 */
import { useRouter } from 'vue-router'
import LayoutIcon from './LayoutIcon.vue'

const props = withDefaults(defineProps<{
  to?: string | { name?: string; path?: string; query?: any; params?: any }
  title?: string
  label?: string
  variant?: 'icon' | 'text' | 'button'
  icon?: string
}>(), {
  variant: 'icon',
  icon: 'Back',
})

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
  <el-button
    v-if="'text' === variant || 'button' === variant"
    :text="'text' === variant"
    :title="title"
    @click="handleClick">
    <LayoutIcon v-if="icon" :name="icon" />
    <span v-if="label || $slots.default"><slot>{{ label }}</slot></span>
  </el-button>
  <LayoutIcon v-else :name="icon" class="fs-pointer" :title="title || '返回'" @click="handleClick" />
</template>

<style lang="scss" scoped>
</style>
