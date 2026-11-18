import DOMPurify from 'dompurify'
import JSON5 from 'json5'
import { sanitizeMarkdownHtml } from './Markdown'

/**
 * Vditor 的静态资源随安装包发布，不能回落到它默认的 unpkg CDN。
 * 开发态由 Vite 插件从 node_modules 提供，构建时复制到 out/renderer/vditor。
 */
export function vditorCdn(): string {
  return new URL('vditor', document.baseURI).toString().replace(/\/$/, '')
}

export function vditorAsset(path: string): string {
  return `${vditorCdn()}/dist/${path.replace(/^\/+/, '')}`
}

const vditorScripts = new Map<string, Promise<void>>()

/**
 * 同一个运行时的多个消息可能同时挂载。不能仅凭同 ID 的 script 已插入 DOM
 * 就认为它执行完毕，否则后挂载的消息会在 window.mermaid 尚未出现时渲染失败。
 * 这里共享同一份加载 Promise，并显式等待 script 的 load / error 事件。
 */
export function loadVditorScript(
  path: string,
  id: string,
  isReady: () => boolean = () => false
): Promise<void> {
  if (isReady()) return Promise.resolve()
  const src = vditorAsset(path)
  const cached = vditorScripts.get(src)
  if (cached) return cached

  const existing = document.getElementById(id) as HTMLScriptElement | null
  const existingState = existing?.dataset.fsBotLoadState
  if (existingState === 'loaded') return Promise.resolve()

  let script = existingState === 'loading' ? existing : null
  if (!script) {
    existing?.remove()
    script = document.createElement('script')
    script.id = id
    script.src = src
    script.async = true
    script.dataset.fsBotLoadState = 'loading'
  }

  const task = new Promise<void>((resolveTask, rejectTask) => {
    script!.addEventListener(
      'load',
      () => {
        script!.dataset.fsBotLoadState = 'loaded'
        resolveTask()
      },
      { once: true }
    )
    script!.addEventListener(
      'error',
      () => {
        script!.remove()
        rejectTask(new Error(`加载失败：${src}`))
      },
      { once: true }
    )
    if (script!.parentNode !== document.head) document.head.appendChild(script!)
  })
  vditorScripts.set(src, task)
  task.catch(() => {
    if (vditorScripts.get(src) === task) vditorScripts.delete(src)
  })
  return task
}

const MAX_DIAGRAM_SOURCE = 256 * 1024

/**
 * Vditor's codeRender only reuses a code block when its inline max-height contains
 * a px unit. Preparing all remaining code blocks this way makes it skip generating
 * its CSP-incompatible inline copy control; ChatMarkdown supplies the copy button.
 */
export const VDITOR_CODE_RENDER_SKIP_HEIGHT = '100000px'

/**
 * Vditor 完成 Markdown → HTML 后、插入 DOM 前调用。
 *
 * Mermaid / ECharts 不交给 Vditor 内置渲染器：前者默认 securityLevel=loose，
 * 后者用 Function() 解析配置，在当前 Electron CSP 下既不应放宽也存在执行风险。
 * 这里先把它们换成普通占位块，正文仍走统一的 DOMPurify 白名单。
 */
export function prepareVditorHtml(html: string): string {
  const template = document.createElement('template')
  template.innerHTML = String(html ?? '')
  for (const [selector, kind] of [
    ['.language-mermaid', 'mermaid'],
    ['.language-echarts', 'echarts']
  ] as const) {
    template.content.querySelectorAll(selector).forEach((element) => {
      const source = String(element.textContent ?? '').trim()
      const block = document.createElement('div')
      const invalid = !source || source.length > MAX_DIAGRAM_SOURCE
      block.className = invalid ? 'fs-diagram-block' : `fs-diagram-block fs-${kind}-block`
      block.setAttribute('data-state', invalid ? 'error' : 'pending')
      const pre = document.createElement('pre')
      pre.className = 'fs-diagram-source'
      pre.textContent = source || '图表源码为空或超过 256 KiB'
      block.appendChild(pre)
      element.replaceWith(block)
    })
  }
  template.content.querySelectorAll<HTMLElement>('pre > code').forEach((code) => {
    code.style.maxHeight = VDITOR_CODE_RENDER_SKIP_HEIGHT
  })
  return sanitizeMarkdownHtml(template.innerHTML)
}

const SVG_NAMESPACE = 'http://www.w3.org/2000/svg'
const MERMAID_LABEL_LINE_HEIGHT = 18

/**
 * DOMPurify 处理字符串形式的 SVG 时会保留 foreignObject，却清空其 XHTML
 * 子节点；Mermaid 11 的节点标签恰好都放在 foreignObject 中，结果就是只剩空框。
 *
 * 在清洗前把标签降级成纯文本 SVG tspan：既能保留多行文字和布局，也不把
 * Mermaid 生成的任意 HTML 直接放回页面。清洗后仍按同一条不可信输出边界处理。
 */
function mermaidSvgTextLabels(svg: string): string {
  if (!svg.includes('foreignObject')) return svg
  // Mermaid 的 XHTML 标签使用 HTML 的 <br> 写法；按 XML 解析会把标签内容截断。
  const parsed = new DOMParser().parseFromString(svg, 'text/html')
  const root = parsed.querySelector('svg')
  if (!root) return svg
  const textMetrics = document.createElement('canvas').getContext('2d')
  if (textMetrics) {
    textMetrics.font = '16px "trebuchet ms", verdana, arial, sans-serif'
  }

  root.querySelectorAll('foreignObject').forEach((label) => {
    const lines: string[] = ['']
    const readText = (node: Node): void => {
      if (node.nodeType === Node.TEXT_NODE) {
        lines[lines.length - 1] += node.textContent ?? ''
        return
      }
      if (!(node instanceof Element)) return
      if (node.localName.toLowerCase() === 'br') {
        lines.push('')
        return
      }
      node.childNodes.forEach(readText)
    }
    label.childNodes.forEach(readText)

    const normalized = lines
      .map((line) => line.replace(/\s+/g, ' ').trim())
      .filter((line, index, all) => line || (index > 0 && index < all.length - 1))
    if (!normalized.length) {
      label.remove()
      return
    }

    const width = Number.parseFloat(label.getAttribute('width') ?? '') || 0
    const height = Number.parseFloat(label.getAttribute('height') ?? '') || 0
    const firstLineY = height / 2 - ((normalized.length - 1) * MERMAID_LABEL_LINE_HEIGHT) / 2
    const text = parsed.createElementNS(SVG_NAMESPACE, 'text')
    text.setAttribute('class', 'fs-mermaid-label')
    text.setAttribute('text-anchor', 'middle')
    text.setAttribute('dominant-baseline', 'middle')
    normalized.forEach((line, index) => {
      const tspan = parsed.createElementNS(SVG_NAMESPACE, 'tspan')
      tspan.setAttribute('x', String(width / 2))
      tspan.setAttribute('y', String(firstLineY + index * MERMAID_LABEL_LINE_HEIGHT))
      tspan.textContent = line || ' '
      // SVG text 不会自动限制在 foreignObject 的宽度内；只收窄确实超宽的行。
      if (textMetrics && width > 0 && textMetrics.measureText(line).width > width) {
        tspan.setAttribute('textLength', String(width))
        tspan.setAttribute('lengthAdjust', 'spacingAndGlyphs')
      }
      text.appendChild(tspan)
    })
    label.replaceWith(text)
  })

  return new XMLSerializer().serializeToString(root)
}

/** Mermaid 生成的是 SVG；仍按同一条“模型输出不可信”边界清洗 */
export function sanitizeMermaidSvg(svg: string): string {
  const source = mermaidSvgTextLabels(String(svg ?? ''))
  return DOMPurify.sanitize(source, {
    USE_PROFILES: { html: true, svg: true, svgFilters: true },
    ADD_ATTR: ['dominant-baseline', 'lengthAdjust', 'target', 'textLength'],
    ALLOWED_URI_REGEXP:
      /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|sms|cid|xmpp|matrix|chat-image):|[^a-z]|[a-z+.-]+(?:[^a-z.:-]|$))/i
  })
}

/**
 * Vditor 的只读预览会在转换之后给代码块注入带 onclick/onmouseover 的复制按钮，
 * 因而不受 transform 中的 DOMPurify 清洗约束。当前 CSP 禁止内联事件处理器，
 * 这里在 Vditor 完成渲染时统一移除，复制行为由 ChatMarkdown 自己的 Vue 按钮接管。
 */
export function removeInlineEventHandlers(root: ParentNode): void {
  root.querySelectorAll('*').forEach((element) => {
    for (const attribute of Array.from(element.attributes)) {
      if (attribute.name.toLowerCase().startsWith('on')) {
        element.removeAttribute(attribute.name)
      }
    }
  })
}

/**
 * ECharts 只接受数据配置，不执行模型给出的 JavaScript。
 * JSON5 负责覆盖单引号、尾逗号和注释等常见写法；函数表达式会在解析阶段直接失败。
 */
export function parseEChartsOption(source: string): Record<string, unknown> {
  let text = String(source ?? '').trim()
  text = text.replace(/^(?:const|let|var)?\s*option\s*=\s*/i, '').replace(/;\s*$/, '')
  if (!text) throw new Error('空的 ECharts 配置')
  const option = JSON5.parse(text)
  const plain = plainOption(option)
  if (!plain || 'object' !== typeof plain || Array.isArray(plain)) {
    throw new Error('ECharts 配置必须是对象')
  }
  const normalized = plain as Record<string, unknown>
  normalizeTitleSubtext(normalized)
  return optimizeEChartsLayout(normalized)
}

function plainOption(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(plainOption)
  if (!value || 'object' !== typeof value) return value
  const result: Record<string, unknown> = {}
  for (const [key, item] of Object.entries(value)) {
    if (['__proto__', 'prototype', 'constructor'].includes(key)) continue
    result[key] = plainOption(item)
  }
  return result
}

/** 模型常把 ECharts 的 subtext 写成 subtitle / subTitle；统一成标准字段。 */
function normalizeTitleSubtext(option: Record<string, unknown>): void {
  const titles = Array.isArray(option.title) ? option.title : option.title ? [option.title] : []
  titles.forEach((item) => {
    if (!isRecord(item)) return
    const alias = [item.subtitle, item.subTitle].find(
      (value) => 'string' === typeof value && Boolean(value.trim())
    )
    if (alias && !('string' === typeof item.subtext && item.subtext.trim())) {
      item.subtext = alias
    }
    delete item.subtitle
    delete item.subTitle
  })
}

const TITLE_ONLY_LEGEND_TOP = 36
const TITLE_WITH_SUBTITLE_LEGEND_TOP = 60
const TITLE_ONLY_GRID_TOP = 64
const TITLE_WITH_SUBTITLE_GRID_TOP = 96

/**
 * 统一整理标题与图例的基础留白：
 * - 顶部 / 左侧图例默认放到标题块下方，避免只有标题或增加了副标题时挤在一起；
 * - 已经明确放在足够靠下位置的图例，以及右侧独立图例保持调用方布局；
 * - 直角坐标图为标题和 yAxis.name 留出独立空间；
 * - 饼图未指定 center 时略向下移，给标题区域留出呼吸空间。
 */
function optimizeEChartsLayout(option: Record<string, unknown>): Record<string, unknown> {
  const title = titleState(option.title)
  if (!title.present) return option

  const safeLegendTop = title.subtitle ? TITLE_WITH_SUBTITLE_LEGEND_TOP : TITLE_ONLY_LEGEND_TOP
  const legends = Array.isArray(option.legend)
    ? option.legend
    : option.legend
      ? [option.legend]
      : []
  legends.forEach((item) => {
    if (!isRecord(item) || !legendNeedsTitleClearance(item, safeLegendTop)) return
    item.top = safeLegendTop
  })
  optimizeGridLayout(option, title.subtitle)

  const series = Array.isArray(option.series) ? option.series : option.series ? [option.series] : []
  series.forEach((item) => {
    if (!isRecord(item) || item.type !== 'pie' || item.center !== undefined) return
    item.center = ['50%', '53%']
  })
  return option
}

function titleState(value: unknown): { present: boolean; subtitle: boolean } {
  const titles = Array.isArray(value) ? value : value ? [value] : []
  let present = false
  let subtitle = false
  for (const item of titles) {
    if (!isRecord(item)) continue
    present ||= Boolean(item.text || item.subtext)
    subtitle ||= Boolean(item.subtext)
  }
  return { present, subtitle }
}

function legendNeedsTitleClearance(legend: Record<string, unknown>, safeTop: number): boolean {
  if (legend.top === 'bottom') return false
  if (legend.bottom !== undefined) return false
  if (legend.right !== undefined && legend.left === undefined) return false
  if ('number' === typeof legend.top && legend.top >= safeTop) return false
  let percentageTop = false
  if ('string' === typeof legend.top && /^\d+(?:\.\d+)?%$/.test(legend.top)) {
    const percent = Number.parseFloat(legend.top)
    if (percent >= 15) return false
    percentageTop = true
  }
  return (
    legend.top === undefined ||
    legend.top === 'auto' ||
    legend.top === 'top' ||
    legend.top === 'middle' ||
    percentageTop ||
    ('number' === typeof legend.top && legend.top < safeTop)
  )
}

function optimizeGridLayout(option: Record<string, unknown>, subtitle: boolean): void {
  if (option.xAxis === undefined && option.yAxis === undefined) return
  const safeTop = subtitle ? TITLE_WITH_SUBTITLE_GRID_TOP : TITLE_ONLY_GRID_TOP
  const grids = Array.isArray(option.grid) ? option.grid : option.grid ? [option.grid] : []
  if (!grids.length) {
    option.grid = { top: safeTop }
    return
  }
  grids.forEach((item) => {
    if (!isRecord(item)) return
    if ('number' === typeof item.top && item.top >= safeTop) return
    if ('string' === typeof item.top && /^\d+(?:\.\d+)?%$/.test(item.top)) {
      const percent = Number.parseFloat(item.top)
      const safePercent = Math.ceil((safeTop / 440) * 100)
      if (percent >= safePercent) return
    }
    item.top = safeTop
  })
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && 'object' === typeof value && !Array.isArray(value)
}

/** 正文里的图源代码：渲染成功后会被 Mermaid SVG 或 ECharts canvas 替换 */
export function diagramSource(block: Element): string {
  return String(block.querySelector('.fs-diagram-source')?.textContent ?? '').trim()
}

/** 渲染失败时保留原始源码，便于用户复制和排查 */
export function markDiagramError(block: HTMLElement, error: unknown): void {
  block.dataset.state = 'error'
  block.setAttribute(
    'title',
    error instanceof Error ? error.message : String(error ?? '图表渲染失败')
  )
}
