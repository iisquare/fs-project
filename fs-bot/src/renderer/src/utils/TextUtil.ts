/**
 * 文本展示口径：把一段正文折成列表里读得下去的一行、按关键词取「命中片段」、切出要高亮的段。
 *
 * 「命中片段」的取法与工具层那份（core/Toolbox 的 memory 检索）是同一套规则，
 * 页面上看到的片段与模型读到的那句才对得上。
 */

/** 单行摘要：折叠换行与连续空白，超长截断——要经得起一行行读 */
export function oneLine(value: unknown, limit: number): string {
  const body = String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()
  return body.length > limit ? `${body.slice(0, limit)}…` : body
}

/**
 * 命中片段：以关键词为中心取一小段上下文（找不到关键词就给开头一段）。
 * span 是关键词前后各留多少个字符；正文为空给「（空）」。
 *
 * 关键词与正文都按折叠空白后的形态找位置：关键词是多行片段（或词与词之间的空白
 * 与正文对不上）时，整段找不到就退到它的第一段——片段仍围着命中处取，而不是回到正文开头。
 */
export function snippet(content: unknown, keyword: string, span = 80): string {
  const body = String(content ?? '')
    .replace(/\s+/g, ' ')
    .trim()
  if (!body) return '（空）'
  const word = String(keyword ?? '')
    .replace(/\s+/g, ' ')
    .trim()
  let index = -1
  let length = word.length
  for (const head of word ? [word, ...word.split(' ')] : []) {
    const at = body.toLowerCase().indexOf(head.toLowerCase())
    if (at >= 0 && (index < 0 || at < index)) {
      index = at
      length = head.length
    }
  }
  if (index < 0) return oneLine(body, span * 2)
  const start = Math.max(0, index - span)
  const end = Math.min(body.length, index + length + span)
  return `${start > 0 ? '…' : ''}${body.slice(start, end)}${end < body.length ? '…' : ''}`
}

/** 角色文案：库里的 role 转成中文说法（用户 / 助手 / 系统），认不出来就照原样给 */
export function roleText(role: string): string {
  if ('user' === role) return '用户'
  if ('assistant' === role) return '助手'
  if ('system' === role) return '系统'
  return role || '未知'
}

/**
 * 关键词高亮：按关键词（不区分大小写）把文本切成若干段，命中段标 hit。
 * 关键词为空就整段给出来（都不命中）——没在检索时也走同一套渲染。
 */
export function highlightParts(
  text: unknown,
  keyword: string
): Array<{ text: string; hit: boolean }> {
  const body = String(text ?? '')
  const word = String(keyword ?? '')
  if (!body) return []
  if (!word) return [{ text: body, hit: false }]
  // 按小写副本找位置：切出来的仍是原文（大小写照旧展示）
  const source = body.toLowerCase()
  const target = word.toLowerCase()
  const parts: Array<{ text: string; hit: boolean }> = []
  let from = 0
  for (;;) {
    const index = source.indexOf(target, from)
    if (index < 0) break
    if (index > from) parts.push({ text: body.slice(from, index), hit: false })
    parts.push({ text: body.slice(index, index + target.length), hit: true })
    from = index + target.length
  }
  if (!parts.length) return [{ text: body, hit: false }]
  if (from < body.length) parts.push({ text: body.slice(from), hit: false })
  return parts
}
