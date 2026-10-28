import type { BrowserWindow } from 'electron'
import { isValidCron, nextRun } from '../shared/cron'

/** 渲染进程只同步调度需要的最小字段，避免每轮把任务正文等无关内容送到主进程 */
export interface ScheduleEntry {
  id: string
  cron: string
  enabled: boolean
}

interface ScheduledEntry extends ScheduleEntry {
  /** 下一次到期时间戳；null 表示表达式无效或一年内不会触发 */
  nextAt: number | null
}

/**
 * 客户端定时调度：主进程只保留一个定时器，始终指向最近一次到期时间。
 *
 * 渲染进程在登录后同步任务快照，任务变更后再同步一次；到点通过 IPC 通知渲染进程执行。
 * 定时器最长睡 1 小时再重新计算，既避免固定轮询，也能在系统时间调整后及时校正。
 */
export class ScheduleManager {
  private window: BrowserWindow | null = null
  private tasks = new Map<string, ScheduledEntry>()
  private timer: ReturnType<typeof setTimeout> | null = null
  /** 休眠期间不触发任务；恢复后按当前时间重算，不补跑错过的到期点 */
  private suspended = false

  private static readonly MAX_TIMER_DELAY = 60 * 60 * 1000

  attach(window: BrowserWindow): void {
    this.window = window
  }

  /**
   * 用渲染进程的最新任务快照替换调度表。
   *
   * 未变化的任务保留原来的 nextAt：改一个任务不会把其他任务的下次执行时间重新推迟。
   */
  sync(input: unknown): void {
    const now = Date.now()
    const next = new Map<string, ScheduledEntry>()
    const rows = Array.isArray(input) ? input : []

    for (const raw of rows) {
      const row = raw as Partial<ScheduleEntry> | null
      const id = String(row?.id ?? '').trim()
      const cron = String(row?.cron ?? '').trim()
      const enabled = true === row?.enabled
      if (!id || !enabled || !isValidCron(cron)) continue

      const existing = this.tasks.get(id)
      if (existing && existing.cron === cron && existing.enabled === enabled) {
        next.set(id, existing)
        continue
      }

      next.set(id, {
        id,
        cron,
        enabled,
        nextAt: nextRun(cron, new Date(now))?.getTime() ?? null
      })
    }

    this.tasks = next
    this.arm()
  }

  /** 系统休眠：先撤掉定时器，避免恢复瞬间把休眠期间错过的任务当作到期任务触发 */
  suspend(): void {
    this.suspended = true
    this.clearTimer()
  }

  /** 系统恢复：只按当前时间重算下一次执行，不补跑 */
  resume(): void {
    this.suspended = false
    this.recalculate()
    this.arm()
  }

  /** 退出登录或窗口关闭：取消定时器并清空调度表 */
  stop(): void {
    this.suspended = false
    this.clearTimer()
    this.tasks.clear()
  }

  private arm(): void {
    this.clearTimer()
    if (this.suspended) return
    const window = this.window
    if (!window || window.isDestroyed()) return

    let earliest = Number.POSITIVE_INFINITY
    for (const task of this.tasks.values()) {
      if (task.nextAt !== null && task.nextAt < earliest) earliest = task.nextAt
    }
    if (!Number.isFinite(earliest)) return

    const delay = Math.min(Math.max(0, earliest - Date.now()), ScheduleManager.MAX_TIMER_DELAY)
    this.timer = setTimeout(() => this.fire(), delay)
    // 定时器不阻止应用退出：应用关闭后不再执行任务
    this.timer.unref()
  }

  private fire(): void {
    this.timer = null
    const window = this.window
    if (!window || window.isDestroyed()) return
    // 恢复事件与定时器回调存在先后竞态：只要还标记为休眠中，就只重算不触发
    if (this.suspended) {
      this.recalculate()
      this.arm()
      return
    }

    const now = Date.now()
    const due: string[] = []
    for (const task of this.tasks.values()) {
      if (task.nextAt !== null && task.nextAt <= now) due.push(task.id)
      // 每次真正醒来都按当前时间重算一次：系统时间或时区变化后不用重启应用才能校正
      task.nextAt = nextRun(task.cron, new Date(now))?.getTime() ?? null
    }

    if (due.length) {
      try {
        window.webContents.send('schedule:trigger', due)
      } catch (error) {
        console.error('[schedule] Failed to notify renderer:', error)
      }
    }
    this.arm()
  }

  /** 以当前时间为起点重算所有任务的下一次执行时间 */
  private recalculate(): void {
    const now = Date.now()
    for (const task of this.tasks.values()) {
      task.nextAt = nextRun(task.cron, new Date(now))?.getTime() ?? null
    }
  }

  private clearTimer(): void {
    if (!this.timer) return
    clearTimeout(this.timer)
    this.timer = null
  }
}
