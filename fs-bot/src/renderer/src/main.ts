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
