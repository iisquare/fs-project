<script setup lang="ts">
/**
 * 接口业务提示 - 在提示正文下方提供响应 data 的折叠查看入口。
 * data 默认收起；没有 data 时只展示消息，避免提示中多出一个无效按钮。
 */
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { computed, ref } from 'vue'

const props = defineProps<{
  message?: unknown,
  data?: unknown,
}>()

const emit = defineEmits<{
  expand: [],
}>()

const expanded = ref(false)
const hasData = computed(() => props.data !== null && props.data !== undefined)

const toggle = () => {
  if (!expanded.value) emit('expand')
  expanded.value = !expanded.value
}

/** JSON 字符串也格式化展示；非 JSON 字符串保持原文，便于查看普通错误信息。 */
const formatData = (value: unknown) => {
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }
  try {
    return JSON.stringify(value, null, 2) ?? String(value)
  } catch {
    return String(value)
  }
}

const dataText = computed(() => formatData(props.data))
</script>

<template>
  <div class="api-response-notice">
    <div class="api-response-notice__head">
      <div class="api-response-notice__message">{{ message }}</div>
      <el-button
        v-if="hasData"
        class="api-response-notice__toggle"
        link
        type="primary"
        size="small"
        :icon="expanded ? ArrowUp : ArrowDown"
        :title="expanded ? '收起详情' : '显示详情'"
        :aria-label="expanded ? '收起详情' : '显示详情'"
        :aria-expanded="expanded"
        @click="toggle"
      />
    </div>
    <pre v-if="expanded" class="api-response-notice__data">{{ dataText }}</pre>
  </div>
</template>

<style lang="scss" scoped>
.api-response-notice {
  @include flex-start-column;
  align-items: stretch;
  gap: 4px;
}

.api-response-notice__head {
  width: 100%;
}

.api-response-notice__message {
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.api-response-notice__toggle {
  position: absolute;
}

.api-response-notice__data {
  box-sizing: border-box;
  width: 100%;
  max-height: 280px;
  margin: 2px 0 0;
  padding: 8px 10px;
  overflow: auto;
  color: var(--el-text-color-regular);
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>

<style lang="scss">
/* 按钮放进通知右上角的操作区，正文保持完整宽度；右侧为关闭按钮预留空间 */
.api-response-notification {
  .api-response-notice__toggle {
    top: 16px;
    right: 42px;
  }

  .el-notification__title {
    padding-right: 76px;
  }
}
</style>
