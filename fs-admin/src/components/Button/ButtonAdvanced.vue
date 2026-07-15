<script setup lang="ts">
/**
 * 展开/收起切换按钮 - 点击可在展开与收起状态间切换，图标随状态变化。
 *
 * @v-model {Boolean}  当前展开状态，true=展开，false=收起
 *
 * @example
 * <button-advanced v-model="filters.advanced" />
 * <template v-if="filters.advanced"></template>
 */
import * as ElementPlusIcons from '@element-plus/icons-vue';

const model: any = defineModel()

</script>

<template>
  <el-button link :icon="model ? ElementPlusIcons.ArrowUp : ElementPlusIcons.ArrowDown" @click="model = !model">{{ model ? '收起' : '展开' }}</el-button>
</template>

<style lang="scss" scoped>
</style>
