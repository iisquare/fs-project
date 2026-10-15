/**
 * SSE 流式调用（POST + fetch 流读取）。
 *
 * 服务端的流式接口都走这里：编排的 `POST /agent/agentic/invokeStream` 与本地客户端的
 * 模型对话 `POST /agent/bot/chat`，报文都是 `{ type, data }`。
 * 不能用 EventSource（它只支持 GET），所以这里用 fetch + ReadableStream 自己解析 SSE 报文。
 *
 * 会话运行在 store 里持有 Stream 实例，页面切换不会中断运行：
 * 只有显式 abort（停止生成 / 退出登录）才会真正断开。
 */

export interface StreamEvent {
  type: string
  data: any
}

/** SSE 事件分隔：空行（兼容 \r\n） */
const SEPARATOR = /\r?\n\r?\n/

export interface StreamOptions {
  url: string
  body: any
  token?: string
  headers?: Record<string, string>
  onEvent: (event: StreamEvent) => void
  /** 收尾回调：无论正常结束、异常还是主动中断都会触发一次 */
  onClose: (result: { aborted: boolean; error?: string }) => void
}

export class SseStream {
  private controller: AbortController | null = null
  private active = false
  private abortRequested = false

  get running(): boolean {
    return this.active
  }

  start(options: StreamOptions): void {
    if (this.active) return
    this.active = true
    this.abortRequested = false
    this.controller = new AbortController()
    void this.consume(options)
  }

  abort(): void {
    if (!this.active) return
    this.abortRequested = true
    this.controller?.abort()
  }

  private async consume(options: StreamOptions): Promise<void> {
    let error: string | undefined
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(options.headers ?? {})
      }
      if (options.token) headers['Authorization'] = 'Bearer ' + options.token
      const response = await fetch(options.url, {
        method: 'POST',
        headers,
        body: JSON.stringify(options.body ?? {}),
        credentials: 'include',
        signal: this.controller?.signal
      })
      if (!response.ok) {
        throw new Error(`流式接口返回异常：HTTP ${response.status}`)
      }
      if (!response.body) throw new Error('流式接口无响应体')
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        // SSE 以空行分隔事件，兼容 \r\n 与 \n
        let match = SEPARATOR.exec(buffer)
        while (match) {
          const block = buffer.slice(0, match.index)
          buffer = buffer.slice(match.index + match[0].length)
          this.handleBlock(block, options)
          match = SEPARATOR.exec(buffer)
        }
      }
      if (buffer.trim()) this.handleBlock(buffer, options)
    } catch (e: any) {
      if (this.abortRequested) {
        // 主动中断不算失败：已上屏的内容保留，只是不再接收后续事件
        this.finish(options, true, undefined)
        return
      }
      error = e?.message || '流式调用失败'
    }
    this.finish(options, false, error)
  }

  private finish(options: StreamOptions, aborted: boolean, error?: string): void {
    this.active = false
    this.controller = null
    options.onClose({ aborted, error })
  }

  private handleBlock(block: string, options: StreamOptions): void {
    const lines = block.split(/\r?\n/)
    const data: string[] = []
    for (const line of lines) {
      if (!line || line.startsWith(':')) continue
      if (!line.startsWith('data:')) continue
      data.push(line.slice(5).replace(/^ /, ''))
    }
    if (data.length < 1) return
    const text = data.join('\n')
    if (!text || '[DONE]' === text) return
    let payload: any
    try {
      payload = JSON.parse(text)
    } catch {
      console.warn('[stream] 报文不是 JSON，已忽略', text)
      return
    }
    if (!payload?.type) return
    options.onEvent({ type: payload.type, data: payload.data })
  }
}
