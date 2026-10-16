<script setup lang="ts">
/**
 * 工具调用节点 - 一轮回复里模型发起的每次调用：函数名、参数与返回结果。
 *
 * 每次调用都是调用过程时间线上的一格（三种节点里最小的那个点，颜色按状态变），
 * 行头先说清「调的是什么」，点开才铺开参数与返回。这里只给节点、不包外层容器：
 * 节点按片段渲染，与轮次、思考过程共用同一条主轴。
 *
 * 展开后除了参数与返回，还把这次调用牵涉到的本机文件列成一行一行的卡片（参数与返回里认出来的路径，
 * 见 core/FileRefs）：点一下用系统默认程序打开，右侧按钮在文件夹中定位，
 * 整张卡片还能往外拖（拖给资源管理器 / 别的应用，原生拖放由主进程发起），
 * 服务端交付的下载地址按外部地址打开、自己就是可拖的链接。交付物不用再自己去那段 JSON 里翻路径。
 */
import { computed, ref } from 'vue'
import type { ChatToolCall } from '@renderer/stores/chat'
import { fileRefsOf, type ChatFileRef } from '@renderer/core/FileRefs'
import { formatDuration } from '@renderer/utils/TimeUtil'
import FileActionUtil from '@renderer/utils/FileActionUtil'

const props = defineProps<{
  calls: ChatToolCall[]
  /** 会话所属项目：相对路径按它的工作区目录解析（打开文件时用） */
  projectId?: string
}>()

/** 展开的节点：默认都收起，点行头才铺开参数与返回 */
const opened = ref<string[]>([])

/** 每次调用涉及的文件：随流式内容一起重算（参数与返回写到一半时还给不出卡片） */
const refs = computed<ChatFileRef[][]>(() => props.calls.map((call) => fileRefsOf(call)))

function openRef(ref: ChatFileRef): void {
  FileActionUtil.open(String(props.projectId ?? ''), ref.path)
}

function revealRef(ref: ChatFileRef): void {
  FileActionUtil.reveal(String(props.projectId ?? ''), ref.path)
}

/** 拖出窗口：把这张卡片上的本机路径交给系统（服务端地址那张是链接，浏览器自己会拖） */
function dragRef(ref: ChatFileRef, event: DragEvent): void {
  FileActionUtil.dragOut(event, String(props.projectId ?? ''), ref.path)
}

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
  if ('stopped' === call.status) return '已停止'
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
      <!-- 这次调用牵涉到的文件：点一下用默认程序打开，右侧在文件夹中定位，整张卡片还能往外拖 -->
      <div v-if="refs[index]?.length" class="tool-block">
        <div class="tool-label">涉及的文件</div>
        <div class="tool-files">
          <div
            v-for="(file, fileIndex) in refs[index]"
            :key="fileIndex"
            class="tool-file"
            :draggable="!file.url"
            @dragstart="dragRef(file, $event)"
          >
            <!-- 服务端交付的文件：给的是下载地址，按外部地址打开（带时效签名，浏览器直接能取） -->
            <a
              v-if="file.url"
              :href="file.url"
              target="_blank"
              rel="noopener noreferrer"
              :title="file.url"
              class="file-open"
            >
              <svg class="file-icon" viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
                <path
                  d="M6.4 1.8h3.2l2.6 2.6v7.8a1 1 0 0 1-1 1H4.8a1 1 0 0 1-1-1V2.8a1 1 0 0 1 1-1z"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.1"
                  stroke-linejoin="round"
                />
              </svg>
              <span class="file-name">{{ file.name }}</span>
              <span class="file-remote">外部地址</span>
            </a>
            <button
              v-else
              type="button"
              :title="file.path"
              class="file-open"
              @click="openRef(file)"
            >
              <svg class="file-icon" viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
                <path
                  v-if="file.dir"
                  d="M1.9 3.5h3.9l1.2 1.6h7.1v7.2a.8.8 0 0 1-.8.8H2.7a.8.8 0 0 1-.8-.8z"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.1"
                  stroke-linejoin="round"
                />
                <path
                  v-else
                  d="M6.4 1.8h3.2l2.6 2.6v7.8a1 1 0 0 1-1 1H4.8a1 1 0 0 1-1-1V2.8a1 1 0 0 1 1-1z"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.1"
                  stroke-linejoin="round"
                />
              </svg>
              <span class="file-name">{{ file.name }}</span>
              <span class="file-path">{{ file.path }}</span>
            </button>
            <el-tooltip v-if="!file.url" content="在文件夹中显示" placement="top">
              <button type="button" class="file-reveal" @click="revealRef(file)">
                <svg viewBox="0 0 16 16" width="11" height="11" aria-hidden="true">
                  <path
                    d="M1.9 3.5h3.9l1.2 1.6h7.1v7.2a.8.8 0 0 1-.8.8H2.7a.8.8 0 0 1-.8-.8z"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.1"
                    stroke-linejoin="round"
                  />
                </svg>
              </button>
            </el-tooltip>
          </div>
        </div>
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

/* 涉及的文件：一行一个，点行身打开、点右侧按钮在文件夹中定位 */
.tool-files {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 2px;
}

.tool-file {
  display: flex;
  align-items: center;
  gap: 2px;
  border-radius: 6px;
  background: var(--el-fill-color-light);

  &:hover {
    background: var(--el-fill-color);
  }
}

.file-open {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: 1;
  min-width: 0;
  padding: 4px 6px;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-family: inherit;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
  text-align: left;
  text-decoration: none;
  cursor: pointer;

  &:hover .file-name {
    color: var(--el-color-primary);
  }

  .file-icon {
    flex: none;
    color: var(--el-text-color-placeholder);
  }

  .file-name {
    flex: none;
    max-width: 45%;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  /* 路径：次要信息，窄下来时先被截断 */
  .file-path {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    color: var(--el-text-color-placeholder);
  }

  .file-remote {
    flex: none;
    margin-left: auto;
    padding: 0 4px;
    border-radius: 3px;
    font-size: 11px;
    color: var(--el-text-color-placeholder);
    background: var(--el-fill-color);
  }
}

.file-reveal {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 2px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--el-text-color-placeholder);
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
    background: var(--el-fill-color);
  }
}
</style>
