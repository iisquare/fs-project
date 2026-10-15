/**
 * 消息时间：当天只给时分，往日补上月日（跨年再补年份）。
 * 气泡下方展示用，比「几分钟前」更便于回看历史。
 */
export function formatMessageTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const time = date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
  if (date.toDateString() === now.toDateString()) return time
  const day = date.toLocaleDateString('zh-CN', {
    ...(date.getFullYear() === now.getFullYear() ? {} : { year: 'numeric' }),
    month: '2-digit',
    day: '2-digit'
  })
  return `${day} ${time}`
}

/**
 * 工具耗时：不足 1 秒给毫秒，超过给一位小数的秒。
 * 没有耗时（还没跑完 / 没有记录）给空串，由调用方决定这一格要不要占位。
 */
export function formatDuration(ms: number): string {
  const value = Number(ms ?? 0)
  if (!value) return ''
  return value < 1000 ? `${value}ms` : `${(value / 1000).toFixed(1)}s`
}

/** 版本发布日期等「只关心哪一天」的场景：毫秒时间戳格式化成 YYYY-MM-DD（没有时间戳给空串） */
export function formatDate(time?: number | null): string {
  const value = Number(time ?? 0)
  if (!value) return ''
  const date = new Date(value)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function groupByTime<T extends { updatedAt: string }>(
  items: T[]
): {
  today: T[]
  yesterday: T[]
  thisWeek: T[]
  earlier: T[]
} {
  const now = new Date()
  const todayStr = now.toDateString()
  const yesterdayStr = new Date(now.getTime() - 86400000).toDateString()
  const weekAgo = new Date(now.getTime() - 7 * 86400000)

  const groups = {
    today: [] as T[],
    yesterday: [] as T[],
    thisWeek: [] as T[],
    earlier: [] as T[]
  }

  for (const item of items) {
    const d = new Date(item.updatedAt)
    const ds = d.toDateString()
    if (ds === todayStr) {
      groups.today.push(item)
    } else if (ds === yesterdayStr) {
      groups.yesterday.push(item)
    } else if (d >= weekAgo) {
      groups.thisWeek.push(item)
    } else {
      groups.earlier.push(item)
    }
  }

  return groups
}

export function formatTime(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const diff = now.getTime() - d.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  if (d.getFullYear() === now.getFullYear()) {
    return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

export const GROUP_LABELS: Record<string, string> = {
  today: '今天',
  yesterday: '昨天',
  thisWeek: '本周',
  earlier: '更早'
}
