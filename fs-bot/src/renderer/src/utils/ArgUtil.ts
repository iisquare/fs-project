/**
 * 工具参数的口径：**`undefined` 与 `null` 都算「没给」**。
 *
 * JSON 里的 `null` 是模型在表达「这个参数没有」；把它当空串会静默做出破坏性动作
 * （清空文件、删掉一段记忆），把它当文本又会往文件里写一个字面的 "null"——
 * 两种都不该发生，所以一律按缺参数处理，由调用方报错并说明「要表示空就给空串」。
 * 空串 `''` 是**给了**，语义由各工具自己定（例如 `file_op` 的 write 空串 = 清空文件）。
 */

/** 这个参数算不算「没给」：undefined 与 null 都算 */
export function missing(value: unknown): boolean {
  return null === value || undefined === value
}

/** 取字符串参数：没给给空串，其余按 String() 转（调用方自己判断哪种情况该报错） */
export function textArg(value: unknown): string {
  return missing(value) ? '' : String(value)
}
