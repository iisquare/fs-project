<script setup lang="ts">
/**
 * 工具调用节点 - 一轮回复里模型发起的每次调用：函数名、参数与返回结果。
 *
 * 每次调用都是调用过程时间线上的一格（三种节点里最小的那个点，颜色按状态变），
 * 行头先说清「调的是什么」，点开才铺开参数与返回。这里只给节点、不包外层容器：
 * 节点按片段渲染，与轮次、思考过程共用同一条主轴。
 */
import { ref } from 'vue'
import type { ChatToolCall } from '@renderer/stores/chat'
import { formatDuration } from '@renderer/utils/TimeUtil'

const props = defineProps<{
  calls: ChatToolCall[]
}>()

/** 展开的节点：默认都收起，点行头才铺开参数与返回 */
const opened = ref<string[]>([])

/** 参数摘要：把 JSON 拆成 key: value，长值截断，一眼看清这次调用要动什么 */
function summary(call: ChatToolCall): string {
  const text = String(call.args ?? '').trim()
  if (!text) return '（无参数）'
  try {
    const value = JSON.parse(text)
    if (!value || 'object' !== typeof value) return String(value)
    const parts = Object.entries(value).map(([key, item]) => {
      const raw = 'string' === typeof item ? item : JSON.stringify(item)
      return `${key}: ${raw.length > 60 ? `${raw.slice(0, 60)}…` : raw}`
    })
    return parts.join('  ') || '（无参数）'
  } catch {
    return text.length > 100 ? `${text.slice(0, 100)}…` : text
  }
}

/** 参数与结果都按 JSON 美化展示：模型给出的大多是一行 JSON，展开后可读性优先 */
function pretty(text: string): string {
  const value = String(text ?? '').trim()
  if (!value) return '（空）'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function key(call: ChatToolCall, index: number): string {
  return call.id || `${call.name}-${index}`
}

function isOpen(call: ChatToolCall, index: number): boolean {
  return opened.value.includes(key(call, index))
}

function toggle(call: ChatToolCall, index: number): void {
  const id = key(call, index)
  opened.value = isOpen(call, index)
    ? opened.value.filter((item) => item !== id)
    : [...opened.value, id]
}

function state(call: ChatToolCall): string {
  if ('success' === call.status) return '已完成'
  if ('failed' === call.status) return '失败'
  return call.result ? '调用中' : '模型正在生成参数…'
}
</script>

<template>
  <div
    class="timeline-step is-call"
    :class="[call.status, { open: isOpen(call, index) }]"
    v-for="(call, index) in props.calls"
    :key="key(call, index)"
  >
    <button
      type="button"
      class="step-head"
      :aria-expanded="isOpen(call, index) ? 'true' : 'false'"
      @click="toggle(call, index)"
    >
      <span class="step-title">{{ call.name || '（未命名调用）' }}</span>
      <span class="step-summary">{{ summary(call) }}</span>
      <el-tag class="tool-scope" size="small" effect="plain" v-if="'server' === call.scope"
        >项目工具</el-tag
      >
      <span class="step-meta">{{ state(call) }} {{ formatDuration(call.duration) }}</span>
      <svg class="step-caret" viewBox="0 0 16 16" width="10" height="10" aria-hidden="true">
        <path
          d="M6 3.6 10.4 8 6 12.4"
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>
    <div class="step-body" v-show="isOpen(call, index)">
      <div class="tool-block">
        <div class="tool-label">参数</div>
        <pre class="tool-text">{{ pretty(call.args) }}</pre>
      </div>
      <div class="tool-block">
        <div class="tool-label">返回</div>
        <el-scrollbar class="tool-text" max-height="260px">
          <pre class="tool-pre" :class="{ failed: 'failed' === call.status }">{{
            call.result || '等待执行…'
          }}</pre>
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '../../assets/timeline' as timeline;

/* 工具调用：三种节点里最小的一种，颜色按调用状态走 */
.timeline-step.is-call {
  @include timeline.node(6px);

  &::after {
    background: var(--el-text-color-placeholder);
  }

  /* 执行中：呼吸的主色点；成功绿、失败红 */
  &.running::after {
    background: var(--el-color-primary);
    animation: tool-pulse 1s ease-in-out infinite;
  }

  &.success::after {
    background: var(--el-color-success);
  }

  &.failed::after {
    background: var(--el-color-danger);
  }

  .step-title {
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    color: var(--el-text-color-regular);
  }

  &:hover .step-title {
    color: var(--el-color-primary);
  }
}

@keyframes tool-pulse {
  0%,
  100% {
    opacity: 0.35;
  }

  50% {
    opacity: 1;
  }
}

.tool-scope {
  flex: none;
}

.tool-block + .tool-block {
  margin-top: 6px;
}

.tool-label {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.tool-text {
  margin-top: 2px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--el-text-color-regular);
  padding: 6px 8px;
  margin-bottom: 0;
}

.tool-pre {
  margin: 0;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  white-space: pre-wrap;
  word-break: break-word;
  color: inherit;

  &.failed {
    color: var(--el-color-danger);
  }
}
</style>
