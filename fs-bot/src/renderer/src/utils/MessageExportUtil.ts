const INLINE_STYLE_PROPERTIES = [
  'display',
  'color',
  'background-color',
  'font-family',
  'font-size',
  'font-weight',
  'font-style',
  'line-height',
  'text-align',
  'text-decoration-line',
  'white-space',
  'word-break',
  'overflow-wrap',
  'vertical-align',
  'margin-top',
  'margin-right',
  'margin-bottom',
  'margin-left',
  'padding-top',
  'padding-right',
  'padding-bottom',
  'padding-left',
  'border-top-width',
  'border-right-width',
  'border-bottom-width',
  'border-left-width',
  'border-top-style',
  'border-right-style',
  'border-bottom-style',
  'border-left-style',
  'border-top-color',
  'border-right-color',
  'border-bottom-color',
  'border-left-color',
  'border-radius'
]

const MAX_INLINE_IMAGE_SIZE = 20 * 1024 * 1024
const WORD_BODY_FONT = '"Microsoft YaHei", "Segoe UI", Arial, sans-serif'
const WORD_CODE_FONT = 'Consolas, "Courier New", monospace'

/** 把渲染树的最终计算样式写进内联样式，打印窗口和 Word 转换都不依赖页面样式表。 */
function inlineComputedStyles(source: Element, target: Element): void {
  const computed = window.getComputedStyle(source)
  const declarations: string[] = []
  INLINE_STYLE_PROPERTIES.forEach((property) => {
    const value = computed.getPropertyValue(property)
    // html-to-docx 会把部分透明背景解析成黑色；透明背景应保持缺省白色纸张。
    if (property === 'background-color' && isTransparentColor(value)) return
    if (property === 'font-family') {
      declarations.push(`font-family:${WORD_BODY_FONT}`)
      return
    }
    if (value) declarations.push(`${property}:${value}`)
  })
  const existing = target.getAttribute('style')?.trim().replace(/;$/, '')
  if (existing) declarations.unshift(existing)
  if (declarations.length) target.setAttribute('style', `${declarations.join(';')};`)

  const sourceChildren = Array.from(source.children)
  const targetChildren = Array.from(target.children)
  sourceChildren.forEach((child, index) => {
    const counterpart = targetChildren[index]
    if (counterpart) inlineComputedStyles(child, counterpart)
  })
}

function isTransparentColor(value: string): boolean {
  const normalized = value.trim().toLowerCase()
  if (!normalized || 'transparent' === normalized) return true
  return /rgba?\([^)]*(?:,\s*0(?:\.0+)?\s*\)|\/\s*0(?:\.0+)?\s*\))/.test(normalized)
}

function appendStyle(element: Element, declarations: string): void {
  const current = element.getAttribute('style')?.trim().replace(/;$/, '') ?? ''
  element.setAttribute('style', `${current ? `${current};` : ''}${declarations}`)
}

function waitForImages(root: HTMLElement): Promise<void> {
  const images = Array.from(root.querySelectorAll('img'))
  return Promise.all(
    images.map((image) => {
      if (image.complete) return Promise.resolve()
      return Promise.race([
        new Promise<void>((resolve) => {
          image.addEventListener('load', () => resolve(), { once: true })
          image.addEventListener('error', () => resolve(), { once: true })
        }),
        new Promise<void>((resolve) => window.setTimeout(resolve, 4000))
      ])
    })
  ).then(() => undefined)
}

function readBlobAsDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result ?? ''))
    reader.onerror = () => reject(reader.error ?? new Error('读取图片失败'))
    reader.readAsDataURL(blob)
  })
}

async function imageAsDataUrl(src: string): Promise<string> {
  if (!src || src.startsWith('data:')) return src
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), 8000)
  try {
    const response = await fetch(src, {
      credentials: 'include',
      signal: controller.signal
    })
    if (!response.ok) return ''
    const blob = await response.blob()
    if (!blob.size || blob.size > MAX_INLINE_IMAGE_SIZE) return ''
    return await readBlobAsDataUrl(blob)
  } catch {
    return ''
  } finally {
    window.clearTimeout(timer)
  }
}

async function inlineImages(root: HTMLElement): Promise<void> {
  const cache = new Map<string, Promise<string>>()
  await Promise.all(
    Array.from(root.querySelectorAll('img')).map(async (image) => {
      const src = image.getAttribute('src') ?? ''
      if (!src || src.startsWith('data:')) return
      let task = cache.get(src)
      if (!task) {
        task = imageAsDataUrl(src)
        cache.set(src, task)
      }
      const dataUrl = await task
      if (dataUrl) image.setAttribute('src', dataUrl)
    })
  )
}

function svgDataUrl(svg: SVGElement): string {
  const clone = svg.cloneNode(true) as SVGElement
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  const serialized = new XMLSerializer().serializeToString(clone)
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(serialized)}`
}

/** Canvas 默认带透明通道：导出前先合成白底，避免 Word 的暗色背景把图表衬成黑块。 */
function canvasAsPngDataUrl(source: HTMLCanvasElement): string {
  if (!source.width || !source.height) throw new Error('图表画布为空')
  const canvas = document.createElement('canvas')
  canvas.width = source.width
  canvas.height = source.height
  const context = canvas.getContext('2d')
  if (!context) throw new Error('图表画布不可用')
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.drawImage(source, 0, 0)
  return canvas.toDataURL('image/png')
}

interface ExportableECharts {
  getDataURL(options?: Record<string, unknown>): string
}

async function chartAsPngDataUrl(source: HTMLCanvasElement): Promise<string> {
  const block = source.closest('.fs-echarts-block') as
    | (HTMLElement & {
        __fsBotECharts?: ExportableECharts
        __fsBotEChartsExport?: (width: number, height: number) => Promise<string>
      })
    | null
  if (block?.__fsBotEChartsExport) {
    try {
      return await block.__fsBotEChartsExport(1600, 500)
    } catch {
      // 离屏重绘失败时回退到当前图表实例。
    }
  }
  const chart = block?.__fsBotECharts
  if (chart) {
    try {
      return chart.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#ffffff'
      })
    } catch {
      // 图表实例已释放时退回画布合成。
    }
  }
  return canvasAsPngDataUrl(source)
}

async function imageElement(source: string, alt: string): Promise<HTMLImageElement> {
  const image = new Image()
  await new Promise<void>((resolve, reject) => {
    image.onload = () => resolve()
    image.onerror = () => reject(new Error('导出图片加载失败'))
    image.src = source
  })
  image.alt = alt
  image.setAttribute('width', String(image.naturalWidth || 1))
  image.setAttribute('height', String(image.naturalHeight || 1))
  return image
}

async function svgAsPngDataUrl(svg: SVGElement, width: number, height: number): Promise<string> {
  const image = new Image()
  const sourceUrl = svgDataUrl(svg)
  await new Promise<void>((resolve, reject) => {
    image.onload = () => resolve()
    image.onerror = () => reject(new Error('SVG 渲染失败'))
    image.src = sourceUrl
  })
  const scale = Math.min(2, Math.max(1, window.devicePixelRatio || 1))
  const canvas = document.createElement('canvas')
  canvas.width = Math.min(2400, Math.max(1, Math.round(width * scale)))
  canvas.height = Math.min(2400, Math.max(1, Math.round(height * scale)))
  const context = canvas.getContext('2d')
  if (!context) throw new Error('图表画布不可用')
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.drawImage(image, 0, 0, canvas.width, canvas.height)
  return canvas.toDataURL('image/png')
}

/**
 * 把聊天区已经渲染好的正文变成可独立保存的 HTML。
 *
 * Canvas、SVG 与普通图片会尽量转成内联地址，这样 PDF 不依赖当前聊天窗口，
 * Word 也不依赖本机鉴权协议；远程图片若受跨域限制而无法内联，会保留原地址，
 * 交由导出端继续加载。
 */
export async function renderedMessageHtml(root: HTMLElement): Promise<string> {
  await Promise.all([document.fonts.ready, waitForImages(root)])

  const clone = root.cloneNode(true) as HTMLElement
  inlineComputedStyles(root, clone)

  clone
    .querySelectorAll('.vditor-copy, .el-scrollbar__bar, [contenteditable], input, textarea')
    .forEach((element) => element.remove())
  clone
    .querySelectorAll('.fs-diagram-block[data-state="ready"] .fs-diagram-source')
    .forEach((element) => element.remove())

  const sourceCanvases = Array.from(root.querySelectorAll('canvas'))
  const clonedCanvases = Array.from(clone.querySelectorAll('canvas'))
  for (const [index, canvas] of clonedCanvases.entries()) {
    const source = sourceCanvases[index]
    if (!source) continue
    try {
      const image = await imageElement(await chartAsPngDataUrl(source), '图表')
      image.setAttribute(
        'style',
        'display:block;width:100%;max-width:100%;height:auto;margin:0 auto;background-color:#fff;'
      )
      const frame = document.createElement('div')
      frame.className = 'fs-echart-export-frame'
      frame.setAttribute(
        'style',
        `display:block;width:100%;max-width:100%;aspect-ratio:${image.naturalWidth}/${image.naturalHeight};margin:0;padding:0;overflow:hidden;`
      )
      frame.appendChild(image)
      canvas.replaceWith(frame)
    } catch {
      // 跨域画布无法读取像素时保留原节点，PDF 仍可直接打印。
    }
  }

  const sourceSvgs = Array.from(root.querySelectorAll('svg'))
  const clonedSvgs = Array.from(clone.querySelectorAll('svg'))
  await Promise.all(
    clonedSvgs.map(async (svg, index) => {
      const source = sourceSvgs[index]
      if (!source) return
      const rect = source.getBoundingClientRect()
      try {
        const image = document.createElement('img')
        image.src = await svgAsPngDataUrl(
          svg as SVGElement,
          Math.max(1, rect.width),
          Math.max(1, rect.height)
        )
        image.alt = '图表'
        image.setAttribute(
          'style',
          'display:block;width:100%;max-width:100%;height:auto;margin:0 auto;background-color:#fff;'
        )
        svg.replaceWith(image)
      } catch {
        // Office 对部分内联 SVG 也可直接识别；转图失败时留给导出端处理。
      }
    })
  )

  // 聊天区为了横向滚动把表格包在 Element Plus 滚动条里，导出时还原成纯表格。
  clone.querySelectorAll('.chat-markdown-table').forEach((wrapper) => {
    const table = wrapper.querySelector('table')
    if (table) wrapper.replaceWith(table)
  })
  clone.querySelectorAll('table').forEach((table) => {
    appendStyle(table, 'width:100%;max-width:100%;border-collapse:collapse;table-layout:auto;')
    table.querySelectorAll('th, td').forEach((cell) => {
      appendStyle(cell, 'white-space:normal;word-break:break-word;overflow-wrap:anywhere;')
    })
  })
  clone.querySelectorAll('.fs-echarts-block').forEach((block) => {
    appendStyle(
      block,
      'width:100%;max-width:100%;margin:14px 0 18px;padding:0;border:0;border-radius:0;background-color:transparent;overflow:visible;'
    )
  })
  clone.querySelectorAll('code').forEach((code) => {
    if (code.closest('pre')) {
      appendStyle(code, `font-family:${WORD_CODE_FONT};font-style:normal;`)
      return
    }
    appendStyle(
      code,
      `display:inline;background-color:#f2f3f5;color:#303133;font-family:${WORD_CODE_FONT};font-style:normal;font-weight:400;`
    )
  })
  clone
    .querySelectorAll('pre')
    .forEach((pre) => appendStyle(pre, `font-family:${WORD_CODE_FONT};font-style:normal;`))

  clone.querySelectorAll('script, link, meta, iframe, object, embed, form').forEach((element) => {
    element.remove()
  })
  clone.querySelectorAll('*').forEach((element) => {
    Array.from(element.attributes).forEach((attribute) => {
      const name = attribute.name.toLowerCase()
      const value = attribute.value.trim().toLowerCase()
      if (
        name.startsWith('on') ||
        (['href', 'src', 'xlink:href'].includes(name) && value.startsWith('javascript:'))
      ) {
        element.removeAttribute(attribute.name)
      }
    })
    if ('button' === element.tagName.toLowerCase()) element.remove()
  })

  await inlineImages(clone)
  clone.querySelectorAll('img').forEach((image) => {
    appendStyle(image, 'max-width:100%;height:auto;')
  })
  appendStyle(clone, 'background-color:#fff;')
  clone.classList.add('message-export-content')
  return clone.outerHTML
}
