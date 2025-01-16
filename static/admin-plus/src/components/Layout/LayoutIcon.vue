<script setup lang="ts">
import DataUtil from '@/utils/DataUtil'
import icons from '@/assets/icons'
import * as ElementPlusIcons from '@element-plus/icons-vue';
defineProps<{
  name: string
}>()
</script>

<template>
  <el-icon v-if="name.includes('.')" v-html="DataUtil.value(icons, name)"></el-icon>
  <el-icon v-else-if="DataUtil.value(ElementPlusIcons, name)">
    <component :is="DataUtil.value(ElementPlusIcons, name)" />
  </el-icon>
</template>

<style lang="scss" scoped>
</style>
