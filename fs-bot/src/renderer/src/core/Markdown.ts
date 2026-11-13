import { marked, type Token, type Tokens } from 'marked'
import DOMPurify from 'dompurify'
import { localPath } from './LocalPath'

/**
 * Markdown 图片预处理：marked 只负责词法识别，最终渲染交给 Vditor。
 * 模型输出属于不可信内容，Vditor 转换结果必须过一遍 DOMPurify 再插入 DOM。
 * 渲染进程手里有 ipc 通道，正文里的脚本绝不能原样上屏。
 * 链接统一补 target="_blank"，交给主进程的窗口打开策略（外部浏览器打开）处理，
 * 否则点击一个普通链接会把整个渲染进程导航走。
 *
 * 知识库图片（形如 ![说明](kb:图片标识)）的标识在上屏前换成实时地址：地址由调用方按标识换取
 * 后随选项传进来（见 utils/KnowledgeImageUtil，**每条正文各自解析、不做跨消息缓存**），
 * 换不到地址时调用方也要给出一张默认图——正文里不会留下 kb: 这种浏览器不认识、只会渲染成破图的地址。
 */

export interface MarkdownOptions {
  /** 知识库图片标识 → 可展示地址（含解析期间的占位图与取不到地址时的默认图） */
  knowledgeImage?: (id: string) => string
  /** 本机图片路径 → 可展示地址（解析期间占位图、失败默认图，由调用方保证） */
  localImage?: (path: string) => string
}

/** 知识库图片标识：kb: 之后是图片标识（正文里存成 `占位地址#kb:标识`，也兼容只带标识的写法） */
const KNOWLEDGE_IMAGE = /(?:^|#)kb:([A-Za-z0-9_-]+)/
/** 本机图片的后缀：只把确实像图片的路径交给调用方去鉴权，普通相对链接仍按链接处理 */
const IMAGE_FILE = /\.(?:png|jpe?g|gif|webp|bmp|avif)$/i

/**
 * DOMPurify 的默认地址白名单 + 本机图片协议。
 * chat-image 的地址由主进程在鉴权后签发，渲染进程拼不出别的文件路径；file: 仍然不放行。
 */
const ALLOWED_URI =
  /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|sms|cid|xmpp|matrix|chat-image):|[^a-z]|[a-z+.-]+(?:[^a-z+.:-]|$))/i

/** 只注册一次（模块级），避免每个组件实例各挂一个钩子 */
DOMPurify.addHook('afterSanitizeAttributes', (node: Node) => {
  const element = node as Element
  if ('A' === element.tagName) {
    element.setAttribute('target', '_blank')
    element.setAttribute('rel', 'noopener noreferrer')
  }
})

/** Markdown 转换结果统一从这里清洗，Vditor 的 transform 回调也复用这一份白名单 */
export function sanitizeMarkdownHtml(html: string): string {
  return DOMPurify.sanitize(String(html ?? ''), {
    ADD_ATTR: ['target'],
    ALLOWED_URI_REGEXP: ALLOWED_URI
  })
}

/** 词法结果里可能继续嵌套 token 的字段（列表、引用、表格单元格等） */
interface NestedTokens {
  tokens?: Token[]
  items?: Token[]
  header?: Array<{ tokens?: Token[] }>
  rows?: Array<Array<{ tokens?: Token[] }>>
}

/**
 * marked 会把图片目标里的反斜杠当 Markdown 转义符处理。Windows 路径因此会被悄悄改坏，
 * 例如 `C:\Users\Lenovo\.fs-bot\a.png` 里的 `\.` 只剩 `.`，主进程随后找不到文件。
 *
 * 图片原始文本仍保留在 token.raw 里，所以统一在解析器开始前把本机图片目标恢复成原文；
 * http(s) 等普通图片地址不受影响。这里只恢复目的地，标题等 Markdown 语义仍交给 marked。
 */
function rawImageHref(raw: string): string {
  const value = String(raw ?? '')
  const marker = value.lastIndexOf('](')
  if (marker < 0 || !value.endsWith(')')) return ''
  const body = value.slice(marker + 2, -1).trim()
  if (!body) return ''

  // 尖括号写法允许路径里有空格：![说明](<C:\my screenshots\a.png>)
  if (body.startsWith('<')) {
    const end = body.indexOf('>')
    return end > 1 ? body.slice(1, end).trim() : ''
  }

  // 普通写法的目的地到第一个未转义的空白为止，空白之后是可选标题
  for (let index = 0; index < body.length; index++) {
    if (!/\s/.test(body[index])) continue
    let slashes = 0
    for (let cursor = index - 1; cursor >= 0 && body[cursor] === '\\'; cursor--) slashes++
    if (slashes % 2 === 0) return body.slice(0, index).trim()
  }
  return body.trim()
}

/** 递归恢复图片 token 的目标，其它 token 原样交给 marked */
function restoreImageHrefs(tokens: Token[]): void {
  for (const token of tokens) {
    if ('image' === token.type) {
      const href = rawImageHref(token.raw)
      if (href) (token as Tokens.Image).href = href
      continue
    }
    const nested = token as unknown as NestedTokens
    if (Array.isArray(nested.tokens)) restoreImageHrefs(nested.tokens)
    if (Array.isArray(nested.items)) restoreImageHrefs(nested.items)
    if (Array.isArray(nested.header)) {
      nested.header.forEach((cell) => cell.tokens && restoreImageHrefs(cell.tokens))
    }
    if (Array.isArray(nested.rows)) {
      nested.rows.forEach((row) =>
        row.forEach((cell) => cell.tokens && restoreImageHrefs(cell.tokens))
      )
    }
  }
}

/** 统一的词法入口：所有正文渲染与图片扫描都必须先经过这里，避免路径口径各算一遍 */
function tokensOf(text: string): Token[] {
  const tokens = marked.lexer(String(text ?? ''))
  restoreImageHrefs(tokens)
  return tokens
}

/**
 * 正文里的图片目的地：按 marked 的词法结果取，路径里的括号、反斜杠与标题不会被正则误切。
 * 只扫词法阶段识别出来的图片，代码块里的同形文本不是图片。
 */
function imageHrefs(text: string): string[] {
  return imageTokens(text).map((token) => String(token.href ?? ''))
}

/** 文档顺序里的图片 token（包括列表、引用、表格单元格里的图片） */
function imageTokens(text: string): Tokens.Image[] {
  const images: Tokens.Image[] = []
  const visit = (tokens: Token[]): void => {
    for (const token of tokens) {
      if ('image' === token.type) {
        images.push(token as Tokens.Image)
        continue
      }
      const nested = token as unknown as {
        tokens?: Token[]
        items?: Token[]
        header?: Array<{ tokens?: Token[] }>
        rows?: Array<Array<{ tokens?: Token[] }>>
      }
      if (Array.isArray(nested.tokens)) visit(nested.tokens)
      if (Array.isArray(nested.items)) visit(nested.items)
      if (Array.isArray(nested.header)) {
        nested.header.forEach((cell) => cell.tokens && visit(cell.tokens))
      }
      if (Array.isArray(nested.rows)) {
        nested.rows.forEach((row) => row.forEach((cell) => cell.tokens && visit(cell.tokens)))
      }
    }
  }
  visit(tokensOf(text))
  return images
}

/**
 * 把正文里的知识库 / 本机图片目的地替换成实际展示地址，再交给 Vditor。
 *
 * marked 在这里只负责识别图片节点与 Windows 路径，不参与 Vditor 的最终渲染。
 * `token.raw` 保留原始 Markdown，因此替换不会破坏用户写的说明和标题。
 */
export function resolveMarkdownImages(text: string, options: MarkdownOptions = {}): string {
  const value = String(text ?? '')
  if (!value) return ''
  let cursor = 0
  let result = ''
  for (const token of imageTokens(value)) {
    const raw = String(token.raw ?? '')
    if (!raw) continue
    const start = value.indexOf(raw, cursor)
    if (start < 0) continue
    const href = String(token.href ?? '')
    const id = options.knowledgeImage ? KNOWLEDGE_IMAGE.exec(href)?.[1] : null
    const path = options.localImage && !id ? localImagePath(href) : ''
    const target = id ? options.knowledgeImage!(id) : path ? options.localImage!(path) : ''
    if (!target) continue
    result += value.slice(cursor, start) + replaceImageTarget(raw, target)
    cursor = start + raw.length
  }
  return result + value.slice(cursor)
}

/** 只替换图片目的地，保留说明文字、括号写法和可选标题 */
function replaceImageTarget(raw: string, target: string): string {
  const marker = raw.lastIndexOf('](')
  if (marker < 0 || !raw.endsWith(')')) return raw
  const body = raw.slice(marker + 2, -1)
  let end = body.length
  if (body.startsWith('<')) {
    const close = body.indexOf('>')
    if (close > 0) end = close + 1
  } else {
    for (let index = 0; index < body.length; index++) {
      if (!/\s/.test(body[index])) continue
      let slashes = 0
      for (let cursor = index - 1; cursor >= 0 && body[cursor] === '\\'; cursor--) slashes++
      if (slashes % 2 === 0) {
        end = index
        break
      }
    }
  }
  let value = target.replace(/</g, '%3C').replace(/>/g, '%3E')
  if (/[\s()]/.test(value)) value = `<${value}>`
  return raw.slice(0, marker + 2) + value + body.slice(end) + ')'
}

/**
 * 图片目的地 → 本机路径：只认显式的 `local:`、`file:`、盘符 / UNC 与带图片后缀的相对路径；
 * http(s)、data:、kb: 等一律不接，仍由默认图片渲染处理。
 */
function localImagePath(href: string): string {
  const raw = String(href ?? '').trim()
  if (!raw) return ''
  const explicit = /^(?:local|file):/i.test(raw)
  const prefixed = /^local:/i.test(raw)
  let value = prefixed ? raw.slice('local:'.length).trim() : raw
  if (!value) return ''
  if (prefixed) {
    try {
      // local: 里的空格通常写成 %20；解不开就按原文找文件
      value = decodeURIComponent(value)
    } catch {
      // 保留原文
    }
  }
  const path = localPath(value)
  // 显式写了 local: / file: 的交给主进程判类型（不支持的格式由默认图兜底）；
  // 裸相对路径必须自带图片后缀，避免把普通链接误当成图片
  return path && (explicit || IMAGE_FILE.test(path)) ? path : ''
}

/**
 * 正文里引用到的知识库图片标识（按出现顺序去重）
 * 调用方拿去批量换取展示地址，渲染时再随选项交回来（见 MarkdownOptions）
 */
export function knowledgeImageIds(text: string): string[] {
  const ids: string[] = []
  for (const href of imageHrefs(text)) {
    const id = KNOWLEDGE_IMAGE.exec(href)?.[1]
    if (id) ids.push(id)
  }
  return Array.from(new Set(ids))
}

/** 正文里引用到的本机图片路径（按出现顺序去重），调用方拿去逐张鉴权并换取展示地址 */
export function localImagePaths(text: string): string[] {
  const paths: string[] = []
  for (const href of imageHrefs(text)) {
    const path = localImagePath(href)
    if (path) paths.push(path)
  }
  return Array.from(new Set(paths))
}
