<script setup lang="ts">
/**
 * 消息行 - 对话页的一行消息：头像 + 气泡（工具调用过程、思考过程、正文、状态）+ 操作条。
 *
 * 助手回复按「模型往返轮次」铺成一条时间线：轮次、思考过程、每次工具调用都落在这条线上，
 * 用点的大小与颜色区分层级（轮次最大、思考次之、调用最小且按状态取色），
 * 时间线之后是这一轮的最终回复正文。
 * 这条过程线在生成时铺开（看得见正做到哪一步），回复一结束就自动收起、只留一行摘要，
 * 打开历史会话时同样是收起状态；要回看过程点开即可。
 *
 * 消息里出现的本机文件都能直接打开：用户消息里的 @ 引用标签点一下就用系统默认程序打开
 * （右侧小按钮在文件夹中定位，悬浮提示给出完整路径），回复正文与工具调用里的路径同理
 * （见 ChatMarkdown 的链接拦截与 ToolCallList 的文件卡片）。
 */
import { computed, ref, watch } from 'vue'
import ChatMarkdown from './ChatMarkdown.vue'
import ToolCallList from './ToolCallList.vue'
import { formatDuration, formatMessageTime } from '@renderer/utils/TimeUtil'
import FileActionUtil from '@renderer/utils/FileActionUtil'
import type { ChatMessage, ChatRound, ChatToolCall } from '@renderer/stores/chat'

const props = defineProps<{
  message: ChatMessage
  /** 会话所属项目：引用里的相对路径按它的工作区目录解析（打开文件时用） */
  projectId?: string
  /** 分支位置：同一条消息有多条分支时显示「◀ 2/3 ▶」 */
  branch?: { index: number; count: number } | null
  /** 用户消息能否「编辑后重新发送」（新起分支，原分支保留） */
  editable?: boolean
  /** 助手回复能否「重新生成」（同一条提问下新起一条回复） */
  regenerable?: boolean
}>()

const emit = defineEmits<{
  switch: [step: number]
  edit: [content: string]
  regenerate: []
}>()

const isUser = computed(() => 'user' === props.message.role)
const streaming = computed(() => 'running' === props.message.status)
/** 有内容的轮次：空轮次（既没调工具也没输出）不占位 */
const rounds = computed<ChatRound[]>(() =>
  (props.message.rounds ?? []).filter(
    (round) => round.reasoning || round.content || (round.calls ?? []).length
  )
)
/** 正在跑的那一轮：流式期间它的思考过程直接铺开，不再收进折叠面板 */
const liveIndex = computed(() => (streaming.value ? rounds.value.length - 1 : -1))
/**
 * 有正文：回复正文按 Markdown 渲染在气泡里。
 * 流式期间 store 把增量实时写回 message.content，所以正文在这里逐字长出来；
 * 末轮的正文归这里，因此时间线里不再重复渲染它（见下面的「中间输出」）。
 */
const hasBody = computed(() => Boolean(String(props.message.content ?? '').trim()))
const idle = computed(
  () =>
    !streaming.value && !hasBody.value && !rounds.value.length && 'failed' !== props.message.status
)

/**
 * 调用过程这条时间线：生成时铺开，回复一结束就自动收起。
 * 历史消息挂载时不是流式状态，因此默认也是收起的（`immediate` 让首屏就按这个结论渲染）。
 */
const processPanels = ref<string[]>([])
watch(
  streaming,
  (value) => {
    processPanels.value = value ? ['process'] : []
  },
  { immediate: true }
)

/** 调用次数求和：过程摘要与单轮摘要共用 */
function callCount(round: ChatRound): number {
  return (round.calls ?? []).length
}

/** 调用耗时求和：还没跑完的步骤没有耗时不参与 */
function callDuration(calls: ChatToolCall[] | undefined): number {
  return (calls ?? []).reduce((sum, call) => sum + (call.duration || 0), 0)
}

/** 过程摘要：收起后靠这一行交代刚刚做了什么（轮次 / 调用次数 / 总耗时） */
const processMeta = computed(() => {
  const parts = [`${rounds.value.length} 轮`]
  const calls = rounds.value.reduce((sum, round) => sum + callCount(round), 0)
  if (calls) parts.push(`${calls} 次调用`)
  const duration = formatDuration(
    rounds.value.reduce((sum, round) => sum + callDuration(round.calls), 0)
  )
  if (duration) parts.push(duration)
  return parts.join(' · ')
})

/** 过程状态点：还在跑就呼吸，失败给红点，正常收尾给绿点，被中断的留灰点 */
const processState = computed(() => {
  if (streaming.value) return 'running'
  if ('failed' === props.message.status) return 'failed'
  if ('stopped' === props.message.status) return ''
  return 'success'
})

/** 单轮摘要：这一轮调了几次、一共花了多久 */
function roundMeta(round: ChatRound): string {
  const parts: string[] = []
  if (callCount(round)) parts.push(`${callCount(round)} 次调用`)
  const duration = formatDuration(callDuration(round.calls))
  if (duration) parts.push(duration)
  return parts.join(' · ')
}

/**
 * 思考过程的展开状态：没手动点过就按「正在跑的那一轮铺开」给默认值，
 * 因此生成时看得见模型在想什么，回复结束（末轮不再是 liveIndex）后默认收起。
 */
const reasoningOpen = ref<Record<number, boolean>>({})

function isReasoningOpen(index: number): boolean {
  const value = reasoningOpen.value[index]
  return undefined === value ? index === liveIndex.value : value
}

function toggleReasoning(index: number): void {
  reasoningOpen.value[index] = !isReasoningOpen(index)
}

/** 流式状态文案：正在调工具时报出工具名，否则按有没有正文区分思考与输出 */
const streamingText = computed(() => {
  const current = rounds.value[rounds.value.length - 1]
  const running = (current?.calls ?? []).find((call) => 'running' === call.status)
  if (running) return `正在调用：${running.name || '工具'}`
  return props.message.content ? '正在输出…' : '正在思考…'
})

const copied = ref(false)
/** 编辑态：改完重新发送会新起一条分支，原提问与原回复都保留 */
const editing = ref(false)
const draft = ref('')

/** 打开引用到的本机文件 / 目录（技能引用给的是技能目录，同样按目录打开） */
function openMention(mention: { path: string }): void {
  FileActionUtil.open(String(props.projectId ?? ''), mention.path)
}

/** 在文件夹中定位这个引用 */
function revealMention(mention: { path: string }): void {
  FileActionUtil.reveal(String(props.projectId ?? ''), mention.path)
}

function startEdit(): void {
  draft.value = String(props.message.content ?? '')
  editing.value = true
}

function submitEdit(): void {
  const content = String(draft.value ?? '').trim()
  if (!content) return
  editing.value = false
  emit('edit', content)
}

/** 编辑框回车发送：Shift / Ctrl / Cmd + Enter 换行，输入法组词中的回车不发送 */
function onEditKeydown(payload: Event | KeyboardEvent): void {
  const event = payload as KeyboardEvent
  if (event.isComposing || 229 === event.keyCode) return
  if ('Enter' !== event.key || event.shiftKey || event.ctrlKey || event.metaKey) return
  event.preventDefault()
  submitEdit()
}

/** 气泡下方的时间：当天只给时分，往日补日期 */
const timeText = computed(() => formatMessageTime(props.message.createdAt))

async function copy(): Promise<void> {
  const text = String(props.message.content ?? '')
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    copied.value = true
    setTimeout(() => (copied.value = false), 1500)
  } catch {
    copied.value = false
  }
}
</script>

<template>
  <div class="chat-message" :class="isUser ? 'is-user' : 'is-assistant'">
    <div class="chat-main">
      <div class="chat-bubble">
        <!-- 用户提问：原文 + @ 引用回显 -->
        <template v-if="isUser">
          <div class="chat-content" v-if="!editing">{{ message.content || '（未输入内容）' }}</div>
          <!-- 编辑提问：改完重新发送会在同一处新起一条分支（原分支保留） -->
          <div class="chat-edit" v-else>
            <el-input
              v-model="draft"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 8 }"
              resize="none"
              @keydown="onEditKeydown"
            />
            <div class="chat-edit-tools">
              <el-button link size="small" @click="editing = false">取消</el-button>
              <el-button type="primary" size="small" :disabled="!draft.trim()" @click="submitEdit"
                >重新发送</el-button
              >
            </div>
          </div>
          <div class="chat-mentions" v-if="message.mentions?.length && !editing">
            <!--
              引用到的本机文件 / 技能：一条引用一个小块（图标 + 名字），
              点一下用默认程序打开，行尾的小按钮在文件夹中定位，完整路径走悬浮提示
            -->
            <el-tooltip
              v-for="mention in message.mentions"
              :key="mention.type + mention.path"
              :content="mention.path"
              placement="top"
            >
              <span
                class="mention-chip"
                :class="`is-${mention.type}`"
                @click="openMention(mention)"
              >
                <svg
                  class="mention-icon"
                  viewBox="0 0 16 16"
                  width="12"
                  height="12"
                  aria-hidden="true"
                >
                  <!-- 技能：四角星；其余是本机文件 -->
                  <path
                    v-if="'skill' === mention.type"
                    d="M8 2 9.4 6.6 14 8 9.4 9.4 8 14 6.6 9.4 2 8 6.6 6.6z"
                    fill="currentColor"
                  />
                  <path
                    v-else
                    d="M6.4 1.8h3.2l2.6 2.6v7.8a1 1 0 0 1-1 1H4.8a1 1 0 0 1-1-1V2.8a1 1 0 0 1 1-1z"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.2"
                    stroke-linejoin="round"
                  />
                </svg>
                <span class="mention-name">{{ mention.name || mention.path }}</span>
                <button
                  type="button"
                  class="mention-reveal"
                  title="在文件夹中显示"
                  @click.stop="revealMention(mention)"
                >
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
              </span>
            </el-tooltip>
          </div>
        </template>
        <!-- 助手回复：调用过程铺成一条时间线（生成时铺开、结束后收起），之后是回复正文 -->
        <template v-else>
          <el-collapse class="chat-process" v-if="rounds.length" v-model="processPanels">
            <el-collapse-item name="process">
              <template #title>
                <span class="process-state" :class="processState"></span>
                <span class="process-title">调用过程</span>
                <span class="process-meta">{{ processMeta }}</span>
              </template>
              <div class="chat-timeline">
                <template v-for="(round, index) in rounds" :key="index">
                  <!-- 轮次：线上最大的那个节点，这一轮从它起头 -->
                  <div class="timeline-step is-round" :class="{ live: index === liveIndex }">
                    <div class="step-head">
                      <span class="step-title">第 {{ index + 1 }} 轮</span>
                      <span class="step-meta" v-if="roundMeta(round)">{{ roundMeta(round) }}</span>
                    </div>
                  </div>
                  <!-- 思考过程：次一级的节点，点行头铺开正文（正在跑的那一轮默认铺开） -->
                  <div
                    class="timeline-step is-reasoning"
                    :class="{ open: isReasoningOpen(index) }"
                    v-if="round.reasoning"
                  >
                    <button
                      type="button"
                      class="step-head"
                      :aria-expanded="isReasoningOpen(index) ? 'true' : 'false'"
                      @click="toggleReasoning(index)"
                    >
                      <span class="step-title">思考过程</span>
                      <svg
                        class="step-caret"
                        viewBox="0 0 16 16"
                        width="10"
                        height="10"
                        aria-hidden="true"
                      >
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
                    <div class="step-body chat-reasoning-text" v-show="isReasoningOpen(index)">
                      {{ round.reasoning }}
                    </div>
                  </div>
                  <!-- 工具调用：这一轮里的每次调用都是线上的一个节点 -->
                  <ToolCallList :calls="round.calls ?? []" :project-id="projectId" />
                  <!-- 中间输出：不是节点，但主轴要接着往下走（末轮的正文就是气泡里的回复，不在这里重复） -->
                  <div class="timeline-content" v-if="round.content && index !== rounds.length - 1">
                    <ChatMarkdown :content="round.content" :project-id="projectId" />
                  </div>
                </template>
              </div>
            </el-collapse-item>
          </el-collapse>
          <div class="chat-error" v-if="'failed' === message.status && !hasBody && !streaming">
            {{ message.content || '本轮未完成' }}
          </div>
          <!-- 回复正文按 Markdown 渲染（用户消息保持原文） -->
          <ChatMarkdown v-else-if="hasBody" :content="message.content" :project-id="projectId" />
          <div class="chat-content placeholder" v-else-if="idle">（无回复内容）</div>
          <div class="chat-streaming" v-if="streaming">
            <span class="chat-streaming-dot" />
            <span>{{ streamingText }}</span>
          </div>
        </template>
      </div>
      <!-- 气泡下方：复制 / 编辑 / 重新生成 / 分支切换 / 异常状态 / 时间 -->
      <div class="chat-toolbar" v-if="!streaming">
        <el-button
          class="chat-action"
          link
          :title="copied ? '已复制' : '复制内容'"
          v-if="hasBody"
          @click="copy"
        >
          <svg v-if="copied" viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
            <path
              d="M3.6 8.4 6.4 11.2 12.4 5"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <svg v-else viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
            <rect
              x="5.9"
              y="5.9"
              width="7.6"
              height="7.6"
              rx="1.7"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
            <path
              d="M10.4 3.9H4.8a1.7 1.7 0 0 0-1.7 1.7v5.6"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linecap="round"
            />
          </svg>
        </el-button>
        <el-button
          class="chat-action chat-edit-action"
          link
          title="编辑后重新发送"
          v-if="isUser && editable"
          @click="startEdit"
        >
          <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
            <path
              d="M3.1 12.9l.5-2.5 6.9-6.9a1.3 1.3 0 0 1 1.9 0l1 1a1.3 1.3 0 0 1 0 1.9l-6.9 6.9-2.5.5z"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linejoin="round"
            />
          </svg>
        </el-button>
        <el-button
          class="chat-action chat-regen"
          link
          title="重新生成"
          v-if="!isUser && regenerable"
          @click="emit('regenerate')"
        >
          <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
            <path
              d="M12.9 8a4.9 4.9 0 1 1-1.5-3.5"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linecap="round"
            />
            <path
              d="M13.1 2.5v3h-3"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </el-button>
        <!-- 分支切换：同一处有多条分支时前后切换 -->
        <span class="chat-branches" v-if="branch && branch.count > 1">
          <button type="button" class="branch-step" title="上一个分支" @click="emit('switch', -1)">
            <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
              <path
                d="M10 3.6 5.6 8l4.4 4.4"
                fill="none"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <span class="branch-text">{{ branch.index }} / {{ branch.count }}</span>
          <button type="button" class="branch-step" title="下一个分支" @click="emit('switch', 1)">
            <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
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
        </span>
        <span class="chat-status" v-if="!isUser && 'stopped' === message.status">已停止</span>
        <span class="chat-status" v-else-if="!isUser && 'failed' === message.status">未完成</span>
        <span class="chat-time" v-if="timeText">{{ timeText }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '../../assets/timeline' as timeline;

/**
 * 消息行：没有头像。助手铺满内容列；用户按内容收宽、整体贴右。
 * 气泡与下方的操作条同属 .chat-main，宽度一致，所以操作条两端能与气泡边缘对齐。
 */
.chat-message {
  display: flex;

  &.is-user {
    justify-content: flex-end;
  }
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  max-width: 100%;
}

.is-assistant .chat-main {
  flex: 1 1 auto;
}

.is-user .chat-main {
  flex: 0 1 auto;
}

.chat-bubble {
  min-width: 0;
  padding: 8px 12px;
  border-radius: 10px;
  /* 内容区底色是页面灰，回复气泡用白底才立得住（与之前对话页的观感一致） */
  border: solid 1px var(--el-border-color-lighter);
  background: var(--el-bg-color);
  text-align: left;
}

.is-user .chat-bubble {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
}

.chat-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--el-text-color-primary);

  &.placeholder {
    color: var(--el-text-color-placeholder);
  }
}

.chat-error {
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
  color: var(--el-color-danger);
}

.chat-mentions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;

  /**
   * 一条引用：图标 + 名字 + 定位按钮。
   * 引用只出现在用户提问里，那一层气泡是主色浅底，所以小块用白底才立得住；
   * 名字过长时在这里收住（完整路径在悬浮提示里），不给气泡撑宽。
   */
  .mention-chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    max-width: 100%;
    height: 22px;
    padding: 0 3px 0 6px;
    border: solid 1px var(--el-border-color-lighter);
    border-radius: 4px;
    background: var(--el-bg-color);
    font-size: 12px;
    line-height: 1;
    color: var(--el-text-color-regular);
    cursor: pointer;
    transition:
      color 0.2s,
      border-color 0.2s;

    &:hover {
      border-color: var(--el-color-primary-light-5);
      color: var(--el-color-primary);

      .mention-reveal {
        opacity: 1;
      }
    }

    /* 技能与文件用图标区分：技能那枚星标走警告色，一眼能和文件分开 */
    &.is-skill .mention-icon {
      color: var(--el-color-warning);
    }

    .mention-icon {
      flex: none;
      color: var(--el-text-color-placeholder);
    }

    .mention-name {
      min-width: 0;
      max-width: 220px;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .mention-reveal {
      flex: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 16px;
      height: 16px;
      padding: 0;
      border: none;
      border-radius: 3px;
      background: transparent;
      color: inherit;
      cursor: pointer;
      opacity: 0.5;
      transition:
        opacity 0.2s,
        background 0.2s;

      &:hover {
        opacity: 1;
        background: var(--el-fill-color);
      }
    }
  }
}

/* 调用过程：整条时间线收在一个折叠面板里 —— 生成时铺开，回复一结束就自动收起 */
.chat-process {
  margin-bottom: 6px;
  border-top: none;
  border-bottom: none;
  /* 行头收成一行摘要的高度：Element Plus 的行头高度来自这个变量，不是 height */
  --el-collapse-header-height: 24px;
  --el-collapse-header-bg-color: transparent;
  --el-collapse-content-bg-color: transparent;

  :deep(.el-collapse-item__header) {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    border-bottom: none;

    /* 标题槽自带一层包装元素，摘要与标题的间距要落在这里 */
    .el-collapse-item__title {
      display: flex;
      align-items: center;
      gap: 6px;
      flex: 1;
      min-width: 0;
    }
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }

  :deep(.el-collapse-item__content) {
    padding-bottom: 0;
  }
}

/* 过程状态点：生成中呼吸，正常收尾绿点、失败红点、被中断留灰点 */
.process-state {
  flex: none;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);

  &.running {
    background: var(--el-color-primary);
    animation: chat-pulse 1s ease-in-out infinite;
  }

  &.success {
    background: var(--el-color-success);
  }

  &.failed {
    background: var(--el-color-danger);
  }
}

.process-title {
  flex: none;
}

.process-meta {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--el-text-color-placeholder);
}

/* 时间线：一轮、思考过程、每次调用都是线上的一格，节点与主轴的画法见 assets/_timeline.scss */
.chat-timeline {
  padding-top: 2px;
}

/* 轮次：最大的节点，空心；正在跑的那一轮实心并呼吸 */
.timeline-step.is-round {
  @include timeline.node(10px);
  padding-bottom: 6px;

  &::after {
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-darker);
  }

  .step-title {
    color: var(--el-text-color-secondary);
  }

  &.live::after {
    background: var(--el-color-primary);
    border-color: var(--el-color-primary);
    animation: chat-pulse 1s ease-in-out infinite;
  }
}

/* 思考过程：次一级的节点，灰度色 */
.timeline-step.is-reasoning {
  @include timeline.node(8px);

  &::after {
    background: var(--el-color-info);
  }

  &:hover .step-title {
    color: var(--el-color-primary);
  }
}

/* 中间输出：不是节点，但主轴要接着往下走 */
.timeline-content {
  position: relative;
  padding: 3px 0 3px 18px;

  &::before {
    @include timeline.rail;
  }
}

/* 思考过程正文：次级节点铺开后的一段文字 */
.chat-reasoning-text {
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--el-text-color-secondary);
}

.chat-streaming {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-placeholder);

  &:first-child {
    margin-top: 0;
  }

  .chat-streaming-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--el-color-primary);
    animation: chat-pulse 1s ease-in-out infinite;
  }
}

@keyframes chat-pulse {
  0%,
  100% {
    opacity: 0.3;
    transform: scale(0.8);
  }

  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

.chat-toolbar {
  display: flex;
  align-items: center;
  /* 助手：复制在左、时间在右，两项挨着，整体左对齐到气泡左边缘 */
  justify-content: flex-start;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);

  /* 助手：复制 → 重新生成 → 分支 → 状态 → 时间 */
  .chat-action {
    height: 18px;
    padding: 0;
    margin: 0;
    line-height: 18px;
    color: var(--el-text-color-placeholder);

    &:hover {
      color: var(--el-color-primary);
    }
  }

  .chat-action {
    order: 1;
  }

  .chat-regen {
    order: 2;
  }

  .chat-branches {
    order: 3;
  }

  .chat-status {
    order: 4;
  }

  .chat-time {
    order: 5;
    flex: none;
    font-size: 11px;
  }
}

/* 用户：次序反过来（时间 → 分支 → 编辑 → 复制），整体右对齐到气泡右边缘 */
.is-user .chat-toolbar {
  justify-content: flex-end;

  .chat-time {
    order: 1;
  }

  .chat-branches {
    order: 2;
  }

  .chat-edit-action {
    order: 3;
  }

  .chat-action {
    order: 4;
  }
}

/* 分支切换：两个箭头夹一个序号，宽度用等宽数字避免跳动 */
.chat-branches {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: none;
  height: 18px;

  .branch-step {
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    padding: 0;
    color: var(--el-text-color-placeholder);
    background: transparent;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    transition: color 0.2s;

    &:hover {
      color: var(--el-color-primary);
    }
  }

  .branch-text {
    flex: none;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }
}

/* 编辑提问：与正文同一字号，底部一行右对齐 */
.chat-edit {
  min-width: 260px;

  :deep(.el-textarea__inner) {
    padding: 0;
    border: none;
    box-shadow: none;
    background: transparent;
    font-size: 13px;
    line-height: 1.7;
  }

  .chat-edit-tools {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 4px;
  }
}
</style>
