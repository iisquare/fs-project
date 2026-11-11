const ALLOWED_PROTOCOLS = new Set(['https:', 'http:', 'mailto:'])

/** 只允许浏览器和邮件协议进入系统 shell，拒绝 file/custom scheme 被网页内容拉起。 */
export function externalUrl(raw: unknown): string | null {
  try {
    const url = new URL(String(raw ?? ''))
    return ALLOWED_PROTOCOLS.has(url.protocol) ? url.toString() : null
  } catch {
    return null
  }
}
