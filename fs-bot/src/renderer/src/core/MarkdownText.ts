import { marked, type Token } from 'marked'

/**
 * Markdown → 单行纯文本：用于状态卡等空间有限的位置。
 * 按 token 结构去掉标题、列表、强调等标记，但保留正文里的下划线、井号和技术标识。
 */
export function markdownToPlainText(text: string): string {
  const output: string[] = []

  const visit = (tokens: Token[]): void => {
    for (const token of tokens) {
      if ('space' === token.type) continue
      if ('br' === token.type) {
        output.push(' ')
        continue
      }
      if ('code' === token.type || 'codespan' === token.type) {
        output.push(String(token.text ?? ''))
        continue
      }
      if ('image' === token.type) {
        output.push(String(token.text || token.href || ''))
        continue
      }
      if ('html' === token.type) {
        const plain = String(token.text ?? '')
          .replace(/<[^>]*>/g, ' ')
          .trim()
        if (plain) output.push(plain)
        continue
      }
      if ('table' === token.type) {
        token.header.forEach((cell) => visit(cell.tokens))
        token.rows.forEach((row) => row.forEach((cell) => visit(cell.tokens)))
        continue
      }
      if ('list' === token.type) {
        token.items.forEach((item) => visit(item.tokens))
        continue
      }

      const nested = token as Token & { tokens?: Token[] }
      if (Array.isArray(nested.tokens) && nested.tokens.length) {
        visit(nested.tokens)
        continue
      }
      const plain = 'text' in token && 'string' === typeof token.text ? token.text.trim() : ''
      if (plain) output.push(plain)
    }
  }

  const value = String(text ?? '')
  if (!value) return ''
  visit(marked.lexer(value))
  return output.join(' ').replace(/\s+/g, ' ').trim()
}
