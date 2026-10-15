/**
 * cron 表达式（5 段：分 时 日 月 周）的最小可用实现。
 *
 * 支持通配符、问号、逗号列表、区间与步长（如 0/15、1-5/2、9-17/2）。
 * 日与周同时指定具体值时按「或」判断，与常见 cron 实现一致；其中一个为通配符时按另一个判断。
 * 定时任务只在客户端触发，够用即可，不引入额外依赖。
 */

const FIELD_RANGES: Array<[number, number]> = [
  [0, 59], // 分
  [0, 23], // 时
  [1, 31], // 日
  [1, 12], // 月
  [0, 6] // 周（0 = 周日）
]

/** 校验表达式：段数与每段的取值都要合法 */
export function isValidCron(cron: string): boolean {
  const fields = String(cron || '')
    .trim()
    .split(/\s+/)
  if (fields.length !== 5) return false
  return fields.every((field, index) =>
    validField(field, FIELD_RANGES[index][0], FIELD_RANGES[index][1])
  )
}

/** 判断某个时刻是否命中表达式 */
export function matchCron(cron: string, date: Date): boolean {
  const fields = String(cron || '')
    .trim()
    .split(/\s+/)
  if (fields.length !== 5) return false
  const values = [
    date.getMinutes(),
    date.getHours(),
    date.getDate(),
    date.getMonth() + 1,
    date.getDay()
  ]
  let dayMatched = true
  let weekMatched = true
  for (let index = 0; index < 5; index++) {
    const matched = matchField(
      fields[index],
      values[index],
      FIELD_RANGES[index][0],
      FIELD_RANGES[index][1]
    )
    if (2 === index) dayMatched = matched
    else if (4 === index) weekMatched = matched
    else if (!matched) return false
  }
  const dayRestricted = !isWildcard(fields[2])
  const weekRestricted = !isWildcard(fields[4])
  if (dayRestricted && weekRestricted) return dayMatched || weekMatched
  return dayMatched && weekMatched
}

/** 计算下一次触发时间：表达式非法或一年内不触发时返回 null */
export function nextRun(cron: string, from = new Date()): Date | null {
  if (!isValidCron(cron)) return null
  const cursor = new Date(from.getTime() + 60 * 1000)
  cursor.setSeconds(0, 0)
  for (let i = 0; i < 366 * 24 * 60; i++) {
    if (matchCron(cron, cursor)) return new Date(cursor)
    cursor.setMinutes(cursor.getMinutes() + 1)
  }
  return null
}

function isWildcard(field: string): boolean {
  return '*' === field || '?' === field
}

function validField(field: string, min: number, max: number): boolean {
  if (!field) return false
  if (isWildcard(field)) return true
  return field.split(',').every((part) => validPart(part.trim(), min, max))
}

function validPart(part: string, min: number, max: number): boolean {
  if (!part) return false
  const [range, stepText] = part.split('/')
  if (stepText !== undefined) {
    const step = Number(stepText)
    if (!Number.isInteger(step) || step < 1) return false
  }
  if (isWildcard(range)) return true
  const bounds = range.split('-')
  if (bounds.length > 2) return false
  const start = Number(bounds[0])
  // 带步长的单值写法（如 0/15）表示「从 0 开始按步长取到区间上界」
  const end = bounds.length > 1 ? Number(bounds[1]) : stepText === undefined ? start : max
  if (!Number.isInteger(start) || !Number.isInteger(end)) return false
  if (start < min || end > max || start > end) return false
  return true
}

function matchField(field: string, value: number, min: number, max: number): boolean {
  if (isWildcard(field)) return true
  return field.split(',').some((part) => matchPart(part.trim(), value, min, max))
}

function matchPart(part: string, value: number, min: number, max: number): boolean {
  if (!part) return false
  const [range, stepText] = part.split('/')
  const step = stepText ? Number(stepText) : 1
  if (!Number.isFinite(step) || step < 1) return false
  let start = min
  let end = max
  if (!isWildcard(range)) {
    const bounds = range.split('-')
    start = Number(bounds[0])
    // 与校验口径一致：0/15 表示从 0 开始按 15 分钟取到区间上界
    end = bounds.length > 1 ? Number(bounds[1]) : stepText ? max : start
    if (!Number.isFinite(start) || !Number.isFinite(end)) return false
  }
  if (value < start || value > end) return false
  return (value - start) % step === 0
}
