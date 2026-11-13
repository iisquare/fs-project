<script setup lang="ts">
/**
 * 回复正文使用 Vditor 的只读预览渲染，和 fs-admin 的编排对话保持一致。
 *
 * 转换后的 HTML 仍经过 DOMPurify；知识库图片、本机图片、本机文件链接和图片查看器
 * 继续走 fs-bot 原有的鉴权与动作链路。Mermaid / ECharts 由本地运行时接管，
 * 不使用 Vditor 默认的 Mermaid loose 模式，也不使用会触发 Function() 的 ECharts 配置解析。
 */
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { ElScrollbar } from 'element-plus'
import { computed, h, onBeforeUnmount, onMounted, ref, render as renderVNode, watch } from 'vue'
import { knowledgeImageIds, localImagePaths, resolveMarkdownImages } from '@renderer/core/Markdown'
import {
  diagramSource,
  markDiagramError,
  parseEChartsOption,
  prepareVditorHtml,
  sanitizeMermaidSvg,
  vditorAsset,
  vditorCdn
} from '@renderer/core/VditorMarkdown'
import KnowledgeImageUtil from '@renderer/utils/KnowledgeImageUtil'
import LocalImageUtil from '@renderer/utils/LocalImageUtil'
import FileActionUtil from '@renderer/utils/FileActionUtil'
import { useChatViewStore } from '@renderer/stores/chatView'
import loadingImage from '@renderer/assets/images/knowledge-image-loading.svg?url'
import fallbackImage from '@renderer/assets/images/knowledge-image-fallback.svg?url'

interface MermaidRuntime {
  initialize(options: Record<string, unknown>): void
  render(id: string, source: string): Promise<{ svg: string } | string> | { svg: string } | string
}

interface EChartsInstance {
  setOption(option: Record<string, unknown>): void
  resize(): void
  dispose(): void
}

interface EChartsRuntime {
  init(element: HTMLElement): EChartsInstance
}

declare global {
  interface Window {
    mermaid?: MermaidRuntime
  }
}

const props = defineProps<{
  content: string
  /** 会话所属项目：正文里的相对路径按它的工作区目录解析（打开文件时用） */
  projectId?: string
  /** 会话标识：本机图片越界读取时，审批申请交回这段对话 */
  conversationId?: string
  /** 模型仍在输出：图表可能尚未闭合，解析失败不能提前标红 */
  streaming?: boolean
  /** 消息已正常完成；停止或失败时保留中性占位，不把半截图表标成错误 */
  complete?: boolean
}>()

const chatView = useChatViewStore()

/**
 * 已解析的图片标识 → 地址
 * 空串表示「服务端没给地址」，与「还没解析」区分开：前者不再重试，后者用占位图
 */
const urls = ref<Record<string, string>>({})

/** 已解析的本机图片路径 → 展示地址（空串表示没拿到，统一回落默认图） */
const localUrls = ref<Record<string, string>>({})

function imageUrl(id: string): string {
  return id in urls.value ? urls.value[id] || fallbackImage : loadingImage
}

function localImageUrl(path: string): string {
  return path in localUrls.value ? localUrls.value[path] || fallbackImage : loadingImage
}

/** 先完成图片地址替换，再交给 Vditor；渲染期间新增的图片会被下一轮刷新补上 */
const source = computed(() =>
  resolveMarkdownImages(props.content, {
    knowledgeImage: imageUrl,
    localImage: localImageUrl
  })
)

function pendingIds(text: string): string[] {
  return knowledgeImageIds(text).filter((id) => !(id in urls.value))
}

async function resolve(text: string): Promise<void> {
  const ids = pendingIds(text)
  if (!ids.length) return
  const result = await KnowledgeImageUtil.resolve(ids)
  const next = { ...urls.value }
  ids.forEach((id) => {
    next[id] = result[id] || ''
  })
  urls.value = next
}

function pendingLocalPaths(text: string): string[] {
  return localImagePaths(text).filter((path) => !(path in localUrls.value))
}

async function resolveLocal(text: string): Promise<void> {
  const list = pendingLocalPaths(text)
  if (!list.length) return
  const projectId = String(props.projectId ?? '')
  const conversationId = String(props.conversationId ?? '')
  const resolved = await Promise.all(
    list.map((path) => LocalImageUtil.resolve(projectId, path, conversationId))
  )
  // 解析期间切了项目或会话：这一批结果按旧权限算，丢弃后等新上下文重新解析
  if (
    projectId !== String(props.projectId ?? '') ||
    conversationId !== String(props.conversationId ?? '')
  ) {
    return
  }
  const next = { ...localUrls.value }
  list.forEach((path, index) => {
    next[path] = resolved[index] || ''
  })
  localUrls.value = next
}

const RESOLVE_WINDOW = 240
let resolveTimer: ReturnType<typeof setTimeout> | null = null

function needsResolve(text: string): boolean {
  return pendingIds(text).length > 0 || pendingLocalPaths(text).length > 0
}

function queueResolve(text: string): void {
  if (resolveTimer || !needsResolve(text)) return
  resolveTimer = setTimeout(() => {
    resolveTimer = null
    void resolve(props.content)
    void resolveLocal(props.content)
  }, RESOLVE_WINDOW)
}

watch(
  () => [props.content, props.projectId, props.conversationId] as const,
  ([, projectId, conversationId], previous) => {
    // 换了项目或会话：旧上下文签发的地址与解析结论都不再适用，清掉重新来
    if (previous && (previous[1] !== projectId || previous[2] !== conversationId)) {
      urls.value = {}
      localUrls.value = {}
    }
    queueResolve(props.content)
  },
  { immediate: true }
)

const rootRef = ref<HTMLDivElement>()
const renderRef = ref<HTMLDivElement>()
const preview = ref<{ list: string[]; index: number } | null>(null)

/** 流式输出时合并高频更新；最终一次内容不会丢，末尾再补一轮渲染 */
const RENDER_WINDOW = 90
let renderTimer: ReturnType<typeof setTimeout> | null = null
let renderSequence = 0
let disposed = false
let chartCleanup: (() => void) | null = null
const tableScrollers: HTMLElement[] = []

function scheduleRender(): void {
  if (renderTimer) return
  renderTimer = setTimeout(() => {
    renderTimer = null
    void renderNow()
  }, RENDER_WINDOW)
}

watch([source, () => Boolean(props.streaming), () => Boolean(props.complete)], scheduleRender)

const scripts = new Map<string, Promise<void>>()

function loadScript(path: string, id: string): Promise<void> {
  if (document.getElementById(id)) return Promise.resolve()
  const src = vditorAsset(path)
  const cached = scripts.get(src)
  if (cached) return cached
  const task = new Promise<void>((resolveTask, rejectTask) => {
    const script = document.createElement('script')
    script.id = id
    script.src = src
    script.async = true
    script.onload = () => resolveTask()
    script.onerror = () => {
      script.remove()
      rejectTask(new Error(`加载失败：${src}`))
    }
    document.head.appendChild(script)
  })
  scripts.set(src, task)
  return task
}

let mermaidReady: Promise<MermaidRuntime> | null = null

function loadMermaid(): Promise<MermaidRuntime> {
  if (mermaidReady) return mermaidReady
  mermaidReady = loadScript('js/mermaid/mermaid.min.js?v=11.6.0', 'fsBotVditorMermaid').then(() => {
    if (!window.mermaid) throw new Error('Mermaid 运行时未加载')
    window.mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      theme: 'default',
      flowchart: { htmlLabels: false, useMaxWidth: true },
      sequence: { useMaxWidth: true },
      gantt: { useMaxWidth: true }
    })
    return window.mermaid
  })
  mermaidReady.catch(() => {
    mermaidReady = null
  })
  return mermaidReady
}

let chartsReady: Promise<EChartsRuntime> | null = null

function loadECharts(): Promise<EChartsRuntime> {
  if (chartsReady) return chartsReady
  chartsReady = import('echarts').then((runtime) => ({ init: runtime.init }))
  chartsReady.catch(() => {
    chartsReady = null
  })
  return chartsReady
}

let mermaidId = 0

async function renderMermaid(block: HTMLElement, sequence: number): Promise<void> {
  const runtime = await loadMermaid()
  if (disposed || sequence !== renderSequence || !block.isConnected) return
  const rendered = await Promise.resolve(
    runtime.render(`fsBotMermaid${Date.now()}${++mermaidId}`, diagramSource(block))
  )
  if (disposed || sequence !== renderSequence || !block.isConnected) return
  const svg = 'string' === typeof rendered ? rendered : rendered.svg
  const safe = sanitizeMermaidSvg(svg)
  if (!safe) throw new Error('Mermaid 输出为空')
  block.innerHTML = safe
  block.dataset.state = 'ready'
}

async function renderECharts(block: HTMLElement, sequence: number): Promise<void> {
  const runtime = await loadECharts()
  if (disposed || sequence !== renderSequence || !block.isConnected) return
  const option = parseEChartsOption(diagramSource(block))
  block.querySelector('.fs-diagram-source')?.remove()
  // 先切到 ready 让 CSS 给 ECharts 固定画布尺寸，再初始化，避免先用占位高度布局。
  block.dataset.state = 'ready'
  const chart = runtime.init(block)
  chart.setOption(option)
  const observer = new ResizeObserver(() => chart.resize())
  observer.observe(block)
  const previous = chartCleanup
  chartCleanup = () => {
    observer.disconnect()
    chart.dispose()
    previous?.()
  }
}

async function renderDiagrams(root: HTMLElement, sequence: number): Promise<void> {
  const jobs: Promise<void>[] = []
  const streamInProgress = Boolean(props.streaming)
  const handleError = (block: HTMLElement, error: unknown): void => {
    if (disposed || sequence !== renderSequence) return
    // 流式输出或未正常完成时 JSON 往往只到一半；保持 pending，不提前标红。
    if (streamInProgress || false === props.complete) {
      block.dataset.state = 'pending'
      block.removeAttribute('title')
      return
    }
    markDiagramError(block, error)
  }
  root.querySelectorAll<HTMLElement>('.fs-mermaid-block').forEach((block) => {
    jobs.push(renderMermaid(block, sequence).catch((error) => handleError(block, error)))
  })
  root.querySelectorAll<HTMLElement>('.fs-echarts-block').forEach((block) => {
    jobs.push(renderECharts(block, sequence).catch((error) => handleError(block, error)))
  })
  await Promise.all(jobs)
  if (sequence !== renderSequence) return
}

function disposeCharts(): void {
  chartCleanup?.()
  chartCleanup = null
}

/**
 * Vditor 输出的是普通 table。这里把每个表格移进真正的 ElScrollbar，
 * 窄窗口下只横向滚动表格，不再撑宽或裁切整条消息。
 */
function mountTableScrollers(root: HTMLElement): void {
  root.querySelectorAll('table').forEach((table) => {
    if (!table.parentNode) return
    const container = document.createElement('div')
    container.className = 'chat-markdown-table'
    table.replaceWith(container)
    tableScrollers.push(container)
    renderVNode(
      h(
        ElScrollbar,
        { class: 'chat-markdown-scroll' },
        {
          default: () =>
            h('div', {
              class: 'chat-markdown-table-content',
              innerHTML: table.outerHTML
            })
        }
      ),
      container
    )
  })
}

function disposeTableScrollers(): void {
  tableScrollers.splice(0).forEach((container) => renderVNode(null, container))
}

async function renderNow(): Promise<void> {
  const host = renderRef.value
  if (!host || disposed) return
  const sequence = ++renderSequence
  disposeCharts()
  disposeTableScrollers()
  const next = document.createElement('div')
  next.className = 'chat-markdown-text'
  host.replaceChildren(next)
  if (!source.value.trim()) return
  try {
    await Vditor.preview(next, source.value, {
      mode: 'light',
      lang: 'zh_CN',
      cdn: vditorCdn(),
      hljs: {
        enable: true,
        lineNumber: true,
        style: 'github'
      },
      transform: prepareVditorHtml
    })
    if (sequence !== renderSequence || disposed) return
    next.querySelectorAll('[onclick], [onmouseover]').forEach((element) => {
      element.removeAttribute('onclick')
      element.removeAttribute('onmouseover')
    })
    mountTableScrollers(next)
    await renderDiagrams(next, sequence)
  } catch (error) {
    if (sequence !== renderSequence || disposed) return
    next.classList.add('chat-markdown-error')
    next.textContent = `Markdown 渲染失败：${
      error instanceof Error ? error.message : String(error)
    }`
  }
}

onMounted(scheduleRender)

onBeforeUnmount(() => {
  disposed = true
  renderSequence++
  if (resolveTimer) clearTimeout(resolveTimer)
  if (renderTimer) clearTimeout(renderTimer)
  disposeCharts()
  disposeTableScrollers()
})

function openPreview(event: MouseEvent): void {
  const image = (event.target as Element | null)?.closest?.('img')
  if (!image || !rootRef.value) return
  const images = Array.from(rootRef.value.querySelectorAll('img')).filter(
    (item) => ![loadingImage, fallbackImage].includes(item.getAttribute('src') ?? '')
  )
  const index = images.indexOf(image as HTMLImageElement)
  if (index < 0) return
  preview.value = {
    list: images.map((item) => item.getAttribute('src') ?? ''),
    index
  }
}

function onClick(event: MouseEvent): void {
  if (onCodeCopy(event)) return
  const anchor = (event.target as Element | null)?.closest?.('a')
  if (anchor) {
    const href = anchor.getAttribute('href') ?? ''
    const path = FileActionUtil.localPath(href)
    if (path) {
      event.preventDefault()
      void FileActionUtil.open(String(props.projectId ?? ''), path)
      return
    }
    if (href && !href.startsWith('#')) {
      event.preventDefault()
      window.open(href, '_blank', 'noopener,noreferrer')
      return
    }
  }
  openPreview(event)
}

function onCodeCopy(event: MouseEvent): boolean {
  const button = (event.target as Element | null)?.closest?.('.vditor-copy')
  if (!button) return false
  const pre = button.closest('pre')
  const code = pre?.querySelector(':scope > code')?.textContent ?? ''
  event.preventDefault()
  void navigator.clipboard
    .writeText(code.replace(/\n$/, ''))
    .then(() => {
      button.querySelector('[aria-label]')?.setAttribute('aria-label', '已复制')
    })
    .catch(() => {})
  return true
}

function fallbackImageOnError(event: Event): void {
  const image = event.target as HTMLImageElement
  if (!image || 'IMG' !== image.tagName || fallbackImage === image.getAttribute('src')) return
  image.src = fallbackImage
}
</script>

<template>
  <div
    ref="rootRef"
    class="chat-markdown"
    :class="{ 'is-table-wrap': 'wrap' === chatView.tableMode }"
    @click="onClick"
    @error.capture="fallbackImageOnError"
  >
    <div ref="renderRef" class="chat-markdown-render" />
    <el-image-viewer
      v-if="preview"
      :url-list="preview.list"
      :initial-index="preview.index"
      teleported
      @close="preview = null"
    />
  </div>
</template>

<style lang="scss" scoped>
/* Vditor 负责 Markdown 结构，气泡仍保持原有正文尺寸与紧凑间距 */
.chat-markdown {
  display: block;
  width: 100%;
  height: auto;
  min-width: 0;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
  color: var(--el-text-color-primary);

  .chat-markdown-render,
  :deep(.chat-markdown-text) {
    display: block;
    width: 100%;
    height: auto;
    min-height: 0;
    max-height: none;
    overflow: visible;
  }

  :deep(.chat-markdown-text) {
    overflow: visible;
    font-size: 13px;
    line-height: 1.7;

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
      margin: 12px 0 6px;
      font-weight: 600;
      line-height: 1.4;
    }

    h1 {
      font-size: 16px;
    }

    h2 {
      font-size: 15px;
    }

    h3,
    h4,
    h5,
    h6 {
      font-size: 14px;
    }

    ul,
    ol {
      margin: 0 0 8px;
      padding-left: 20px;
    }

    li {
      margin: 2px 0;
    }

    blockquote {
      margin: 0 0 8px;
      padding: 2px 0 2px 10px;
      border-left: solid 3px var(--el-border-color);
      color: var(--el-text-color-secondary);
    }

    a {
      color: var(--el-color-primary);
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }

    hr {
      margin: 10px 0;
      border: none;
      border-top: solid 1px var(--el-border-color-lighter);
    }

    img {
      max-width: 100%;
      height: auto;
      border-radius: 6px;
      cursor: zoom-in;
    }

    pre {
      border: solid 1px var(--el-border-color-lighter);
      border-radius: 8px;
      background: var(--el-fill-color-light);
    }

    table {
      display: table;
      width: max-content;
      min-width: 100%;
      margin: 0 0 8px;
      border-collapse: collapse;
      table-layout: auto;
    }

    th,
    td {
      padding: 4px 8px;
      border: solid 1px var(--el-border-color-lighter);
      font-size: 12px;
      text-align: left;
      word-break: break-word;
      white-space: nowrap;
    }

    th {
      font-weight: 600;
      background: var(--el-fill-color-light);
    }
  }

  :deep(.chat-markdown-error) {
    padding: 8px 10px;
    border: solid 1px var(--el-color-danger-light-5);
    border-radius: 8px;
    color: var(--el-color-danger);
    white-space: pre-wrap;
  }

  :deep(.fs-diagram-block) {
    margin: 14px 0 18px;
    padding: 12px;
    border: solid 1px var(--el-border-color-lighter);
    border-radius: 8px;
    overflow: auto;
    background: var(--el-fill-color-blank);

    &[data-state='pending'] {
      min-height: 72px;
    }

    &[data-state='error'] {
      border-color: var(--el-color-danger-light-5);
    }

    .fs-diagram-source {
      margin: 0;
      border: none;
      background: transparent;
      font-size: 12px;
    }

    svg {
      display: block;
      max-width: 100%;
      height: auto;
      margin: 0 auto;
    }
  }

  :deep(.fs-echarts-block) {
    &[data-state='ready'] {
      height: 440px;
      min-height: 380px;
      padding: 12px 12px 16px;
    }
  }

  :deep(.chat-markdown-table) {
    width: 100%;
    min-width: 0;
    max-width: 100%;
    margin: 0 0 8px;
  }

  :deep(.chat-markdown-scroll) {
    width: 100%;
    height: auto;

    .el-scrollbar__view {
      min-width: 100%;
    }
  }

  :deep(.chat-markdown-table-content) {
    width: max-content;
    min-width: 100%;

    table {
      width: max-content;
      min-width: 100%;
      margin: 0;
    }
  }

  &.is-table-wrap {
    :deep(.chat-markdown-text) {
      table {
        width: 100%;
        min-width: 100%;
        max-width: 100%;
        table-layout: auto;
      }

      th,
      td {
        min-width: 56px;
        white-space: normal;
        word-break: break-word;
        overflow-wrap: anywhere;
      }
    }

    :deep(.chat-markdown-table-content) {
      width: 100%;
      min-width: 0;
      max-width: 100%;

      table {
        width: 100%;
        min-width: 100%;
        max-width: 100%;
      }
    }
  }
}
</style>
