import { marked, Renderer, type MarkedOptions, type Token, type Tokens } from 'marked'
import DOMPurify from 'dompurify'
import { highlightCode } from './Highlight'

/**
 * Markdown 渲染：模型输出属于不可信内容，marked 只做转换，结果必须过一遍 DOMPurify 再插入 DOM。
 * 渲染进程手里有 ipc 通道，正文里的脚本绝不能原样上屏。
 * 链接统一补 target="_blank"，交给主进程的窗口打开策略（外部浏览器打开）处理，
 * 否则点击一个普通链接会把整个渲染进程导航走。
 *
 * 知识库图片（形如 ![说明](kb:图片标识)）的标识在上屏前换成实时地址：地址由调用方按标识换取
 * 后随选项传进来（见 utils/KnowledgeImageUtil，**每条正文各自解析、不做跨消息缓存**），
 * 换不到地址时调用方也要给出一张默认图——正文里不会留下 kb: 这种浏览器不认识、只会渲染成破图的地址。
 */

marked.setOptions({ gfm: true, breaks: true })

/**
 * 渲染选项：marked 会把未识别的选项原样带进渲染器（类型里没有这一项，取值处从 this.options 收窄），
 * 这里补上知识库图片的解析入口
 */
export interface MarkdownOptions extends MarkedOptions {
  /** 知识库图片标识 → 可展示地址（含解析期间的占位图与取不到地址时的默认图） */
  knowledgeImage?: (id: string) => string
}

/**
 * 正文里的图片引用：先取括号里的那一截（目的地，可带标题），再按标识判断是不是知识库图片。
 * 要以 `)` 收尾才算数：流式输出时先冒出来的是没写完的半截引用，等它写完再认
 */
const IMAGE_DESTINATION = /!\[[^\]]*\]\(([^)\n]*)\)/g
/** 知识库图片标识：kb: 之后是图片标识（正文里存成 `占位地址#kb:标识`，也兼容只带标识的写法） */
const KNOWLEDGE_IMAGE = /(?:^|#)kb:([A-Za-z0-9_-]+)/

/** 渲染器上下文：这次渲染的选项挂在渲染器上 */
interface RendererContext {
  options?: MarkdownOptions
}

/** 自定义渲染：代码块自己着色，知识库图片把标识换成实时地址 */
marked.use({
  renderer: {
    /**
     * 代码块着色：顶层的围栏代码块由 ChatCode 单独渲染（见 markdownParts），
     * 这里管的是留在 Markdown 里的那些（写在列表 / 引用内部的），走同一套高亮与配色，
     * 免得同一个回复里两种代码块一个有色一个没色。
     */
    code(token: Tokens.Code): string {
      const info = String(token?.lang ?? '').trim()
      const name = info.split(/\s+/, 1)[0]
      const className = name ? ` language-${name}` : ''
      return `<pre><code class="hljs${className}">${highlightCode(token?.text ?? '', info)}</code></pre>`
    },
    /**
     * 图片：知识库图片先把标识换成实时地址，其余交给默认渲染
     * 只认词法阶段识别出来的图片，代码块里的同形文本不是图片、不会被换
     */
    image(this: RendererContext, token: Tokens.Image): string {
      const resolve = this?.options?.knowledgeImage
      const id = resolve ? KNOWLEDGE_IMAGE.exec(String(token?.href ?? ''))?.[1] : null
      const item = id ? { ...token, href: resolve!(id) } : token
      return Renderer.prototype.image.call(this as unknown as Renderer, item)
    }
  }
})

/** 只注册一次（模块级），避免每个组件实例各挂一个钩子 */
DOMPurify.addHook('afterSanitizeAttributes', (node: Node) => {
  const element = node as Element
  if ('A' === element.tagName) {
    element.setAttribute('target', '_blank')
    element.setAttribute('rel', 'noopener noreferrer')
  }
})

export function renderMarkdown(text: string, options: MarkdownOptions = {}): string {
  const value = String(text ?? '')
  if (!value) return ''
  const rendered = marked.parse(value, { ...options, async: false })
  return DOMPurify.sanitize(rendered, { ADD_ATTR: ['target'] })
}

/**
 * 正文里引用到的知识库图片标识（按出现顺序去重）
 * 调用方拿去批量换取展示地址，渲染时再随选项交回来（见 MarkdownOptions）
 */
export function knowledgeImageIds(text: string): string[] {
  const ids: string[] = []
  for (const matched of String(text ?? '').matchAll(IMAGE_DESTINATION)) {
    const id = KNOWLEDGE_IMAGE.exec(String(matched[1] ?? ''))?.[1]
    if (id) ids.push(id)
  }
  return Array.from(new Set(ids))
}

/** 正文分段：代码块单独取出交给组件渲染（行号、复制），其余仍是 Markdown 片段 */
export type MarkdownPart =
  | { type: 'text'; html: string }
  | { type: 'code'; lang: string; code: string }

/**
 * 按顶层词法单元把正文切成「普通片段 + 代码块」。
 * 连续的普通内容一起解析，避免切成多个 html 块后块级间距被断开。
 * 说明：只处理顶层的围栏代码块；写在列表 / 引用内部的那种仍按普通 Markdown 渲染。
 */
export function markdownParts(text: string, options: MarkdownOptions = {}): MarkdownPart[] {
  const value = String(text ?? '')
  if (!value) return []
  const parts: MarkdownPart[] = []
  let buffer: Token[] = []
  const flush = (): void => {
    if (!buffer.length) return
    /**
     * 必须带上 marked.defaults：marked 的 parser(tokens, options) 不会像 parse() 那样并入默认项，
     * 而 use() 注册的渲染器就在默认项里——漏掉它就退回内置渲染器，
     * 正文里的知识库图片换不到地址（还是 kb: 标识，被清洗掉后一片空白），代码块也不再着色
     */
    const html = marked.parser(buffer, { ...marked.defaults, ...options, async: false })
    parts.push({ type: 'text', html: DOMPurify.sanitize(html, { ADD_ATTR: ['target'] }) })
    buffer = []
  }
  for (const token of marked.lexer(value)) {
    if ('code' === token.type) {
      flush()
      parts.push({
        type: 'code',
        lang: String(token.lang ?? '').trim(),
        code: String(token.text ?? '')
      })
      continue
    }
    buffer.push(token)
  }
  flush()
  return parts
}
