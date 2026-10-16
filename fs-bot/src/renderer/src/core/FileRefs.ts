import type { ChatToolCall } from '@renderer/stores/chat'

/**
 * 工具调用里「涉及的文件」：参数与返回中凡是能认出是本机路径（或服务端给出的下载地址）的都挑出来，
 * 交给界面渲染成可打开的文件卡片（见 components/Chat/ToolCallList.vue）。
 *
 * 只做提取、不做校验：路径是否存在、能不能打开由主进程在点击那一刻判定（见 WorkspaceManager.open），
 * 所以规则可以宽松些——多挑出一个点不开的，比漏掉一个交付物更可惜。
 * 参数与返回在流式期间是没写完的 JSON，解析不出来就不给卡片，调用跑完自然会出现。
 */

/** 能点开的一项：本机文件 / 目录，或服务端给出的下载地址 */
export interface ChatFileRef {
  /** 展示名：路径末段 */
  name: string
  /** 本机路径（绝对路径或工作区相对路径），交付地址那一类为空 */
  path: string
  /** 服务端交付文件的下载地址，本机文件为空 */
  url: string
  dir: boolean
}

/** 一次调用最多给几张卡片：命令输出里可能满屏路径，够用就行 */
const MAX_REFS = 8

/**
 * 本机绝对路径：Windows 盘符（`D:\…`）、UNC 共享（`\\server\share\…`）、
 * POSIX 绝对路径（`/Users/x/a.md`，要求末段带后缀，免得把正文里一串斜杠当路径）。
 * 按常见中英文标点与换行收住：结果文案多是「已写入 D:\x\a.md（123 字节）」这种写法。
 * 空白不当作结束（路径里带空格很常见），随后由 trimProse 把尾巴上的说明文字去掉。
 * 前面不许再跟字母数字：否则 `https://…` 里的 `s://` 会被当成「盘符 + 分隔符」认下来。
 */
const WINDOWS_PATH =
  /(?<![A-Za-z0-9])(?:[A-Za-z]:[\\/]|\\\\)[^"'`<>|?*（）()【】[\]，,；;：:\r\n]+/g
const POSIX_PATH = /(?:^|[\s"'`（(【[，,；;：:])(\/(?:[\w.@+-]+\/)+[\w.@+-]+\.[A-Za-z0-9]+)/g

/** 参数里可能放文件路径的键：名字太泛的（name）不算，免得把技能名、任务名当路径 */
const PATH_ARG_KEYS = /^(path|file|filepath|file_path|file_name|filename|target|output|to)$/i

/** 返回里可能放交付文件地址的键：只认说得明白是下载的那几个 */
const URL_KEYS = /^(download|downloadurl|download_url|downloadlink|fileurl|file_url)$/i

/**
 * 文件工具按 action 取这次真正动过的那一个路径：
 * 读 / 写 / 上传是 path，移动取目的地（原路径已经不在），建目录与列目录给目录卡片；
 * 删除过的、只是检索命中的都不给——点开一个已经不在或没动过的文件只会添乱。
 *
 * `delivered` 标记这一项算不算「交付物」：写入的文件与移动到的位置算，读取 / 列目录 / 上传 / 删除
 * 都只是过程（右侧的「交付的文件」按这个口径列，见 deliveredRefsOf）。
 */
const FILE_OP_PATHS: Record<string, { keys: string[]; dir: boolean; delivered: boolean }> = {
  read: { keys: ['path'], dir: false, delivered: false },
  write: { keys: ['path'], dir: false, delivered: true },
  upload: { keys: ['path'], dir: false, delivered: false },
  move: { keys: ['to'], dir: false, delivered: true },
  mkdir: { keys: ['path'], dir: true, delivered: false },
  list: { keys: ['path'], dir: true, delivered: false }
}

/** 参数与返回都是文本：能解析成对象 / 数组就用解析结果，否则按纯文本处理 */
function parseJson(text: unknown): unknown {
  const value = String(text ?? '').trim()
  if (!value || !/^[[{]/.test(value)) return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

/** 路径末段：卡片上显示的名字 */
function nameOf(path: string): string {
  const parts = String(path ?? '')
    .split(/[\\/]/)
    .filter((item) => !!item)
  return parts[parts.length - 1] ?? String(path ?? '')
}

/** 像本机路径的字符串：带分隔符才认，URL 与单独一个文件名不算（单独的名字没法定位） */
function pathLike(value: unknown): string {
  const text = String(value ?? '').trim()
  if (!text || /^[a-z][a-z0-9+.-]*:\/\//i.test(text)) return ''
  if (/^[a-z]:[\\/]/i.test(text) || text.startsWith('\\\\')) return text
  return /[\\/]/.test(text) ? text : ''
}

/**
 * 文案里捡出来的路径可能连着说明文字（「D:\x\dist 用时 3s」）：按空白切词，
 * 从尾部丢掉既没有分隔符也没有后缀的那些词，留下真正的路径部分。
 */
function trimProse(value: string): string {
  const parts = String(value ?? '')
    .trim()
    .split(/\s+/)
  while (parts.length > 1) {
    const last = parts[parts.length - 1]
    if (/[\\/]/.test(last) || last.includes('.')) break
    parts.pop()
  }
  return parts.join(' ')
}

/** 按键名在 JSON 里递归找字符串值（数组也走一遍），最多下探四层 */
function collect(value: unknown, keys: RegExp, hit: (text: string) => void, depth = 0): void {
  if (depth > 4 || !value) return
  if (Array.isArray(value)) {
    value.forEach((item) => collect(item, keys, hit, depth + 1))
    return
  }
  if ('object' !== typeof value) return
  Object.entries(value as Record<string, unknown>).forEach(([key, item]) => {
    if ('string' === typeof item) {
      if (keys.test(key)) hit(item)
      return
    }
    collect(item, keys, hit, depth + 1)
  })
}

/**
 * 一次调用涉及的文件：本地文件工具按 action 取参数里的路径；
 * 其余工具（服务端工具、命令）参数按键名找，返回是 JSON 就按键名找、
 * 是文本就从文案里捡绝对路径（服务端工具与命令输出大多是文本）。
 *
 * `deliveredOnly` 只留交付物：文件工具只认写入与移动，其它工具只认返回里带出来的路径与下载地址
 * （参数多半是「要处理什么」，不算交付）；做交付清单时用这一档。
 */
function collectRefs(call: ChatToolCall, deliveredOnly: boolean): ChatFileRef[] {
  const refs: ChatFileRef[] = []
  const seen = new Set<string>()
  const push = (path: unknown, dir = false): void => {
    const value = String(path ?? '').trim()
    if (!value || seen.has(value) || refs.length >= MAX_REFS) return
    seen.add(value)
    refs.push({ name: nameOf(value), path: value, url: '', dir })
  }
  const pushUrl = (url: unknown): void => {
    const value = String(url ?? '').trim()
    if (!/^https?:\/\//i.test(value) || seen.has(value) || refs.length >= MAX_REFS) return
    seen.add(value)
    // 名字取地址末尾那一段（去掉查询串）：末尾没有段名（以 / 收尾）就退回主机名
    const clean = value.split(/[?#]/)[0]
    const segment = nameOf(clean)
    let name = /[\\/]$/.test(clean) ? '' : segment
    if (!name || name === clean) {
      try {
        name = new URL(value).host
      } catch {
        name = '交付文件'
      }
    }
    refs.push({ name, path: '', url: value, dir: false })
  }

  const name = String(call?.name ?? '')
  const args = parseJson(call?.args)
  const outer = String(call?.result ?? '')
  const isFileOp = 'file_op' === name
  if (isFileOp) {
    const source: Record<string, unknown> = 'object' === typeof args && args ? { ...args } : {}
    const rule = FILE_OP_PATHS[String(source.action ?? '')]
    if (rule && (!deliveredOnly || rule.delivered)) {
      rule.keys.forEach((key) => push(pathLike(source[key]), rule.dir))
    }
  } else if (!deliveredOnly) {
    collect(args, PATH_ARG_KEYS, (text) => push(pathLike(text)))
  }

  const result = parseJson(call?.result)
  if (result) {
    collect(result, PATH_ARG_KEYS, (text) => push(pathLike(text)))
    collect(result, URL_KEYS, pushUrl)
  } else if (!isFileOp) {
    // 返回不是 JSON：从文案里捡本机绝对路径（文件工具的返回不含路径，跳过免得与参数重复）
    for (const matched of outer.matchAll(WINDOWS_PATH)) push(trimProse(matched[0]))
    if (refs.length < MAX_REFS) {
      for (const matched of outer.matchAll(POSIX_PATH)) push(matched[1])
    }
  }
  return refs
}

/** 一次调用涉及的文件（含读取、列目录这类过程）：工具调用卡片按这份口径列 */
export function fileRefsOf(call: ChatToolCall): ChatFileRef[] {
  return collectRefs(call, false)
}

/**
 * 这一次调用**交付**出来的东西：写入的文件、移动到的位置、服务端给出的下载地址。
 * 对话右侧的「交付的文件」按它汇总（见 components/Chat/ConversationFiles.vue）。
 */
export function deliveredRefsOf(call: ChatToolCall): ChatFileRef[] {
  return collectRefs(call, true)
}
