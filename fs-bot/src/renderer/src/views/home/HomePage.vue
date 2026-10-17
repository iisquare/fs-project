<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { page, projectCreateRoute } from '@renderer/router/config'
import { useProjectStore } from '@renderer/stores/project'
import { useConversationStore, type QueuedSend } from '@renderer/stores/conversation'
import { useSettingsStore } from '@renderer/stores/settings'
import { useSkillStore } from '@renderer/stores/skill'
import { useCatalogStore } from '@renderer/stores/catalog'
import { useUserStore } from '@renderer/stores/user'
import { useFilesStore, FILES_WIDTH_MIN, FILES_WIDTH_MAX } from '@renderer/stores/files'
import { useWindowStore } from '@renderer/stores/window'
import BotApi from '@renderer/api/agent/BotApi'
import { errorMessage } from '@renderer/utils/ErrorUtil'
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
/** 一次发送只走一条链路：确认框打开或提交中时，重复回车不再叠一条消息 */
const sending = ref(false)
const pageLoading = ref(false)
const pageError = ref('')
const composerRef = ref<any>(null)
/** 消息区滚动条实例与它真正的滚动元素（el-scrollbar 内容包在内部 wrap 里） */
const scrollRef = ref<any>(null)
const listRef = ref<HTMLElement | null>(null)
/** 是否贴着底部：贴着才跟随新内容，向上翻阅历史时不被拉回 */
const atBottom = ref(true)
/** 拖入计数：拖过里面的子元素会连着来一串 enter / leave，按进出次数决定提示层的显隐 */
const dragging = ref(0)

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
/** 这段会话待发送的消息：生成中提交后排队，当前一轮收尾后自动发出 */
const queuedMessages = computed<QueuedSend[]>(() =>
  conversation.value ? conversationStore.queuedOf(conversation.value.id) : []
)

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

async function loadPageData(): Promise<void> {
  pageLoading.value = true
  pageError.value = ''
  const results = await Promise.allSettled([
    projectStore.fetchProjects(),
    settingsStore.fetchSettings(),
    skillStore.fetchInstalled(),
    // 模型清单要给出「服务端声明的上下文窗口」，圆环的比例按它算
    catalogStore.fetchModels()
  ])
  const failures = results
    .filter((item): item is PromiseRejectedResult => 'rejected' === item.status)
    .map((item) => errorMessage(item.reason, '界面数据加载失败'))
  if (catalogStore.modelError) failures.push(catalogStore.modelError)
  if (failures.length) {
    pageError.value = failures[0]
    ElMessage.error(pageError.value)
  }
  try {
    if (conversationStore.currentId) await conversationStore.select(conversationStore.currentId)
  } catch (error) {
    pageError.value = errorMessage(error, '会话内容加载失败')
    ElMessage.error(pageError.value)
  } finally {
    pageLoading.value = false
  }
}

onMounted(() => {
  void loadPageData()
})

/** 记住原会话的输入：会话被删掉时 currentId 已清空，不再把草稿写回那个标识 */
function rememberDraft(id?: string): void {
  if (!id) return
  const exists =
    conversationStore.current?.id === id ||
    conversationStore.conversations.some((item) => item.id === id)
  if (exists) conversationStore.saveComposerDraft(id, draft.value, mentions.value)
}

watch(
  () => conversationStore.currentId,
  async (id, previousId) => {
    // 切走前先把原会话没发出的文字与 @ 引用收好，切回来时原样放回输入框
    rememberDraft(previousId)
    const restored = id
      ? conversationStore.composerDraftOf(id)
      : { text: '', mentions: [] as ChatMention[] }
    draft.value = restored.text
    mentions.value = restored.mentions
    if (!id) return
    try {
      await conversationStore.select(id)
      await nextTick()
      scrollBottom(true)
    } catch (error) {
      pageError.value = errorMessage(error, '会话内容加载失败')
      ElMessage.error(pageError.value)
    }
  },
  { immediate: true }
)

// 输入过程中就记到当前会话上：任何切换 / 离开页面的路径都能拿到最新一份，不只依赖切走那一刻
watch(
  [draft, mentions],
  () => {
    if (conversationStore.currentId) {
      conversationStore.saveComposerDraft(conversationStore.currentId, draft.value, mentions.value)
    }
  },
  { deep: true }
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

/*
 * 文件 / 文件夹拖进对话列：拖放区是中间那一列（消息区 + 输入区），不是只有输入框那一小块；
 * 右侧文件区不算——那里的整行自己还有「拖出去」的用法，别让两种拖放挤在一块。
 * 松手时把取到的本机路径交给输入框（ChatComposer 暴露的 addPaths），按光标位置写成 `@路径 `——
 * 与「@ → 选择本地文件 / 文件夹」完全同一条路，落在哪一格由输入框说了算。
 */

/** 拖进来的是文件（而不是选中的文字 / 链接）才接：其余拖动照旧交给文本区 */
function carriesFiles(event: DragEvent): boolean {
  const types = event.dataTransfer?.types
  return !!types && Array.from(types).includes('Files')
}

/** 生成中也可以继续输入 / 拖入，发送时自动进入该会话的队列 */
function acceptsDrop(): boolean {
  return true
}

function onDragEnter(event: DragEvent): void {
  if (!acceptsDrop() || !carriesFiles(event)) return
  event.preventDefault()
  dragging.value += 1
}

/** 拖到对话列上方：必须拦掉默认行为，否则松手时窗口会直接跳到 file:// 打开这个文件 */
function onDragOver(event: DragEvent): void {
  if (!acceptsDrop() || !carriesFiles(event)) return
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy'
}

/**
 * 拖出对话列：按进出次数递减；指针已经落在对话列之外时一次收干净，
 * 免得 enter / leave 偶尔配不上对、提示层挂在屏幕上不消失。
 */
function onDragLeave(event: DragEvent): void {
  if (!dragging.value) return
  const box = (event.currentTarget as HTMLElement | null)?.getBoundingClientRect()
  // 拿不到坐标（两轴都是 0）时按「还在里面」处理：宁可多减一次，也不误判成已经离开
  const known = 0 !== event.clientX || 0 !== event.clientY
  const inside =
    !known ||
    (!!box &&
      event.clientX >= box.left &&
      event.clientX <= box.right &&
      event.clientY >= box.top &&
      event.clientY <= box.bottom)
  dragging.value = inside ? Math.max(0, dragging.value - 1) : 0
}

/** 松手：把拖进来的文件 / 文件夹登记成 @ 引用 */
function onDrop(event: DragEvent): void {
  dragging.value = 0
  if (!carriesFiles(event)) return
  event.preventDefault()
  if (!acceptsDrop()) return
  const files = Array.from(event.dataTransfer?.files ?? [])
  // File 对象只在渲染进程里，路径得由 preload 用 webUtils 取（见 preload 的 filePaths）
  const paths = window.ipc.filePaths(files)
  if (!paths.length) return
  composerRef.value?.addPaths?.(paths)
}

/** 拖到窗口外松手时不会再有 leave：收个尾，别让提示层挂在屏幕上 */
function resetDragging(): void {
  dragging.value = 0
}

onMounted(() => window.addEventListener('dragend', resetDragging))

onBeforeUnmount(() => {
  window.removeEventListener('dragend', resetDragging)
  // 离开对话页也要留住正在输入的内容，回到这段会话时继续写
  rememberDraft(conversationStore.currentId)
})

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

/** 待发送消息的短标签：正文优先，只有 @ 引用时退回引用名 */
function queuedLabel(item: QueuedSend): string {
  const text = String(item.params.query ?? '').trim()
  const label =
    text ||
    (item.params.mentions ?? [])
      .map((mention) => mention.name)
      .filter(Boolean)
      .join(' ')
  const value = label || '待发送消息'
  return value.length > 36 ? `${value.slice(0, 36)}…` : value
}

function removeQueued(item: QueuedSend): void {
  if (conversation.value) conversationStore.dequeue(conversation.value.id, item.id)
}

function sameMentions(left: ChatMention[], right: ChatMention[]): boolean {
  return JSON.stringify(left) === JSON.stringify(right)
}

/**
 * 发送成功后只清掉「刚发出去的那一份」：
 * 发送期间用户又补了字、或已经切到别的会话时，输入框里新的内容必须原样留着；
 * 原会话在切走时保存的草稿也只在这份内容仍与发送内容一致时才清。
 */
function clearSentDraft(id: string, text: string, rows: ChatMention[]): void {
  const saved = conversationStore.composerDraftOf(id)
  if (saved.text === text && sameMentions(saved.mentions, rows)) {
    conversationStore.clearComposerDraft(id)
  }
  if (conversationStore.currentId !== id) return
  if (draft.value !== text || !sameMentions(mentions.value, rows)) return
  draft.value = ''
  mentions.value = []
}

async function send(): Promise<void> {
  if (!conversation.value || sending.value) return
  const profile = chatProfile(project.value)
  if (!profile.model) {
    ElMessage.warning('该会话所属的项目没有可用模型，请先在项目管理里配置')
    return
  }
  const sourceText = draft.value
  const text = sourceText.trim()
  if (!text && !mentions.value.length) return
  const conversationId = conversation.value.id
  const sourceMentions = mentions.value.slice()
  sending.value = true
  try {
    // 走会话 store：新会话的第一条消息会把会话带进历史，之后再交给 chat store 跑；
    // 生成中自动进队列，不打断当前轮次
    const result = await conversationStore.send({
      conversationId,
      ...profile,
      query: text,
      mentions: collectMentions(text)
    })
    clearSentDraft(conversationId, sourceText, sourceMentions)
    if (conversationStore.currentId === conversationId) {
      atBottom.value = true
      void nextTick(() => scrollBottom(true))
    }
    if ('queued' === result) ElMessage.success('已加入队列，将在当前轮次结束时一起发送')
  } finally {
    sending.value = false
  }
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
    const stopped = await conversationStore.clearMessages(conversation.value.id)
    ElMessage.success(stopped ? '已停止生成并清空当前对话' : '已清空当前对话')
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
  <div class="home-page" v-loading="pageLoading || conversationStore.opening">
    <el-alert
      v-if="pageError"
      class="page-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="pageError"
    >
      <el-button text type="primary" size="small" @click="loadPageData">重新加载</el-button>
    </el-alert>
    <!-- 没有会话：按项目开启新对话 -->
    <div v-if="!conversation" class="start-panel">
      <div class="start-inner">
        <div class="ui-page-header start-header">
          <div class="ui-page-header__copy">
            <h1 class="ui-page-title">选择项目开始对话</h1>
            <p class="ui-page-desc">
              会话按项目创建：模型、工具与工作区权限都取自所属项目，工作区外的访问按访问策略放行。
            </p>
          </div>
        </div>
        <div v-if="projectStore.projects.length === 0" class="start-empty ui-panel">
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
          <!--
            中间的对话列就是拖放区（见 onDragEnter / onDrop）：消息区与输入区都算，
            右侧文件区不算——那里的整行另有「拖出去」的用法
          -->
          <div
            class="conversation-column"
            @dragenter="onDragEnter"
            @dragover="onDragOver"
            @dragleave="onDragLeave"
            @drop="onDrop"
          >
            <!-- 消息区：滚动容器 + 电梯导航（导航在容器之外，不随消息一起滚） -->
            <div class="conversation-body">
              <el-scrollbar class="message-list" :ref="setScrollRef" @scroll="onScroll">
                <div class="message-column">
                  <div v-if="messages.length === 0" class="message-empty">
                    <span class="message-empty-mark" aria-hidden="true">
                      <svg viewBox="0 0 24 24">
                        <path
                          d="M12 3.5 13.7 9l5.5 1.7-5.5 1.7L12 18l-1.7-5.6L4.8 10.7 10.3 9 12 3.5Z"
                        />
                        <path
                          d="M18.8 16.4 19.5 18.5 21.6 19.2 19.5 19.9 18.8 22 18.1 19.9 16 19.2 18.1 18.5 18.8 16.4Z"
                        />
                      </svg>
                    </span>
                    <p class="message-empty-title">开始新对话</p>
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
                    :conversation-id="session?.conversationId ?? ''"
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
                <!-- 生成中提交的消息：排队可见、可撤掉；当前一轮收尾后按顺序自动发出 -->
                <div v-if="queuedMessages.length" class="composer-queue">
                  <span class="composer-queue-label">待发送</span>
                  <el-tag
                    v-for="item in queuedMessages"
                    :key="item.id"
                    class="composer-queue-item"
                    size="small"
                    effect="plain"
                    closable
                    :title="item.params.query"
                    @close="removeQueued(item)"
                  >
                    {{ queuedLabel(item) }}
                  </el-tag>
                </div>
                <ChatComposer
                  ref="composerRef"
                  v-model="draft"
                  v-model:mentions="mentions"
                  :candidates="candidates"
                  :commands="commands"
                  :policy="settingsStore.workspacePolicy"
                  :context="context"
                  :running="running"
                  @send="send"
                  @stop="stop"
                  @command="runCommand"
                  @update:context-limit="setContextLimit"
                  @update:policy="setPolicy"
                />
              </div>
            </footer>

            <!-- 拖入文件时的提示层：对话列描一圈虚线 + 一句话，松手即把文件 / 文件夹引用进输入框 -->
            <div v-if="dragging" class="conversation-drop" aria-hidden="true">
              <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
                <path
                  d="M8 10.2V2.6m0 0L5.1 5.5M8 2.6l2.9 2.9"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
                <path
                  d="M2.6 10.4v1.8a1.2 1.2 0 0 0 1.2 1.2h8.4a1.2 1.2 0 0 0 1.2-1.2v-1.8"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              <span>松开即可引用到输入框</span>
            </div>
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

.page-alert {
  margin: 12px 20px 0;
  flex: none;
}

/* ── 未选会话：选择项目 ─────────────────────────────── */

.start-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--ui-page-padding-top) var(--ui-page-padding-x) var(--ui-page-padding-bottom);
}

.start-inner {
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
}

.start-header {
  margin-bottom: 20px;
}

.start-empty {
  padding: 10px;
}

.project-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.project-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--ui-card-border);
  border-radius: 12px;
  background: var(--ui-surface);
  box-shadow: var(--ui-card-shadow);
  cursor: pointer;
  transition:
    border-color 0.2s,
    background-color 0.2s,
    box-shadow 0.2s;

  &:hover {
    border-color: color-mix(in srgb, var(--el-color-primary) 18%, var(--ui-card-border));
    background: var(--ui-surface-hover);
    box-shadow: var(--ui-card-shadow-hover);
  }

  :deep(.el-avatar) {
    flex-shrink: 0;
    color: var(--el-color-primary);
    font-size: 13px;
    font-weight: 600;
    background: var(--el-color-primary-light-9);
  }

  .project-text {
    flex: 1;
    min-width: 0;
  }

  .project-name {
    font-size: 14px;
    font-weight: 550;
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

/* 拖入文件时盖住对话列（消息区 + 输入区）的一层：描一圈虚线 + 一句提示，松手即在输入框里引用 */
.conversation-drop {
  position: absolute;
  /* 贴住对话列本身（不缩进）：虚线就是这一列的边界，右侧文件区不在其中 */
  inset: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 10px;
  border: dashed 1px var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 13px;
  /* 提示层只负责提示：不接管指针，投放在这一列的任何一处都算数 */
  pointer-events: none;
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
  /* 拖入文件的提示层铺在这一列上：定位基准 */
  position: relative;
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
  padding: 6px 16px;
  background: transparent;

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
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 72px 0 0;
  text-align: center;
  color: var(--el-text-color-placeholder);

  p {
    font-size: 13px;
    line-height: 1.8;
  }

  .message-empty-mark {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 46px;
    height: 46px;
    margin-bottom: 14px;
    color: var(--el-color-primary);
    background: color-mix(in srgb, var(--el-color-primary) 8%, var(--el-bg-color));
    border: 1px solid
      color-mix(in srgb, var(--el-color-primary) 14%, var(--el-border-color-lighter));
    border-radius: 14px;

    svg {
      width: 25px;
      height: 25px;
      fill: none;
      stroke: currentColor;
      stroke-width: 1.4;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  }

  .message-empty-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--ev-c-text-1);
  }

  .message-empty-tip {
    max-width: 520px;
    margin-top: 6px;
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

/* 排队区：与输入框同宽，一行放不下时横向滚动；每条都能单独撤掉 */
.composer-queue {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.composer-queue-label {
  flex: none;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.composer-queue-item {
  flex: none;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
