<script setup lang="ts">
/**
 * 搜索切换按钮 - 圆形图标按钮，点击可展示/隐藏搜索栏。
 *
 * @prop {Boolean}  loading  - 加载中状态，绑至 v-model:loading，默认 false
 *
 * @example
 * <button-search v-model:loading="searching" @click="toggleSearch" />
 */
import * as ElementPlusIcons from '@element-plus/icons-vue';

const loading = defineModel('loading', { type: Boolean, default: false })

</script>

<template>
  <el-button :icon="ElementPlusIcons.Search" circle title="展示/隐藏搜索栏" :loading="loading" />
</template>

<style lang="scss" scoped>
</style>
