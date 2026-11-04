/** Browser Agent 只能停留于公开网页协议；本机文件与其他应用协议必须显式拒绝。 */
export function normalizeWebUrl(value: unknown): string {
  const text = String(value ?? '').trim()
  if (!text) throw new Error('open 需要 url（要打开的网址）')
  const local = /^(localhost|127\.0\.0\.1|\[::1\])(?::\d+)?(?:\/|$)/i.test(text)
  const scheme = /^[a-z][a-z\d+.-]*:/i.exec(text)
  // localhost:5173 这类“主机:端口”不是协议；冒号后是端口时按无协议网址处理。
  const hostPort = scheme && /^\d+(?:[/?#]|$)/.test(text.slice(scheme[0].length))
  const candidate = scheme && !hostPort ? text : `${local ? 'http' : 'https'}://${text}`
  let parsed: URL
  try {
    parsed = new URL(candidate)
  } catch {
    throw new Error(`网址格式不正确：${text.slice(0, 200)}`)
  }
  if ('http:' !== parsed.protocol && 'https:' !== parsed.protocol) {
    throw new Error(
      `Browser Agent 只允许打开 http / https 网址，不能使用 ${parsed.protocol || '未知'} 协议`
    )
  }
  return parsed.toString()
}

/** 空白新页允许存在；已有页面和导航结果则只接受 http / https。 */
export function safeWebPageUrl(value: string): boolean {
  if (!value || 'about:blank' === value) return true
  try {
    const protocol = new URL(value).protocol
    return 'http:' === protocol || 'https:' === protocol
  } catch {
    return false
  }
}
