import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 对话正文的本地显示偏好。
 *
 * 表格默认保持 Vditor 原本的不换行布局，靠横向滚动查看长列；
 * 代码块同样默认保留原始行宽，由用户决定是否跟随消息宽度折行。
 * 这是界面偏好，记在本机即可，切换会话后沿用上次选择。
 */
export type ContentDisplayMode = 'wrap' | 'nowrap'
export type TableDisplayMode = ContentDisplayMode
export type CodeDisplayMode = ContentDisplayMode

const TABLE_MODE_STORAGE_KEY = 'fs_chat_table_mode'
const CODE_MODE_STORAGE_KEY = 'fs_chat_code_mode'

function restoreMode(key: string): ContentDisplayMode {
  return 'wrap' === localStorage.getItem(key) ? 'wrap' : 'nowrap'
}

export const useChatViewStore = defineStore('chatView', () => {
  const tableMode = ref<TableDisplayMode>(restoreMode(TABLE_MODE_STORAGE_KEY))
  const codeMode = ref<CodeDisplayMode>(restoreMode(CODE_MODE_STORAGE_KEY))

  function setTableMode(value: string): void {
    const next: TableDisplayMode = 'wrap' === value ? 'wrap' : 'nowrap'
    if (next === tableMode.value) return
    tableMode.value = next
    localStorage.setItem(TABLE_MODE_STORAGE_KEY, next)
  }

  function setCodeMode(value: string): void {
    const next: CodeDisplayMode = 'wrap' === value ? 'wrap' : 'nowrap'
    if (next === codeMode.value) return
    codeMode.value = next
    localStorage.setItem(CODE_MODE_STORAGE_KEY, next)
  }

  return { tableMode, setTableMode, codeMode, setCodeMode }
})
