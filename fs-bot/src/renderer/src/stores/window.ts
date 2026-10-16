import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 窗口置顶（钉住）状态：钉住后窗口收成 400×600 的置顶窄条（主进程见 main/index.ts 的
 * window:toggle-pin），界面上「钉住」就等于只留对话 —— 左侧会话栏与右侧文件区一并收起、
 * 对话头部那个文件区开关也不再出现。
 *
 * 状态放在这里而不是 TitleBar 自己手里：管收起的 TitleBar（components/Layout）与
 * 藏开关的 HomePage（views/home）不在一条父子线上，共读这一份才不会各记一个相反的结论。
 *
 * 只记在内存里、不落本机记录：主进程那份窗口位置同样随进程消失，重启后回到常规窗口，
 * 界面若还留着一个「已钉住」的结论，两边的口径就打架了。
 */
export const useWindowStore = defineStore('window', () => {
  const pinned = ref(false)

  return { pinned }
})
