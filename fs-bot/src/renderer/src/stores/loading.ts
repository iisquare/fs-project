import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const DEFAULT_TEXT = '正在加载...'

interface LoadingTask {
  id: number
  text: string
}

/**
 * 全局加载遮罩。
 *
 * 只用于客户端启动与登录页进入主界面这类整窗切换：各自领一个任务、
 * 完成后只撤销自己的那一份；只要还有任务没结束，遮罩就继续挡在最上层。
 * 普通路由切换和页面自己的业务数据加载都不走这里。
 */
export const useLoadingStore = defineStore('loading', () => {
  const tasks = ref<LoadingTask[]>([])
  let nextId = 0

  const visible = computed(() => tasks.value.length > 0)
  const text = computed(() => tasks.value[tasks.value.length - 1]?.text ?? DEFAULT_TEXT)

  /** 开始一段加载，返回只可调用一次的结束函数 */
  function show(message = DEFAULT_TEXT): () => void {
    const id = ++nextId
    const task: LoadingTask = { id, text: message }
    tasks.value.push(task)
    let done = false
    return () => {
      if (done) return
      done = true
      tasks.value = tasks.value.filter((item) => item.id !== id)
    }
  }

  return { visible, text, show }
})
