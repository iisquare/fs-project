<script setup lang="ts">
/**
 * Markdown 编辑器 - 基于 Vditor 的所见即所得 Markdown 编辑/预览组件（通用组件）
 *
 * 图片能力通过可选属性注入，组件本身不依赖任何业务接口：
 * - imageScheme   正文中图片标识的协议名，默认 kb，形如 ![说明](kb:标识)
 * - resolveImages 批量解析图片标识为可展示地址，缺省时不做处理
 * - uploadImage   上传图片并返回 { id, url, alt }，缺省时禁用图片上传
 * - loadingImage  地址解析完成前的占位图片，需为可访问的静态资源
 * - fallbackImage 解析不到地址时的兜底图片
 *
 * 渲染态不直接使用标识作为 src：标识会先换成 loadingImage#协议:标识 这样的占位地址，
 * 避免浏览器按未知协议加载失败，保存时再原样还原，正文里始终只有标识
 * 只读态双击图片打开查看器，同一段正文里的图片按出现顺序左右切换；编辑态单击图片同样可预览
 *
 * @v-model  {String}   内容（双向绑定主值）
 * @prop     {Boolean}  readonly     - 是否只读（仅预览），默认 false
 * @prop     {Number}   height       - 编辑器高度(px)，默认 400
 * @prop     {String}   placeholder  - 占位提示文本
 * @prop     {Boolean}  resizable    - 是否允许拖拽调整高度，默认 false
 *
 * @example
 * <markdown-editor v-model="content" :resolve-images="resolve" :upload-image="upload" />
 */
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import DOMPurify from 'dompurify'
import JSON5 from 'json5'
import { ElScrollbar } from 'element-plus'
import { h, nextTick, onBeforeUnmount, onMounted, ref, render as renderVNode, watch } from 'vue'

const model: any = defineModel()
const {
  readonly = false,
  height = 400,
  placeholder = '',
  resizable = false,
  imageScheme = 'kb',
  loadingImage = '/images/loading.svg',
  fallbackImage = '/images/no-permit.png',
  resolveImages,
  uploadImage,
  streaming = false,
} = defineProps({
  readonly: { type: Boolean, required: false },
  height: { type: Number, required: false },
  placeholder: { type: String, required: false },
  resizable: { type: Boolean, required: false },
  imageScheme: { type: String, required: false },
  loadingImage: { type: String, required: false },
  fallbackImage: { type: String, required: false },
  resolveImages: { type: Function, required: false },
  uploadImage: { type: Function, required: false },
  streaming: { type: Boolean, required: false },
})

const editorRef = ref<HTMLDivElement>()
const previewRef = ref<HTMLDivElement>()
let vditor: Vditor | null = null
let observer: MutationObserver | null = null
let refreshScheduled = false
let previewSequence = 0
let previewTimer: number | null = null
let previewBusy = false
let previewDirty = false
let pendingPreviewContent = ''
const imageUrls: Record<string, string> = {}
const PREVIEW_STREAM_INTERVAL = 80

interface EChartsInstance {
  setOption(option: Record<string, unknown>): void
  resize(): void
  dispose(): void
}

interface EChartsRuntime {
  init(element: HTMLElement): EChartsInstance
}

const chartInstances: EChartsInstance[] = []
const chartObservers: ResizeObserver[] = []
const tableScrollHosts: HTMLElement[] = []
const codeScrollHosts: HTMLElement[] = []
let chartsReady: Promise<EChartsRuntime> | null = null

const CODE_RENDER_SKIP_HEIGHT = '100000px'
const MAX_ECHARTS_SOURCE = 256 * 1024

const isRecord = (value: unknown): value is Record<string, unknown> =>
  Boolean(value) && 'object' === typeof value && !Array.isArray(value)

const plainOption = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(plainOption)
  if (!value || 'object' !== typeof value) return value
  const result: Record<string, unknown> = {}
  Object.entries(value).forEach(([key, item]) => {
    if (['__proto__', 'prototype', 'constructor'].includes(key)) return
    result[key] = plainOption(item)
  })
  return result
}

/**
 * ECharts 只接收数据配置；JSON5 兼容模型常见的单引号、尾逗号和注释，
 * 函数表达式会在解析阶段直接失败，不会作为脚本执行。
 */
const parseChartOption = (source: string): Record<string, unknown> => {
  const text = String(source ?? '')
    .trim()
    .replace(/^(?:const|let|var)?\s*option\s*=\s*/i, '')
    .replace(/;\s*$/, '')
  if (!text) throw new Error('图表配置为空')
  const parsed = plainOption(JSON5.parse(text))
  if (!isRecord(parsed)) throw new Error('图表配置必须是对象')
  return optimizeChartLayout(parsed)
}

const titleState = (value: unknown): { present: boolean, subtitle: boolean } => {
  const titles = Array.isArray(value) ? value : value ? [value] : []
  let present = false
  let subtitle = false
  titles.forEach((item) => {
    if (!isRecord(item)) return
    const alias = [item.subtitle, item.subTitle].find(
      (value) => 'string' === typeof value && Boolean(value.trim()),
    )
    if (alias && !('string' === typeof item.subtext && item.subtext.trim())) item.subtext = alias
    delete item.subtitle
    delete item.subTitle
    present ||= Boolean(item.text || item.subtext)
    subtitle ||= Boolean(item.subtext)
  })
  return { present, subtitle }
}

/** 统一标题、图例和绘图区的留白，避免模型生成的配置把元素挤在一起。 */
const optimizeChartLayout = (option: Record<string, unknown>): Record<string, unknown> => {
  const title = titleState(option.title)
  if (!title.present) return option

  const safeLegendTop = title.subtitle ? 60 : 36
  const legends = Array.isArray(option.legend) ? option.legend : option.legend ? [option.legend] : []
  legends.forEach((legend) => {
    if (!isRecord(legend) || legend.bottom !== undefined) return
    if (legend.right !== undefined && legend.left === undefined) return
    if ('number' === typeof legend.top && legend.top >= safeLegendTop) return
    if ('string' === typeof legend.top && /^\d+(?:\.\d+)?%$/.test(legend.top)) {
      if (Number.parseFloat(legend.top) >= 15) return
    }
    legend.top = safeLegendTop
  })

  if (option.xAxis !== undefined || option.yAxis !== undefined) {
    const safeGridTop = title.subtitle ? 96 : 64
    const grids = Array.isArray(option.grid) ? option.grid : option.grid ? [option.grid] : []
    if (!grids.length) {
      option.grid = { top: safeGridTop }
    } else {
      grids.forEach((grid) => {
        if (!isRecord(grid)) return
        if ('number' === typeof grid.top && grid.top >= safeGridTop) return
        grid.top = safeGridTop
      })
    }
  }

  const series = Array.isArray(option.series) ? option.series : option.series ? [option.series] : []
  const pieLegend: string[] = []
  series.forEach((item) => {
    if (!isRecord(item) || item.type !== 'pie') return
    const data = Array.isArray(item.data) ? item.data : []
    const names = data
      .map((entry) => isRecord(entry) ? String(entry.name ?? '').trim() : '')
      .filter(Boolean)
    pieLegend.push(...names)
    if (data.length > 4) {
      // 窄气泡里外侧标签会互相遮挡；改用可滚动图例，悬浮或点击时仍可看单项数值。
      const label = isRecord(item.label) ? item.label : {}
      if (label.show !== true) label.show = false
      const labelLine = isRecord(item.labelLine) ? item.labelLine : {}
      if (label.show !== true) labelLine.show = false
      item.label = label
      item.labelLine = labelLine
      item.avoidLabelOverlap = true
    }
    if (item.center === undefined) item.center = ['50%', '53%']
    if (item.itemStyle === undefined) item.itemStyle = { borderColor: '#fff', borderWidth: 2 }
  })
  if (pieLegend.length > 4 && option.legend === undefined) {
    option.legend = {
      type: 'scroll',
      bottom: 2,
      left: 'center',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 12,
      textStyle: { fontSize: 11, color: '#606266' },
    }
  }
  if (series.some((item) => isRecord(item) && item.type === 'pie') && option.tooltip === undefined) {
    option.tooltip = { trigger: 'item', confine: true }
  }
  return option
}

/**
 * Vditor 的动态资源必须和已安装包使用同一版本。
 * 版本号由 Vite 从 node_modules/vditor/package.json 注入，路径与构建产物一一对应。
 */
const vditorCdn = `${import.meta.env.BASE_URL.replace(/\/$/, '')}/vditor/${__VDITOR_VERSION__}`

const schemeName = () => (imageScheme || 'kb').replace(/[^A-Za-z0-9_-]/g, '')
const escapeRe = (text: string) => (text || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/**
 * 图片引用匹配，每次返回新实例，避免正则的 lastIndex 相互影响
 */
const imagePattern = () => new RegExp(`\\]\\([^)]*${schemeName()}:([A-Za-z0-9_-]+)\\)`, 'g')
const placeholderPrefix = () => `${loadingImage}#${schemeName()}:`
const placeholderPattern = () => new RegExp(`\\]\\(${escapeRe(loadingImage)}#${schemeName()}:([A-Za-z0-9_-]+)\\)`, 'g')

/**
 * 进入编辑器：图片标识替换为可解析的占位地址
 */
const normalizeIn = (content: string) => {
  if (!content || !loadingImage) return content ?? ''
  return (content ?? '').replace(imagePattern(), (all: string, id: string) => {
    return `](${loadingImage}#${schemeName()}:${id})`
  })
}

/**
 * 出编辑器：占位地址还原为图片标识
 * 取值出口必须统一走这里，否则占位地址会被写回正文
 */
const normalizeOut = (content: string) => {
  if (!content) return content ?? ''
  let result = content
  // 兜底：图片弹层里若被改成解析后的真实地址，还原为占位引用，避免绝对地址写入正文
  for (const [id, url] of Object.entries(imageUrls)) {
    if (!url) continue
    result = result.split(`](${url})`).join(`](${placeholderPrefix()}${id})`)
  }
  return result
}

const getContent = () => {
  return vditor ? normalizeOut(vditor.getValue()) : (model.value ?? '')
}

const setContent = (content: string) => {
  vditor?.setValue(normalizeIn(content ?? ''))
}

/**
 * 收集正文中尚未取得展示地址的图片标识
 */
const unresolvedIds = (content: string) => {
  const ids: string[] = []
  for (const matched of (content ?? '').matchAll(imagePattern())) {
    if (!imageUrls[matched[1]]) ids.push(matched[1])
  }
  return Array.from(new Set(ids))
}

/**
 * 批量解析图片展示地址，未返回的图片回落到兜底图
 */
const resolve = async (content: string) => {
  const ids = unresolvedIds(content)
  if (typeof resolveImages !== 'function' || ids.length === 0) return
  try {
    const data = await (resolveImages as any)(ids) || {}
    ids.forEach((id: string) => { imageUrls[id] = data[id] || fallbackImage })
  } catch {
    ids.forEach((id: string) => { imageUrls[id] = fallbackImage })
  }
}

/**
 * 为编辑区中的占位图片补充展示地址
 * 只动附加类名与内联自定义属性，不修改 src；这些内容在保存时会被 Markdown 转换丢弃，标识不受影响
 */
const decorate = () => {
  const element = editorRef.value
  if (!element) return
  const prefix = placeholderPrefix()
  element.querySelectorAll<HTMLImageElement>('img[src*="#"]').forEach(item => {
    const src = item.getAttribute('src') || ''
    if (!src.startsWith(prefix)) return
    const url = imageUrls[src.slice(prefix.length)] || fallbackImage
    item.classList.add('md-image')
    item.style.setProperty('--md-image', `url("${url}")`)
  })
}

/**
 * 只读预览：渲染前把图片标识替换为实时地址，预览结果不回流正文，可安全改写
 */
/**
 * 软换行转硬换行：Markdown 里单个换行默认渲染成空格，而模型输出通常按行排版，
 * 这里给行尾补两个空格让换行生效（代码块内部与块级语法行不动，避免污染）
 */
const hardBreaks = (content: string) => {
  let fence = false
  return String(content ?? '').split('\n').map((line) => {
    if (/^\s*(```|~~~)/.test(line)) {
      fence = !fence
      return line
    }
    if (fence || '' === line.trim()) return line
    // 表格/标题/列表/引用等块级语法行本身就有换行语义，不再补
    if (/^\s*([-*+]|\d+\.|>|#{1,6}\s|\||:::)/.test(line)) return line
    return line + '  '
  }).join('\n')
}

/**
 * Vditor 默认会把 echarts 代码块交给它内置的 ECharts 运行时，并使用动态
 * Function 解析配置。这里先换成受控占位块，再由本组件用 JSON5 + ECharts 实例渲染，
 * 同时统一关闭 Vditor 自带的代码复制按钮，避免和标题栏里的复制入口重复。
 */
const preparePreviewHtml = (html: string): string => {
  const template = document.createElement('template')
  template.innerHTML = String(html ?? '')
  template.content.querySelectorAll<HTMLElement>('.language-echarts').forEach((element) => {
    const source = String(element.textContent ?? '').trim()
    const threshold = source.length > MAX_ECHARTS_SOURCE
    const block = document.createElement('div')
    block.className = 'fs-markdown-echarts'
    block.dataset.state = threshold ? 'error' : 'pending'
    if (threshold) block.title = '图表配置超过 256 KiB'
    const sourceBlock = document.createElement('pre')
    sourceBlock.className = 'fs-markdown-echarts-source'
    sourceBlock.textContent = threshold ? '图表配置超过 256 KiB' : source || '图表配置为空'
    block.appendChild(sourceBlock)
    element.replaceWith(block)
  })
  template.content.querySelectorAll<HTMLElement>('pre > code').forEach((code) => {
    code.style.maxHeight = CODE_RENDER_SKIP_HEIGHT
  })
  return DOMPurify.sanitize(template.innerHTML)
}

const cleanupPreviewEnhancements = () => {
  chartObservers.splice(0).forEach((observer) => observer.disconnect())
  chartInstances.splice(0).forEach((chart) => chart.dispose())
  tableScrollHosts.splice(0).forEach((host) => renderVNode(null, host))
  codeScrollHosts.splice(0).forEach((host) => renderVNode(null, host))
}

const loadECharts = (): Promise<EChartsRuntime> => {
  if (!chartsReady) {
    chartsReady = import('echarts').then((runtime) => ({ init: runtime.init }))
    chartsReady.catch(() => {
      chartsReady = null
    })
  }
  return chartsReady
}

const waitForHighlight = async (root: HTMLElement): Promise<void> => {
  const codes = Array.from(root.querySelectorAll<HTMLElement>('pre > code'))
  if (!codes.length) return
  const deadline = performance.now() + 1600
  while (performance.now() < deadline) {
    if (codes.every((code) => code.classList.contains('hljs') && code.classList.contains('vditor-linenumber'))) {
      return
    }
    await new Promise<void>((resolve) => window.setTimeout(resolve, 32))
  }
}

const renderCharts = async (root: HTMLElement, sequence: number): Promise<void> => {
  const blocks = Array.from(root.querySelectorAll<HTMLElement>('.fs-markdown-echarts'))
  if (!blocks.length) return
  const runtime = await loadECharts()
  for (const block of blocks) {
    if (sequence !== previewSequence || !block.isConnected) return
    if (block.dataset.state === 'error') continue
    try {
      const source = String(block.querySelector('.fs-markdown-echarts-source')?.textContent ?? '').trim()
      const option = parseChartOption(source)
      block.replaceChildren()
      block.dataset.state = 'ready'
      block.removeAttribute('title')
      const chart = runtime.init(block)
      chart.setOption(option)
      chartInstances.push(chart)
      if (typeof ResizeObserver !== 'undefined') {
        const observer = new ResizeObserver(() => chart.resize())
        observer.observe(block)
        chartObservers.push(observer)
      }
    } catch (error) {
      // 流式输出中的配置通常还没闭合，保持占位而不是频繁闪红。
      if (streaming) {
        block.dataset.state = 'pending'
        continue
      }
      block.dataset.state = 'error'
      block.setAttribute('title', error instanceof Error ? error.message : String(error))
    }
  }
}

const mountTables = (root: HTMLElement) => {
  root.querySelectorAll<HTMLTableElement>('table').forEach((table) => {
    if (!table.parentNode) return
    const host = document.createElement('div')
    host.className = 'fs-markdown-table'
    table.replaceWith(host)
    tableScrollHosts.push(host)
    renderVNode(
      h(
        ElScrollbar,
        {
          class: 'fs-markdown-table-scroll',
          tabindex: 0,
          role: 'region',
          'aria-label': '可横向滚动的表格',
        },
        {
          default: () => h('div', {
            class: 'fs-markdown-table-content',
            innerHTML: table.outerHTML,
          }),
        },
      ),
      host,
    )
  })
}

const codeLanguage = (code: HTMLElement): string => {
  const language = Array.from(code.classList)
    .find((name) => name.startsWith('language-'))
    ?.slice('language-'.length)
  return language || '代码'
}

const codeText = (code: HTMLElement): string => {
  const clone = code.cloneNode(true) as HTMLElement
  clone
    .querySelectorAll('.vditor-linenumber__temp, .vditor-linenumber__rows')
    .forEach((element) => element.remove())
  return String(clone.textContent ?? '').replace(/\n$/, '')
}

const writeClipboard = async (text: string): Promise<void> => {
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.cssText = 'position:fixed;left:-100000px;top:0;'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    textarea.remove()
  }
}

const mountCodeBlocks = (root: HTMLElement) => {
  root.querySelectorAll<HTMLPreElement>('pre').forEach((pre) => {
    const code = pre.querySelector<HTMLElement>(':scope > code')
    if (!code || !pre.parentNode) return
    const language = codeLanguage(code)
    pre.querySelector<HTMLElement>(':scope > .vditor-copy')?.remove()
    const block = document.createElement('div')
    block.className = 'fs-markdown-code'
    block.dataset.language = language

    const shell = document.createElement('div')
    shell.className = 'fs-markdown-code-shell'
    const header = document.createElement('div')
    header.className = 'fs-markdown-code-header'
    const type = document.createElement('span')
    type.className = 'fs-markdown-code-type'
    const dot = document.createElement('span')
    dot.className = 'fs-markdown-code-dot'
    dot.setAttribute('aria-hidden', 'true')
    type.append(dot, document.createTextNode(language))

    const button = document.createElement('button')
    button.type = 'button'
    button.className = 'fs-markdown-code-copy'
    button.dataset.markdownCopy = ''
    button.title = '复制代码'
    button.setAttribute('aria-label', '复制代码')
    button.innerHTML = `
      <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
        <rect x="5" y="5" width="7.5" height="8" rx="1.3" fill="none" stroke="currentColor" stroke-width="1.2"></rect>
        <path d="M3.5 10.7V3.8c0-.7.6-1.3 1.3-1.3h5.7" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"></path>
      </svg>
      <span>复制</span>
    `
    header.append(type, button)

    const body = document.createElement('div')
    body.className = 'fs-markdown-code-body-host'
    pre.replaceWith(block)
    shell.append(header, body)
    block.appendChild(shell)
    codeScrollHosts.push(body)
    renderVNode(
      h(
        ElScrollbar,
        {
          class: 'fs-markdown-code-body',
          maxHeight: 'min(520px, 55vh)',
          tabindex: 0,
          role: 'region',
          'aria-label': `${language} 代码`,
        },
        {
          default: () => h('div', {
            class: 'fs-markdown-code-content',
            innerHTML: pre.outerHTML,
          }),
        },
      ),
      body,
    )
  })
}

const loadPreview = async (content: string) => {
  const host = previewRef.value
  if (!host) return
  const sequence = ++previewSequence
  if (!content) {
    cleanupPreviewEnhancements()
    const empty = document.createElement('span')
    empty.className = 'fs-markdown-empty'
    empty.textContent = '暂无内容'
    host.replaceChildren(empty)
    return
  }
  // 兼容内容里残留占位地址的情况，先统一还原为标识再解析
  const html = hardBreaks(normalizeOut(content)).replace(imagePattern(), (all: string, id: string) => {
    return `](${imageUrls[id] || fallbackImage})`
  })
  const body = document.createElement('div')
  body.className = 'fs-markdown-preview-body'
  try {
    // 先解析到离屏节点。流式输出时旧内容保持可见，避免每次增量都先塌陷再撑开。
    await Vditor.preview(body, html, {
      cdn: vditorCdn,
      mode: 'light',
      hljs: {
        enable: true,
        lineNumber: true,
        style: 'github',
      },
      transform: preparePreviewHtml,
    })
    if (sequence !== previewSequence) return
    // 代码高亮和图表通常在最终内容上才稳定，流式期间不等待它们，避免阻塞下一帧。
    if (!streaming) {
      await waitForHighlight(body)
      if (sequence !== previewSequence) return
    }
    cleanupPreviewEnhancements()
    host.replaceChildren(body)
    mountTables(body)
    mountCodeBlocks(body)
    if (!streaming) await renderCharts(body, sequence)
  } catch (error) {
    if (sequence !== previewSequence) return
    cleanupPreviewEnhancements()
    const failed = document.createElement('div')
    failed.className = 'fs-markdown-error'
    failed.textContent = `Markdown 渲染失败：${error instanceof Error ? error.message : String(error)}`
    host.replaceChildren(failed)
  }
}

const refresh = async (content?: string) => {
  const text = content ?? getContent()
  await resolve(text)
  if (readonly) {
    await loadPreview(text)
  } else {
    nextTick(() => decorate())
  }
}

const clearPreviewTimer = () => {
  if (previewTimer === null) return
  window.clearTimeout(previewTimer)
  previewTimer = null
}

/**
 * 串行合并流式重绘：最多每 80ms 解析一次最新内容，上一帧未结束时只记脏标记，
 * 不再为每个 token 启动一轮 Markdown 解析和 DOM 替换。输出结束时立即补最终一帧。
 */
const runPreviewQueue = async () => {
  if (previewBusy || !previewDirty) return
  previewBusy = true
  try {
    while (previewDirty) {
      previewDirty = false
      await refresh(pendingPreviewContent)
      if (previewDirty && streaming) {
        await new Promise<void>((resolve) => window.setTimeout(resolve, PREVIEW_STREAM_INTERVAL))
      }
    }
  } finally {
    previewBusy = false
    if (previewDirty) schedulePreviewRefresh(pendingPreviewContent)
  }
}

const schedulePreviewRefresh = (content: string) => {
  pendingPreviewContent = content
  previewDirty = true
  if (previewBusy) return
  if (!streaming) {
    clearPreviewTimer()
    void runPreviewQueue()
    return
  }
  if (previewTimer !== null) return
  previewTimer = window.setTimeout(() => {
    previewTimer = null
    void runPreviewQueue()
  }, PREVIEW_STREAM_INTERVAL)
}

/**
 * 编辑区 DOM 变化时重新装饰图片
 * 切换编辑模式、撤销重做等操作会重建 DOM，此时需要重新套用已解析的地址
 */
const scheduleRefresh = () => {
  if (refreshScheduled) return
  refreshScheduled = true
  requestAnimationFrame(() => {
    refreshScheduled = false
    refresh()
  })
}


const previewVisible = ref(false)
const previewUrlList = ref<string[]>([])
const previewIndex = ref(0)

/**
 * 自定义图片预览
 * Vditor 自带预览层用的是 img 的 src 属性（编辑器里是占位地址），且点击即关闭；
 * 这里改用解析后的真实地址，并交给 Element Plus 的查看器承载层级与关闭行为
 */
const openPreview = (element: HTMLElement) => {
  const image = element as HTMLImageElement
  const source = image.getAttribute('src') || ''
  const prefix = placeholderPrefix()
  const id = source.startsWith(prefix) ? source.slice(prefix.length) : ''
  const current = id ? imageUrls[id] : imageUrls[source]
  if (!current) return
  const urls = Object.values(imageUrls).filter(Boolean) as string[]
  previewUrlList.value = urls.length > 0 ? urls : [current]
  previewIndex.value = Math.max(0, previewUrlList.value.indexOf(current))
  previewVisible.value = true
}

const handlePreviewClick = (event: MouseEvent) => {
  const button = (event.target as Element | null)?.closest?.<HTMLButtonElement>('[data-markdown-copy]')
  if (!button) return
  const code = button.closest('.fs-markdown-code')?.querySelector('pre > code')
  if (!code) return
  event.preventDefault()
  event.stopImmediatePropagation()
  void writeClipboard(codeText(code as HTMLElement)).then(() => {
    button.classList.add('is-copied')
    button.title = '已复制代码'
    button.setAttribute('aria-label', '已复制代码')
    const label = button.querySelector('span')
    if (label) label.textContent = '已复制'
    window.setTimeout(() => {
      if (!button.isConnected) return
      button.classList.remove('is-copied')
      button.title = '复制代码'
      button.setAttribute('aria-label', '复制代码')
      if (label) label.textContent = '复制'
    }, 1500)
  }).catch(() => {})
}

/**
 * 只读预览的图片查看
 * 图片地址取自渲染结果（知识库图片已按当前授权实时签发），双击图片打开查看器；
 * 同一段 Markdown 里的图片按出现顺序一起传入，查看器里可左右切换
 */
const handlePreviewDblClick = (event: MouseEvent) => {
  const image = (event.target as Element | null)?.closest?.('img') as HTMLImageElement | null
  const source = image?.getAttribute('src') || ''
  // 兜底图不是内容本身，双击它没有可放大的东西
  if (!source || source === fallbackImage) return
  const urls = Array.from(previewRef.value?.querySelectorAll('img') ?? [])
    .map((item) => item.getAttribute('src') || '')
    .filter((url) => !!url && url !== fallbackImage)
  previewUrlList.value = urls.length > 0 ? urls : [source]
  previewIndex.value = Math.max(0, previewUrlList.value.indexOf(source))
  previewVisible.value = true
}

const observeEditor = () => {
  const element = editorRef.value
  if (!element || observer) return
  observer = new MutationObserver(() => scheduleRefresh())
  observer.observe(element, { childList: true, subtree: true, attributes: true, attributeFilter: ['src'] })
}

const loadEditor = () => {
  if (!editorRef.value) return
  vditor = new Vditor(editorRef.value, {
    cdn: vditorCdn,
    height,
    mode: 'wysiwyg',
    lang: 'zh_CN',
    value: normalizeIn(model.value ?? ''),
    placeholder: placeholder,
    cache: { enable: false },
    image: { preview: openPreview } as any,
    // Vditor 未对 customWysiwygToolbar 作空值保护，未提供时点击图片等元素会抛异常
    customWysiwygToolbar: (type: string, element: HTMLElement) => {
      // 图片标识由正文维护，不允许在弹层里改写 src
      if (type !== 'image' || !element) return
      const input = element.querySelector('input')
      if (input) input.setAttribute('readonly', 'readonly')
    },
    toolbar: [
      'headings', 'bold', 'italic', 'strike', '|',
      'list', 'ordered-list', 'check', '|',
      'quote', 'code', 'inline-code', 'table', 'line', '|',
      'link', 'upload', '|',
      'undo', 'redo', '|',
      'edit-mode', 'outline', 'fullscreen',
    ],
    counter: {
      enable: true,
    },
    resize: {
      enable: resizable,
      position: 'bottom',
    },
    upload: {
      accept: 'image/*',
      multiple: false,
      handler: async (files: File[]) => {
        const file = files && files.length > 0 ? files[0] : null
        if (!file || !vditor) return null
        if (typeof uploadImage !== 'function') {
          vditor.tip('当前场景不支持上传图片')
          return null
        }
        const data: any = await (uploadImage as any)(file)
        if (!data || !data.id) return null
        imageUrls[data.id] = data.url || fallbackImage
        vditor.insertMD(`![${data.alt || file.name}](${placeholderPrefix()}${data.id})`)
        await refresh()
        return null
      },
    },
    input(value: string) {
      const content = normalizeOut(value)
      model.value = content
      refresh(content)
    },
    after() {
      if (model.value) {
        vditor!.setValue(normalizeIn(model.value))
      }
      refresh()
    },
  })
}

watch([() => model.value, () => streaming], () => {
  if (readonly) {
    schedulePreviewRefresh(model.value ?? '')
  } else if (vditor && model.value !== normalizeOut(vditor.getValue())) {
    vditor.setValue(normalizeIn(model.value ?? ''))
  }
})

onMounted(() => {
  nextTick(() => {
    if (readonly) {
      schedulePreviewRefresh(model.value ?? '')
    } else {
      loadEditor()
      observeEditor()
    }
  })
})

onBeforeUnmount(() => {
  previewSequence++
  clearPreviewTimer()
  previewDirty = false
  cleanupPreviewEnhancements()
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (vditor) {
    model.value = normalizeOut(vditor.getValue())
    vditor.destroy()
    vditor = null
  }
})

defineExpose({ getContent, setContent })
</script>

<template>
  <div class="fs-markdown-editor">
    <div
      ref="previewRef"
      class="fs-markdown-preview"
      v-if="readonly"
      @click.capture="handlePreviewClick"
      @dblclick="handlePreviewDblClick"></div>
    <div ref="editorRef" v-else></div>
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewUrlList"
      :initial-index="previewIndex"
      :teleported="true"
      @close="previewVisible = false"
    />
  </div>
</template>

<style lang="scss" scoped>
.fs-markdown-editor {
  width: 100%;
  line-height: normal;
}
.fs-markdown-preview {
  padding: 8px 0;
  word-break: break-word;
  .fs-markdown-empty,
  .fs-markdown-preview-body {
    display: block;
    min-width: 0;
  }
  .fs-markdown-empty {
    color: var(--el-text-color-placeholder);
  }
  :deep(p:last-child) {
    margin-bottom: 0;
  }
  :deep(.fs-markdown-preview-body) {
    width: 100%;
    min-width: 0;
    max-width: 100%;
    color: var(--el-text-color-primary);

    > :first-child {
      margin-top: 0 !important;
    }

    > :last-child {
      margin-bottom: 0 !important;
    }

    p {
      margin: 0 0 8px;
    }

    h1,
    h2,
    h3,
    h4,
    h5,
    h6 {
      margin: 13px 0 7px;
      font-weight: 600;
      line-height: 1.4;
    }

    h1 {
      font-size: 18px;
    }

    h2 {
      font-size: 16px;
    }

    h3 {
      font-size: 15px;
    }

    h4,
    h5,
    h6 {
      font-size: 13px;
    }

    ul,
    ol {
      margin: 0 0 8px;
      padding-left: 20px;
    }

    li {
      line-height: inherit;
    }

    li + li {
      margin-top: 3px;
    }

    li > p {
      margin-bottom: 3px;
    }

    blockquote {
      margin: 0 0 9px;
      padding: 6px 10px;
      border-left: 3px solid var(--el-color-primary-light-5);
      border-radius: 0 6px 6px 0;
      background: var(--el-fill-color-lighter);
      color: var(--el-text-color-secondary);
    }

    hr {
      margin: 14px 0;
      border: 0;
      border-top: 1px solid var(--el-border-color-lighter);
    }

    a {
      color: var(--el-color-primary);
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }

    code:not(pre code) {
      padding: 1px 5px;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 4px;
      background: var(--el-fill-color-light);
      color: var(--el-color-danger-dark-2);
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 0.88em;
      overflow-wrap: anywhere;
    }

    .fs-markdown-table {
      width: 100%;
      max-width: 100%;
      margin: 0 0 10px;
      overflow: hidden;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 7px;
      background: var(--el-bg-color);
      outline: none;
    }

    .fs-markdown-table-scroll {
      width: 100%;
      outline: none;
    }

    .fs-markdown-table-content {
      width: max-content;
      min-width: 100%;
      background: var(--el-bg-color);
    }

    table {
      display: table !important;
      width: max-content;
      min-width: 100%;
      margin: 0 !important;
      border: 0;
      border-collapse: collapse;
      table-layout: auto;
      font-size: 12px;
      line-height: 1.6;
    }

    .fs-markdown-table table {
      /* 外框始终占满气泡；表格按内容展开，超宽时只在表格内部横向滚动。 */
      width: max-content !important;
      min-width: 100%;
    }

    /* Vditor 默认给每条 tr 加的 #c6cbd1 顶线会和单元格分隔线叠成深色顶边。 */
    .fs-markdown-table table tr {
      border-top: 0;
    }

    .fs-markdown-table .el-scrollbar__view {
      min-width: 100%;
    }

    .fs-markdown-table .el-scrollbar__bar.is-horizontal {
      height: 6px;
    }

    th,
    td {
      padding: 6px 9px;
      border: 0;
      border-right: 1px solid var(--el-border-color-lighter);
      border-bottom: 1px solid var(--el-border-color-lighter);
      text-align: left;
      white-space: nowrap;
    }

    th:last-child,
    td:last-child {
      border-right: 0;
    }

    tr:last-child td {
      border-bottom: 0;
    }

    th {
      color: var(--el-text-color-regular);
      font-weight: 600;
      background: var(--el-fill-color-lighter);
    }

    th[align='right'],
    td[align='right'] {
      text-align: right;
    }

    th[align='center'],
    td[align='center'] {
      text-align: center;
    }

    tbody tr:nth-child(even) {
      background: color-mix(in srgb, var(--el-fill-color-lighter) 55%, transparent);
    }

    .fs-markdown-code {
      width: 100%;
      min-width: 0;
      max-width: 100%;
      margin: 10px 0 12px;
    }

    .fs-markdown-code-shell {
      width: 100%;
      min-width: 0;
      overflow: hidden;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
      background: var(--el-fill-color-light);
      box-shadow: 0 1px 2px rgb(0 0 0 / 3%);
    }

    .fs-markdown-code-header {
      display: flex;
      min-height: 32px;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 0 8px 0 11px;
      border-bottom: 1px solid var(--el-border-color-lighter);
      background:
        linear-gradient(90deg, rgb(255 255 255 / 45%), transparent 62%),
        var(--el-fill-color-light);
    }

    .fs-markdown-code-type {
      display: inline-flex;
      min-width: 0;
      align-items: center;
      gap: 7px;
      overflow: hidden;
      color: var(--el-text-color-secondary);
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 11px;
      font-weight: 500;
      line-height: 1;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .fs-markdown-code-dot {
      flex: none;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--el-color-primary);
      box-shadow: 0 0 0 3px color-mix(in srgb, var(--el-color-primary) 11%, transparent);
    }

    .fs-markdown-code-copy {
      display: inline-flex;
      flex: none;
      height: 24px;
      align-items: center;
      gap: 5px;
      padding: 0 7px;
      border: 0;
      border-radius: 5px;
      background: transparent;
      color: var(--el-text-color-secondary);
      font: inherit;
      font-size: 11.5px;
      line-height: 1;
      cursor: pointer;
      transition: color 0.16s, background-color 0.16s;

      &:hover,
      &:focus-visible,
      &.is-copied {
        background: color-mix(in srgb, var(--el-bg-color) 72%, transparent);
        color: var(--el-color-primary);
      }

      &:focus-visible {
        outline: 1px solid var(--el-color-primary-light-5);
        outline-offset: 0;
      }
    }

    .fs-markdown-code-body-host {
      width: 100%;
      min-width: 0;
    }

    .fs-markdown-code-body {
      width: 100%;
      overflow: hidden;
      outline: none;
      background: var(--el-fill-color-light);
    }

    .fs-markdown-code-body .el-scrollbar__wrap {
      max-height: min(520px, 55vh);
      overscroll-behavior: contain;
    }

    .fs-markdown-code-body .el-scrollbar__view {
      min-width: 100%;
    }

    .fs-markdown-code-content {
      width: max-content;
      min-width: 100%;
    }

    .fs-markdown-code-content pre {
      width: max-content;
      min-width: 100%;
      margin: 0 !important;
      overflow: visible;
      border: 0 !important;
      border-radius: 0;
      background: transparent !important;
    }

    .fs-markdown-code-content pre > code {
      display: block;
      width: max-content;
      min-width: 100%;
      margin: 0;
      padding: 12px 14px 14px;
      overflow: visible;
      border: 0;
      border-radius: 0;
      background: transparent !important;
      background-image: none !important;
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, "Liberation Mono", monospace;
      font-size: 11.5px;
      line-height: 1.7;
      tab-size: 2;
      white-space: pre;
      word-break: normal;
      overflow-wrap: normal;
    }

    .fs-markdown-code-content code.vditor-linenumber {
      padding-left: 4.5em;
    }

    .fs-markdown-code-content .vditor-linenumber__rows {
      top: 12px;
      bottom: 14px;
      width: 3.5em;
      border-right: 1px solid var(--el-border-color-lighter);
      background: var(--el-fill-color-light);
    }

    .fs-markdown-code-content .vditor-linenumber__rows > span::before {
      padding-right: 0.9em;
      color: var(--el-text-color-placeholder);
      font-variant-numeric: tabular-nums;
    }

    .fs-markdown-code-content .vditor-linenumber__temp {
      display: none !important;
    }

    .fs-markdown-echarts {
      width: 100%;
      max-width: 100%;
      margin: 14px 0 16px;
      padding: 12px 12px 16px;
      overflow: hidden;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
      background: var(--el-bg-color);

      &[data-state='ready'] {
        height: clamp(320px, 42vh, 440px);
        min-height: 300px;
      }

      &[data-state='pending'],
      &[data-state='error'] {
        display: flex;
        min-height: 76px;
        align-items: center;
        padding: 12px 14px;
        overflow: auto;
      }

      &[data-state='pending'] {
        color: var(--el-text-color-secondary);
        background:
          linear-gradient(100deg, transparent 22%, rgb(255 255 255 / 48%) 45%, transparent 68%)
            0 0 / 220% 100%,
          var(--el-fill-color-lighter);
        animation: fs-markdown-chart-loading 1.8s linear infinite;
      }

      &[data-state='error'] {
        border-color: var(--el-color-danger-light-5);
        background: var(--el-color-danger-light-9);
      }
    }

    .fs-markdown-echarts-source {
      width: max-content;
      min-width: 100%;
      margin: 0 !important;
      padding: 0 !important;
      border: 0 !important;
      background: transparent !important;
      color: var(--el-text-color-secondary);
      font-size: 11.5px;
      line-height: 1.6;
      white-space: pre;
    }

    .fs-markdown-error {
      padding: 9px 11px;
      border: 1px solid var(--el-color-danger-light-5);
      border-radius: 7px;
      background: var(--el-color-danger-light-9);
      color: var(--el-color-danger);
      white-space: pre-wrap;
    }
  }
  /* 双击打开查看器：给出可交互的提示 */
  :deep(img) {
    max-width: 100%;
    cursor: zoom-in;
  }
}
@keyframes fs-markdown-chart-loading {
  to {
    background-position: 220% 0, 0 0;
  }
}
// 编辑区图片：src 是占位地址（保存时会还原为标识），解析完成后通过 content 换成真实地址
:deep(img.md-image) {
  content: var(--md-image);
  max-width: 100%;
}
</style>

<style lang="scss">
// Vditor 的图片预览层插入在 body 上，需要压过 Element Plus 抽屉的层级，否则被抽屉遮挡
.vditor-img {
  z-index: 4000;
}
</style>
