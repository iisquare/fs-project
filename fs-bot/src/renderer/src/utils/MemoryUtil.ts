/**
 * 长期记忆（`projects.memory`）的文本编辑口径：算「改完的记忆长什么样」以及回执要用的数字。
 *
 * 记忆是「一行一条」的短文本、而且每轮都在上下文里（模型看得到原文），所以编辑一律按**字面量**
 * 原样匹配替换——宁可报错让它照原文重写，也不猜着改（与 `file_op` 的 replace 同一套心思，
 * 只是这里不做空白分段的兜底匹配）。`write` 的整段替换与空串清空、`replace` 的局部替换
 * 都在这里落地；真正落库（项目 store）由调用方做，所以这一层都是纯函数、可以单独跑。
 */
import { missing, textArg } from './ArgUtil'
import { oneLine } from './TextUtil'

/** 只把换行统一成 `\n`（不 trim）：替换内容要原样拼进记忆里，空白也是内容的一部分 */
export function normalizeEol(value: unknown): string {
  return String(value ?? '').replace(/\r\n?/g, '\n')
}

/** 记忆的规范形态：换行统一成 `\n`、去掉首尾空白——存进库的最终形态一律过一遍 */
export function normalizeMemory(value: unknown): string {
  return normalizeEol(value).trim()
}

/** 行数：空记忆算 0 行（回执里的「共 N 行」用它） */
export function memoryLines(value: unknown): number {
  const text = normalizeMemory(value)
  return text ? text.split('\n').length : 0
}

/** 某个片段在文本里出现的次数（不重叠、按字面量） */
export function countOccurrences(text: string, part: string): number {
  if (!part) return 0
  let count = 0
  let at = text.indexOf(part)
  while (at >= 0) {
    count++
    at = text.indexOf(part, at + part.length)
  }
  return count
}

/**
 * 替换头一处（按字面量，新文里的 `$&`、`$1` 这类不当替换语法——用切片而不是 String.replace）。
 * 找不到原样返回。
 */
export function replaceFirst(text: string, part: string, next: string): string {
  const at = text.indexOf(part)
  if (at < 0) return text
  return text.slice(0, at) + next + text.slice(at + part.length)
}

/** 替换全部出现（同样按字面量；用 split / join，新文里的 `$` 系列原样写出） */
export function replaceAll(text: string, part: string, next: string): string {
  if (!part) return text
  return text.split(part).join(next)
}

/** 一次记忆改动：改写后的全文 + 回执要用的数字（都由纯函数算出来） */
export interface MemoryEdit {
  /** 改写后的记忆全文（已按记忆口径 trim） */
  memory: string
  /** 改动前的行数 */
  before: number
  /** 改动后的行数 */
  after: number
  /** 内容是否真的变了：没变就别写库（免得白改一次「最后更新」） */
  changed: boolean
  /** 这次是怎么写的：append 追加 / replace 整段替换（回执里说明用） */
  mode: 'append' | 'replace'
  /** 是不是把记忆清空了（没有内容可下发） */
  cleared: boolean
  /** replace：替换了几处；write 恒为 1 */
  count: number
  /** replace：命中起始行（1 起；write 为 0） */
  line: number
  /** replace：命中结束行（与 line 相同表示单行命中） */
  endLine: number
  /** replace：被替换掉的原文 */
  find: string
  /**
   * replace：find 是「去掉结尾换行后」才命中的（记忆首尾不留空白，而模型常把整行连换行一起给），
   * 回执里据它说明一句，免得调用方以为命中的就是自己给的原文
   */
  adjusted: boolean
  /** 换上去的新文（可能为空串 = 删掉那一段） */
  content: string
}

/**
 * `write` 动作的落点：`mode=append`（默认）追加到现有记忆后面；`mode=replace` 整段替换。
 * `content` 允许空串——配 `replace` 就是**清空记忆**；配 `append` 没有意义，直接报错并指路。
 */
export function applyWrite(
  previous: unknown,
  args: { mode?: unknown; content?: unknown }
): MemoryEdit {
  const mode =
    String(args?.mode ?? '')
      .trim()
      .toLowerCase() || 'append'
  if ('append' !== mode && 'replace' !== mode) {
    throw new Error(`未知的写法：${mode}（可选：append 追加 / replace 整段替换）`)
  }
  // content 允许空串（配 replace 就是清空记忆），所以只认「没给」（含 null）这一种缺参数
  if (missing(args?.content)) {
    throw new Error(
      'write 需要 content（要写进记忆的内容；要清空记忆就给空串，并把 mode 设为 replace）'
    )
  }
  const before = normalizeMemory(previous)
  const content = normalizeMemory(textArg(args.content))
  if (!content && 'append' === mode) {
    throw new Error('append 时 content 不能为空：清空记忆要用 mode=replace 并把 content 给空串')
  }
  const memory = 'append' === mode ? [before, content].filter(Boolean).join('\n') : content
  return {
    memory,
    before: memoryLines(before),
    after: memoryLines(memory),
    changed: memory !== before,
    mode: 'append' === mode ? 'append' : 'replace',
    cleared: !memory,
    count: 1,
    line: 0,
    endLine: 0,
    find: '',
    adjusted: false,
    content
  }
}

/**
 * `replace` 动作的落点：把记忆里的 `find` 换成 `content`（`content` 给空串就是删掉那一段）。
 *
 * - 按字面量、原样匹配（多行片段、特殊字符都按原文；`find` 与 `content` 都只统一换行、不 trim，
 *   所以「删掉一个空行」这种编辑也成立——只有整段记忆的首尾空白会被收掉）；
 * - 默认要求**恰好一处**命中：0 处或多处都失败并说清原因（多处时提示给 all 或把 find 写具体）；
 * - 换完仍是空 → 等于清空（回执里说明）。
 */
export function applyReplace(
  previous: unknown,
  args: { find?: unknown; content?: unknown; all?: unknown }
): MemoryEdit {
  if (missing(args?.find) || '' === textArg(args.find)) {
    throw new Error('replace 需要 find（记忆里要被替换掉的原文）')
  }
  if (missing(args?.content)) {
    throw new Error('replace 需要 content（替换成的内容；要删掉 find 那一段就给空串）')
  }
  const before = normalizeMemory(previous)
  const given = normalizeEol(textArg(args.find))
  const content = normalizeEol(textArg(args.content))
  // 记忆首尾不留空白（存进去就 trim 过），而模型常把整行连结尾换行一起给：
  // 整段找不到时，按去掉结尾换行的原文再找一次（回执里说明这次是调整过的）
  let find = given
  let adjusted = false
  if (!countOccurrences(before, find) && /\n+$/.test(find)) {
    const stripped = find.replace(/\n+$/, '')
    if (stripped && countOccurrences(before, stripped)) {
      find = stripped
      adjusted = true
    }
  }
  if (find === content) throw new Error('find 与要替换成的内容一样，没有可替换的东西')
  const count = countOccurrences(before, find)
  if (!count) {
    throw new Error(
      `记忆里没有找到：${oneLine(given, 80)}` +
        `${before ? '（记忆首尾不留空白：要找整行就照原文给、别带结尾换行）' : '（当前记忆是空的，先用 write 写点东西）'}`
    )
  }
  const all = true === args?.all || 'true' === String(args?.all)
  if (!all && count > 1) {
    throw new Error(
      `记忆里命中 ${count} 处：要全部替换就把 all 给 true，或把 find 写得更具体（带上前后文）`
    )
  }
  const memory = normalizeMemory(
    all ? replaceAll(before, find, content) : replaceFirst(before, find, content)
  )
  const at = Math.max(0, before.indexOf(find))
  const line = before.slice(0, at).split('\n').length
  // 结束行取「命中最后一个字符」所在的行：find 以换行结尾时不多算一行
  const lastLine = before.slice(0, at + Math.max(1, find.length) - 1).split('\n').length
  return {
    memory,
    before: memoryLines(before),
    after: memoryLines(memory),
    changed: memory !== before,
    mode: 'replace',
    cleared: !memory,
    count: all ? count : 1,
    line,
    endLine: Math.max(line, lastLine),
    find,
    adjusted,
    content
  }
}
