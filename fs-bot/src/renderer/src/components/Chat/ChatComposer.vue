<script setup lang="ts">
/**
 * 输入区 - 一个圆角输入框：上方是 @ 引用回显，中间是文本区，底部是访问策略与发送按钮。
 *
 * 四个入口：
 * - `@` 引用：本机文件 / 文件夹（选中即开系统对话框）与已安装技能（输入 @ 弹出候选，回车插入，
 *   发送时把引用内容补进上下文）；
 * - 拖入：文件 / 文件夹拖到中间对话列（消息区 + 输入区）的任意位置都算数（拖放区在页面上，
 *   见 views/home/HomePage.vue；页面把取到的路径交给这里的 addPaths，与系统对话框选进来同一条路）；
 * - 粘贴：从文件管理器复制来的文件 / 文件夹写成 @ 路径；剪贴板截图由主进程保存为临时文件后，
 *   同样写成 @ 路径，不经过 base64 或文件服务；
 * - `/` 执行内置指令（如基于当前项目新建对话），指令由页面处理。
 *
 * 回车发送、Shift + Enter 换行，中文输入法组词中的回车不发送。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PolicyIcon from './PolicyIcon.vue'
import ContextRing, { type ContextInfo } from './ContextRing.vue'
import type { ChatMention } from '@renderer/stores/chat'

export interface MentionCandidate {
  /** action 是本机选择这一类动作（不是候选），skill 是可直接引用的已安装技能 */
  type: 'skill' | 'action'
  name: string
  path: string
  /** 插入后 @ 后面那一段文本：技能是 skill:名称 */
  token: string
  /** action 专用：这种动作打开哪种系统对话框 */
  action?: 'file' | 'directory'
}

export interface ComposerCommand {
  name: string
  label: string
  description: string
}

const props = defineProps<{
  modelValue: string
  mentions: ChatMention[]
  candidates: MentionCandidate[]
  commands: ComposerCommand[]
  /** 工作区外的访问策略：在底部直接切换 */
  policy: string
  /** 上下文用量（圆环）：没有会话时为 null，不渲染圆环 */
  context?: ContextInfo | null
  disabled?: boolean
  running?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:mentions': [value: ChatMention[]]
  'update:policy': [value: string]
  send: []
  stop: []
  command: [name: string]
  /** 手工调整上下文窗口：null 表示恢复服务端声明的窗口 */
  'update:contextLimit': [value: number | null]
}>()

const inputRef = ref<any>(null)
/** 底衬层：只画引用的底色块，文字与光标仍由文本区负责 */
const mirrorRef = ref<HTMLElement | null>(null)
const menu = ref<{ kind: '' | 'mention' | 'command'; index: number; from: number; to: number }>({
  kind: '',
  index: 0,
  from: 0,
  to: 0
})
const query = ref('')
/** 输入区有没有可发送的内容：生成中只有非空时才多给一个发送按钮（停止按钮始终在） */
const hasDraft = computed(
  () => !!String(props.modelValue ?? '').trim() || props.mentions.length > 0
)
const placeholder = computed(() =>
  props.running
    ? '当前回复生成中，输入后按 Enter 会自动加入队列…'
    : '输入内容；可粘贴文件、文件夹或截图，@ 引用本机文件，/ 执行内置指令'
)

const POLICY_LABELS: Record<string, string> = {
  ask: '请求批准',
  auto: '帮我批准',
  deny: '自动拒绝',
  full: '完全访问权限'
}

/** 策略的固定顺序：下拉菜单按它排列 */
const POLICY_ORDER = ['ask', 'auto', 'deny', 'full']

/**
 * 本机选择这两条固定排在引用列表最前：它们不是候选，而是打开系统对话框的动作，
 * 与其它条目一起参与上下键选择与回车确认。系统对话框不能同时选文件与文件夹，所以拆成两条。
 */
const LOCAL_ACTIONS: MentionCandidate[] = [
  { type: 'action', name: '选择本地文件', path: '', token: '', action: 'file' },
  { type: 'action', name: '选择本地文件夹', path: '', token: '', action: 'directory' }
]

/** 每种策略在菜单里的补充说明：只讲松紧，不复述「工作区」这类上下文 */
const POLICY_HINTS: Record<string, string> = {
  ask: '逐个弹窗确认',
  auto: '直接放行',
  deny: '一律拒绝',
  full: '全部放行'
}

function textarea(): HTMLTextAreaElement | null {
  return (inputRef.value?.textarea as HTMLTextAreaElement) ?? null
}

/** 引用在输入框里的那一小段文本：技能是 skill:名称，本机选择的文件 / 文件夹是绝对路径 */
function tokenOf(mention: ChatMention): string {
  return mention.token ?? ('skill' === mention.type ? `skill:${mention.name}` : mention.path)
}

function escapeHtml(text: string): string {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 底衬内容：把已经登记的引用（@ 出来或 + 号选中的文件 / 技能）的 `@片段` 包成一个底色块。
 * 文字本身透明、宽度与文本区完全一致，所以底块正好落在对应的那段文字下面；
 * 文本区在上层照常显示文字、光标与选区，引用看上去就是输入框里的一个标签。
 */
const mirrorHtml = computed<string>(() => {
  const text = String(props.modelValue ?? '')
  if (!text) return ''
  const tokens = props.mentions
    .map((mention) => tokenOf(mention))
    .filter((token) => token && text.includes(`@${token}`))
    .sort((a, b) => b.length - a.length)
  if (!tokens.length) return escapeHtml(text)
  const pattern = new RegExp(`@(${tokens.map(escapeRegExp).join('|')})`, 'g')
  let html = ''
  let from = 0
  let match = pattern.exec(text)
  while (match) {
    html += escapeHtml(text.slice(from, match.index))
    html += `<span class="mention-chip">${escapeHtml(match[0])}</span>`
    from = match.index + match[0].length
    match = pattern.exec(text)
  }
  return html + escapeHtml(text.slice(from))
})

/** 候选按 token 匹配；最多给 20 条（技能一般不会这么多，兜个底） */
const matched = computed<MentionCandidate[]>(() => {
  const text = query.value.toLowerCase()
  const rows = text
    ? props.candidates.filter((item) => item.token.toLowerCase().includes(text))
    : props.candidates
  return rows.slice(0, 20)
})

/** 键盘上下移动用一个序列：本机选择那两条动作在前，随后是命中的技能，与菜单里的顺序一致 */
const ordered = computed<MentionCandidate[]>(() => [...LOCAL_ACTIONS, ...matched.value])
const orderedRows = computed(() => ordered.value.map((item, index) => ({ item, index })))
const actionRows = computed(() => orderedRows.value.filter((row) => 'action' === row.item.type))
const skillRows = computed(() => orderedRows.value.filter((row) => 'skill' === row.item.type))

/** 第一条真正的候选（跳过本机选择那两条动作）：开始打字后高亮跟着输入落到它上面 */
const firstCandidate = computed<number>(
  () => orderedRows.value.find((row) => 'action' !== row.item.type)?.index ?? 0
)

const matchedCommands = computed<ComposerCommand[]>(() => {
  const text = query.value.toLowerCase()
  // query 是 / 后面那一段，比较时要跳过指令名自带的斜杠，否则 /n 一个都匹配不上、菜单当场收起
  const rows = text
    ? props.commands.filter((item) => item.name.slice(1).toLowerCase().startsWith(text))
    : props.commands
  return rows.slice(0, 20)
})

/** 当前菜单的条目数：用于上下移动与回车选中（菜单收起时没有条目，回车就照常发送） */
const menuSize = computed(() => {
  if ('mention' === menu.value.kind) return menuOpen.value ? ordered.value.length : 0
  return 'command' === menu.value.kind ? matchedCommands.value.length : 0
})

/**
 * 菜单是否展示：引用菜单在没打关键词时始终展示（里面还有「选择本地文件」这类动作与技能清单），
 * 打了关键词却没有命中的技能就收起——那多半是在手写一段路径或正文，回车照常发送。
 * 指令菜单没有候选同样收起。
 */
const menuOpen = computed(() => {
  if ('mention' === menu.value.kind) return !query.value || 0 < skillRows.value.length
  return 'command' === menu.value.kind && 0 < matchedCommands.value.length
})

function setValue(value: string): void {
  emit('update:modelValue', value)
}

/**
 * 重算菜单：光标所在的那一段是不是 @ 引用或 / 指令。
 * 取值优先用原生输入框里的当前内容（输入事件里 props 还没同步过来），光标同理。
 */
function refreshMenu(input?: string): void {
  const element = textarea()
  // 输入事件会显式传当前文本；点击等原生事件的回调不能把 Event 当成文本传进来
  const value = typeof input === 'string' ? input : (element?.value ?? props.modelValue ?? '')
  const caret = element ? (element.selectionStart ?? value.length) : value.length
  const before = value.slice(0, caret)
  // / 指令：光标前最近的 / 之后还没有空白，且它本身在行首或空白之后（文件路径里的斜杠因此不会误触发）
  const slash = before.lastIndexOf('/')
  if (slash >= 0 && (0 === slash || /\s/.test(before[slash - 1]))) {
    const token = before.slice(slash + 1)
    if (!/\s/.test(token) && !props.disabled) {
      query.value = token
      // 高亮跟着输入走：始终停在第一条命中的指令上，回车即执行它
      menu.value = { kind: 'command', index: 0, from: slash, to: caret }
      return
    }
  }
  const at = before.lastIndexOf('@')
  // 只要光标前最近的 @ 之后还没有空白，就把那一段当成引用关键词（@ 可以紧跟正文，不必先敲空格）
  if (at >= 0) {
    const token = before.slice(at + 1)
    if (!/\s/.test(token)) {
      query.value = token
      // 已经开始打字就把高亮落到第一条命中的候选上；空白查询时停在最上面一条
      menu.value = { kind: 'mention', index: token ? firstCandidate.value : 0, from: at, to: caret }
      return
    }
  }
  query.value = ''
  menu.value = { kind: '', index: 0, from: 0, to: 0 }
}

function move(step: number): void {
  if (!menuSize.value) return
  menu.value.index = (menu.value.index + step + menuSize.value) % menuSize.value
}

/** 收起菜单：采纳候选后立刻收起，避免等下一次输入事件才消失 */
function closeMenu(): void {
  query.value = ''
  menu.value = { kind: '', index: 0, from: 0, to: 0 }
}

/**
 * 采纳当前候选：@ 引用把选中的路径写回输入框并登记引用，
 * / 指令把这一行交给页面执行（输入框随即清空，指令不进入对话内容）。
 */
function pick(): void {
  const kind = menu.value.kind
  const index = menu.value.index
  const range = { from: menu.value.from, to: menu.value.to }
  // 先把选中的那一条取出来再收起菜单：收起会把查询词清空，列表随之恢复成全量，
  // 那时再按 index 取就会取到另一条（之前 /po 回车会执行 /new 就是这个原因）
  const command = 'command' === kind ? matchedCommands.value[index] : null
  const item = 'mention' === kind ? ordered.value[index] : null
  closeMenu()
  if (command) {
    // 只移除输入框里的这一小段指令文本，前后内容保留（指令允许写在句子中间）
    const value = textarea()?.value ?? props.modelValue
    setValue(`${value.slice(0, range.from)}${value.slice(range.to)}`)
    emit('command', command.name)
    return
  }
  if (!item) return
  // 文件 / 文件夹这两条打系统对话框，不往输入框里插文本
  if ('action' === item.type) {
    void pickPaths(item.action ?? 'file', range)
    return
  }
  const reference = { type: 'skill' as const, name: item.name, path: item.path, token: item.token }
  const value = textarea()?.value ?? props.modelValue
  const next = `${value.slice(0, range.from)}@${item.token} ${value.slice(range.to)}`
  setValue(next)
  addMention(reference)
  nextTick(() => {
    const element = textarea()
    if (!element) return
    const caret = range.from + item.token.length + 2
    element.setSelectionRange(caret, caret)
    element.focus()
  })
}

/** 可以登记成引用的候选：本机选择那一条是动作，不进引用列表 */
function addMention(item: {
  type: 'file' | 'skill'
  name: string
  path: string
  token: string
}): void {
  const rows = props.mentions.filter((mention) => mention.path !== item.path)
  rows.push({ type: item.type, name: item.name, path: item.path, token: item.token })
  emit('update:mentions', rows)
}

/** 路径末段：引用本地文件 / 文件夹时用它做展示名 */
function baseName(path: string): string {
  const parts = String(path ?? '').split(/[\\/]/)
  return parts[parts.length - 1] || path
}

/**
 * 把一批本机路径接进输入框：登记成 @ 引用（同一路径不再重复登记），并把 `@路径 ` 写到插入点。
 * 拖入与「@ → 选择本地文件 / 文件夹」两条路都走这里，落地的结果一致——
 * 接进来时不限制路径、也不做权限判定，真正读取（内容或目录清单进上下文）时才按工作区权限与访问策略校验。
 * `range` 是唤起菜单时那段 `@关键词`：先摘掉它，否则输入框里会留下一个孤零零的 @；
 * 不给 range 就插在光标处（输入框没聚焦时按「追加到末尾」处理）。
 */
function addPaths(paths: string[], range?: { from: number; to: number }): void {
  const element = textarea()
  const raw = element?.value ?? props.modelValue
  const focused = !!element && document.activeElement === element
  let value = raw
  let at = focused ? (element?.selectionStart ?? raw.length) : raw.length
  if (range) {
    const start = Math.max(0, Math.min(range.from, value.length))
    const end = Math.max(start, Math.min(range.to, value.length))
    value = `${value.slice(0, start)}${value.slice(end)}`
    at = start
  }
  const rows = props.mentions.slice()
  const tokens: string[] = []
  for (const path of paths) {
    if (!rows.some((mention) => mention.path === path)) {
      rows.push({ type: 'file', name: baseName(path), path, token: path })
    }
    // 这一段路径已经在输入框里（手抄的、或先前引用留下的）就不再写一遍
    if (!value.includes(`@${path}`) && !tokens.includes(`@${path}`)) tokens.push(`@${path}`)
  }
  const head = value.slice(0, at)
  const tail = value.slice(at)
  let insert = ''
  if (tokens.length) {
    // 路径挨着正文会被读成一整段：前一个字符不是空白就补一个空格；
    // 末尾留一个空格接着往下写，后面本来就有空白时就不留（免得挤出一个双空格）
    let body = tokens.map((token) => `${token} `).join('')
    if (tail && /^\s/.test(tail)) body = body.replace(/\s+$/, '')
    insert = `${head && !/\s$/.test(head) ? ' ' : ''}${body}`
  }
  const next = `${head}${insert}${tail}`
  setValue(next)
  emit('update:mentions', rows)
  closeMenu()
  nextTick(() => {
    const target = textarea()
    if (!target) return
    const caret = at + insert.length
    target.setSelectionRange(caret, caret)
    target.focus()
  })
}

/**
 * 引用本地文件 / 文件夹：走系统对话框，两种入口各自只选一种（原生对话框没法混选）。
 * 选到的路径交给 addPaths 落地。
 */
async function pickPaths(
  kind: 'file' | 'directory',
  range?: { from: number; to: number }
): Promise<void> {
  if (props.disabled) return
  const paths = await window.ipc.workspace.pickPaths(kind)
  if (!paths?.length) return
  addPaths(paths, range)
}

/** 截图没有路径：由主进程从系统剪贴板保存到临时目录，再把临时路径接进输入框 */
async function addClipboardImage(): Promise<void> {
  try {
    const path = await window.ipc.workspace.saveClipboardImage()
    if (path) addPaths([path])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : String(error))
  }
}

/** 粘贴：文件 / 文件夹与截图最终都写成 @ 路径；文字粘贴不拦 */
function onPaste(event: ClipboardEvent): void {
  if (props.disabled) return
  const clipboard = event.clipboardData
  const files = Array.from(clipboard?.files ?? [])
  if (!files.length && clipboard?.items) {
    for (const item of Array.from(clipboard.items)) {
      if ('file' !== item.kind) continue
      const file = item.getAsFile()
      if (file) files.push(file)
    }
  }
  if (!files.length) return
  const paths: string[] = []
  let hasImage = false
  for (const file of files) {
    const path = window.ipc.filePaths([file])[0]
    if (path) paths.push(path)
    else if (file.type.startsWith('image/')) hasImage = true
  }
  if (!paths.length && !hasImage) return
  event.preventDefault()
  event.stopPropagation()
  closeMenu()
  if (paths.length) addPaths(paths)
  if (hasImage) void addClipboardImage()
}

/**
 * 左下角的 + 号：等同手打一个 @，就地弹出文件 / 技能候选。
 * 光标前一个字符不是空白时补一个空格，插入的 @ 才会被当成新的引用片段；
 * 输入框没聚焦时按「追加到末尾」处理。
 */
function insertMention(): void {
  if (props.disabled) return
  const element = textarea()
  const value = element?.value ?? props.modelValue
  const focused = !!element && document.activeElement === element
  const caret = focused ? (element?.selectionStart ?? value.length) : value.length
  const before = 0 < caret ? value.slice(caret - 1, caret) : ''
  const insert = before && !/\s/.test(before) ? ' @' : '@'
  const next = `${value.slice(0, caret)}${insert}${value.slice(caret)}`
  setValue(next)
  syncMentions(next)
  nextTick(() => {
    const target = textarea()
    if (!target) return
    const position = caret + insert.length
    target.setSelectionRange(position, position)
    target.focus()
    refreshMenu(next)
  })
}

/** 输入变化：菜单跟着光标走，引用随文本同步（删掉 @ 那一段就不再引用） */
function onInput(value: string): void {
  setValue(value)
  syncMentions(value)
  refreshMenu(value)
}

function syncMentions(value: string): void {
  const rows = props.mentions.filter((mention) => {
    return value.includes(`@${tokenOf(mention)}`)
  })
  if (rows.length !== props.mentions.length) emit('update:mentions', rows)
}

/** 鼠标点到某一项：先把高亮挪过去再采纳，避免点到与键盘高亮不一致的条目 */
function selectAt(index: number): void {
  menu.value.index = index
  pick()
}

function submit(): void {
  const text = String(props.modelValue ?? '').trim()
  if (!text && !props.mentions.length) return
  if (props.disabled) return
  // 整段就是一个已知指令（菜单已收起时手打回车）时按指令执行，不发成消息
  const exact = props.commands.find((item) => item.name === text)
  if (exact) {
    setValue('')
    emit('command', exact.name)
    return
  }
  emit('send')
}

function onKeydown(payload: Event | KeyboardEvent): void {
  const event = payload as KeyboardEvent
  if (event.isComposing || 229 === event.keyCode) return
  // Ctrl / Cmd + Enter：文本区不会自带换行，手动在光标处插入（菜单开着也照样换行）
  if ('Enter' === event.key && (event.ctrlKey || event.metaKey)) {
    event.preventDefault()
    insertNewline()
    return
  }
  if (menuSize.value) {
    if ('ArrowDown' === event.key) {
      move(1)
      event.preventDefault()
      return
    }
    if ('ArrowUp' === event.key) {
      move(-1)
      event.preventDefault()
      return
    }
    if ('Escape' === event.key) {
      closeMenu()
      event.preventDefault()
      return
    }
    if ('Tab' === event.key || ('Enter' === event.key && !event.shiftKey)) {
      pick()
      event.preventDefault()
      return
    }
  }
  if ('Enter' === event.key && !event.shiftKey) {
    // 引用菜单开着但没有候选时不发送：先收起菜单，避免把半截 @ 关键词发出去
    if (menuOpen.value) {
      event.preventDefault()
      closeMenu()
      return
    }
    event.preventDefault()
    submit()
  }
}

/** 在光标处插入换行：Shift + Enter 走浏览器默认行为，Ctrl / Cmd + Enter 由这里补上 */
function insertNewline(): void {
  const element = textarea()
  const value = element?.value ?? props.modelValue
  const start = element?.selectionStart ?? value.length
  const end = element?.selectionEnd ?? start
  const next = `${value.slice(0, start)}\n${value.slice(end)}`
  setValue(next)
  syncMentions(next)
  nextTick(() => {
    const target = textarea()
    if (!target) return
    target.setSelectionRange(start + 1, start + 1)
    target.focus()
    refreshMenu(next)
  })
}

function focus(): void {
  textarea()?.focus()
}

/** 文本区内部滚动（内容超过 8 行）时，底衬跟着走，标签不会错位 */
function syncScroll(): void {
  const element = textarea()
  const mirror = mirrorRef.value
  if (!element || !mirror) return
  mirror.scrollTop = element.scrollTop
  mirror.scrollLeft = element.scrollLeft
}

onMounted(() => {
  textarea()?.addEventListener('scroll', syncScroll)
})

onBeforeUnmount(() => {
  textarea()?.removeEventListener('scroll', syncScroll)
})

/** 页面上的拖放区用得到 addPaths：把拖进来的路径写成 @ 引用，与系统对话框选进来同一条路 */
defineExpose({ focus, addPaths })
</script>

<template>
  <div class="chat-composer">
    <!-- 候选项浮层：@ 引用与 / 指令共用一处，浮在输入框上方 -->
    <el-scrollbar class="composer-menu" max-height="480px" v-if="menuOpen">
      <div class="menu-head">
        <span class="menu-title">{{
          'command' === menu.kind ? '内置指令' : '引用本机文件与技能'
        }}</span>
        <span class="menu-hint">↑ ↓ 选择 · Enter 确认 · Esc 取消</span>
      </div>

      <!-- / 指令 -->
      <ul class="menu-list" v-if="'command' === menu.kind">
        <li
          class="menu-item"
          :class="{ active: index === menu.index }"
          v-for="(item, index) in matchedCommands"
          :key="item.name"
          @mouseenter="menu.index = index"
          @mousedown.prevent="selectAt(index)"
        >
          <span class="menu-name">{{ item.name }}</span>
          <span class="menu-desc">{{ item.label }} — {{ item.description }}</span>
        </li>
      </ul>

      <!-- @ 引用：本机选择在最前，随后是已安装技能 -->
      <template v-else>
        <ul class="menu-list menu-actions">
          <!-- 本机选择：文件与文件夹各一条（系统对话框无法混选），选中即打开对话框 -->
          <li
            class="menu-item is-action"
            :class="{ active: row.index === menu.index }"
            v-for="row in actionRows"
            :key="row.item.action"
            @mouseenter="menu.index = row.index"
            @mousedown.prevent="selectAt(row.index)"
          >
            <svg class="menu-icon" viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
              <path
                v-if="'directory' === row.item.action"
                d="M2.6 4.4a1.2 1.2 0 0 1 1.2-1.2h2.3l1.2 1.4h5.1a1.2 1.2 0 0 1 1.2 1.2v5.8a1.2 1.2 0 0 1-1.2 1.2H3.8a1.2 1.2 0 0 1-1.2-1.2z"
                fill="none"
                stroke="currentColor"
                stroke-width="1.2"
                stroke-linejoin="round"
              />
              <path
                v-else
                d="M10.9 5.6 6.1 10.4a1.9 1.9 0 0 1-2.7-2.7l5-5a3.1 3.1 0 0 1 4.4 4.4l-5 5a1.5 1.5 0 0 1-2.1-2.1l4.4-4.4"
                fill="none"
                stroke="currentColor"
                stroke-width="1.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <span class="menu-name">{{ row.item.name }}</span>
          </li>
        </ul>
        <!-- 已安装技能：同一套标题行 + 单行列表 -->
        <div class="menu-group" v-if="skillRows.length">
          <div class="menu-group-head">
            <span class="menu-group-title">已安装技能</span>
          </div>
          <ul class="menu-list">
            <li
              class="menu-item"
              :class="{ active: row.index === menu.index }"
              v-for="row in skillRows"
              :key="row.item.path"
              @mouseenter="menu.index = row.index"
              @mousedown.prevent="selectAt(row.index)"
            >
              <span class="menu-name">@{{ row.item.token }}</span>
              <span class="menu-desc">{{ row.item.name }}</span>
            </li>
          </ul>
        </div>
        <div class="menu-empty" v-if="!skillRows.length">还没有安装技能</div>
      </template>
    </el-scrollbar>

    <div class="composer-box" @paste="onPaste">
      <!--
        引用按标签就地显示：底衬层给已经登记的 @片段 铺一块底色，
        文本区照常负责文字、光标与选区，删掉 @ 那一段标签也就跟着消失
      -->
      <div class="composer-field">
        <div class="composer-mirror" ref="mirrorRef" aria-hidden="true" v-html="mirrorHtml" />
        <el-input
          ref="inputRef"
          class="composer-input"
          :model-value="modelValue"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 8 }"
          resize="none"
          :disabled="disabled"
          :placeholder="placeholder"
          @update:model-value="onInput"
          @keydown="onKeydown"
          @click="refreshMenu()"
        />
      </div>
      <div class="composer-tools">
        <div class="composer-tools-main">
          <!-- 引用入口：与手打 @ 同一套候选，点一下就地弹出文件 / 技能清单 -->
          <el-tooltip content="引用本机文件或技能" placement="top">
            <button
              type="button"
              class="composer-plus"
              title="引用本机文件或技能"
              :disabled="disabled"
              @mousedown.prevent="insertMention"
            >
              <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
                <path
                  d="M8 3.2v9.6M3.2 8h9.6"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                />
              </svg>
            </button>
          </el-tooltip>
          <!-- 访问策略：图标 + 当前口径，点开直接换 -->
          <el-dropdown
            trigger="click"
            popper-class="composer-policy-dropdown"
            @command="(name: string) => emit('update:policy', name)"
          >
            <span
              class="composer-policy"
              :title="`本地访问策略：${POLICY_LABELS[policy] ?? policy}`"
            >
              <PolicyIcon :mode="policy" />
              <span class="composer-policy-name">{{ POLICY_LABELS[policy] ?? policy }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in POLICY_ORDER"
                  :key="item"
                  :command="item"
                  :disabled="item === policy"
                >
                  <span class="policy-option">
                    <PolicyIcon :mode="item" />
                    <span>{{ POLICY_LABELS[item] }}</span>
                    <span class="policy-option-hint">{{ POLICY_HINTS[item] }}</span>
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="composer-tools-end">
          <!-- 上下文用量圆环：贴着发送按钮左边，点开可手工指定窗口大小 -->
          <ContextRing
            v-if="context"
            :context="context"
            @update:limit="(value: number | null) => emit('update:contextLimit', value)"
          />
          <!-- 生成中按输入状态只显示一个主按钮：空输入是停止，有输入是灰色发送（进队列） -->
          <el-button
            class="composer-stop"
            v-if="running && !hasDraft"
            circle
            type="danger"
            plain
            title="停止生成"
            @click="emit('stop')"
          >
            <!-- 方形停字块：与发送的纸飞机保持相近的视觉重量 -->
            <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
              <rect x="3.6" y="3.6" width="8.8" height="8.8" rx="2" fill="currentColor" />
            </svg>
          </el-button>
          <el-button
            v-else
            class="composer-send"
            circle
            :type="running ? 'info' : 'primary'"
            :title="running ? '发送并加入队列' : '发送（Enter 发送，Shift / Ctrl + Enter 换行）'"
            :disabled="disabled || !hasDraft"
            @click="submit"
          >
            <!-- 发送图标与编排对话页一致（Element Plus 的 Promotion） -->
            <svg viewBox="0 0 1024 1024" width="15" height="15" aria-hidden="true">
              <path
                fill="currentColor"
                d="m64 448 832-320-128 704-446.08-243.328L832 192 242.816 545.472zm256 512V657.024L512 768z"
              />
            </svg>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-composer {
  position: relative;
  width: 100%;
}

/* 候选菜单：贴着输入框上沿浮起，不改变页面排版 */
.composer-menu {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(100% + 6px);
  z-index: 20;
  /**
   * 宽度与输入框一致，高度按内容长：el-scrollbar 默认 height: 100%，
   * 绝对定位时会去对齐输入框那一行的高（只有几十像素），列表再长也只露出一两条。
   */
  height: auto;
  padding: 4px 4px 6px;
  border-radius: 12px;
  border: solid 1px var(--el-border-color-light);
  background: var(--el-bg-color);
  box-shadow: 0 10px 30px rgb(0 0 0 / 12%);

  /* 顶部说明与快捷键提示：一行两端 */
  .menu-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 2px 8px 6px;

    .menu-title {
      font-size: 12px;
      font-weight: 500;
      color: var(--el-text-color-regular);
    }

    .menu-hint {
      flex: none;
      font-size: 11px;
      color: var(--el-text-color-placeholder);
    }
  }

  /* 分组：已安装技能一段 */
  .menu-group + .menu-group {
    margin-top: 2px;
  }

  /* 分组标题行：标题在左，「添加」这类动作在右，一行放下 */
  .menu-group-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 6px 8px 2px;
  }

  .menu-group-title {
    font-size: 11px;
    line-height: 1.4;
    color: var(--el-text-color-placeholder);
  }

  /* 本机选择这两条：图标 + 名称 + 右侧说明，与候选同构，整段用细线和工作区候选分开 */
  .menu-actions {
    margin: 0 0 2px;
    padding-bottom: 6px;
    border-bottom: solid 1px var(--el-border-color-lighter);
  }

  .menu-item.is-action {
    .menu-icon {
      flex: none;
      color: var(--el-text-color-secondary);
    }

    /* 动作行是中文短语，不用候选那样的等宽字体 */
    .menu-name {
      font-family: inherit;
      font-size: 13px;
    }

    &.active {
      .menu-icon {
        color: var(--el-color-primary);
      }
    }
  }

  /* 去掉 ul 自带的左缩进：菜单左侧那一条空白就是它 */
  .menu-list {
    margin: 0;
    padding: 0;
  }

  .menu-item {
    display: flex;
    align-items: center;
    gap: 10px;
    min-height: 26px;
    padding: 4px 8px;
    border-radius: 6px;
    cursor: pointer;
    transition: background-color 0.15s;

    &.active {
      background: var(--el-color-primary-light-9);

      .menu-name {
        color: var(--el-color-primary);
      }
    }
  }

  .menu-name {
    flex: 0 1 auto;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 12px;
    line-height: 18px;
    color: var(--el-text-color-primary);
  }

  .menu-desc {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    text-align: right;
    font-size: 12px;
    line-height: 18px;
    color: var(--el-text-color-placeholder);
  }

  .menu-empty {
    padding: 14px 8px;
    text-align: center;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}

.composer-box {
  /* 整块输入框（文本区 + 工具行）至少 60px：内容少时也不至于挤成一条 */
  display: flex;
  flex-direction: column;
  min-height: 100px;
  padding: 8px 12px;
  border-radius: 10px;
  border: solid 1px var(--el-border-color);
  background: var(--el-bg-color);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;

  &:hover {
    border-color: var(--el-border-color-darker);
  }

  &:focus-within {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 3px var(--el-color-primary-light-9);
  }
}

/* 输入区：底衬与文本区叠在同一块位置上，字体与换行规则必须完全一致 */
.composer-field {
  position: relative;
  /* 文本区吃掉多出来的高度，工具行始终贴着底部 */
  flex: 1 1 auto;
}

.composer-mirror {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
  /* 文字只用来占位（宽度与文本区逐字对齐），可见的只有标签底色 */
  color: transparent;
  /* 与文本区同样的内边距：标签底色的外溢（左右 2px、上下 2px）落在这圈内边距里，
     否则行首 / 行顶那一小块会被 overflow: hidden 裁掉，看着像描边被盖住了 */
  padding: 2px;
  /* 排版属性与文本区逐项对齐：任何一项不同，两层的字宽就会有差，底色就对不上文字 */
  font-family: inherit;
  font-size: 13px;
  font-weight: 400;
  font-style: normal;
  font-variant: normal;
  letter-spacing: normal;
  word-spacing: normal;
  line-height: 1.7;
  text-indent: 0;
  text-rendering: auto;
  tab-size: 4;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
  user-select: none;

  /**
   * 标签底色：四周各 2px 内边距。
   * 左右那 2px 用负外边距抵掉 → 占位宽度与纯文本一字不差，折行位置不会跑偏；
   * 纵向那 2px 不参与行内排版（纵向内边距不影响行高），只把底色撑到能盖住字形，
   * 又不会像 3px 那样在行顶留出过大的空白。
   */
  :deep(.mention-chip) {
    margin: 0 -2px;
    padding: 2px 2px;
    border-radius: 4px;
    background: var(--el-color-primary-light-9);
    box-shadow: inset 0 0 0 1px var(--el-color-primary-light-7);
  }
}

.composer-input {
  /* 文本区叠在底衬之上：文字、光标与选区都按原样工作 */
  position: relative;
  z-index: 1;
  width: 100%;

  :deep(.el-textarea__inner) {
    /* 与底衬层一致的内边距：两层同偏移，文字与标签底色才对得上 */
    padding: 2px;
    border: none;
    box-shadow: none;
    background: transparent;
    resize: none;
    /* 与底衬层逐项一致（含显式继承字体：textarea 的浏览器默认字体是等宽） */
    font-family: inherit;
    font-size: 13px;
    font-weight: 400;
    font-style: normal;
    font-variant: normal;
    letter-spacing: normal;
    word-spacing: normal;
    line-height: 1.7;
    text-indent: 0;
    text-rendering: auto;
    tab-size: 4;
    white-space: pre-wrap;
    word-break: break-word;
    overflow-wrap: break-word;
  }
}

.composer-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 4px;

  &:first-child {
    margin-top: 0;
  }
}

.composer-tools-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

/* 引用入口：只有一个加号，不描边不铺底，悬停转主色 */
.composer-plus {
  flex: none;
  width: 18px;
  height: 18px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s;

  &:hover:not(:disabled) {
    color: var(--el-color-primary);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }
}

/* 策略入口：图标 + 当前口径，颜色即当前松紧 */
.composer-policy {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: none;
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition:
    color 0.2s,
    background-color 0.2s;

  &:hover {
    color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }
}

.composer-policy-name {
  white-space: nowrap;
}

/* 下拉里的每一项：图标 + 名称 + 一句松紧说明 */
.policy-option {
  display: flex;
  align-items: center;
  gap: 6px;

  .policy-option-hint {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}

.composer-tools-end {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;

  /* 发送 / 停止按钮里的图标按块级居中，不受按钮默认行高影响 */
  svg {
    display: block;
  }
}
</style>

<!-- 策略下拉被 teleport 到 body，脱离组件根节点，只能用非 scoped 样式命中 -->
<style lang="scss">
.composer-policy-dropdown.el-popper {
  padding: 4px;
  border-radius: 10px;
  overflow: hidden;
}

.composer-policy-dropdown .el-dropdown-menu {
  padding: 0;
}

.composer-policy-dropdown .el-dropdown-menu__item {
  border-radius: 6px;
}
</style>
