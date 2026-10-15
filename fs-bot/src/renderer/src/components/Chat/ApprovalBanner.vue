<script setup lang="ts">
/**
 * 工作区外的访问申请 - 横幅（不是弹窗：不挡消息、不挡输入，也不打断这一轮对话）。
 *
 * 模型调用的本地工具（读文件 / 写文件 / 执行命令）越过项目声明的目录时，主进程把这次申请推上来等答复：
 * 拒绝 / 允许本次 / 始终允许（顺带把策略切成「帮我批准」，后续不再逐个询问）三个动作都是一步到位，
 * 不再先勾选再确认——同意一件事不该点两次。
 * 命令**每次**都要在这里过一遍（命令的落点不限于工作目录，不能按 cwd 放行），
 * 所以横幅对命令给的是「执行命令」的口径：连命令原文一起给出，人看着命令决定。
 *
 * **跟着对话走**：只画属于本段对话的申请（归属见 `stores/approval`），摆在那段对话的输入框上方；
 * 别处触发的申请由侧边栏会话项上的「待确认」点提示，点进去才由这里的横幅接手。
 *
 * 版式：首行「标题在左、三个动作在右」左右两端分布，动作不侵入下面的内容区——
 * 路径 / 命令 / 说明因此独占整行宽度（命令原文常常很长，被按钮挤掉就没法看清了）。
 */
import { computed } from 'vue'
import { useApprovalStore } from '@renderer/stores/approval'

const props = defineProps<{
  /** 这段对话的标识：横幅只画属于它的申请（没有归属信息的申请哪段对话都接） */
  conversationId: string
}>()

const approval = useApprovalStore()

/** 本段对话要答复的那一条：**逐条**确认，答复后换下一条 */
const current = computed(() => approval.currentOf(props.conversationId))
/** 本段对话下还等着的条数（不含当前这条） */
const waiting = computed(() => Math.max(0, approval.pendingOf(props.conversationId) - 1))
/** 这次申请要做什么：命令单独一句，读 / 写共用文件的口径 */
const isCommand = computed(() => 'command' === current.value?.need)
/** 标题：命令不按工作区算，说「工作区外」会误导 */
const title = computed(() => (isCommand.value ? '本地命令执行申请' : '工作区外的访问申请'))
const needLabel = computed(() => {
  if ('command' === current.value?.need) return '执行命令'
  return 'write' === current.value?.need ? '写入' : '读取'
})
</script>

<template>
  <div v-if="current" class="approval-banner">
    <div class="banner-head">
      <svg class="banner-icon" viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
        <path
          d="M8 1.8 14.6 13.4H1.4L8 1.8Z"
          fill="none"
          stroke="currentColor"
          stroke-width="1.2"
          stroke-linejoin="round"
        />
        <path d="M8 6.2v3.1" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
        <circle cx="8" cy="11.6" r="0.75" fill="currentColor" />
      </svg>
      <span class="banner-title">{{ title }}</span>
      <el-tag size="small" type="warning" effect="plain">
        {{ needLabel }}
      </el-tag>
      <span v-if="waiting > 0" class="banner-more">另有 {{ waiting }} 条待确认</span>
      <div class="banner-actions">
        <el-button
          class="banner-btn"
          size="small"
          text
          type="danger"
          :loading="approval.isSubmitting(current)"
          title="拒绝这一步：失败原因会作为工具返回交给模型，本轮对话不中断"
          @click="approval.answer(current, false)"
        >
          <!-- 图标走 #icon 插槽：Element Plus 会把它包进 el-icon，自动跟着按钮的尺寸与字色走 -->
          <template #icon>
            <svg viewBox="0 0 16 16" aria-hidden="true">
              <path
                d="M4.4 4.4 11.6 11.6M11.6 4.4 4.4 11.6"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
              />
            </svg>
          </template>
          拒绝
        </el-button>
        <el-button
          class="banner-btn"
          size="small"
          text
          type="primary"
          :loading="approval.isSubmitting(current)"
          title="只放行这一步：下一次同类访问还会再问一遍"
          @click="approval.answer(current, true)"
        >
          <template #icon>
            <svg viewBox="0 0 16 16" aria-hidden="true">
              <path
                d="M3.9 8.5 6.6 11.2 12.1 5.7"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </template>
          允许本次
        </el-button>
        <el-button
          class="banner-btn"
          size="small"
          text
          type="success"
          :loading="approval.isSubmitting(current)"
          title="放行这一次，并把访问策略切成「帮我批准」：后续工作区外的访问不再逐个询问"
          @click="approval.answer(current, true, true)"
        >
          <!-- 圈内对勾：与「允许本次」的裸勾拉开差别；双勾在 12px 下糊成一团，试过不好认 -->
          <template #icon>
            <svg viewBox="0 0 16 16" aria-hidden="true">
              <circle cx="8" cy="8" r="6.4" fill="none" stroke="currentColor" stroke-width="1.3" />
              <path
                d="M5.2 8.4 7.2 10.4 10.9 6.1"
                fill="none"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </template>
          始终允许
        </el-button>
      </div>
    </div>
    <!-- 内容块：与标题左对齐，独占整行宽度 -->
    <div class="banner-content">
      <div class="banner-line">
        <span class="banner-label">{{ isCommand ? '目录' : '路径' }}</span>
        <span class="banner-text">{{ current.path }}</span>
      </div>
      <div v-if="isCommand && current.command" class="banner-line">
        <span class="banner-label">命令</span>
        <span class="banner-text">{{ current.command }}</span>
      </div>
      <div class="banner-note">
        {{
          isCommand
            ? '命令以本机身份运行，能改动的范围不限于工作目录；拒绝会把失败原因作为工具返回交给模型，本轮不会中断。'
            : '允许后模型才能继续这一步；拒绝会把失败原因作为工具返回交给模型，本轮不会中断。'
        }}
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/**
 * 横幅：左侧一条警示色竖线，首行「标题 + 动作」左右两端分布，内容独占其下的整行。
 * 底色与描边都用 warning 的浅色档，与对话页的警示样式保持一套口径。
 */
.approval-banner {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 12px 8px 10px;
  border: 1px solid var(--el-color-warning-light-5);
  border-left: 3px solid var(--el-color-warning);
  border-radius: 8px;
  background: var(--el-color-warning-light-9);
}

/* 首行：图标、标题、标签在左，动作靠 margin-left:auto 推到另一端；一行挤不下时动作顺势换行 */
.banner-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.banner-icon {
  flex: none;
  color: var(--el-color-warning);
}

.banner-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.banner-more {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 内容块：左边空出图标那一列（14px 图标 + 8px 间距），正文与标题同一条竖线 */
.banner-content {
  padding-left: 22px;
}

.banner-line {
  display: flex;
  gap: 8px;
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.7;
}

.banner-label {
  flex: none;
  width: 28px;
  color: var(--el-text-color-placeholder);
}

.banner-text {
  min-width: 0;
  word-break: break-all;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: var(--el-text-color-regular);
}

.banner-note {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
}

/**
 * 三个动作：左右两端分布里的「右端」。
 * 都是文字按钮，视觉间隔 = 行内 gap + 两侧按钮的内边距：取 gap 2px / 内边距 7px，标签之间约 16px。
 */
.banner-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: none;
  margin-left: auto;
}

/* 文字按钮（无边框、无底），悬停才浮出一层浅底，横幅因此不会被按钮压得很重 */
.banner-btn {
  height: 28px;
  padding: 0 7px;
  border-radius: 6px;
  font-weight: 500;
}

/* 横幅自带琥珀底色，Element Plus 默认的 hover 底色太浅看不出来，压深一档 */
.banner-btn.el-button:hover,
.banner-btn.el-button:focus-visible {
  background: var(--el-fill-color);
}
</style>
