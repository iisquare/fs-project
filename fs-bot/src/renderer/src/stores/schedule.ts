import { ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import TaskDb from '@renderer/db/TaskDb'
import type { ScheduleTask } from '@renderer/db/TaskDb'
import { isValidCron, matchCron, nextRun } from '@renderer/utils/CronUtil'
import { errorMessage } from '@renderer/utils/ErrorUtil'
import { useProjectStore } from './project'
import { useConversationStore } from './conversation'
import { chatProfile } from './chat'

/**
 * 定时任务：按 cron 表达式在本地触发项目对话。
 *
 * 主进程只保留一个指向最近到期时间的定时器，渲染进程只同步任务快照并负责执行；
 * 触发后走与手动提问完全一样的链路：新建会话 + 流式调用，页面不开着也照样执行。
 * 表达式为 5 段：分 时 日 月 周，支持通配符、逗号列表、区间与步长写法（如 0/15、1-5/2）。
 */

export const useScheduleStore = defineStore('schedule', () => {
  const tasks = ref<ScheduleTask[]>([])
  const loading = ref(false)
  const running = ref(false)

  /** 主进程到点通知的订阅；只保留一份，重复 start 不会叠加监听 */
  let unsubscribe: (() => void) | null = null
  let started = false

  type MutationResult = { code: number; message: string; data: unknown }

  async function fetchTasks(): Promise<ScheduleTask[]> {
    loading.value = true
    try {
      const result = await TaskDb.list()
      if (!ApiUtil.succeed(result)) {
        throw new Error(String(ApiUtil.message(result) || '定时任务读取失败'))
      }
      tasks.value = ApiUtil.data(result) as ScheduleTask[]
      await syncScheduler()
    } finally {
      loading.value = false
    }
    return tasks.value
  }

  async function create(data: Partial<ScheduleTask>): Promise<MutationResult> {
    const result = await TaskDb.create(data)
    await fetchTasks()
    return result
  }

  async function update(id: string, data: Partial<ScheduleTask>): Promise<MutationResult> {
    const result = await TaskDb.update(id, data)
    await fetchTasks()
    return result
  }

  async function remove(id: string): Promise<MutationResult> {
    const result = await TaskDb.delete(id)
    await fetchTasks()
    return result
  }

  async function toggle(task: ScheduleTask): Promise<MutationResult> {
    return update(task.id, { enabled: !task.enabled })
  }

  /** 启动调度：应用登录后调用一次，主进程只保留一个按最近到期时间唤醒的定时器 */
  async function start(): Promise<void> {
    if (started) return
    started = true
    if (!unsubscribe) {
      unsubscribe = window.ipc.schedule.onTrigger((ids) => {
        void onTriggered(ids)
      })
    }
    try {
      if (!(await syncScheduler())) throw new Error('定时调度未能启动')
      running.value = true
    } catch (error) {
      started = false
      running.value = false
      unsubscribe?.()
      unsubscribe = null
      throw error
    }
  }

  function stop(): void {
    started = false
    running.value = false
    unsubscribe?.()
    unsubscribe = null
    void window.ipc.schedule.stop().catch((error) => {
      console.error('[schedule] Failed to stop scheduler:', error)
    })
  }

  /** 任务快照同步到主进程：只传调度所需字段，正文留在渲染进程 */
  async function syncScheduler(): Promise<boolean> {
    if (!started) return false
    try {
      await window.ipc.schedule.sync(
        tasks.value.map((task) => ({
          id: task.id,
          cron: task.cron,
          enabled: task.enabled
        }))
      )
      return true
    } catch (error) {
      // 任务已经落库，页面不该因为调度同步失败显示保存失败；留在控制台便于排查
      console.error('[schedule] Failed to sync scheduler:', error)
      return false
    }
  }

  /** 主进程到点：按标识取当前任务，逐条执行；一条失败不影响其他任务 */
  async function onTriggered(ids: string[]): Promise<void> {
    if (!started || !Array.isArray(ids) || !ids.length) return
    const wanted = new Set(ids.map((id) => String(id)))
    for (const task of tasks.value) {
      if (!wanted.has(task.id) || !task.enabled || !isValidCron(task.cron)) continue
      try {
        await run(task)
      } catch (error) {
        console.error(`[schedule] Task ${task.id} failed:`, error)
      }
    }
  }

  /** 立即执行一次（页面上的「运行一次」与调度触发共用同一条链路） */
  async function run(task: ScheduleTask): Promise<boolean> {
    const projectStore = useProjectStore()
    const project = projectStore.find(task.projectId)
    if (!project) {
      await TaskDb.report(task.id, 'failed', '项目不存在或已删除')
      await fetchTasks()
      return false
    }
    try {
      const conversationStore = useConversationStore()
      const conversation = await conversationStore.create(project, `${task.name}`)
      // 会话 store 负责把这条新会话落进历史，chat store 只管把这一轮跑起来
      await conversationStore.send({
        conversationId: conversation.id,
        ...chatProfile(project),
        query: task.prompt,
        background: true
      })
      await TaskDb.report(task.id, 'triggered', `已发起对话：${task.prompt.slice(0, 40)}`)
      await fetchTasks()
      return true
    } catch (error) {
      await TaskDb.report(task.id, 'failed', errorMessage(error, '定时任务执行失败')).catch(
        () => {}
      )
      await fetchTasks().catch(() => {})
      throw error
    }
  }

  return {
    tasks,
    loading,
    running,
    fetchTasks,
    create,
    update,
    remove,
    toggle,
    run,
    start,
    stop
  }
})

export { isValidCron, matchCron, nextRun }
