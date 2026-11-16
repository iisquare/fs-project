<script setup lang="ts">
/**
 * 执行过程时间线（面向用户的轻量视图）：
 * 节点＝时间线的一项（图标按节点类型），节点内部＝该节点调用的工具（小圆点）；
 * 节点的状态与耗时合并成与工具调用一致的「（成功 · 871 毫秒）」文本，不再单独挂状态标签。
 * 运行中取实时的 step / round 事件，完成后取运行结果（或按 logId 拉取的运行日志）里的步骤。
 *
 * - 收起时只有标题一行；运行中强制展开并实时刷新，完成后保留展开状态，可手动收起；
 * - 步骤数据由父页面负责（历史会话按 logId 懒加载），展开时 emit open 通知父页面加载；
 * - 运行中节点的「执行中 12s」由组件自己的秒表维护，父页面不用管。
 *
 * @prop {Array} steps        完成后的步骤（含 output.rounds）
 * @prop {Array} progress     运行中的节点进度（step 事件）
 * @prop {Array} rounds       运行中的 ReAct 轮次（round 事件）
 * @prop {Boolean} streaming  是否运行中
 * @prop {Boolean} loading    步骤是否加载中
 * @prop {Number} logId       运行日志标识（标题中展示）
 * @emits open 展开时触发，父页面据此按 logId 懒加载步骤
 * @example
 * <agentic-timeline :steps="stepsOf(item)" :progress="item.progress" :rounds="item.rounds"
 *   :streaming="item.streaming" :loading="stepLoading[String(item.logId)]" :log-id="item.logId"
 *   @open="loadSteps(item)" />
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import AgenticUtil from '@/utils/AgenticUtil'

const {
  steps = [],
  progress = [],
  rounds = [],
  streaming = false,
  loading = false,
  logId = 0,
} = defineProps<{
  steps?: any[]
  progress?: any[]
  rounds?: any[]
  streaming?: boolean
  loading?: boolean
  logId?: any
}>()
const emit = defineEmits(['open'])

/** 展开状态：运行中强制展开；完成后保留展开状态，用户可手动收起 */
const open = ref(false)
const expanded = computed(() => streaming || open.value)
const handleChange = (names: any) => {
  const list: any[] = Array.isArray(names) ? names : [names]
  open.value = list.indexOf('steps') >= 0
  if (open.value) emit('open')
}

/** 运行秒表：执行中的节点显示已耗时，避免长时间无输出时看起来像卡死 */
const nowTick = ref(Date.now())
let tickTimer: any = null
watch(() => streaming, (running) => {
  window.clearInterval(tickTimer)
  tickTimer = null
  if (!running) return
  nowTick.value = Date.now()
  tickTimer = window.setInterval(() => { nowTick.value = Date.now() }, 1000)
}, { immediate: true })
onBeforeUnmount(() => window.clearInterval(tickTimer))

/** 节点耗时：执行中显示已耗时，结束后显示本节点耗时 */
const nodeDuration = (node: any) => {
  if ('running' !== node?.state) return node?.duration ? `${node.duration} 毫秒` : ''
  const seconds = Math.max(0, Math.round((nowTick.value - (node?.startedAt ?? nowTick.value)) / 1000))
  return seconds > 0 ? `${seconds}s` : ''
}

/**
 * 节点状态文案：与工具调用一致的「（状态 · 耗时）」形式，替代单独的状态标签；
 * 执行中是「（执行中 12s）」，结束后是「（成功 · 871 毫秒）」，没有耗时时只留状态。
 */
const nodeStateText = (node: any) => {
  const duration = nodeDuration(node)
  if ('running' === node?.state) return `（执行中${duration ? ` ${duration}` : ''}）`
  const state = 'failed' === node?.state ? '失败' : '成功'
  return duration ? `（${state} · ${duration}）` : `（${state}）`
}

/** 工具调用文案：调用中 / 成功 / 失败 + 耗时；没有 call 的轮次表示模型正在推理 */
const callText = (call: any) => {
  if (call?.thinking) return `${call.label} · 模型推理中`
  const state = undefined === call?.status ? '调用中' : (2 === call.status ? '失败' : '成功')
  const duration = call?.duration ? ` · ${call.duration} 毫秒` : ''
  return `${call.label} · 工具方法：${call.method}（${state}${duration}）`
}

/** 完成后：节点输出的调用链 → 每个工具方法一条小圆点记录 */
const stepCalls = (step: any) => {
  const rows: any[] = []
  AgenticUtil.stepRounds([step]).forEach((entry: any) => {
    const label = AgenticUtil.roundLabel(entry.round?.round ?? entry.roundIndex + 1, entry.round?.maxRounds)
    entry.calls.forEach((call: any) => {
      rows.push({ key: `${label}-${call.id ?? call.method}`, label, method: call.method, status: call.status, duration: call.duration })
    })
  })
  return rows
}

/** 运行中：该节点的实时轮次 → 模型推理中一行 + 每个工具方法一条（按 id 更新状态与耗时） */
const liveCalls = (step: any) => {
  const rows: any[] = []
  ;(rounds ?? [])
    .filter((round: any) => String(round?.nodeId ?? '') === String(step?.id ?? ''))
    .forEach((round: any) => {
      const label = AgenticUtil.roundLabel(round?.round, round?.maxRounds)
      const calls: any[] = Array.isArray(round?.calls) ? round.calls : []
      calls.forEach((call: any) => {
        rows.push({ key: `${label}-${call.id ?? call.method}`, label, method: call.method, status: call.status, duration: call.duration })
      })
      if (!calls.length && 'running' === round?.state) rows.push({ key: `${label}-thinking`, label, thinking: true })
    })
  return rows
}

/** 节点视图：名称、状态、耗时与所属节点的工具调用 */
const nodes = computed<any[]>(() => {
  if (streaming) {
    return (progress ?? []).map((step: any) => ({
      key: String(step?.id ?? step?.name ?? ''),
      name: step?.name || step?.id || '',
      type: step?.type,
      state: 'running' === step?.state ? 'running' : ('failed' === step?.state || 2 === step?.status ? 'failed' : 'success'),
      startedAt: step?.startedAt,
      duration: step?.duration,
      calls: liveCalls(step),
    }))
  }
  return (steps ?? []).map((step: any, index: number) => ({
    key: String(step?.id ?? index),
    name: step?.name || step?.id || '',
    type: step?.type,
    state: 2 === step?.status ? 'failed' : 'success',
    duration: step?.duration,
    calls: stepCalls(step),
  }))
})

/**
 * 时间线条目（合并展示）：节点一项，紧跟其后是该节点的每条工具调用（小圆点），
 * 都在同一条时间线上按执行顺序排列，便于顺着一条线读完整轮过程。
 */
const entries = computed<any[]>(() => {
  const list: any[] = []
  nodes.value.forEach((node: any) => {
    list.push({ kind: 'node', key: node.key, ...node })
    ;(node.calls ?? []).forEach((call: any) => {
      list.push({ kind: 'call', key: `${node.key}-${call.key}`, node, ...call })
    })
  })
  return list
})

/** 过程摘要：状态点单独展示，节点数与日志标识作为次级信息跟在标题后。 */
const processMeta = computed(() => {
  if (!streaming) return `${(steps ?? []).length} 个节点${logId ? ` · 日志 ${logId}` : ''}`
  const running: any = nodes.value.find((node: any) => 'running' === node.state)
  return `${nodes.value.length} 个节点${running ? ` · 执行中：${running.name}` : ' · 准备中'}`
})

const processState = computed(() => {
  if (streaming) return 'running'
  return nodes.value.some((node: any) => 'failed' === node.state) ? 'failed' : 'success'
})
</script>

<template>
  <el-collapse class="steps" :model-value="expanded ? ['steps'] : []" @change="handleChange">
    <el-collapse-item name="steps">
      <template #title>
        <span class="process-state" :class="processState"></span>
        <span class="process-title">执行过程</span>
        <span class="process-meta">{{ processMeta }}</span>
      </template>
      <div class="steps-body" v-loading="loading">
        <el-timeline class="steps-timeline">
          <!-- 节点与工具调用共用一条时间线：节点用类型图标，工具调用用小圆点 -->
          <template :key="entry.key" v-for="entry in entries">
            <el-timeline-item class="node-item" hide-timestamp v-if="'node' === entry.kind">
              <template #dot>
                <span class="node-dot" :class="{ 'is-running': 'running' === entry.state, 'is-failed': 'failed' === entry.state }">
                  <layout-icon :name="AgenticUtil.nodeIcon(entry.type)" />
                </span>
              </template>
              <div class="node-head">
                <span class="node-name">{{ entry.name }}</span>
                <span class="node-meta">{{ nodeStateText(entry) }}</span>
              </div>
            </el-timeline-item>
            <el-timeline-item class="call-item" hide-timestamp v-else>
              <template #dot>
                <span
                  class="call-dot"
                  :class="{ 'is-running': entry.thinking || undefined === entry.status, 'is-failed': 2 === entry.status }"></span>
              </template>
              <span class="call-text">{{ callText(entry) }}</span>
            </el-timeline-item>
          </template>
        </el-timeline>
        <div class="steps-empty" v-if="!nodes.length">正在准备执行过程…</div>
      </div>
    </el-collapse-item>
  </el-collapse>
</template>

<style lang="scss" scoped>
/* 折叠面板头：默认 48px 对气泡内的过程信息太高，压到 22px（头部高度与行高都跟随该变量） */
.steps.el-collapse {
  --el-collapse-header-height: 30px;
  /* 与思考过程同处理：折叠面板自带的上下边框与白色底都去掉 */
  border-top: none;
  border-bottom: none;
  --el-collapse-header-bg-color: transparent;
  --el-collapse-content-bg-color: transparent;
  /* 与下面的「思考过程」贴紧一些，过程类信息不占太多纵向空间 */
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  :deep(.el-collapse-item__header) {
    gap: 7px;
    padding: 0 2px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    border-bottom: none;
  }
  :deep(.el-collapse-item__title) {
    display: flex;
    flex: 1;
    min-width: 0;
    align-items: center;
    gap: 7px;
  }
  :deep(.el-collapse-item__arrow) {
    flex: none;
    margin: 0 2px 0 8px;
    color: var(--el-text-color-placeholder);
  }
  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
  :deep(.el-collapse-item__content) {
    padding-bottom: 2px;
  }
}
.process-state {
  flex: none;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
  &.running {
    background: var(--el-color-primary);
    animation: process-pulse 1s ease-in-out infinite;
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
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.process-meta {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  text-overflow: ellipsis;
  white-space: nowrap;
}
@keyframes process-pulse {
  0%, 100% {
    opacity: 0.35;
    transform: scale(0.82);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}
.steps-body {
  /* 不再限制最大高度：消息区本身就是滚动容器，套一层会出现多条滚动条 */
  padding: 6px 0;
}
/* 节点（一项一个图标）与节点内部的工具调用（小圆点）共用一条时间线 */
.steps-timeline.el-timeline {
  padding-left: 14px;
  /* 时间线根自带 14px 字号，按气泡内的字号收敛 */
  font-size: 12px;
  --el-timeline-node-color: var(--el-border-color-lighter);
}
/* 节点图标槽默认无偏移，按图标尺寸补回偏移，让图标落在导轨上 */
:deep(.node-item .el-timeline-item__dot) {
  /* 图标 20px：水平中心 = -5 + 10 = 5px（导轨中心）；纵向与标题行（24px）中心对齐 */
  left: -5px;
  top: -1px;
}
/* 工具调用的小圆点：6px 偶数尺寸 + 2px 偏移，中心正好落在 5px 的导轨上 */
:deep(.call-item .el-timeline-item__dot) {
  left: 2px;
  /* 文字行高 18px：中心 9px，圆点半径 3px → top = -3（内容上移 3px）+ 9 - 3 */
  top: 3px;
}
/* 节点图标：按节点类型取图标，执行中/失败用颜色区分（与状态文案一致） */
.node-dot {
  @include flex-center();
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 12px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border: solid 1px var(--el-color-primary-light-7);
  &.is-running {
    border-color: var(--el-color-primary);
  }
  &.is-failed {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
    border-color: var(--el-color-danger-light-5);
  }
}
/* 时间线内容默认主色，节点名与工具方法按下面的层级单独设色 */
:deep(.el-timeline-item__content) {
  color: var(--el-text-color-secondary);
}
:deep(.el-timeline-item) {
  padding-bottom: 10px;
}
/* 节点内容让出图标宽度 */
:deep(.el-timeline-item.is-start .el-timeline-item__wrapper) {
  padding-left: 24px;
}
:deep(.el-timeline-item:last-child) {
  padding-bottom: 0;
}
/* 一个节点：名称 + 状态与耗时文本（形如「（成功 · 871 毫秒）」；工具调用是时间线上的下一条，不嵌在节点里） */
.node-head {
  @include flex-start();
  flex-wrap: wrap;
  gap: 6px;
  .node-name {
    font-weight: 500;
    color: var(--el-text-color-primary);
  }
  .node-meta {
    color: var(--el-text-color-placeholder);
  }
}
/* 工具调用：小圆点 + 一行文字（推理中/调用中用主色，失败用红色） */
.call-item {
  :deep(.el-timeline-item__content) {
    min-width: 0;
  }
  .call-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--el-border-color-darker);
    &.is-running {
      background: var(--el-color-primary);
    }
    &.is-failed {
      background: var(--el-color-danger);
    }
  }
  .call-text {
    min-width: 0;
    /* 固定行高：小圆点按它计算纵向偏移，避免不同页面继承到不同 line-height */
    line-height: 18px;
    word-break: break-word;
  }
}
/* 折叠面板的占位：还没拿到节点时给个提示，避免展开后一片空白 */
.steps-empty {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
