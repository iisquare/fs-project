import './assets/main.scss'
import 'element-plus/dist/index.css'
/* 代码块配色：与 fs-admin（Vditor 内置的那套）同一个 github 主题 */
import 'highlight.js/styles/github.css'

import { createApp, nextTick } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useLoadingStore } from './stores/loading'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

/**
 * 把文件拖进窗口时，浏览器默认会让窗口直接跳到那个文件（file://），整个界面就此丢掉。
 * 这里全窗口拦一道：对话区自己处理拖入（见 views/home/HomePage.vue，整块对话区都是拖放区），
 * 别处拖进来的文件不响应也不跳转。只拦「带文件」的那种拖动——文字与链接的拖动照旧，
 * 往文本区里拖一段文字仍然能落进去。
 */
function blockFileDrop(event: DragEvent): void {
  const types = event.dataTransfer?.types
  if (types && Array.from(types).includes('Files')) event.preventDefault()
}

window.addEventListener('dragover', blockFileDrop)
window.addEventListener('drop', blockFileDrop)

/**
 * 立即挂载界面，让顶层加载遮罩从第一帧就在。
 *
 * 登录态还原与用户库初始化交给路由守卫：首次导航会在守卫里等它们完成，
 * 期间 router-view 先空着，App 顶层的遮罩负责动画并挡住交互；
 * 以前把 mount 压到初始化之后，已登录用户会先看到一段空白窗口。
 */
const appLoading = useLoadingStore(pinia)
const releaseBoot = appLoading.show('正在启动客户端...')

app.use(router)
app.mount('#app')

// 首屏路由完成后还要等两帧，确保主界面已经绘制，再撤掉启动遮罩
void router
  .isReady()
  .catch((error) => console.error('[App] Failed to restore session:', error))
  .finally(async () => {
    await nextTick()
    await new Promise<void>((resolve) => {
      requestAnimationFrame(() => requestAnimationFrame(() => resolve()))
    })
    releaseBoot()
  })
