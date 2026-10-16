import { spawn, type ChildProcess } from 'child_process'
import {
  closeSync,
  existsSync,
  mkdirSync,
  openSync,
  readSync,
  readdirSync,
  readFileSync,
  renameSync,
  rmSync,
  statSync,
  writeFileSync
} from 'fs'
import { randomUUID } from 'crypto'
import { release as osRelease } from 'os'
import { basename, dirname, extname, isAbsolute, join, resolve, sep } from 'path'
import { shell } from 'electron'
import { mergeProjectEnv, redactSecretValues, type ProjectEnvVar } from './project-env'

/**
 * 项目工作区文件访问：每个项目声明自己的目录与权限（只读 / 只写 / 读写），
 * 所有文件操作都在主进程里按「这次操作所属项目的那份声明」鉴权。
 *
 * 会话按项目创建，所以每一次文件操作都属于某个项目：调用方只说「哪个项目」，
 * 清单本身由主进程从用户库读（见 ProjectRootsResolver 的注册方），
 * 项目之间互不干扰，多个项目可以同时跑各自的会话。
 * 渲染进程拿不到文件系统句柄，只能走这里。
 *
 * 工作区**之外**的路径按访问策略处理（`workspacePolicy`，在系统设置里切换）：
 * 请求批准逐个询问、帮我批准直接放行、自动拒绝直接失败、完全访问权限等同本项目自己的目录。
 * 策略的判定与询问都在主进程完成，渲染进程改不了结论。
 *
 * 清单里可以标一个**主目录**（`primary`）：命令省略 `cwd` 时在它里面跑，相对路径优先按它解析。
 * 没标的（旧配置、分享来的配置）一律按第一个目录，行为与从前一致。
 *
 * 本地命令（`exec`）共用这套访问策略，但**不共用「落在授权目录内就放行」这一条**：
 * 命令的作用范围不限于工作目录，工作区内运行同样按策略确认（原因见 `exec` 的说明），
 * 推给界面的申请里带上命令原文——让人看着是哪条命令再决定。
 *
 * 命令的进程环境由**项目环境变量**补齐（项目编辑器里的那一格，见 `project-env.ts`）：
 * 同一个 `java -version` 在不同项目里落到不同 JDK 上靠的就是它。这些值只进本机命令的进程环境，
 * 不下发给模型（本机说明里只有变量名），也不随分享上传；标了「密钥」的值在命令输出里换成 `***`，
 * 免得一条命令把密钥原样带进对话。
 *
 * `open` / `reveal` 是**用户自己点出来**的动作（把对话里的文件交给系统默认程序或文件管理器），
 * 不是模型要读写它，因此不走工作区外的访问策略：路径只做归一与存在性判断，
 * 否则用户点开自己刚 @ 引用的文件还要先等一次审批。可执行 / 脚本类文件另有默认拒绝（见 `open`）。
 */

export type WorkspacePermission = 'read' | 'write' | 'readwrite'

/** 一次本地访问想做什么：读文件 / 写文件 / 执行命令（命令与写文件同级，都要求写权限） */
export type WorkspaceNeed = 'read' | 'write' | 'command'

/** 工作区外文件的访问策略：请求批准 / 帮我批准 / 自动拒绝 / 完全访问权限 */
export type WorkspacePolicy = 'ask' | 'auto' | 'deny' | 'full'

export interface WorkspaceRoot {
  path: string
  permission: WorkspacePermission
  /** 主目录：命令默认在它里面执行、相对路径优先按它解析；一份清单里至多一个 */
  primary?: boolean
}

/** 一次访问的来处：命令原文（审批要看）与发起它的会话（申请归属哪段对话） */
export interface WorkspaceContext {
  /** 执行命令时的命令原文 */
  command?: string
  /** 发起这次访问的会话标识：审批申请只交给这段对话处理，各对话互不干扰 */
  conversationId?: string
}

/** 工作区外的访问申请：主进程判定不了时交给界面问一次 */
export interface WorkspaceApproval {
  id: string
  projectId: string
  path: string
  /** 这次想做什么：读取 / 写入 / 执行命令 */
  need: WorkspaceNeed
  /** 执行命令时带上的命令原文：越界执行时让人看着命令再决定 */
  command?: string
  /** 发起这次访问的会话：界面据此把申请交回那段对话（缺省表示来处不明） */
  conversationId?: string
}

/** 本地命令的执行结果：退出码为 null 表示被信号终止或没能启动 */
export interface WorkspaceCommandResult {
  command: string
  cwd: string
  code: number | null
  signal: string
  timedOut: boolean
  /** 用户按了停止：进程树已被收掉，这不是命令自己的结束（与超时区分开） */
  cancelled: boolean
  /** 耗时（毫秒） */
  duration: number
  stdout: string
  stderr: string
}

/**
 * 正在跑的一条命令：停止生成时按会话把它收掉（见 WorkspaceManager.cancel）。
 * 注册与摘除都在 runCommand 里成对完成，跑完（正常、超时、停止）都会自己摘掉。
 */
interface RunningCommand {
  /** 发起这条命令的会话：停止按会话收，各对话互不干扰 */
  conversationId: string
  /** 收掉这条命令：杀掉进程树、按「已停止」立刻收尾 */
  stop: () => void
}

/** 界面给出的答复：允许本次放行，或顺带把策略改成「帮我批准」 */
export interface WorkspaceApprovalResult {
  id: string
  allow: boolean
  /** 勾选「始终允许」时同步把策略切到「帮我批准」 */
  always?: boolean
}

/** 工作区文件清单项 */
export interface WorkspaceFile {
  name: string
  path: string
  /** 命中的工作区目录：界面据此展示相对路径 */
  root: string
  ext: string
  dir: boolean
}

export interface WorkspaceMatch {
  path: string
  /** 命中的起始行（从 1 开始） */
  line: number
  /** 命中的结束行：单行命中与 line 相同（展示成 `12-15`） */
  endLine: number
  text: string
}

/** 文本替换的范围：行号 1 起、闭区间（都省略就是整个文件）；all 表示范围内的全部命中都替换 */
export interface WorkspaceReplaceScope {
  startLine?: number
  endLine?: number
  all?: boolean
}

/** 一次文本替换的回执：替换了什么、怎么匹配上的、文件的行尾与编码有没有被动过 */
export interface WorkspaceReplaceResult {
  path: string
  /** 实际替换的处数 */
  count: number
  /**
   * 命中口径：`literal` = find 整段按字面量命中（含换行）；
   * `segments` = find 按空白切成几段、各段按原顺序命中（原文的空白与 find 不同，回执里说明）
   */
  match: 'literal' | 'segments'
  /** 替换涉及的首行与末行（1 起、闭区间） */
  startLine: number
  endLine: number
  /** 被替换掉的原文（折叠空白后截断，多处时是头一处） */
  before: string
  /** 换上去的新文（折叠空白后截断） */
  after: string
  /** 写回后的行尾风格：原文件的行尾被原样保留（CRLF 文件不会被换成 LF） */
  eol: 'lf' | 'crlf'
  /** 原文件带 BOM 时写回仍带 BOM */
  bom: boolean
  /** 写回后的字节数 */
  size: number
}

/** 引用一段本地路径的结果：文件给内容、目录给清单，超长文件按截断给出 */
export interface WorkspaceReference {
  path: string
  dir: boolean
  size: number
  text: string
  truncated: boolean
}

/** 上传目的地：服务端的上传接口地址与当前会话令牌（文件本体由主进程读、由主进程送） */
export interface WorkspaceUploadTarget {
  url: string
  token: string
}

/** 上传回执：文件服务给出的文件标识与文件信息，随后即可按 id 引用该文件 */
export interface WorkspaceFileUpload {
  /** 本机文件的绝对路径 */
  path: string
  /** 文件标识：交给服务端工具方法（如 multimodalAnalyze）按 id 引用本文件 */
  id: string
  name: string
  type: string
  suffix: string
  size: number
}

/** 打开 / 定位本机文件的结果：ok 为 false 时 message 是不能照做的原因 */
export interface WorkspaceOpenResult {
  ok: boolean
  /** 归一后的路径（定位失败时是调用方给的那一段） */
  path: string
  /** 拒绝的原因是「可执行 / 脚本文件」：界面确认后带 force 再来一次 */
  unsafe?: boolean
  message?: string
}

/** 项目目录清单的来源：主进程按项目标识给出该项目声明的目录（读不到就返回空，一律拒绝） */
export type ProjectRootsResolver = (projectId: string) => WorkspaceRoot[]

/** 项目环境变量的来源：主进程按项目标识给出该项目配置的变量（读不到就返回空） */
export type ProjectEnvResolver = (projectId: string) => ProjectEnvVar[]

export interface WorkspaceEntry {
  name: string
  path: string
  dir: boolean
  size: number
  updatedAt: number
  ext: string
}

export interface WorkspaceNode extends WorkspaceEntry {
  permission: WorkspacePermission | null
}

const MAX_TEXT_SIZE = 2 * 1024 * 1024 // 单文件预览上限 2MB，避免大文件把渲染进程拖垮
/** 引用进对话上下文时的读取上限：比预览宽得多，超出的部分按截断处理而不是直接拒绝 */
const MAX_REFERENCE = 8 * 1024 * 1024
const REFERENCE_BINARY_SNIFF = 4096
/** 上传到文件服务的单文件上限（与服务端 multipart 上限同一量级） */
const MAX_UPLOAD_SIZE = 100 * 1024 * 1024
/** 上传超时：几十 MB 的图片 / 视频要给足时间，卡住也要能收尾 */
const UPLOAD_TIMEOUT = 5 * 60 * 1000
/**
 * 后缀 → contentType：本机文件交给服务端时带上的类型。
 * 服务端存的就是这一份（缺类型时按后缀补的也是这套表），所以多媒体文件要认得出来。
 */
const UPLOAD_MIME_TYPES: Record<string, string> = {
  png: 'image/png',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  webp: 'image/webp',
  gif: 'image/gif',
  bmp: 'image/bmp',
  svg: 'image/svg+xml',
  mp4: 'video/mp4',
  webm: 'video/webm',
  mov: 'video/quicktime',
  m4v: 'video/x-m4v',
  mkv: 'video/x-matroska',
  avi: 'video/x-msvideo',
  mp3: 'audio/mpeg',
  wav: 'audio/wav',
  m4a: 'audio/mp4',
  aac: 'audio/aac',
  flac: 'audio/flac',
  ogg: 'audio/ogg',
  pdf: 'application/pdf',
  txt: 'text/plain',
  md: 'text/markdown',
  csv: 'text/csv',
  json: 'application/json',
  xml: 'application/xml',
  html: 'text/html',
  zip: 'application/zip',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xls: 'application/vnd.ms-excel',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ppt: 'application/vnd.ms-powerpoint',
  pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
}
const MAX_FILES = 3000 // 遍历条目上限（清单与文本检索共用），够用且不至于把一次遍历拖太久
const MAX_FILE_DEPTH = 8 // 清单递归深度上限
/**
 * 可执行 / 脚本类后缀：这些文件「打开」就是「运行」，而对话里给出的路径可能来自模型输出，
 * 不能一点就执行——默认拒绝，界面确认过（force）才交给系统默认程序。
 */
const EXECUTABLE_EXTS = new Set([
  'exe',
  'com',
  'bat',
  'cmd',
  'msi',
  'msix',
  'appx',
  'scr',
  'pif',
  'cpl',
  'hta',
  'vbs',
  'vbe',
  'js',
  'jse',
  'wsf',
  'wsh',
  'ps1',
  'psm1',
  'reg',
  'lnk',
  'url',
  'jar',
  'dll',
  'so',
  'dylib',
  'sh',
  'command',
  'desktop'
])
const MAX_SEARCH_FILE = 1024 * 1024 // 文本检索单文件上限 1MB
const MAX_SEARCH_MATCH = 200 // 文本检索命中条数上限
const MAX_SEARCH_LINE = 240 // 单行命中的展示长度
/**
 * 文本替换的单文件上限 8MB：替换要整块读进内存、改完再整块写回，
 * 比检索（1MB）宽，但也不能没边——几百 MB 的文件不是拿来「改一处字」的。
 */
const MAX_REPLACE_SIZE = 8 * 1024 * 1024
/** 命令输出单流上限：超出时保留头尾，中间折成一行说明 */
const MAX_COMMAND_OUTPUT = 32 * 1024
/** 命令超时：默认 60 秒，最长 10 分钟（装依赖、跑构建也够） */
const COMMAND_TIMEOUT = 60
const MAX_COMMAND_TIMEOUT = 600
/** 清单与检索一律跳过的目录：依赖与产物，进去只会把结果淹没 */
const IGNORED_DIRS = new Set([
  '.git',
  '.gradle',
  '.idea',
  '.next',
  '.nuxt',
  '.pnpm-store',
  '.venv',
  '__pycache__',
  'build',
  'dist',
  'node_modules',
  'out',
  'target',
  'venv'
])

/** 动作文案：权限不足、策略拒绝、申请弹窗都用它 */
const NEED_LABELS: Record<WorkspaceNeed, string> = {
  read: '读取',
  write: '写入',
  command: '执行命令'
}

/** 目录权限的说法：给模型的本机说明里带上 */
const PERMISSION_LABELS: Record<WorkspacePermission, string> = {
  read: '只读',
  write: '只写',
  readwrite: '读写'
}

/** 命令的执行 shell：在进程内不变，启动时算一次 */
const COMMAND_SHELL =
  'win32' === process.platform ? basename(process.env.ComSpec || 'cmd.exe') : '/bin/sh'

/** 系统说法（含架构）：版本号编不出 Windows 10 / 11 的说法，就按 build 编一个，编不出原样给 */
function systemLabel(): string {
  const release = osRelease()
  const arch = process.arch
  if ('win32' === process.platform) {
    if (!release.startsWith('10.0.')) return `Windows ${release}（${arch}）`
    const name = Number(release.split('.')[2] ?? 0) >= 22000 ? 'Windows 11' : 'Windows 10'
    return `${name}（${release}，${arch}）`
  }
  if ('darwin' === process.platform) return `macOS（Darwin ${release}，${arch}）`
  return `Linux（${release}，${arch}）`
}

const SYSTEM = systemLabel()

/**
 * 命令输出解码：先按 UTF-8 解；出现替换字符说明不是 UTF-8（Windows 控制台常见 GBK），
 * 再按 GBK 解一次，仍然解不动就保留 UTF-8 的结果（宁可留几个问号，也不整段丢掉）。
 */
function decodeOutput(buffer: Buffer): string {
  const text = buffer.toString('utf8')
  if (!text.includes('\uFFFD')) return text
  try {
    const gbk = new TextDecoder('gbk').decode(buffer)
    return gbk.includes('\uFFFD') ? text : gbk
  } catch {
    return text
  }
}

/** 文件类型：按后缀给出 contentType，判断不出来按二进制（服务端仍会按后缀与内容自行核对） */
function mimeOf(name: string): string {
  return (
    UPLOAD_MIME_TYPES[
      extname(String(name ?? ''))
        .replace(/^\./, '')
        .toLowerCase()
    ] ?? ''
  )
}

/** 异常取文案：Error 取 message，其它值转字符串 */
function reason(error: unknown): string {
  return error instanceof Error ? error.message : String(error ?? '')
}

/** 回执文案截断：服务端返回非 JSON 时只留开头，避免把整页 HTML 抛给模型 */
function cut(text: string, limit = 200): string {
  const value = String(text ?? '').trim()
  return value.length <= limit ? value : value.slice(0, limit) + '...'
}

/**
 * 检索关键词切段：按空白（空格、制表、换行）切开，空段丢掉。
 * 从对话里整段粘来的多行片段靠它还原成「按原顺序出现的几段」——库里那一段的缩进与换行
 * 未必与粘来的那份一致，逐段匹配、段间允许任意空白才兜得住（见 WorkspaceManager.search）。
 */
function searchSegments(keyword: string): string[] {
  return String(keyword ?? '')
    .split(/\s+/)
    .filter(Boolean)
}

/** 正则里的字面量转义：检索关键词一律按原文匹配，不当正则元字符用 */
function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** 一段文本里的换行数：命中位置据此折算行号 */
function countNewlines(text: string): number {
  return (text.match(/\n/g) ?? []).length
}

/** 一行摘要：折叠换行与连续空白后截断——检索命中、失败提示里的关键词用 */
function oneLine(value: unknown, limit = 200): string {
  return cut(
    String(value ?? '')
      .replace(/\s+/g, ' ')
      .trim(),
    limit
  )
}

/**
 * 替换回执里的片段：把换行与制表写成 `\n` / `\t`、其余原样保留后截断。
 * 「替掉了什么」必须看得见空白——按分段口径命中时，两段之间的缩进也在替换范围内，
 * 折叠掉就没法判断替换内容该不该自带缩进（见 WorkspaceManager.replace）。
 */
function excerpt(value: unknown, limit = 200): string {
  const text = String(value ?? '')
    .replace(/\r\n?/g, '\n')
    .replace(/\t/g, '\\t')
    .replace(/\n/g, '\\n')
  return cut(text, limit)
}

/**
 * 一次关键词命中：字符下标区间与折算出的行号（1 起、闭区间）。
 *
 * `loose` 说明这次命中是怎么来的：`false` = find 整段按字面量命中（含 find 里的换行）；
 * `true` = find 按空白切成几段、各段按原顺序出现（段与段之间允许任意空白，
 * 所以从对话里整段粘来、缩进与换行对不上的片段也能命中）。
 */
interface KeywordHit {
  start: number
  end: number
  line: number
  endLine: number
  loose: boolean
}

/**
 * 关键词命中（文本里的字符区间）：**先按字面量整段找，一条都没有、而关键词又能按空白切成
 * 多段时**再按分段口径找一遍——顺序仍然是「各段按原顺序」，不是把关键词拆成几个词各找各的。
 *
 * from / to 是字符下标，命中必须整个落在区间内（替换用它限定行范围）。行号按命中位置的
 * 换行数折算，末行取「命中最后一个字符所在的行走」，所以命中以换行结尾时不会多算一行。
 */
function keywordHits(text: string, keyword: string, from = 0, to = text.length): KeywordHit[] {
  const find = String(keyword ?? '')
  if (!find || to <= from) return []
  const hitAt = (start: number, length: number, loose: boolean): KeywordHit => {
    const end = start + length
    const line = countNewlines(text.slice(0, start)) + 1
    return {
      start,
      end,
      line,
      endLine: countNewlines(text.slice(0, Math.max(start, end - 1))) + 1,
      loose
    }
  }
  const literal = (): KeywordHit[] => {
    const hits: KeywordHit[] = []
    let at = text.indexOf(find, from)
    while (at >= 0) {
      if (at + find.length > to) break
      hits.push(hitAt(at, find.length, false))
      at = text.indexOf(find, at + find.length)
    }
    return hits
  }
  const loose = (): KeywordHit[] => {
    const segments = searchSegments(find)
    if (segments.length < 2) return []
    const pattern = new RegExp(segments.map(escapeRegExp).join('\\s+'), 'g')
    const hits: KeywordHit[] = []
    // 只在区间内扫：越界的命中不该出现（行范围限定的是「在哪儿替换」）
    for (const match of text.slice(from, to).matchAll(pattern)) {
      hits.push(hitAt(from + (match.index ?? 0), match[0].length, true))
    }
    return hits
  }
  const exact = literal()
  return exact.length ? exact : loose()
}

/**
 * 单条命令的一条输出流：上限内的字节全留，超限时保留头尾各一半，中间折成一行说明。
 * 只留头会把报错丢掉、只留尾会把开头丢掉，两头各留一半最实用。
 */
class OutputCollector {
  private head: Buffer[] = []
  private tail: Buffer[] = []
  private headSize = 0
  private tailSize = 0
  private size = 0

  constructor(private limit: number) {}

  push(chunk: Buffer): void {
    this.size += chunk.length
    const half = Math.floor(this.limit / 2)
    let rest = chunk
    if (this.headSize < half) {
      const part = rest.subarray(0, half - this.headSize)
      this.head.push(part)
      this.headSize += part.length
      rest = rest.subarray(part.length)
    }
    if (!rest.length) return
    this.tail.push(rest)
    this.tailSize += rest.length
    // 尾部只留最近的一半：一直往里塞会把内存撑爆
    while (this.tailSize > half && this.tail.length) {
      const first = this.tail[0]
      const over = this.tailSize - half
      if (first.length <= over) {
        this.tail.shift()
        this.tailSize -= first.length
        continue
      }
      this.tail[0] = first.subarray(over)
      this.tailSize -= over
    }
  }

  text(): string {
    const head = decodeOutput(Buffer.concat(this.head))
    const omitted = this.size - this.headSize - this.tailSize
    if (omitted < 1) return head
    return `${head}\n……（中间省略 ${omitted} 字节）……\n${decodeOutput(Buffer.concat(this.tail))}`
  }
}

export class WorkspaceManager {
  private static instance: WorkspaceManager

  private resolveProjectRoots: ProjectRootsResolver | null = null

  private resolveProjectEnv: ProjectEnvResolver | null = null

  /** 工作区外文件的访问策略：默认逐个询问，由系统设置同步过来 */
  private policy: WorkspacePolicy = 'ask'

  /** 询问界面的一次性回调：由主进程注册（把申请推给渲染进程并等答复） */
  private approval: ((request: WorkspaceApproval) => Promise<boolean>) | null = null

  /**
   * 正在执行的命令：停止生成时按会话成批收掉。
   * 命令跑在自己的会话里，一轮对话按下停止后不该还有命令留在后台跑
   * （尤其是它跑完还会把结果交回对话循环，让整轮对话接着往下走）。
   */
  private commands = new Set<RunningCommand>()

  static getInstance(): WorkspaceManager {
    if (!WorkspaceManager.instance) {
      WorkspaceManager.instance = new WorkspaceManager()
    }
    return WorkspaceManager.instance
  }

  /** 注入清单来源：由主进程在启动时注册，按项目标识取该项目声明的目录 */
  setProjectRootsResolver(resolver: ProjectRootsResolver): void {
    this.resolveProjectRoots = resolver
  }

  /** 注入环境变量来源：由主进程在启动时注册，按项目标识取该项目配置的变量 */
  setProjectEnvResolver(resolver: ProjectEnvResolver): void {
    this.resolveProjectEnv = resolver
  }

  /** 注入询问通道：主进程启动时注册，把工作区外的访问申请推给界面 */
  setApprovalHandler(handler: (request: WorkspaceApproval) => Promise<boolean>): void {
    this.approval = handler
  }

  getPolicy(): WorkspacePolicy {
    return this.policy
  }

  setPolicy(value: unknown): WorkspacePolicy {
    const text = String(value ?? '').trim()
    if ('ask' === text || 'auto' === text || 'deny' === text || 'full' === text) this.policy = text
    return this.policy
  }

  /**
   * 某个项目的有效清单：路径归一化为绝对路径，重复项按「权限取大」合并，
   * 主目录标记同样合并（同一路径写了两遍，其中一份标了主目录就算主目录）。
   * 返回顺序就是项目里的声明顺序——「当前目录」不看这个顺序，见 `primaryOf`。
   */
  private roots(projectId: string): WorkspaceRoot[] {
    const list = this.resolveProjectRoots?.(String(projectId ?? '')) ?? []
    const merged = new Map<string, WorkspaceRoot>()
    for (const item of list ?? []) {
      const path = resolve(String(item?.path ?? ''))
      const permission = this.permission(item?.permission)
      if (!path || !permission) continue
      const current = merged.get(path)
      merged.set(path, {
        path,
        permission: current ? this.merge(current.permission, permission) : permission,
        primary: true === current?.primary || true === item?.primary
      })
    }
    return [...merged.values()]
  }

  /**
   * 主目录：清单里标了 primary 的那一个，没标的（旧配置、分享来的配置）取第一个目录。
   * 命令的默认工作目录与相对路径的优先解析都只认这一个入口，
   * 免得「当前目录」在各处各算一遍、算法还对不上。
   */
  private primaryOf(roots: WorkspaceRoot[]): WorkspaceRoot | null {
    return roots.find((root) => true === root.primary) ?? roots[0] ?? null
  }

  /**
   * 某个项目的环境变量：读不到（项目已删、列坏了、变量名不合规）就当没配。
   * 它是「有就带上」的附加项，不该因为一份读不出来的配置让命令执行失败。
   */
  private envVars(projectId: string): ProjectEnvVar[] {
    try {
      return this.resolveProjectEnv?.(String(projectId ?? '')) ?? []
    } catch {
      return []
    }
  }

  /**
   * 给模型的本机说明：运行环境（系统 / 架构 / shell / 路径样式 / 当前时间）与工作区口径
   * （目录与权限、主目录、命令的默认工作目录、相对路径解析、工作区外的访问策略）。
   * 项目配了环境变量时另列一节，**只给变量名、不给值**：模型据此知道命令里可以引用哪些名字，
   * 而密钥这类值不出本机。
   *
   * 只列事实、不写解释——这段随每轮请求下发，占的是上下文。渲染进程把它追加在项目系统提示词
   * 之后；目录、主目录与策略都在这里现算，改了配置下一轮就是新的说法。
   */
  describe(projectId: string): string {
    const roots = this.roots(projectId)
    const primary = this.primaryOf(roots)
    const env = this.envVars(projectId)
    const lines = [
      `# 环境信息`,
      `## 本机环境`,
      `- 操作系统：${SYSTEM}`,
      `- 命令执行：${COMMAND_SHELL}`
    ]
    lines.push(`## 工作区`)
    if (roots.length < 1) {
      lines.push('未声明工作区目录：文件操作必须给绝对路径，执行命令会被拒绝')
    } else {
      roots.forEach((root) => {
        const marks = [PERMISSION_LABELS[root.permission]]
        if (primary && root.path === primary.path) marks.push('主目录')
        lines.push(`- ${root.path}（${marks.join('，')}）`)
      })
    }
    if (env.length) {
      // 有密钥时提前说一句「读不到值」：省下模型打印一遍、看到 *** 再猜的那一轮
      const masked = env.some((item) => item.secret)
        ? '；密钥类变量的值不出现在输出里（被换成 ***）'
        : ''
      lines.push(`## 环境变量`)
      lines.push(
        `- 执行命令时带上（值不下发，需要时按 shell 写法引用）：${env
          .map((item) => item.key)
          .join('、')}${masked}`
      )
    }
    return lines.join('\n')
  }

  /** 列出目录：只读及以上权限可读 */
  async list(projectId: string, path: string, conversationId = ''): Promise<WorkspaceNode[]> {
    const target = await this.resolve(projectId, path, 'read', { conversationId })
    return this.entries(projectId, target.path)
  }

  /** 列出已经鉴权过的目录：引用（reference）也用这里，避免同一路径被鉴权两次（会重复弹批准） */
  private entries(projectId: string, path: string): WorkspaceNode[] {
    const target = { path: resolve(String(path ?? '')) }
    if (!existsSync(target.path)) return []
    if (!statSync(target.path).isDirectory()) throw new Error('目标不是目录：' + target.path)
    return readdirSync(target.path, { withFileTypes: true })
      .map((entry) => {
        const child = join(target.path, entry.name)
        const stat = statSync(child)
        const node: WorkspaceNode = {
          name: entry.name,
          path: child,
          dir: entry.isDirectory(),
          size: entry.isDirectory() ? 0 : stat.size,
          updatedAt: stat.mtimeMs,
          ext: entry.isDirectory() ? '' : extname(entry.name).replace(/^\./, ''),
          permission: null
        }
        try {
          node.permission = this.inside(projectId, child)?.permission ?? null
        } catch {
          node.permission = null
        }
        return node
      })
      .sort((a, b) => {
        if (a.dir !== b.dir) return a.dir ? -1 : 1
        return a.name.localeCompare(b.name)
      })
  }

  async read(
    projectId: string,
    path: string,
    conversationId = ''
  ): Promise<{ path: string; content: string; size: number }> {
    const target = await this.resolve(projectId, path, 'read', { conversationId })
    const stat = statSync(target.path)
    if (stat.isDirectory()) throw new Error('目标不是文件：' + target.path)
    if (stat.size > MAX_TEXT_SIZE)
      throw new Error('文件超过预览上限（2MB）：' + basename(target.path))
    return { path: target.path, content: readFileSync(target.path, 'utf8'), size: stat.size }
  }

  /**
   * 用系统默认程序打开一个本机文件，目录交给文件管理器。
   *
   * 这是用户点出来的动作，所以只做「路径归一 + 存在性判断 + 可执行文件防护」：
   * 不走工作区权限与访问策略，工作区外的文件也照开——用户是看着提示里的完整路径点的，
   * 与模型自己越界读文件不是一回事。
   *
   * `force` 只对付最后一条：可执行 / 脚本类文件默认不打开（打开即运行），界面确认后再放行。
   */
  async open(projectId: string, path: string, force = false): Promise<WorkspaceOpenResult> {
    const target = this.locate(projectId, path)
    if (target.message) return { ok: false, path: target.path, message: target.message }
    if (!force && this.executable(target.path)) {
      return {
        ok: false,
        unsafe: true,
        path: target.path,
        message: '这是可执行 / 脚本文件，没有直接打开'
      }
    }
    // openPath 交给系统默认程序：成功返回空串，否则是不能打开的原因（没有关联程序等）
    const failure = await shell.openPath(target.path)
    if (failure) return { ok: false, path: target.path, message: `打开失败：${failure}` }
    return { ok: true, path: target.path }
  }

  /** 在系统文件管理器中定位这个文件 / 目录：可执行文件用它代替直接打开，任何文件都点得动 */
  async reveal(projectId: string, path: string): Promise<WorkspaceOpenResult> {
    const target = this.locate(projectId, path)
    if (target.message) return { ok: false, path: target.path, message: target.message }
    shell.showItemInFolder(target.path)
    return { ok: true, path: target.path }
  }

  /**
   * 拖出窗口的目标路径：把工作区里的文件 / 文件夹拖给别的应用（资源管理器、聊天窗口、编辑器……）。
   * 与 open / reveal 同一套口径——用户自己拖出来的动作，只做路径归一与存在性判断，不套工作区外的访问策略
   * （拖出去只是让系统把这一份交给目标应用，跟我们读写它无关，可执行文件也照拖——复制出去不等于运行）。
   * 真正的原生拖放要 WebContents，发起那一端在主进程的 ipc 里（见 index.ts 的 workspace:drag）。
   */
  drag(projectId: string, path: string): WorkspaceOpenResult {
    const target = this.locate(projectId, path)
    if (target.message) return { ok: false, path: target.path, message: target.message }
    return { ok: true, path: target.path }
  }

  /**
   * 路径定位：绝对路径原样用，相对路径按项目目录解析（与文件工具同一套口径，见 absolute）。
   * 只判断存在性、不做权限判定——结论由调用方按用途决定（open / reveal 都不套策略）。
   */
  private locate(projectId: string, path: string): { path: string; message?: string } {
    let target = ''
    try {
      target = this.absolute(projectId, path)
    } catch (error) {
      return { path: String(path ?? '').trim(), message: reason(error) }
    }
    if (!existsSync(target)) {
      return { path: target, message: `文件不存在（可能已被移动或删除）：${target}` }
    }
    return { path: target }
  }

  /** 目录一律当作可打开（交给文件管理器），只有文件才看后缀是不是可执行 / 脚本类 */
  private executable(path: string): boolean {
    try {
      if (statSync(path).isDirectory()) return false
    } catch {
      return false
    }
    return EXECUTABLE_EXTS.has(extname(path).replace(/^\./, '').toLowerCase())
  }

  async write(
    projectId: string,
    path: string,
    content: string,
    conversationId = ''
  ): Promise<{ path: string; size: number }> {
    const target = await this.resolve(projectId, path, 'write', { conversationId })
    mkdirSync(dirname(target.path), { recursive: true })
    writeFileSync(target.path, String(content ?? ''), 'utf8')
    return { path: target.path, size: statSync(target.path).size }
  }

  /**
   * 文本替换：把文件里的一段原文换成新文，整块读进来在主进程里改、再整块写回。
   *
   * - 查找口径与检索同一套（见 keywordHits）：先按字面量整段找（find 里的换行对应文件里的
   *   换行，CRLF 与 LF 先在内部统一，所以从对话里复制的多行片段能直接命中）；一条都没命中、
   *   而 find 又能按空白切成多段时，退到「各段按原顺序出现、段间任意空白」的口径，
   *   回执里的 match 会说明这次是按哪一种命中的。find 与替换内容都按字面量，
   *   特殊字符（正则元字符、%、反斜杠、引号）与换行原样使用。
   * - 范围：startLine / endLine 给行范围（1 起、闭区间，省略即整个文件），命中必须整个落在
   *   范围内；范围内默认要求恰好一处命中，0 处或多处都失败并把位置回报给调用方（免得改错地方），
   *   要一次改多处就显式给 all。
   * - 保真：行尾（LF / CRLF）与 BOM 按原样保留——内部按 LF 处理，回写时还原成原行尾，
   *   替换内容里的换行也按原行尾写出；只处理 UTF-8 文本，二进制（含 NUL 字节）与非 UTF-8
   *   （按 UTF-8 解出来是乱码，例如 GBK 文件）直接拒绝，不做静默转码；超过 8MB 拒绝。
   * - 权限与 write 同一套：命中项目目录按目录权限放行，工作区外按访问策略申请或拒绝。
   */
  async replace(
    projectId: string,
    path: string,
    find: string,
    replacement: string,
    scope: WorkspaceReplaceScope = {},
    conversationId = ''
  ): Promise<WorkspaceReplaceResult> {
    const target = await this.resolve(projectId, path, 'write', { conversationId })
    if (!existsSync(target.path)) {
      throw new Error(`文件不存在（可能已被移动或删除）：${target.path}`)
    }
    const stat = statSync(target.path)
    if (stat.isDirectory()) throw new Error('目标不是文件：' + target.path)
    if (stat.size > MAX_REPLACE_SIZE) {
      throw new Error(`文件超过替换上限（8MB）：${basename(target.path)}`)
    }
    const buffer = readFileSync(target.path)
    if (buffer.includes(0)) {
      throw new Error(`这个文件像是二进制（含 NUL 字节），没有替换：${basename(target.path)}`)
    }
    const bom = 0xef === buffer[0] && 0xbb === buffer[1] && 0xbf === buffer[2]
    const body = buffer.subarray(bom ? 3 : 0).toString('utf8')
    if (body.includes('\uFFFD')) {
      throw new Error(
        `这个文件不是 UTF-8 文本（按 UTF-8 解出来是乱码），没有替换：${basename(target.path)}`
      )
    }
    // 全是 CRLF 才按 CRLF 写回：混用的文件按 LF 处理，不猜
    const crlfCount = (body.match(/\r\n/g) ?? []).length
    const eol: 'lf' | 'crlf' =
      crlfCount > 0 && crlfCount === (body.match(/\n/g) ?? []).length ? 'crlf' : 'lf'
    // 内部统一成 LF 视图：行号才与 read 给出的、以及调用方写的一一对上
    const text = body.replace(/\r\n/g, '\n')
    const needle = String(find ?? '').replace(/\r\n?/g, '\n')
    if (!needle) throw new Error('replace 需要 find（要被替换掉的原文）')
    const next = String(replacement ?? '').replace(/\r\n?/g, '\n')
    if (needle === next) throw new Error('find 与要替换成的内容一样，没有可替换的东西')

    // 行范围 → 字符区间（闭区间；末行的换行不算在范围内，替换一整行不会把行尾吃掉）
    const lines = text.split('\n')
    const lineCount = lines.length
    const fromLine = Math.min(lineCount, Math.max(1, Math.floor(Number(scope.startLine ?? 1) || 1)))
    const toLine = Math.max(
      fromLine,
      Math.min(lineCount, Math.floor(Number(scope.endLine ?? lineCount) || lineCount))
    )
    let from = 0
    for (let index = 0; index < fromLine - 1; index++) from += lines[index].length + 1
    let to = from
    for (let index = fromLine - 1; index < toLine; index++) {
      to += lines[index].length + (index < toLine - 1 ? 1 : 0)
    }
    const where = fromLine === toLine ? `第 ${fromLine} 行` : `第 ${fromLine}-${toLine} 行`

    const hits = keywordHits(text, needle, from, to)
    if (!hits.length) {
      throw new Error(
        `${where}里没有找到要替换的内容（字面量与分段两种口径都试过）：${excerpt(needle, 120)}`
      )
    }
    if (true !== scope.all && hits.length > 1) {
      const at = hits
        .slice(0, 5)
        .map((hit) =>
          hit.line === hit.endLine ? `第 ${hit.line} 行` : `第 ${hit.line}-${hit.endLine} 行`
        )
        .join('、')
      throw new Error(
        `${where}里命中 ${hits.length} 处（${at}${hits.length > 5 ? ' 等' : ''}）：` +
          '要全部替换就把 all 给 true，或者用 start_line / end_line 把范围收到其中一处'
      )
    }
    const chosen = true === scope.all ? hits : hits.slice(0, 1)
    // 从后往前替换：前面的命中区间才不受后面改动的影响
    let output = text
    for (let index = chosen.length - 1; index >= 0; index--) {
      const hit = chosen[index]
      output = output.slice(0, hit.start) + next + output.slice(hit.end)
    }
    const written =
      'crlf' === eol
        ? Buffer.from(output.replace(/\n/g, '\r\n'), 'utf8')
        : Buffer.from(output, 'utf8')
    writeFileSync(
      target.path,
      bom ? Buffer.concat([Buffer.from([0xef, 0xbb, 0xbf]), written]) : written
    )
    return {
      path: target.path,
      count: chosen.length,
      match: chosen[0].loose ? 'segments' : 'literal',
      startLine: chosen[0].line,
      endLine: chosen[chosen.length - 1].endLine,
      before: excerpt(text.slice(chosen[0].start, chosen[0].end)),
      after: excerpt(next),
      eol,
      bom,
      size: statSync(target.path).size
    }
  }

  /**
   * 引用一段本地路径进对话上下文：文件给内容、目录给清单，调用方不区分两者。
   * 与其它文件操作同一套鉴权（命中项目目录按目录权限，越界按访问策略），
   * 但读取上限比文件预览宽得多：超长文件按截断给出并说明，而不是直接失败；
   * 二进制文件只回一句说明，避免把一堆乱码塞进上下文。
   */
  async reference(
    projectId: string,
    path: string,
    conversationId = ''
  ): Promise<WorkspaceReference> {
    const target = await this.resolve(projectId, path, 'read', { conversationId })
    const stat = statSync(target.path)
    if (stat.isDirectory()) {
      // 路径已经鉴权过，直接列目录：不再走一次 resolve，否则同一路径会重复申请批准
      const rows = this.entries(projectId, target.path)
      const text = rows
        .map((row) => (row.dir ? `[目录] ${row.name}` : `[文件] ${row.name}（${row.size} 字节）`))
        .join('\n')
      return { path: target.path, dir: true, size: 0, text, truncated: false }
    }
    // 只读到上限为止：几百 MB 的文件也不该被整块读进内存
    const capacity = Math.min(stat.size, Math.max(MAX_REFERENCE, REFERENCE_BINARY_SNIFF))
    const buffer = Buffer.allocUnsafe(capacity)
    const fd = openSync(target.path, 'r')
    let length = 0
    try {
      length = readSync(fd, buffer, 0, capacity, 0)
    } finally {
      closeSync(fd)
    }
    const head = buffer.subarray(0, Math.min(length, REFERENCE_BINARY_SNIFF))
    if (head.includes(0)) {
      return {
        path: target.path,
        dir: false,
        size: stat.size,
        text: `（二进制文件，未读取内容，共 ${stat.size} 字节）`,
        truncated: false
      }
    }
    const truncated = stat.size > length
    const text = buffer.subarray(0, truncated ? MAX_REFERENCE : length).toString('utf8')
    return { path: target.path, dir: false, size: stat.size, text, truncated }
  }

  /**
   * 上传本机文件到文件服务（图床）：内容由主进程按 multipart 直传服务端，回执里带回文件标识（id）等信息。
   *
   * 服务端读不到本机的磁盘，所以本机路径进不了服务端的工具方法（如图片交给 multimodalAnalyze 分析）——
   * 有本地文件时的顺序是：先经这里传一次拿 id，再把 id 交给工具方法。
   * 鉴权与其它文件操作同一套：命中项目目录按目录权限，越界按访问策略确认；读取没被放行就不会上传。
   */
  async upload(
    projectId: string,
    path: string,
    target: WorkspaceUploadTarget,
    conversationId = ''
  ): Promise<WorkspaceFileUpload> {
    const url = String(target?.url ?? '').trim()
    if (!url) throw new Error('上传地址无效：请登录后重试')
    const token = String(target?.token ?? '').trim()
    const resolved = await this.resolve(projectId, path, 'read', { conversationId })
    const stat = statSync(resolved.path)
    if (stat.isDirectory()) throw new Error('目标不是文件：' + resolved.path)
    const name = basename(resolved.path)
    if (stat.size > MAX_UPLOAD_SIZE) {
      throw new Error(`文件超过上传上限（${MAX_UPLOAD_SIZE / 1024 / 1024}MB）：${name}`)
    }
    const form = new FormData()
    form.append(
      'file',
      new Blob([new Uint8Array(readFileSync(resolved.path))], { type: mimeOf(name) }),
      name
    )
    let response: Response
    try {
      response = await fetch(url, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: form,
        signal: AbortSignal.timeout(UPLOAD_TIMEOUT)
      })
    } catch (error) {
      throw new Error(`上传失败（请求异常）：${reason(error)}`)
    }
    const text = await response.text().catch(() => '')
    let payload: { code?: unknown; message?: unknown; data?: Record<string, unknown> } | null = null
    try {
      payload = JSON.parse(text)
    } catch {
      payload = null
    }
    if (!response.ok) throw new Error(`上传失败（HTTP ${response.status}）：${cut(text)}`)
    if (!payload || 0 !== Number(payload.code)) {
      throw new Error(`上传失败：${String(payload?.message ?? '') || cut(text)}`)
    }
    const data = payload.data ?? {}
    if (!data['id']) throw new Error('上传失败：服务端未返回文件标识')
    return {
      path: resolved.path,
      id: String(data['id']),
      name: String(data['name'] ?? name),
      type: String(data['type'] ?? ''),
      suffix: String(data['suffix'] ?? ''),
      size: Number(data['size'] ?? stat.size)
    }
  }

  async mkdir(projectId: string, path: string, conversationId = ''): Promise<{ path: string }> {
    const target = await this.resolve(projectId, path, 'write', { conversationId })
    mkdirSync(target.path, { recursive: true })
    return { path: target.path }
  }

  async rename(
    projectId: string,
    path: string,
    next: string,
    conversationId = ''
  ): Promise<{ path: string }> {
    const from = await this.resolve(projectId, path, 'write', { conversationId })
    const to = await this.resolve(projectId, next, 'write', { conversationId })
    if (!existsSync(from.path)) throw new Error('目标不存在：' + from.path)
    mkdirSync(dirname(to.path), { recursive: true })
    renameSync(from.path, to.path)
    return { path: to.path }
  }

  /** 删除：限定在该项目已声明的目录内，且不允许删除声明目录本身 */
  async remove(projectId: string, path: string, conversationId = ''): Promise<{ path: string }> {
    const target = await this.resolve(projectId, path, 'write', { conversationId })
    if (this.roots(projectId).some((root) => resolve(root.path) === target.path)) {
      throw new Error('不允许删除工作区授权目录本身：' + target.path)
    }
    rmSync(target.path, { recursive: true, force: true })
    return { path: target.path }
  }

  /**
   * 工作区文件清单：只列已声明目录内的文件与目录，供按名称检索（对话输入区不再铺这份清单，
   * 由本地文件的「选择本地…」与文件工具各走各的路）。
   * 依赖目录与产物目录一律跳过，总量与深度都有上限，避免大工程把一次遍历拖太久。
   */
  files(projectId: string): WorkspaceFile[] {
    const result: WorkspaceFile[] = []
    for (const root of this.roots(projectId)) {
      this.walk(root, resolve(root.path), result, 0)
      if (result.length >= MAX_FILES) break
    }
    return result.slice(0, MAX_FILES)
  }

  private walk(root: WorkspaceRoot, path: string, result: WorkspaceFile[], depth: number): void {
    if (depth > MAX_FILE_DEPTH || result.length >= MAX_FILES) return
    let entries
    try {
      entries = readdirSync(path, { withFileTypes: true })
    } catch {
      return // 读不动的目录直接跳过，不让清单整体失败
    }
    for (const entry of entries) {
      if (result.length >= MAX_FILES) return
      const child = join(path, entry.name)
      if (entry.isDirectory()) {
        if (IGNORED_DIRS.has(entry.name)) continue
        // 目录本身也是一条可引用项：不区分文件与目录，选中都只是记一个路径
        result.push({
          name: entry.name,
          path: child,
          root: resolve(root.path),
          ext: '',
          dir: true
        })
        this.walk(root, child, result, depth + 1)
        continue
      }
      if (!entry.isFile()) continue
      result.push({
        name: entry.name,
        path: child,
        root: resolve(root.path),
        ext: extname(entry.name).replace(/^\./, '').toLowerCase(),
        dir: false
      })
    }
  }

  /**
   * 工作区内的文本检索：逐文件找关键词，只读到命中上限为止。
   *
   * 匹配口径见 `keywordHits`：先按字面量整段找（含换行），一条都没有、而关键词又能按空白
   * 切成多段时，再按「各段按原顺序出现、段间任意空白」找一遍——从对话里整段粘来的多行片段
   * 靠它兜住，换行、缩进、\r\n 与 \n 的差别都不影响，跨行的命中给出行号范围；
   * 关键词一律按字面量（正则元字符都不当语法用）。
   * 同一行只报一条（一行里出现多次不为多条）；二进制（含 NUL 字节）与超大文件跳过，命中文本截断展示。
   */
  search(projectId: string, keyword: string): WorkspaceMatch[] {
    const raw = String(keyword ?? '').trim()
    if (!raw) return []
    const result: WorkspaceMatch[] = []
    const files: WorkspaceFile[] = []
    for (const root of this.roots(projectId)) this.walk(root, resolve(root.path), files, 0)
    for (const file of files) {
      if (result.length >= MAX_SEARCH_MATCH) break
      let content: Buffer
      try {
        const stat = statSync(file.path)
        if (!stat.isFile() || stat.size > MAX_SEARCH_FILE) continue
        content = readFileSync(file.path)
      } catch {
        continue
      }
      if (content.includes(0)) continue // 含 NUL 字节按二进制处理
      const text = content.toString('utf8')
      const seen = new Set<number>()
      for (const hit of keywordHits(text, raw)) {
        if (seen.has(hit.line)) continue
        seen.add(hit.line)
        // 给「所在行」而不是只给命中的那几个字：从命中起始行的行首到结束行的行尾
        // （跨行的命中给整段）——模型据此判断上下文，也能直接把这段拿去做替换的 find
        const head = text.lastIndexOf('\n', hit.start - 1) + 1
        const tail = text.indexOf('\n', hit.end - 1)
        result.push({
          path: file.path,
          line: hit.line,
          endLine: hit.endLine,
          text: oneLine(text.slice(head, tail < 0 ? text.length : tail), MAX_SEARCH_LINE)
        })
        if (result.length >= MAX_SEARCH_MATCH) break
      }
    }
    return result
  }

  /**
   * 在本机执行一条命令（系统 shell：Windows 走 cmd、macOS / Linux 走 sh）。
   *
   * **命令一次也不跳访问策略**，工作目录在不在授权目录内都一样：
   * 命令能落到磁盘的任何地方，工作目录只是它的起点——拿「cwd 在工作区内」当放行理由，
   * 就等于给出一条绕过文件权限的路（工作区外写不了文件，改用命令写就够了）。
   * 所以 ask 逐条确认、auto / full 放行、deny 直接拒绝；工作目录命中授权目录时另外要求写权限
   * （只读目录不跑命令）。命令本身不做字符串层面的拦截：shell 里绕开关键字太容易，
   * 与其给人「拦住了」的错觉，不如让人看着命令原文再决定；退出码、输出与耗时原样回给调用方。
   *
   * 进程环境 = 当前进程的环境 + 项目里配置的环境变量（覆盖同名项、`PATH` 前置，见 `project-env.ts`），
   * 所以「装了两个 JDK、按项目切换」这类诉求由项目配置决定，而不是改系统环境变量。
   * 标了「密钥」的值在输出里被换成 `***`：非密钥的随用随打，密钥的连回显都不给。
   *
   * 省略 `cwd` 时在主目录里跑（项目没标主目录就是第一个目录），相对路径的 `cwd` 按同一套主目录优先的规则解析。
   */
  async exec(
    projectId: string,
    command: string,
    cwd = '',
    timeout = 0,
    conversationId = ''
  ): Promise<WorkspaceCommandResult> {
    const text = String(command ?? '').trim()
    if (!text) throw new Error('命令不能为空')
    const roots = this.roots(projectId)
    if (roots.length < 1) throw new Error(`项目未声明工作区目录，无法执行命令：${projectId}`)
    // 省略 cwd 时用项目的主目录：命令默认在「当前目录」里跑，而不是碰巧排在第一个的那个
    const primary = this.primaryOf(roots) ?? roots[0]
    const workdir = this.absolute(projectId, String(cwd ?? '').trim() || primary.path)
    const hit = this.inside(projectId, workdir)
    if (hit && !this.allowed(hit.permission, 'write')) {
      throw new Error(`权限不足，无法执行命令（该目录未授予写权限）：${workdir}`)
    }
    // 工作目录只决定命令从哪里跑：放行与否一律交给访问策略（命中授权目录也要过这一道）
    await this.access(projectId, workdir, 'command', { command: text, conversationId })
    if (!existsSync(workdir)) throw new Error('工作目录不存在：' + workdir)
    if (!statSync(workdir).isDirectory()) throw new Error('工作目录不是目录：' + workdir)
    return this.runCommand(
      text,
      workdir,
      this.timeout(timeout),
      conversationId,
      this.envVars(projectId)
    )
  }

  /** 超时归一：非正数取默认值，超出上限按上限算 */
  private timeout(value: unknown): number {
    const seconds = Math.floor(Number(value ?? 0) || 0)
    if (seconds < 1) return COMMAND_TIMEOUT
    return Math.min(seconds, MAX_COMMAND_TIMEOUT)
  }

  /**
   * 停止这段会话正在跑的命令：进程树收掉、等待中的调用立刻按「已停止」收尾。
   *
   * 一轮对话按停止时，界面只断得掉模型流；命令跑在主进程里，断流管不着它。
   * 不收的话这条命令会一直跑到自己的超时，跑完还会把结果交回对话循环，
   * 整轮对话就接着往下走了——那正是「停下来之后还在跑」的来处。
   * 返回被收掉的命令条数（没有就返回 0，调用方不必区分）。
   */
  cancel(conversationId: string): number {
    const id = String(conversationId ?? '')
    if (!id) return 0
    let stopped = 0
    for (const command of [...this.commands]) {
      if (command.conversationId !== id) continue
      stopped++
      command.stop()
    }
    return stopped
  }

  /**
   * 真正跑命令：输出按上限裁剪，超时把进程收掉，结果一次给全（调用方不用管流的事）。
   * 命令在跑期间登记在案的（按会话可收），收尾时不管走哪条路都会摘掉。
   */
  private runCommand(
    command: string,
    cwd: string,
    timeout: number,
    conversationId = '',
    vars: ProjectEnvVar[] = []
  ): Promise<WorkspaceCommandResult> {
    const started = Date.now()
    return new Promise<WorkspaceCommandResult>((settle) => {
      const child = spawn(command, {
        cwd,
        shell: true,
        windowsHide: true,
        env: mergeProjectEnv(process.env, vars)
      })
      const stdout = new OutputCollector(MAX_COMMAND_OUTPUT)
      const stderr = new OutputCollector(MAX_COMMAND_OUTPUT)
      let timedOut = false
      let cancelled = false
      let settled = false
      let timer: NodeJS.Timeout | null = null
      /** 这一条命令在册子上的样子：停止时按它收，收尾时摘掉（跑完不留痕） */
      let running: RunningCommand | null = null
      const finish = (code: number | null, signal: NodeJS.Signals | null): void => {
        if (settled) return
        settled = true
        if (running) this.commands.delete(running)
        if (timer) clearTimeout(timer)
        settle({
          command,
          cwd,
          code,
          signal: String(signal ?? ''),
          timedOut,
          cancelled,
          duration: Date.now() - started,
          stdout: redactSecretValues(stdout.text(), vars),
          stderr: redactSecretValues(stderr.text(), vars)
        })
      }
      running = {
        conversationId: String(conversationId ?? ''),
        stop: () => {
          if (settled) return
          cancelled = true
          this.kill(child)
          // 与超时同一口径：先收进程树，再立刻收尾——树没能在这一刻清干净，
          // 也不该让等着这一步的调用悬着（挂着的工具调用正是「卡住」的样子）
          finish(null, null)
        }
      }
      if (running.conversationId) this.commands.add(running)
      timer = setTimeout(() => {
        timedOut = true
        this.kill(child)
        // 立刻按超时收尾：进程树没能在这一刻清干净，也不该让这一步悬着
        finish(null, 'SIGKILL')
      }, timeout * 1000)
      child.stdout?.on('data', (chunk: Buffer) => stdout.push(chunk))
      child.stderr?.on('data', (chunk: Buffer) => stderr.push(chunk))
      // 启动失败（命令不存在、目录被删）走 error，正常结束走 close，两条路都要收尾
      child.on('error', (error) => {
        stderr.push(Buffer.from(`启动失败：${error.message}`, 'utf8'))
        finish(null, null)
      })
      child.on('close', (code, signal) => finish(code, signal))
    })
  }

  /**
   * 超时终止：Windows 上的 shell 不会带走子进程，用 taskkill 按进程树收掉——
   * 必须趁 shell 还活着（进程树是沿父进程找的，先把 shell 杀掉就找不到子进程了）；
   * 其余平台给 shell 发 SIGKILL，taskkill 调不起来时同样兜底 kill。
   */
  private kill(child: ChildProcess): void {
    const pid = child.pid
    if (pid && 'win32' === process.platform) {
      try {
        spawn('taskkill', ['/pid', String(pid), '/T', '/F'], { windowsHide: true })
        return
      } catch {
        // 调不起 taskkill 时走下面的 kill 兜底
      }
    }
    try {
      child.kill('SIGKILL')
    } catch {
      // 进程可能已经退出，忽略
    }
  }

  /** 解析并鉴权：返回命中项目清单里的哪个目录以及实际权限 */
  private async resolve(
    projectId: string,
    path: string,
    need: WorkspaceNeed,
    context: WorkspaceContext = {}
  ): Promise<{ path: string; root: WorkspaceRoot | null }> {
    const target = this.absolute(projectId, path)
    const hit = this.inside(projectId, target)
    if (hit) {
      if (!this.allowed(hit.permission, need)) {
        throw new Error(`权限不足，无法${NEED_LABELS[need]}：${target}`)
      }
      return { path: target, root: hit }
    }
    // 工作区之外：按访问策略决定放行还是拒绝，询问与判定都在这里完成
    await this.access(projectId, target, need, context)
    return { path: target, root: null }
  }

  /**
   * 路径归一：绝对路径原样使用，相对路径按项目声明的目录解析。
   * 检索顺序是主目录优先、其余按声明顺序（没标主目录时就是原来的顺序）：
   * 先取已经存在的那一份，都没有就落到主目录上——新建文件因此默认落在主目录里。
   * 这样模型既能直接写绝对路径，也能按工作区里的相对路径操作。
   */
  private absolute(projectId: string, path: string): string {
    const text = String(path ?? '').trim()
    if (!text) throw new Error('路径无效：' + path)
    if (isAbsolute(text)) {
      const target = resolve(text)
      if (target.length < 2) throw new Error('路径无效：' + path)
      return target
    }
    const roots = this.roots(projectId)
    if (roots.length < 1) throw new Error(`项目未声明工作区目录：${projectId}`)
    const primary = this.primaryOf(roots)
    const order = primary ? [primary, ...roots.filter((root) => root.path !== primary.path)] : roots
    for (const root of order) {
      const candidate = resolve(root.path, text)
      if (existsSync(candidate)) return candidate
    }
    return resolve((primary ?? roots[0]).path, text)
  }

  /** 命中项目清单里的哪个目录：不在任何目录内时返回 null */
  private inside(projectId: string, path: string): WorkspaceRoot | null {
    const target = resolve(String(path ?? ''))
    for (const root of this.roots(projectId)) {
      const base = resolve(root.path)
      if (target === base || target.startsWith(base + sep)) return root
    }
    return null
  }

  /**
   * 访问策略闸门：请求批准走审批通道（界面横幅），帮我批准直接放行，自动拒绝直接失败，
   * 完全访问权限等同放行。文件操作只在「工作区之外」时进来，命令则每次都进来（见 exec）。
   *
   * 两者的说法不同：文件的失败原因要讲清是路径越界，命令则要带上命令原文——
   * 命令的落点不限于工作目录，人得看着是哪条命令才能决定。
   */
  private async access(
    projectId: string,
    path: string,
    need: WorkspaceNeed,
    context: WorkspaceContext = {}
  ): Promise<void> {
    const command = String(context.command ?? '')
    // 命令：讲命令原文；文件：讲路径越界
    const action = command ? '执行命令' : `工作区外的${NEED_LABELS[need]}`
    const detail = command || path
    if ('deny' === this.policy) {
      throw new Error(`已按访问策略自动拒绝${action}：${detail}`)
    }
    if ('full' === this.policy || 'auto' === this.policy) return
    if (!this.approval) throw new Error(`界面未就绪，无法确认${action}：${detail}`)
    const request: WorkspaceApproval = {
      id: randomUUID(),
      projectId: String(projectId ?? ''),
      path,
      need
    }
    if (command) request.command = command
    // 会话标识一路带到申请上：界面据此把申请交回发起它的那段对话，互不串门
    if (context.conversationId) request.conversationId = String(context.conversationId)
    const allow = await this.approval(request)
    if (!allow) throw new Error(`${action}未获批准，已跳过：${detail}`)
  }

  private allowed(permission: WorkspacePermission, need: WorkspaceNeed): boolean {
    if ('readwrite' === permission) return true
    // 执行命令与写文件同级：命令能改的东西和写文件一样多，只读目录不放行
    return 'read' === need ? 'read' === permission : 'write' === permission
  }

  private permission(value: unknown): WorkspacePermission | null {
    const text = String(value ?? '').trim()
    if ('read' === text || 'write' === text || 'readwrite' === text) return text
    return null
  }

  private merge(
    current: WorkspacePermission | undefined,
    next: WorkspacePermission
  ): WorkspacePermission {
    if (!current || current === next) return next
    if ('readwrite' === current || 'readwrite' === next) return 'readwrite'
    return 'readwrite' // 既有读又有写等同于读写
  }
}
