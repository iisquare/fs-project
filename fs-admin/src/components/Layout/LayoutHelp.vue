<script setup lang="ts">
/**
 * 表单标签帮助图标 - 把字段说明从输入框下方移到标签后，悬浮展示
 *
 * @prop {String} text 说明文字，为空时不渲染
 */
import { QuestionFilled } from '@element-plus/icons-vue'

defineProps<{ text?: string }>()
</script>

<template>
  <el-tooltip v-if="text" :content="text" placement="top" :show-after="200">
    <el-icon class="fs-help"><QuestionFilled /></el-icon>
  </el-tooltip>
</template>

<style lang="scss" scoped>
.fs-help {
  margin-left: 4px;
  vertical-align: -2px;
  color: var(--el-text-color-placeholder);
  cursor: help;
}
</style>
