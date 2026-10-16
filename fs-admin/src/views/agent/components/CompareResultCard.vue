<script setup lang="ts">
/**
 * 模型对比 - 单个模型的结果卡片。
 *
 * 版面分三层：标题行（模型名、状态、耗时等信息靠右）、内容区（思考 + 正文）、
 * 底部（参数标签 ｜ 反馈、复制、时间靠右）。耗时属于「这轮跑得怎么样」，跟标题同行；
 * 时间属于「什么时候跑的」，压到底部右侧，两者不抢同一块位置。
 *
 * 思考与正文分两处：思考在可折叠块里（流式时直接展开），正文在下面。
 * 流式过程中正文用纯文本渲染、结束后才交给 Markdown——每个增量都跑一次
 * Markdown 渲染，多模型并发时开销太大；打开「原文」则始终按原始文本展示。
 *
 * @prop  {Object}  result   - 该模型本轮的结果（流式中会持续变化，带 id 才能反馈）
 * @prop  {Boolean} raw      - 显示原文：不做 Markdown 解析
 * @prop  {Boolean} closable - 是否显示单模型的中断按钮
 * @emits stop               - 请求中断该模型的生成
 * @emits feedback           - 提交反馈：{ emotion, tag, content }
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { VideoPause } from '@element-plus/icons-vue'
import ButtonCopy from '@/components/Button/ButtonCopy.vue'
import ChatFeedback from '@/components/Chat/ChatFeedback.vue'
import ChatLoading from '@/components/Chat/ChatLoading.vue'
import MarkdownEditor from '@/components/Editor/MarkdownEditor.vue'
import DateUtil from '@/utils/DateUtil'
import type { CompareResult } from '@/types/compare'

const { result, raw = false, closable = true } = defineProps<{
  result: CompareResult
  raw?: boolean
  closable?: boolean
}>()
const emit = defineEmits<{
  stop: []
  feedback: [payload: { emotion: string, tag: string, content: string }]
}>()

/** 状态文案与标签配色 */
const STATES: Record<string, { text: string, type: string }> = {
  idle: { text: '等待', type: 'info' },
  connect: { text: '连接中', type: 'info' },
  streaming: { text: '生成中', type: 'primary' },
  finish: { text: '完成', type: 'success' },
  abort: { text: '已中断', type: 'warning' },
  error: { text: '失败', type: 'danger' },
}
const state = computed(() => STATES[result.state] ?? STATES.idle)
const streaming = computed(() => 'idle' === result.state || 'connect' === result.state || 'streaming' === result.state)

/**
 * 计时：流式期间需要一个本地节拍才能看到「耗时」在走。
 * 只在生成中开着，结束后清掉，不长期占用定时器。
 */
const now = ref(Date.now())
let timer: any = null
const tick = (on: boolean) => {
  window.clearInterval(timer)
  timer = null
  if (!on) return
  now.value = Date.now()
  timer = window.setInterval(() => { now.value = Date.now() }, 200)
}
watch(streaming, tick, { immediate: true })
onBeforeUnmount(() => window.clearInterval(timer))

/** 耗时：结束后取固定值，生成中按当前时间算 */
const duration = computed(() => {
  if (!result.createdTime) return ''
  const end = result.finishedTime || (streaming.value ? now.value : 0)
  if (!end) return ''
  return ((end - result.createdTime) / 1000).toFixed(1) + 's'
})

/** 首字耗时：从发起到第一个增量到达 */
const firstToken = computed(() => {
  if (!result.createdTime || !result.firstTokenTime) return ''
  return ((result.firstTokenTime - result.createdTime) / 1000).toFixed(1) + 's'
})

/** 参数回显：让对照一眼看出各模型差在哪几项 */
const meta = computed(() => [
  undefined === result.payload?.temperature ? '' : `温度 ${result.payload.temperature}`,
  undefined === result.payload?.maxTokens ? '' : `最大输出 ${result.payload.maxTokens}`,
  undefined === result.payload?.thinkMode ? '' : `思考 ${result.payload.thinkMode}`,
  undefined === result.payload?.thinkEffort ? '' : `强度 ${result.payload.thinkEffort}`,
].filter((item: string) => !!item))

const copyContent = computed(() => result.content || result.reasoning || '')

/**
 * 反馈：ChatFeedback 用 cancel 表示撤回，而接口的语义是「再次提交同一情绪即取消」，
 * 所以撤回时把它翻成当前情绪再提交一次，接口那边就不必多一个 cancel 态。
 */
const handleFeedback = (payload: { emotion: string, tag: string, content: string }) => {
  const emotion = 'cancel' === payload.emotion ? (result.feedbackEmotion || 'positive') : payload.emotion
  emit('feedback', { emotion, tag: payload.tag, content: payload.content })
}
</script>

<template>
  <el-card class="result-card" :bordered="false" shadow="never">
    <template #header>
      <div class="card-head">
        <!-- 左端是标题与状态，右端是耗时等信息与停止按钮，两端分布 -->
        <div class="head-left">
          <span class="card-model" :title="result.model">{{ result.model || '未命名模型' }}</span>
          <el-tag class="card-state" size="small" effect="plain" :type="state.type as any">{{ state.text }}</el-tag>
        </div>
        <div class="head-right">
          <div class="card-meta">
            <span v-if="duration">耗时 {{ duration }}</span>
            <span v-if="firstToken">首字 {{ firstToken }}</span>
            <span v-if="result.finishReason">结束 {{ result.finishReason }}</span>
          </div>
          <el-button
            class="card-stop"
            v-if="closable && streaming"
            link
            :icon="VideoPause"
            title="停止该模型"
            @click="emit('stop')" />
        </div>
      </div>
    </template>
    <!--
      失败：直接在卡片里给原因，不用点开才知道。
      中断同样带原因（自动兜底、手动终止、连接断开各有说法），用警示色与「失败」区分开。
    -->
    <el-alert
      class="card-error"
      v-if="result.error"
      :title="result.error"
      :type="'abort' === result.state ? 'warning' : 'error'"
      :closable="false"
      show-icon />
    <!-- 内容区滚动也走 el-scrollbar：与本页其它滚动区一致 -->
    <el-scrollbar class="card-body">
      <div class="card-body-inner">
        <!-- 思考：流式时展开看，结束后收进折叠块 -->
        <div class="card-reasoning" v-if="result.reasoning && streaming">{{ result.reasoning }}</div>
        <el-collapse class="card-reasoning" v-else-if="result.reasoning">
          <el-collapse-item title="思考过程">
            <div class="card-reasoning-text">{{ result.reasoning }}</div>
          </el-collapse-item>
        </el-collapse>
        <!-- 正文：流式时纯文本；结束后按渲染，除非打开了「原文」 -->
        <pre class="card-text" v-if="result.content && (streaming || raw)">{{ result.content }}</pre>
        <MarkdownEditor class="card-markdown" v-else-if="result.content" :model-value="result.content" readonly />
        <!-- 空态：区分「还没开始」「正在连接」「跑完了但没内容」 -->
        <div class="card-empty" v-else-if="streaming">
          <ChatLoading />
          <span>{{ 'idle' === result.state ? '等待发起' : '正在生成…' }}</span>
        </div>
        <div class="card-empty" v-else-if="!result.error">（无输出内容）</div>
      </div>
    </el-scrollbar>
    <template #footer>
      <div class="card-foot">
        <!-- 参数标签与按钮都留在左端，只有时间靠右 -->
        <div class="foot-left">
          <el-space class="foot-tags" :size="4">
            <el-tag v-for="item in meta" :key="item" size="small" effect="plain" type="info">{{ item }}</el-tag>
          </el-space>
          <div class="foot-actions">
            <ButtonCopy :content="copyContent" title="复制输出内容" />
            <!-- 落库后才有 callId，那之前评了也没处存，直接禁用 -->
            <ChatFeedback
              :emotion="result.feedbackEmotion || ''"
              :tag="result.feedbackTag || ''"
              :content="result.feedbackContent || ''"
              :disabled="!result.id"
              @submit="handleFeedback" />
          </div>
        </div>
        <span class="foot-time" v-if="result.createdTime">{{ DateUtil.format(result.createdTime) }}</span>
      </div>
    </template>
  </el-card>
</template>

<style lang="scss" scoped>
.result-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: solid 1px var(--el-border-color-lighter);
  border-radius: 6px;
  :deep(.el-card__header) {
    padding: 8px 12px;
  }
  :deep(.el-card__body) {
    flex: 1;
    min-height: 0;
    padding: 10px 12px;
  }
  :deep(.el-card__footer) {
    padding: 8px 12px;
  }
}
/* 标题行：模型名与状态在左，耗时等信息与停止按钮在右，两端分布；
   卡片窄到放不下时右侧整组换到下一行 */
.card-head {
  @include flex-between();
  flex-wrap: wrap;
  gap: 6px 8px;
  .head-left {
    @include flex-start();
    flex: 1;
    /* 留一个下限：再窄就让右侧换行，而不是把模型名压成一个字 */
    min-width: 120px;
    gap: 6px;
    .card-model {
      min-width: 0;
      font-size: 13px;
      font-weight: 500;
      color: var(--el-text-color-primary);
      @include text-wrap();
    }
    .card-state {
      flex: none;
    }
  }
  .head-right {
    @include flex-start();
    flex: none;
    gap: 10px;
  }
  .card-meta {
    @include flex-start();
    flex: none;
    gap: 10px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    white-space: nowrap;
  }
  .card-stop {
    flex: none;
  }
}
.card-error {
  margin-bottom: 8px;
}
/* 正文区：撑满卡片，但给个上限高度，多个模型并排时卡片长度不互相拉长 */
.card-body {
  height: 100%;
  max-height: 460px;
}
/* 留白放内层，滚动条才贴着卡片内边缘 */
.card-body-inner {
  padding-right: 4px;
}
.card-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.card-markdown {
  font-size: 13px;
  :deep(.fs-markdown-preview) {
    padding: 0;
    font-size: 13px;
  }
}
/* 思考块：折叠面板去掉上下边框，避免在卡片里出现横向色块 */
.card-reasoning {
  margin: 0 0 6px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  &.el-collapse {
    border-top: none;
    border-bottom: none;
    --el-collapse-header-bg-color: transparent;
    --el-collapse-content-bg-color: transparent;
    --el-collapse-header-height: 24px;
  }
  :deep(.el-collapse-item__header) {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    border-bottom: none;
  }
  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
  :deep(.el-collapse-item__content) {
    padding-bottom: 2px;
  }
}
.card-reasoning-text {
  font-size: 12px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}
.card-empty {
  @include flex-center();
  gap: 8px;
  min-height: 80px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
/* 底部：参数标签与按钮在左，只有时间靠右；窄卡片下时间换到下一行 */
.card-foot {
  @include flex-between();
  flex-wrap: wrap;
  gap: 6px 10px;
  .foot-left {
    @include flex-start();
    flex: 1 1 auto;
    min-width: 180px;
    gap: 10px;
    /* 标签可收缩换行，窄卡片下按钮不会被挤出视野 */
    .foot-tags {
      min-width: 0;
    }
    .foot-actions {
      @include flex-start();
      flex: none;
      gap: 8px;
      /* 踩：默认仍是中性灰（与点赞一致），只有鼠标划过时给红色——
         ChatFeedback 是共享组件，视觉差异按本仓库的约定由使用方覆盖，不改它本身 */
      :deep(.feedback-trigger .feedback-icon:hover) {
        color: var(--el-color-danger);
      }
    }
  }
  /* 时间靠右，用等宽数字：多个模型并排时数字与冒号能对齐，扫一眼不乱 */
  .foot-time {
    flex: none;
    font-size: 11px;
    font-variant-numeric: tabular-nums;
    letter-spacing: 0.2px;
    color: var(--el-text-color-placeholder);
    white-space: nowrap;
  }
}
</style>
