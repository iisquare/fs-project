/** 模型回复导出的目标格式 */
export type MessageExportFormat = 'docx' | 'pdf' | 'markdown' | 'txt'

/** 从预览窗口交给主进程的导出请求：html 是已经渲染并内联样式的正文快照 */
export interface MessageExportRequest {
  format: MessageExportFormat
  /** 不带后缀的默认文件名 */
  filename: string
  title: string
  /** Word / PDF 使用已经渲染并内联样式的正文快照 */
  html?: string
  /** Markdown / TXT 使用的文本内容 */
  content?: string
}

/** 导出完成后的保存路径；用户取消保存时为空串 */
export interface MessageExportResult {
  path: string
}

export interface MessagePrintRequest {
  title: string
  html: string
}

export interface MessagePrintResult {
  printed: boolean
  canceled: boolean
}

/** 打开独立回复预览窗口所需的只读快照 */
export interface MessagePreviewRequest {
  title: string
  content: string
  projectId: string
  conversationId: string
}

export interface MessagePreviewPayload extends MessagePreviewRequest {
  id: string
}
