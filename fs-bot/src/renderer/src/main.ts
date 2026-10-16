import './assets/main.scss'
import 'element-plus/dist/index.css'
/* 代码块配色：与 fs-admin（Vditor 内置的那套）同一个 github 主题 */
import 'highlight.js/styles/github.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

/**
 * 把文件拖进窗口时，浏览器默认会让窗口直接跳到那个文件（file://），整个界面就此丢掉。
 * 这里全窗口拦一道：输入框自己处理拖入（见 components/Chat/ChatComposer.vue），
 * 别处拖进来的文件不响应也不跳转。只拦「带文件」的那种拖动——文字与链接的拖动照旧，
 * 往文本区里拖一段文字仍然能落进去。
 */
function blockFileDrop(event: DragEvent): void {
  const types = event.dataTransfer?.types
  if (types && Array.from(types).includes('Files')) event.preventDefault()
}

window.addEventListener('dragover', blockFileDrop)
window.addEventListener('drop', blockFileDrop)

/** 还原登录态的等待上限：服务端不可达时也把界面放出来，别一直挡在门外 */
const RESTORE_TIMEOUT = 10 * 1000

/**
 * 还原登录态、打开用户库之后再挂载界面。
 *
 * 子组件的 mounted 早于 App 的 mounted，页面一挂载就会读本地库（项目 / 会话 / 定时任务），
 * 而用户库要等登录态还原后由主进程打开：先挂载只会读到
 * 「User database not initialized」，什么都准备不出来。
 */
async function start(): Promise<void> {
  await Promise.race([
    useUserStore(pinia).initialize(),
    new Promise((resolve) => setTimeout(resolve, RESTORE_TIMEOUT))
  ]).catch((error) => console.error('[App] Failed to restore session:', error))
  app.use(router)
  app.mount('#app')
}

void start()
