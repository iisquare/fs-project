<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { page, projectCreateRoute } from '@renderer/router/config'
import { useProjectStore } from '@renderer/stores/project'
import { useConversationStore } from '@renderer/stores/conversation'
import { useSettingsStore } from '@renderer/stores/settings'
import { useSkillStore } from '@renderer/stores/skill'
import { useCatalogStore } from '@renderer/stores/catalog'
import { useUserStore } from '@renderer/stores/user'
import { useFilesStore, FILES_WIDTH_MIN, FILES_WIDTH_MAX } from '@renderer/stores/files'
import { useWindowStore } from '@renderer/stores/window'
import BotApi from '@renderer/api/agent/BotApi'
import { chatProfile, useChatStore, type ChatMention } from '@renderer/stores/chat'
import type { Project } from '@renderer/db/ProjectDb'
import ChatMessage from '@renderer/components/Chat/ChatMessage.vue'
import ChatElevator from '@renderer/components/Chat/ChatElevator.vue'
import ApprovalBanner from '@renderer/components/Chat/ApprovalBanner.vue'
import ConversationFiles from '@renderer/components/Chat/ConversationFiles.vue'
import type { ContextInfo } from '@renderer/components/Chat/ContextRing.vue'
import ChatComposer, {
  type ComposerCommand,
  type MentionCandidate
} from '@renderer/components/Chat/ChatComposer.vue'

/**
 * 对话页：会话按项目创建，消息用项目选定的模型直接对话，历史存在本地。
 *
 * 一轮对话是一段「模型 → 工具 → 模型」的循环：模型要调工具就调，
 * 本地工具（文件与命令）由主进程按项目权限与工作区外访问策略执行，项目工具交给服务端；
 * 调用过程与结果实时写在消息上，完整调用链随下一轮一起带上。
 *
 * 运行态在 chat store 上，切换会话 / 切到别的页面都不会中断本轮生成；
 * 页面上只负责把当前会话画出来，并提供停止、@ 引用与内置指令。
 */

const router = useRouter()
const projectStore = useProjectStore()
const conversationStore = useConversationStore()
const settingsStore = useSettingsStore()
const skillStore = useSkillStore()
const catalogStore = useCatalogStore()
const userStore = useUserStore()
const chatStore = useChatStore()

const draft = ref('')
const mentions = ref<ChatMention[]>([])
const composerRef = ref<any>(null)
/** 消息区滚动条实例与它真正的滚动元素（el-scrollbar 内容包在内部 wrap 里） */
const scrollRef = ref<any>(null)
const listRef = ref<HTMLElement | null>(null)
/** 是否贴着底部：贴着才跟随新内容，向上翻阅历史时不被拉回 */
const atBottom = ref(true)

/**
 * 右侧文件区存在 store 里，所有会话共用：默认收起，展开与否由这里的按钮说了算
 * （进对话不替用户做主，展开状态也只在本次运行内有效）；拖出来的宽度与当前页签下次打开还在（见 stores/files）。
 */
const files = useFilesStore()
/** 窗口是否钉住（置顶窄条）：钉住后只留对话，右上角这个文件区开关也藏起来 */
const win = useWindowStore()
/** 中间对话栏的最小宽度：与 $chat-min 同一口径，分隔条拖不到比这更窄 */
const CHAT_PANEL_MIN = 320

/**
 * el-scrollbar 挂载后才拿得到内部滚动元素，之后再通知电梯导航换基准。
 * ElScrollbar 暴露的是 `wrapRef`（组件里可能已被解包成元素，两种形状都兼容一下）。
 */
function setScrollRef(instance: any): void {
  scrollRef.value = instance
  void nextTick(() => {
    const wrap = instance?.wrapRef
    listRef.value = wrap?.value ?? wrap ?? null
  })
}

/** 内置指令：输入 / 弹出候选并执行（如基于当前项目新建对话） */
const commands: ComposerCommand[] = [
  { name: '/new', label: '新建对话', description: '在当前项目下开启一轮新对话' },
  { name: '/project', label: '切换项目', description: '回到项目选择面板，换一个项目开会话' },
  { name: '/workspace', label: '项目与工作区', description: '打开项目管理，维护工作区目录与权限' },
  { name: '/skill', label: '技能管理', description: '打开技能管理，安装或移除本地技能' },
  { name: '/policy', label: '本地访问策略', description: '切换工作区外文件与命令的访问策略' },
  {
    name: '/upload',
    label: '上传文件',
    description:
      '选择本机文件上传到图床，拿到文件标识后交给模型分析（如图片交给 multimodalAnalyze）'
  },
  { name: '/stop', label: '停止生成', description: '中断本轮生成与工具调用' },
  { name: '/clear', label: '清空当前对话', description: '删除这个会话的本地消息记录' },
  { name: '/help', label: '帮助', description: '查看可用指令与 @ 引用用法' }
]
const POLICY_ORDER = ['ask', 'auto', 'deny', 'full']
const POLICY_LABELS: Record<string, string> = {
  ask: '请求批准',
  auto: '帮我批准',
  deny: '自动拒绝',
  full: '完全访问权限'
}

/** 当前会话：历史里的那条，或还没发过消息、还没落库的新会话 */
const conversation = computed(() => conversationStore.current)
/** 会话所属项目：模型、工具与工作区权限都按它算 */
const project = computed(() =>
  conversation.value ? projectStore.find(conversation.value.projectId) : null
)
const session = computed(() =>
  conversationStore.currentId ? chatStore.find(conversationStore.currentId) : null
)
/** 页面只渲染当前分支路径：编辑提问 / 重新生成新起的分支靠「◀ n/m ▶」切回 */
const messages = computed(() => (session.value ? chatStore.visible(session.value) : []))
const running = computed(() => session.value?.running === true)

/** 圆环口径里的模型：按项目当前配置取（与发送时一致），项目里换了模型立刻跟着换 */
const contextModel = computed(() => {
  const current = session.value
  if (!current) return ''
  return chatProfile(project.value).model || current.model
})

/** 项目里手工指定的上下文窗口：0 表示不在项目里指定 */
const projectContext = computed(() =>
  Math.max(0, Math.floor(Number(project.value?.contextLength ?? 0)) || 0)
)

/**
 * 上下文用量（输入框旁的圆环）：下一轮请求的估算用量 + 这个模型生效的窗口大小。
 * 窗口以项目里手工指定的那份为准（固化在项目配置里），项目没指定时退回服务端在模型清单里声明的窗口；
 * 两份都没有时窗口未知（limit 为 0），圆环退化成问号，点开填一个即可。
 */
const context = computed<ContextInfo | null>(() => {
  const current = session.value
  if (!current) return null
  const model = contextModel.value
  const declared = catalogStore.modelContext(model)
  const custom = projectContext.value
  return {
    model,
    used: chatStore.contextUsage(current, draft.value),
    limit: custom > 0 ? custom : declared,
    declared,
    custom: custom > 0
  }
})

/** 圆环里改的窗口大小：固化到项目配置里（本项目的所有会话共用）；0 / null 表示回到服务端声明的窗口 */
function setContextLimit(value: number | null): void {
  const current = project.value
  if (!current) return
  const length = Math.max(0, Math.floor(Number(value ?? 0)) || 0)
  void projectStore.update(current.id, { contextLength: length })
}

/**
 * @ 引用候选：已安装技能（本机文件与文件夹走输入框里那两条「选择本地…」动作，不开候选清单——
 * 工作区里的文件让模型自己按路径找，不必在这里铺一份清单出来）
 */
const candidates = computed<MentionCandidate[]>(() =>
  skillStore.installed.map((item) => ({
    type: 'skill' as const,
    name: item.name,
    path: item.path,
    token: `skill:${item.name}`
  }))
)

onMounted(async () => {
  await projectStore.fetchProjects()
  await settingsStore.fetchSettings()
  await skillStore.fetchInstalled()
  // 模型清单要给出「服务端声明的上下文窗口」，圆环的比例按它算
  await catalogStore.fetchModels()
  if (conversationStore.currentId) await conversationStore.select(conversationStore.currentId)
})

watch(
  () => conversationStore.currentId,
  async (id) => {
    if (!id) return
    await conversationStore.select(id)
    draft.value = ''
    mentions.value = []
    await nextTick()
    scrollBottom(true)
  }
)

// 内容变化时跟随滚动：贴底才跟着走
watch(
  () => messages.value.length,
  async () => {
    await nextTick()
    scrollBottom(true)
  }
)
watch(
  () => (session.value ? `${session.value.running}` : ''),
  async () => {
    await nextTick()
    if (atBottom.value) scrollBottom()
  }
)
// 流式增量不会改变消息数量，额外按内容长度变化跟随一次
watch(
  () =>
    messages.value
      .map((item) =>
        [
          item.content.length,
          item.reasoning.length,
          ...(item.rounds ?? []).map(
            (round) =>
              round.content.length +
              round.reasoning.length +
              (round.calls ?? []).length * 1000 +
              (round.calls ?? []).reduce((sum, call) => sum + call.result.length, 0)
          )
        ].join(':')
      )
      .join('|'),
  async () => {
    await nextTick()
    if (atBottom.value) scrollBottom()
  }
)

function onScroll(): void {
  const element = listRef.value
  if (!element) return
  atBottom.value = element.scrollHeight - element.scrollTop - element.clientHeight < 40
}

function scrollBottom(force = false): void {
  const element = listRef.value
  if (!element) return
  if (force) atBottom.value = true
  element.scrollTop = element.scrollHeight
}

/** 在某个项目里开新会话：会话从此按这个项目的模型与权限跑 */
async function startConversation(item: Project): Promise<void> {
  const created = await conversationStore.create(item)
  await conversationStore.select(created.id)
  await nextTick()
  composerRef.value?.focus?.()
}

/** 换一个项目开会话：先回到「选择项目」面板（当前会话继续在后台跑） */
function chooseProject(): void {
  if (projectStore.projects.length < 1) {
    openProjectPage(true)
    return
  }
  conversationStore.deselect()
}

/** 打开项目管理页；createNew 为真时直接落到新建表单 */
function openProjectPage(createNew: boolean): void {
  router.push(createNew ? projectCreateRoute() : page.project)
}

function send(): void {
  if (!conversation.value) return
  const profile = chatProfile(project.value)
  if (!profile.model) {
    ElMessage.warning('该会话所属的项目没有可用模型，请先在项目管理里配置')
    return
  }
  const text = draft.value.trim()
  // 走会话 store：新会话的第一条消息会把会话带进历史，之后再交给 chat store 跑
  void conversationStore.send({
    conversationId: conversation.value.id,
    ...profile,
    query: text,
    mentions: collectMentions(text)
  })
  draft.value = ''
  mentions.value = []
  atBottom.value = true
  void nextTick(() => scrollBottom(true))
}

/**
 * 汇总这一轮的 @ 引用：候选里选的照单全收，另外把输入里手写的绝对路径也接进来——
 * 这类路径不一定在工作区内，读取时同样按工作区外的访问策略处理。
 */
function collectMentions(text: string): ChatMention[] {
  const rows = mentions.value.slice()
  const known = new Set(rows.map((item) => item.token ?? item.path))
  const pattern = /(^|\s)@([^\s@]+)/g
  let match = pattern.exec(text)
  while (match) {
    const token = match[2]
    if (!known.has(token) && /^(?:[a-zA-Z]:[\\/]|\\\\|\/)/.test(token)) {
      known.add(token)
      rows.push({ type: 'file', name: token, path: token, token })
    }
    match = pattern.exec(text)
  }
  return rows
}

function stop(): void {
  if (conversation.value) chatStore.stop(conversation.value.id)
}

/** 分支切换：按当前消息的兄弟节点前后移动（只改展示与续写位置，不动消息本身） */
function switchBranch(message: any, step: number): void {
  if (session.value) chatStore.switchBranch(session.value, message, step)
}

/** 编辑提问后重新发送：新提问接在被编辑消息的父节点下，形成新分支 */
function editMessage(message: any, content: string): void {
  if (session.value) chatStore.edit(session.value, message, content)
}

/** 重新生成：不重复落用户消息，只在同一条提问下新增一条回复 */
function regenerateMessage(message: any): void {
  if (session.value) chatStore.regenerate(session.value, message)
}

/** 内置指令：/ 输入框里选中或手打回车时到这里执行 */
async function runCommand(name: string): Promise<void> {
  if ('/new' === name) {
    // 基于当前项目直接开一轮新对话；没有项目时回到项目面板去建
    if (project.value) await startConversation(project.value)
    else chooseProject()
    return
  }
  if ('/project' === name) {
    chooseProject()
    return
  }
  if ('/workspace' === name) {
    router.push(page.project)
    return
  }
  if ('/skill' === name) {
    router.push(page.skill)
    return
  }
  if ('/policy' === name) {
    const index = POLICY_ORDER.indexOf(settingsStore.workspacePolicy)
    const next = POLICY_ORDER[(index + 1) % POLICY_ORDER.length]
    await settingsStore.setWorkspacePolicy(next)
    ElMessage.success(`本地访问策略：${POLICY_LABELS[next] ?? next}`)
    return
  }
  if ('/upload' === name) {
    await uploadFiles()
    return
  }
  if ('/stop' === name) {
    if (!running.value) {
      ElMessage.info('当前没有正在生成的回复')
      return
    }
    stop()
    return
  }
  if ('/clear' === name) {
    if (!conversation.value) return
    await conversationStore.clearMessages(conversation.value.id)
    ElMessage.success('已清空当前对话')
    return
  }
  if ('/help' === name) {
    await ElMessageBox.alert(
      `<div class="command-help">${commands
        .map((item) => `<p><code>${item.name}</code> ${item.label} — ${item.description}</p>`)
        .join(
          ''
        )}<p>输入 <code>@</code> 引用本机文件或已安装技能，引用内容会随这一轮一起交给模型。</p></div>`,
      '可用指令',
      { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' }
    ).catch(() => {})
    return
  }
}

async function setPolicy(value: string): Promise<void> {
  await settingsStore.setWorkspacePolicy(value)
  ElMessage.success(`本地访问策略：${POLICY_LABELS[value] ?? value}`)
}

/**
 * /upload：选本机文件上传到图床（文件服务），把拿到的「文件标识」等信息写进输入框。
 *
 * 服务端读不到本机磁盘，本机路径传给 multimodalAnalyze 这类工具方法会被拒绝；
 * 先在这里传一次、拿到 id，模型按 id 引用就能分析。模型也可以自己调本地工具 file_op（action=upload）
 * 做同样的事（它手上有路径时更顺手），两条路走的是同一个上传通道与同一份本地访问策略。
 */
async function uploadFiles(): Promise<void> {
  const current = project.value
  if (!current) {
    chooseProject()
    return
  }
  const paths = await window.ipc.workspace.pickPaths('file')
  if (!paths.length) return
  const target = { url: BotApi.uploadUrl(), token: userStore.info.token }
  const lines: string[] = []
  for (const path of paths) {
    try {
      const result = await window.ipc.workspace.upload(current.id, path, target)
      lines.push(
        `【上传文件】${result.name} 文件标识：${result.id} 类型：${result.type || '未知'} 大小：${result.size} 字节`
      )
    } catch (error) {
      ElMessage.error(`${path} 上传失败：${error instanceof Error ? error.message : String(error)}`)
    }
  }
  if (!lines.length) return
  lines.push('（要分析这些文件，把上面的文件标识交给模型即可——本机路径交给服务端工具方法会被拒绝）')
  draft.value = [String(draft.value ?? '').trim(), ...lines].filter(Boolean).join('\n')
  void nextTick(() => composerRef.value?.focus?.())
}
</script>

<template>
  <div class="home-page">
    <!-- 没有会话：按项目开启新对话 -->
    <div v-if="!conversation" class="start-panel">
      <div class="start-inner">
        <h2>选择项目开始对话</h2>
        <p class="start-desc">
          会话按项目创建：模型、工具与工作区权限都取自所属项目，工作区外的访问按访问策略放行。
        </p>
        <div v-if="projectStore.projects.length === 0" class="start-empty">
          <el-empty description="暂无项目">
            <el-button type="primary" @click="openProjectPage(true)">创建项目</el-button>
          </el-empty>
        </div>
        <div v-else class="project-list">
          <div
            class="project-card"
            v-for="item in projectStore.projects"
            :key="item.id"
            @click="startConversation(item)"
          >
            <el-avatar :size="32" :src="item.icon">{{ item.name.charAt(0) }}</el-avatar>
            <div class="project-text">
              <div class="project-name">{{ item.name }}</div>
              <div class="project-desc">{{ item.description || '暂无描述' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 会话视图 -->
    <div v-else class="conversation-view">
      <header class="conversation-header">
        <el-avatar class="header-avatar" :size="26" :src="project?.icon">{{
          (project?.name || '未选择项目').charAt(0)
        }}</el-avatar>
        <span class="conv-project">{{ project?.name || '未选择项目' }}</span>
        <span class="conv-title">{{ conversation.title || '新对话' }}</span>
        <el-tag v-if="running" class="conv-tag" size="small" type="success" effect="plain"
          >运行中</el-tag
        >
        <!-- 右上角：展开 / 收起右侧文件区（钉住窗口时只留对话，这个开关一并收起来） -->
        <el-tooltip
          v-if="!win.pinned"
          :content="files.visible ? '收起文件区' : '展开文件区'"
          placement="bottom"
        >
          <el-button
            class="files-toggle"
            link
            :class="{ 'is-open': files.visible }"
            @click="files.toggle()"
          >
            <svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true">
              <rect
                x="1.7"
                y="3.1"
                width="12.6"
                height="9.8"
                rx="1.6"
                fill="none"
                stroke="currentColor"
                stroke-width="1.2"
              />
              <path d="M10.3 3.1v9.8" fill="none" stroke="currentColor" stroke-width="1.2" />
            </svg>
          </el-button>
        </el-tooltip>
      </header>

      <!--
        对话主体：中间是消息区与输入区，右边是文件区。
        中右之间的分隔条拖出来的宽度记在 store 里；左边的会话栏由外层布局的 splitter 管（见 views/index.vue）
      -->
      <el-splitter class="conversation-splitter" @resize-end="files.saveWidth()">
        <el-splitter-panel :min="CHAT_PANEL_MIN">
          <div class="conversation-column">
            <!-- 消息区：滚动容器 + 电梯导航（导航在容器之外，不随消息一起滚） -->
            <div class="conversation-body">
              <el-scrollbar class="message-list" :ref="setScrollRef" @scroll="onScroll">
                <div class="message-column">
                  <div v-if="messages.length === 0" class="message-empty">
                    <p>开始新对话</p>
                    <p class="message-empty-tip">
                      输入内容后回车发送；输入 <code>@</code> 引用本机文件或技能，输入
                      <code>/</code> 执行内置指令。回复会实时流式输出，工具调用过程与结果同步展示。
                    </p>
                  </div>
                  <ChatMessage
                    class="message"
                    v-for="message in messages"
                    :key="message.id"
                    :message="message"
                    :project-id="session?.projectId ?? ''"
                    :branch="session ? chatStore.branchOf(session, message) : null"
                    :editable="
                      !running && 'user' === message.role && !!String(message.content ?? '').trim()
                    "
                    :regenerable="!running && 'assistant' === message.role && !!message.parentId"
                    @switch="(step: number) => switchBranch(message, step)"
                    @edit="(content: string) => editMessage(message, content)"
                    @regenerate="regenerateMessage(message)"
                  />
                  <el-alert
                    v-if="session?.notice"
                    class="message-error"
                    type="warning"
                    :title="session.notice"
                    :closable="false"
                    show-icon
                  />
                  <el-alert
                    v-if="session?.error"
                    class="message-error"
                    type="error"
                    :title="session.error"
                    :closable="false"
                    show-icon
                  />
                </div>
              </el-scrollbar>
              <ChatElevator
                :target="listRef"
                :revision="`${conversation.id}:${session?.leafId ?? ''}:${messages.length}`"
                selector=".chat-message.is-user"
                text-selector=".chat-content"
              />
            </div>

            <footer class="composer-area">
              <div class="composer-column">
                <!-- 访问申请：只画本段对话的（跟着对话走），别处触发的由侧边栏的「待确认」点提示 -->
                <ApprovalBanner class="composer-approval" :conversation-id="conversation.id" />
                <ChatComposer
                  ref="composerRef"
                  v-model="draft"
                  v-model:mentions="mentions"
                  :candidates="candidates"
                  :commands="commands"
                  :policy="settingsStore.workspacePolicy"
                  :context="context"
                  :running="running"
                  :disabled="running"
                  @send="send"
                  @stop="stop"
                  @command="runCommand"
                  @update:context-limit="setContextLimit"
                  @update:policy="setPolicy"
                />
              </div>
            </footer>
          </div>
        </el-splitter-panel>

        <!-- 右侧文件区：项目工作区目录树 + 这段对话交付的文件（右上角按钮展开 / 收起） -->
        <el-splitter-panel
          v-if="files.visible"
          :size="files.width"
          :min="FILES_WIDTH_MIN"
          :max="FILES_WIDTH_MAX"
          @update:size="files.setWidth"
        >
          <ConversationFiles
            :project-id="session?.projectId ?? ''"
            :messages="messages"
            :roots="project?.workspace ?? []"
            :running="running"
          />
        </el-splitter-panel>
      </el-splitter>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/**
 * 对话列几何：消息列与输入框共用同一套宽度与左右内边距（没有头像，
 * 气泡左右边缘因此与输入框对齐），尺寸用固定像素，窗口不够宽时由内容区横向滚动。
 * 左右内边距 16px，与输入框的下边距、会话头部的左右内边距一致：四边同宽，
 * 气泡贴到头尾；右侧那 16px 里再分给滚动条与电梯导航（见 ChatElevator）。
 * 最小宽度取 320px：默认窗口（900px）下左侧栏与右侧文件区都展开时，内容列刚好放得下。
 */
$chat-min: 320px; /* 内容列最小宽度：窗口太窄时由内容区横向滚动兜底 */
$chat-max: 768px; /* 内容列最大宽度：气泡与输入框共用 */

.home-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /**
   * 不铺自己的底色：整块右侧区域沿用内容区的页面底色，
   * 与顶部的窗口按钮组（关闭 / 最小化 / 最大化）连成一片，不再有一条横向的色差。
   */
}

/* ── 未选会话：选择项目 ─────────────────────────────── */

.start-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 40px 40px;
}

.start-inner {
  width: 100%;
  max-width: 860px;
  margin: 0 auto;

  h2 {
    font-size: 20px;
    color: var(--ev-c-text-1);
  }
}

.start-desc {
  margin: 8px 0 24px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ev-c-text-3);
}

.start-empty {
  padding: 40px 0;
}

.project-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}

.project-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: solid 1px var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition:
    border-color 0.2s,
    background-color 0.2s,
    box-shadow 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
    box-shadow: 0 2px 8px rgb(0 0 0 / 6%);
  }

  .project-text {
    flex: 1;
    min-width: 0;
  }

  .project-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-primary);
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .project-desc {
    margin-top: 2px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}

/* ── 会话视图 ───────────────────────────────────────── */

.conversation-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 中右分隔：拖分隔条调文件区宽度（宽度记在 stores/files） */
.conversation-splitter {
  flex: 1;
  min-height: 0;

  /* 面板自带的 overflow: auto 会在对话区 / 文件区之外再套一层滚动条，
     滚动统一交给面板内部的组件自己管（消息区的 el-scrollbar、文件区的页签） */
  :deep(.el-splitter-panel) {
    overflow: hidden;
  }
}

.conversation-column {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}

.conversation-header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  /* 上边距压小：标题栏之下直接是会话信息，不额外留一段空白 */
  padding: 4px 16px;
  border-bottom: solid 1px var(--el-border-color-lighter);

  .header-avatar {
    flex: none;
    font-size: 13px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  .conv-project {
    flex: none;
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }

  .conv-title {
    min-width: 0;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .conv-tag {
    flex: none;
  }

  /* 右上角的文件区开关：靠右端，展开时用主色标出来 */
  .files-toggle {
    flex: none;
    margin-left: auto;
    padding: 0;
    color: var(--el-text-color-placeholder);

    &:hover {
      color: var(--el-color-primary);
    }

    &.is-open {
      color: var(--el-color-primary);
    }
  }
}

/* 消息区外层：给右侧电梯导航提供定位基准（导航不随消息一起滚动） */
.conversation-body {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
}

.message-list {
  flex: 1;
  min-height: 0;

  /* 内边距落到滚动内容上：滚动条贴容器边缘，内容与输入框保持同一条竖线。只给消息区自己这一层（wrap > view 直接子级）：消息里的代码块、工具返回各有 el-scrollbar，跟着吃这份内边距会把块内正文顶歪 */
  :deep(> .el-scrollbar__wrap > .el-scrollbar__view) {
    padding: 10px 16px 8px;
  }
}

.message-column {
  width: 100%;
  min-width: $chat-min;
  max-width: $chat-max;
  margin: 0 auto;

  .message + .message {
    margin-top: 16px;
  }
}

.message-empty {
  padding: 40px 0 0;
  text-align: center;
  color: var(--el-text-color-placeholder);

  p {
    font-size: 13px;
    line-height: 1.8;
  }

  .message-empty-tip {
    margin-top: 8px;
    font-size: 12px;
  }

  code {
    padding: 0 4px;
    border-radius: 4px;
    background: var(--el-fill-color-light);
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  }
}

.message-error {
  margin-top: 12px;
}

.composer-area {
  flex: none;
  width: 100%;
  /* 左右内边距与消息区一致：输入框与气泡同一条竖线；下边距同为 16px */
  padding: 12px 16px 16px;
}

.composer-column {
  width: 100%;
  min-width: $chat-min;
  max-width: $chat-max;
  margin: 0 auto;
}

.composer-approval {
  margin-bottom: 8px;
}
</style>
