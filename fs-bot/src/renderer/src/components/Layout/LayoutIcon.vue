<script setup lang="ts">
import icons from '@renderer/assets/icons'
import DataUtil from '@renderer/utils/DataUtil';

// 图标值（如 menu.apps）是一段原始 SVG 字符串，不能写成 <el-icon v-html="...">：
// el-icon 的根节点是 <i class="el-icon">，它把透传的 innerHTML 挂到该 <i> 上，
// 而同一节点里还渲染了 default 插槽，Vue 会警告 innerHTML prop 会覆盖子节点。
// 这里直接用同一个 <i class="el-icon"> 承载 v-html，DOM 与 el-icon 的渲染结果一致。
const props = defineProps<{
  name: any
  filled?: boolean
}>()
</script>

<template>
  <i v-if="name && name.includes('.')" class="el-icon" v-html="DataUtil.value(icons, name)"></i>
  <el-icon v-else>
    <component :is="name" :filled="filled" />
  </el-icon>
</template>
