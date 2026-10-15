/**
 * 词元数估算：客户端手里没有分词器（各家模型的分词表也不同），
 * 上下文的体量提示按字符类别折算，只用于界面上的用量圆环，不参与计费与请求裁剪。
 *
 * 折算口径（宁可略高，也不要到了撑爆窗口才发现）：
 * - ASCII：每 4 个字符 1 个词元（英文与代码的常见口径）；
 * - 表意文字（中日韩）：每字 0.7 个词元（现代分词器对中文多在 1 字 1 词元上下，
 *   常见词组会并成一个词元，取 0.7 更接近实际）；
 * - 其余字符（带音标的拉丁文、西里尔文、emoji 等）：每字 1 个词元；
 * - 每条消息另有固定的结构开销（role、分隔符等）。
 */

/** 每条消息的固定结构开销：role 与消息分隔符折算出的词元数 */
export const MESSAGE_OVERHEAD = 4

/** 估算结果缓存：输入框每敲一个字都会重算一遍历史，同样的字符串直接取上次的结果 */
const cached = new Map<string, number>()

/** 缓存条目上限：超了整表清空，历史里的内容下一轮会再次命中 */
const CACHE_LIMIT = 256

/** 表意文字与全角符号：按字计数的那些字符 */
function ideographic(code: number): boolean {
  return (
    (code >= 0x2e80 && code <= 0x9fff) || // 部首扩展 / 假名 / 注音 / 汉字
    (code >= 0xac00 && code <= 0xd7af) || // 谚文音节
    (code >= 0xf900 && code <= 0xfaff) || // 兼容汉字
    (code >= 0xff00 && code <= 0xffef) // 全角字符
  )
}

/** 一段文本折算成词元数：空文本为 0 */
export function estimateTokens(text: string): number {
  const value = String(text ?? '')
  if (!value) return 0
  const hit = cached.get(value)
  if (hit !== undefined) return hit
  let ascii = 0
  let wide = 0
  let other = 0
  for (let index = 0; index < value.length; index++) {
    const code = value.charCodeAt(index)
    if (code < 0x80) ascii++
    else if (ideographic(code)) wide++
    else other++
  }
  const tokens = Math.ceil(ascii / 4 + wide * 0.7 + other)
  if (cached.size >= CACHE_LIMIT) cached.clear()
  cached.set(value, tokens)
  return tokens
}

/** 结构化数据（工具定义等）折算成词元数：按序列化后的文本估，取不到就按 0 计 */
export function estimateJson(value: unknown): number {
  try {
    return estimateTokens(JSON.stringify(value) ?? '')
  } catch {
    return 0
  }
}

/**
 * 词元数的展示写法：三位一逗的整数值。
 * 不做「128K」这类缩写——窗口是 128000 还是 131072 差着 3K，缩写反而看不出差别。
 */
export function formatTokens(value: number): string {
  const count = Math.max(0, Math.round(Number(value) || 0))
  return String(count).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}
