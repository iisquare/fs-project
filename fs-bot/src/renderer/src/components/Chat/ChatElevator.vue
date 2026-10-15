<script setup lang="ts">
/**
 * 对话电梯导航：为消息列表里的用户消息在右侧生成一列横条，
 * 悬停 / 聚焦时展开成内容列表，点击平滑定位到对应气泡。
 *
 * 与页面解耦：父页面传入滚动容器、命中消息的选择器与摘要文本选择器即可复用。
 * - 摆放：组件自身绝对定位（贴容器右侧垂直居中），父容器需为 position: relative，
 *   且组件要放在滚动容器之外，导航才不会跟着消息一起滚；
 * - 采集：内容版本号变化时重新扫描容器，父页面不必提供数据；
 * - 同步：组件自己监听容器滚动来更新「当前所在条」。
 */
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

const {
  target = null,
  selector = '.chat-message.is-user',
  textSelector = '',
  min = 2,
  revision = 0
} = defineProps<{
  target?: HTMLElement | null
  selector?: string
  textSelector?: string
  /** 达到该条数才显示：只有一条时没有定位意义 */
  min?: number
  /** 内容版本号（会话标识 + 消息条数），变化时重新采集 */
  revision?: string | number
}>()

interface ElevatorMark {
  key: string
  index: number
  text: string
}

const marks = ref<ElevatorMark[]>([])
/** 当前所在条：最下面一条已越过容器顶部的消息 */
const activeMark = ref('')
/** 悬停 / 聚焦的条：文字与横条共用同一个状态，两侧变化保持同步 */
const hoverMark = ref('')
const open = ref(false)
const rootRef = ref<any>(null)

function rows(): HTMLElement[] {
  return target ? Array.from(target.querySelectorAll<HTMLElement>(selector)) : []
}

/** 当前所在条：取最下面一条已越过容器顶部的消息 */
function syncActive(): void {
  const element = target
  if (!element) return
  const base = element.getBoundingClientRect().top
  let current = ''
  rows().forEach((row, index) => {
    if (row.getBoundingClientRect().top - base <= 28) current = `item-${index}`
  })
  activeMark.value = current
}

/** 采集导航条目：摘要直接取自已渲染的消息 */
function collect(): void {
  if (!target) {
    marks.value = []
    return
  }
  marks.value = rows().map((row, index) => {
    const source: HTMLElement = textSelector
      ? (row.querySelector<HTMLElement>(textSelector) ?? row)
      : row
    const text = String(source.textContent ?? '')
      .replace(/\s+/g, ' ')
      .trim()
    return {
      key: `item-${index}`,
      index,
      text: `${index + 1}. ${text.slice(0, 40) || '（附件消息）'}`
    }
  })
  syncActive()
}

/** 定位：按容器内偏移量平滑滚动，不调用 scrollIntoView（避免连带滚动外层布局） */
function scrollTo(mark: ElevatorMark): void {
  const element = target
  const row = rows()[mark.index]
  if (!element || !row) return
  const offset = row.getBoundingClientRect().top - element.getBoundingClientRect().top
  element.scrollTo({ top: element.scrollTop + offset - 12, behavior: 'smooth' })
  activeMark.value = mark.key
}

/** 展开或当前位置变化时，把对应条目滚进可视化区（横条列与展开列表都可滚动） */
function revealActive(): void {
  // ElScrollbar 暴露的是 wrapRef（可能已被解包成元素，两种形状都兼容）
  const wrap = rootRef.value?.wrapRef
  const element: HTMLElement | null = wrap?.value ?? wrap ?? null
  const row = element?.querySelector<HTMLElement>('.elevator-item.is-active')
  if (!element || !row) return
  const top = row.offsetTop
  if (top >= element.scrollTop && top + row.offsetHeight <= element.scrollTop + element.clientHeight)
    return
  element.scrollTo({
    top: Math.max(0, top - element.clientHeight / 2 + row.offsetHeight / 2),
    behavior: 'smooth'
  })
}

// 容器或内容变化：重新采集条目
watch(() => [target, revision], () => nextTick(collect), { immediate: true })
// 容器滚动：组件自己同步当前条
watch(
  () => target,
  (element, previous) => {
    previous?.removeEventListener('scroll', syncActive)
    element?.addEventListener('scroll', syncActive, { passive: true })
    nextTick(syncActive)
  },
  { immediate: true }
)
watch([open, activeMark], () => nextTick(revealActive))
onBeforeUnmount(() => target?.removeEventListener('scroll', syncActive))
</script>

<template>
  <!-- 横条列：收起只显示横条，悬停 / 聚焦展开成内容列表 -->
  <el-scrollbar
    class="elevator"
    :class="{ 'is-open': open }"
    ref="rootRef"
    v-if="marks.length >= min"
    @mouseenter="open = true"
    @mouseleave="open = false"
    @focusin="open = true"
    @focusout="open = false"
  >
    <div
      class="elevator-item"
      :class="{ 'is-hover': mark.key === hoverMark, 'is-active': mark.key === activeMark }"
      :key="mark.key"
      v-for="mark in marks"
      @mouseenter="hoverMark = mark.key"
      @mouseleave="hoverMark = ''"
      @click="scrollTo(mark)"
    >
      <span class="elevator-text">{{ mark.text }}</span>
      <span
        class="elevator-bar"
        tabindex="0"
        @focus="hoverMark = mark.key"
        @blur="hoverMark = ''"
        @keydown.enter.prevent="scrollTo(mark)"
        @keydown.space.prevent="scrollTo(mark)"
      />
    </div>
  </el-scrollbar>
</template>

<style lang="scss" scoped>
/**
 * 电梯导航：贴容器右侧垂直居中。每条消息一行（文字 + 行尾横条），
 * 横条跟着自己那一行走，所以文字与横条永远对齐；收起只留横条，
 * 展开后整块共用同一块底板，视觉上是一个整体。
 * 消息列的右侧内边距只有 16px，还得让开 6px 滚动条：横条收成 8px，
 * 正好卡在「滚动条 8px — 气泡边 16px」这条空缝里，两态都不压气泡。
 */
.elevator {
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  /* el-scrollbar 默认 height: 100%：这里按内容定高，太长时才由 max-height 收敛 */
  height: auto;
  max-height: 60vh;
  z-index: 1;

  /* 条目一行一条：文字在左、横条在行尾 */
  :deep(.el-scrollbar__view) {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 0;
  }

  /* 收起态只有一列细横条，不放滚动条 */
  &:not(.is-open) {
    :deep(.el-scrollbar__bar) {
      display: none;
    }
  }

  /* 展开：文字与横条同处一块底板；右移 8px（内边距 7 + 描边 1）让横条位置保持不变 */
  &.is-open {
    right: 0;
    min-width: 220px;
    max-width: 320px;
    padding: 8px 7px;
    border-radius: 10px;
    border: solid 1px var(--el-border-color-lighter);
    background: var(--el-bg-color);
    box-shadow: 0 4px 16px rgb(0 0 0 / 8%);

    :deep(.el-scrollbar__view) {
      align-items: stretch;
    }
  }

  .elevator-item {
    /* 行高固定：收起时这一行只有横条，展开后同一行放文字，两态行位置一致 */
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    cursor: pointer;

    .elevator-text {
      flex: 1 1 auto;
      min-width: 0;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      font-size: 12px;
      line-height: 16px;
      color: var(--el-text-color-regular);
      transition: color 0.2s;
    }

    &.is-hover {
      .elevator-text {
        color: var(--el-text-color-primary);
      }

      .elevator-bar {
        background: var(--el-text-color-primary);
      }
    }

    /* 当前所在位置：文字用主色，横条比其它条长一点点 */
    &.is-active {
      .elevator-text {
        color: var(--el-color-primary);
      }

      .elevator-bar {
        width: 12px;
        background: var(--el-color-primary);
      }
    }
  }

  .elevator-bar {
    flex: none;
    width: 8px;
    height: 2px;
    border-radius: 1px;
    /* 默认灰 → hover 黑 → 选中主色，三档区分 */
    background: var(--el-text-color-placeholder);
    transition: background-color 0.2s;
  }
}

/* 收起状态：隐藏文字，只留一列横条 */
.elevator:not(.is-open) .elevator-text {
  display: none;
}
</style>
