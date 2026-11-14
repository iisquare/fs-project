import { BrowserWindow, dialog } from 'electron'
import { mkdtempSync, rmSync, writeFileSync } from 'fs'
import { tmpdir } from 'os'
import { join } from 'path'
import HTMLtoDOCX from '@turbodocx/html-to-docx'
import { normalizeDocxLayout } from './docx-layout'
import type {
  MessageExportFormat,
  MessageExportRequest,
  MessageExportResult,
  MessagePrintRequest,
  MessagePrintResult
} from '../shared/message-export'

const MAX_EXPORT_HTML_SIZE = 60 * 1024 * 1024

const DOWNLOAD_FILTERS: Record<MessageExportFormat, Electron.FileFilter[]> = {
  docx: [
    { name: 'Word 文档 (*.docx)', extensions: ['docx'] },
    { name: '所有文件', extensions: ['*'] }
  ],
  pdf: [
    { name: 'PDF 文档 (*.pdf)', extensions: ['pdf'] },
    { name: '所有文件', extensions: ['*'] }
  ],
  markdown: [
    { name: 'Markdown 文档 (*.md)', extensions: ['md'] },
    { name: '所有文件', extensions: ['*'] }
  ],
  txt: [
    { name: '纯文本 (*.txt)', extensions: ['txt'] },
    { name: '所有文件', extensions: ['*'] }
  ]
}

const EXTENSIONS: Record<MessageExportFormat, string> = {
  docx: 'docx',
  pdf: 'pdf',
  markdown: 'md',
  txt: 'txt'
}

/** 打印与 Word 共用的一套基础样式；正文自身的样式已由渲染进程内联。 */
function exportDocument(title: string, body: string): string {
  const safeTitle = escapeHtml(title || '模型回复')
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data: http: https: chat-image:; style-src 'unsafe-inline'; font-src data:;">
  <title>${safeTitle}</title>
  <style>
    @page { size: A4; margin: 16mm 18mm 18mm; }
    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; background: #fff; }
    body {
      color: #303133;
      font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", Arial, sans-serif;
      font-size: 13px;
      line-height: 1.72;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    main { width: 100%; max-width: 100%; }
    h1, h2, h3, h4, h5, h6 { break-after: avoid; color: #1f2329; }
    h1 { font-size: 24px; margin: 22px 0 12px; }
    h2 { font-size: 19px; margin: 20px 0 10px; }
    h3 { font-size: 16px; margin: 17px 0 8px; }
    h4, h5, h6 { font-size: 14px; margin: 14px 0 7px; }
    p { margin: 0 0 9px; }
    ul, ol { margin: 0 0 9px; padding-left: 24px; }
    li { margin: 3px 0; }
    blockquote {
      margin: 10px 0;
      padding: 5px 12px;
      border-left: 3px solid #c0c4cc;
      color: #606266;
    }
    a { color: #337ecc; text-decoration: none; }
    img, svg { max-width: 100%; height: auto; }
    figure { margin: 10px 0; text-align: center; break-inside: avoid; }
    pre {
      margin: 9px 0;
      padding: 10px 12px;
      overflow: visible;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      border: 1px solid #e4e7ed;
      border-radius: 6px;
      background: #f7f8fa;
      font-family: Consolas, "SFMono-Regular", "Courier New", monospace;
      font-size: 12px;
      line-height: 1.55;
      break-inside: avoid;
    }
    code {
      font-family: Consolas, "SFMono-Regular", "Courier New", monospace;
      font-size: 0.94em;
    }
    :not(pre) > code {
      padding: 1px 4px;
      border-radius: 3px;
      background: #f2f3f5;
    }
    table {
      width: 100%;
      margin: 9px 0;
      border-collapse: collapse;
      table-layout: auto;
    }
    th, td {
      padding: 5px 8px;
      border: 1px solid #dcdfe6;
      text-align: left;
      vertical-align: top;
      white-space: normal;
      overflow-wrap: anywhere;
    }
    th { background: #f5f7fa; font-weight: 600; }
    hr { margin: 16px 0; border: 0; border-top: 1px solid #e4e7ed; }
    .fs-diagram-block {
      margin: 14px 0;
      padding: 10px;
      border: 1px solid #e4e7ed;
      border-radius: 6px;
      break-inside: avoid;
    }
    .fs-echarts-block {
      width: 100%;
      max-width: 100%;
      margin: 14px 0 18px;
      padding: 0;
      overflow: visible;
      text-align: center;
      break-inside: avoid;
    }
    .fs-echarts-block img {
      display: block;
      width: 100% !important;
      max-width: 100% !important;
      height: auto;
      margin: 0 auto;
    }
    .fs-echart-export-frame {
      display: flex;
      width: 100% !important;
      max-width: 100% !important;
      aspect-ratio: 8 / 3 !important;
      align-items: center;
      justify-content: center;
      margin: 0;
      padding: 0;
      overflow: hidden;
    }
    .fs-echart-export-frame img {
      display: block;
      width: 100% !important;
      max-width: 100% !important;
      height: 100% !important;
      object-fit: contain;
      object-position: center center;
    }
  </style>
</head>
<body><main>${body}</main></body>
</html>`
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function safeFilename(value: string, format: MessageExportFormat): string {
  const base = Array.from(String(value ?? ''))
    .map((character) =>
      /[<>:"/\\|?*]/.test(character) || character.charCodeAt(0) < 32 ? '-' : character
    )
    .join('')
    .replace(/[. ]+$/g, '')
    .trim()
    .slice(0, 120)
  const stem = base || '模型回复'
  return `${stem}.${EXTENSIONS[format]}`
}

function normalizeFormat(value: unknown): MessageExportFormat {
  if ('pdf' === value || 'markdown' === value || 'txt' === value) return value
  return 'docx'
}

function asBuffer(output: ArrayBuffer | Blob | Buffer): Buffer {
  if (Buffer.isBuffer(output)) return output
  if (output instanceof ArrayBuffer) return Buffer.from(output)
  throw new Error('Word 文档生成结果格式不受支持')
}

async function renderPdf(html: string): Promise<Buffer> {
  const tempDir = mkdtempSync(join(tmpdir(), 'fs-bot-message-export-'))
  const htmlPath = join(tempDir, 'message.html')
  const printWindow = new BrowserWindow({
    show: false,
    minWidth: 720,
    minHeight: 560,
    backgroundColor: '#ffffff',
    webPreferences: {
      sandbox: true,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  try {
    writeFileSync(htmlPath, html, 'utf8')
    printWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
    await printWindow.loadFile(htmlPath)
    await waitForPrintAssets(printWindow)
    return await printWindow.webContents.printToPDF({
      printBackground: true,
      preferCSSPageSize: true,
      pageSize: 'A4',
      margins: { top: 0, bottom: 0, left: 0, right: 0 }
    })
  } finally {
    if (!printWindow.isDestroyed()) printWindow.destroy()
    rmSync(tempDir, { recursive: true, force: true })
  }
}

async function waitForPrintAssets(printWindow: BrowserWindow): Promise<void> {
  await printWindow.webContents.executeJavaScript(
    `(async () => {
      await Promise.all(Array.from(document.images).map((image) => {
        if (image.complete) return Promise.resolve()
        return Promise.race([
          new Promise((resolve) => {
            image.addEventListener('load', resolve, { once: true })
            image.addEventListener('error', resolve, { once: true })
          }),
          new Promise((resolve) => setTimeout(resolve, 5000))
        ])
      }))
      if (document.fonts && document.fonts.ready) await document.fonts.ready
      return true
    })()`,
    true
  )
}

/** 使用与 PDF 导出一致的独立 HTML 打开系统打印对话框，避免打印预览区滚动条。 */
export async function printMessage(
  window: BrowserWindow | null,
  request: MessagePrintRequest
): Promise<MessagePrintResult> {
  if (!window) throw new Error('窗口不可用，无法打开打印对话框')
  const html = String(request?.html ?? '')
  if (!html.trim()) throw new Error('没有可打印的回复内容')
  if (Buffer.byteLength(html, 'utf8') > MAX_EXPORT_HTML_SIZE) {
    throw new Error('回复内容过大，暂时无法打印')
  }

  const title = String(request?.title ?? '').trim() || '模型回复'
  const tempDir = mkdtempSync(join(tmpdir(), 'fs-bot-message-print-'))
  const htmlPath = join(tempDir, 'message-print.html')
  const printWindow = new BrowserWindow({
    show: false,
    minWidth: 720,
    minHeight: 560,
    backgroundColor: '#ffffff',
    webPreferences: {
      sandbox: true,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  try {
    writeFileSync(htmlPath, exportDocument(title, html), 'utf8')
    printWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
    await printWindow.loadFile(htmlPath)
    await waitForPrintAssets(printWindow)
    return await new Promise<MessagePrintResult>((resolve, reject) => {
      printWindow.webContents.print(
        {
          silent: false,
          printBackground: true,
          pageSize: 'A4'
        },
        (success, failureReason) => {
          if (success) {
            resolve({ printed: true, canceled: false })
            return
          }
          if (/cancel/i.test(failureReason || '')) {
            resolve({ printed: false, canceled: true })
            return
          }
          reject(new Error(failureReason || '打印任务提交失败'))
        }
      )
    })
  } finally {
    if (!printWindow.isDestroyed()) printWindow.destroy()
    rmSync(tempDir, { recursive: true, force: true })
  }
}

async function renderDocx(html: string, title: string): Promise<Buffer> {
  const output = await HTMLtoDOCX(html, null, {
    title,
    creator: 'fs-bot',
    lang: 'zh-CN',
    font: 'Microsoft YaHei',
    fontSize: 22,
    pageSize: {
      width: 11906,
      height: 16838
    },
    margins: {
      top: 960,
      right: 1080,
      bottom: 1080,
      left: 1080
    },
    table: {
      row: { cantSplit: true },
      borderOptions: { size: 1, color: 'DCDFE6' }
    },
    imageProcessing: {
      downloadTimeout: 8000,
      maxRetries: 1,
      svgHandling: 'auto'
    }
  } as Parameters<typeof HTMLtoDOCX>[2])
  return normalizeDocxLayout(asBuffer(output))
}

/** 导出当前预览正文；取消保存时返回空路径。 */
export async function exportMessage(
  window: BrowserWindow | null,
  request: MessageExportRequest
): Promise<MessageExportResult> {
  if (!window) throw new Error('窗口不可用，无法打开保存对话框')
  const format = normalizeFormat(request?.format)
  const html = String(request?.html ?? '')
  const content = String(request?.content ?? '')
  const isDocument = 'docx' === format || 'pdf' === format
  if (isDocument && !html.trim()) throw new Error('没有可导出的回复内容')
  if (!isDocument && !content.trim()) throw new Error('没有可导出的回复内容')
  if (Buffer.byteLength(isDocument ? html : content, 'utf8') > MAX_EXPORT_HTML_SIZE) {
    throw new Error('回复内容过大，暂时无法导出')
  }

  const title = String(request?.title ?? '').trim() || '模型回复'
  const defaultPath = safeFilename(request?.filename, format)
  const result = await dialog.showSaveDialog(window, {
    title:
      'docx' === format
        ? '导出 Word 文档'
        : 'pdf' === format
          ? '导出 PDF 文档'
          : 'markdown' === format
            ? '导出 Markdown 文档'
            : '导出纯文本',
    defaultPath,
    filters: DOWNLOAD_FILTERS[format]
  })
  if (result.canceled || !result.filePath) return { path: '' }

  if (!isDocument) {
    writeFileSync(result.filePath, content, 'utf8')
    return { path: result.filePath }
  }

  const documentHtml = exportDocument(title, html)
  const output =
    'pdf' === format ? await renderPdf(documentHtml) : await renderDocx(documentHtml, title)
  writeFileSync(result.filePath, output)
  return { path: result.filePath }
}
