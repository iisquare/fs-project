import { ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import TaskDb from '@renderer/db/TaskDb'
import type { ScheduleTask } from '@renderer/db/TaskDb'
import { isValidCron, matchCron, nextRun } from '@renderer/utils/CronUtil'
import { useProjectStore } from './project'
import { useConversationStore } from './conversation'
import { chatProfile } from './chat'

/**
 * 定时任务：按 cron 表达式在本地触发项目对话。
 *
 * 调度跑在渲染进程（登录态就在这边，调用编排接口不需要额外的凭据通路），
 * 触发后走与手动提问完全一样的链路：新建会话 + 流式调用，页面不开着也照样执行。
 * 表达式为 5 段：分 时 日 月 周，支持通配符、逗号列表、区间与步长写法（如 0/15、1-5/2）。
 */

const TICK_INTERVAL = 30 * 1000

export const useScheduleStore = defineStore('schedule', () => {
  const tasks = ref<ScheduleTask[]>([])
  const loading = ref(false)
  const running = ref(false)

  let timer: ReturnType<typeof setInterval> | null = null
  /** 同一分钟只触发一次：30s 一跳会让每分钟命中两次 */
  let lastTickKey = ''

  async function fetchTasks(): Promise<ScheduleTask[]> {
    loading.value = true
    try {
      const result: any = await TaskDb.list()
      tasks.value = ApiUtil.succeed(result) ? (ApiUtil.data(result) as ScheduleTask[]) : []
    } finally {
      loading.value = false
    }
    return tasks.value
  }

  async function create(data: Partial<ScheduleTask>) {
    const result: any = await TaskDb.create(data)
    await fetchTasks()
    return result
  }

  async function update(id: string, data: Partial<ScheduleTask>) {
    const result: any = await TaskDb.update(id, data)
    await fetchTasks()
    return result
  }

  async function remove(id: string) {
    const result: any = await TaskDb.delete(id)
    await fetchTasks()
    return result
  }

  async function toggle(task: ScheduleTask) {
    return update(task.id, { enabled: !task.enabled })
  }

  /** 启动调度：应用登录后调用一次，重复调用不会叠加定时器 */
  function start(): void {
    if (timer) return
    running.value = true
    timer = setInterval(() => {
      void tick()
    }, TICK_INTERVAL)
  }

  function stop(): void {
    if (timer) clearInterval(timer)
    timer = null
    running.value = false
  }

  async function tick(): Promise<void> {
    const now = new Date()
    const key = `${now.getFullYear()}-${now.getMonth()}-${now.getDate()}-${now.getHours()}-${now.getMinutes()}`
    if (key === lastTickKey) return
    lastTickKey = key
    if (tasks.value.length < 1) return
    for (const task of tasks.value) {
      if (!task.enabled || !isValidCron(task.cron)) continue
      if (!matchCron(task.cron, now)) continue
      await run(task)
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
