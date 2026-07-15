<script setup lang="ts">
/**
 * 图标组件 - 统一的图标渲染器，支持自定义 SVG 图标（通过 icons 映射）和 Element Plus 图标库。
 *
 * @prop {String|undefined} name - 图标名称。含 '.' 时按路径从 icons 映射中查找 SVG；否则从 Element Plus 图标库查找
 *
 * @example
 * <!-- Element Plus 图标 -->
 * <layout-icon name="Edit" />
 *
 * <!-- 自定义 SVG 图标（按路径） -->
 * <layout-icon name="layout.toolbar" />
 */
import DataUtil from '@/utils/DataUtil'
import icons from '@/assets/icons'
import * as ElementPlusIcons from '@element-plus/icons-vue';
defineProps<{
  name: string | undefined
}>()
</script>

<template>
  <el-icon v-if="name && name.includes('.')" v-html="DataUtil.value(icons, name)"></el-icon>
  <el-icon v-else-if="name && DataUtil.value(ElementPlusIcons, name)">
    <component :is="DataUtil.value(ElementPlusIcons, name)" />
  </el-icon>
</template>

<style lang="scss" scoped>
</style>
