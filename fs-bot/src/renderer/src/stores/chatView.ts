import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 对话正文的本地显示偏好。
 *
 * 表格默认保持 Vditor 原本的不换行布局，靠横向滚动查看长列；
 * 用户也可以切到自动换行，让列内容跟随消息宽度折行。
 * 这是界面偏好，记在本机即可，切换会话后沿用上次选择。
 */
export type TableDisplayMode = 'wrap' | 'nowrap'

const TABLE_MODE_STORAGE_KEY = 'fs_chat_table_mode'

function restoreTableMode(): TableDisplayMode {
  return 'wrap' === localStorage.getItem(TABLE_MODE_STORAGE_KEY) ? 'wrap' : 'nowrap'
}

export const useChatViewStore = defineStore('chatView', () => {
  const tableMode = ref<TableDisplayMode>(restoreTableMode())

  function setTableMode(value: string): void {
    const next: TableDisplayMode = 'wrap' === value ? 'wrap' : 'nowrap'
    if (next === tableMode.value) return
    tableMode.value = next
    localStorage.setItem(TABLE_MODE_STORAGE_KEY, next)
  }

  return { tableMode, setTableMode }
})
