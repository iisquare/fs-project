<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue';

const model: any = defineModel()

</script>

<template>
  <el-button link :icon="model ? ElementPlusIcons.ArrowUp : ElementPlusIcons.ArrowDown" @click="model = !model">{{ model ? '收起' : '展开' }}</el-button>
</template>

<style lang="scss" scoped>
</style>
