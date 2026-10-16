import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 右侧文件区：展开状态、宽度与当前页签都记在本机，下次打开保持一致。
 * 与左侧会话栏（stores/sidebar）分开两份：两者各自拖各自的宽度，互不影响。
 */
const OPEN_STORAGE_KEY = 'fs_files_open'
const WIDTH_STORAGE_KEY = 'fs_files_width'
const TAB_STORAGE_KEY = 'fs_files_tab'

/** 宽度上下限（px）：再窄放不下文件名，再宽会把对话区挤没 */
export const FILES_WIDTH_MIN = 200
export const FILES_WIDTH_MAX = 520
/** 默认宽度：够放一列文件名的同时不挤占对话区 */
const FILES_WIDTH_DEFAULT = 268

/** 页签：工作区目录树 / 这段对话交付的文件 */
export type FilesTab = 'workspace' | 'delivered'

/** 夹到上下限内的整数值：拖拽给的是小数，存坏的数据也可能是任意数 */
function clampWidth(size: number): number {
  return Math.min(Math.max(Math.round(size), FILES_WIDTH_MIN), FILES_WIDTH_MAX)
}

/** 读取上次的宽度：没存过或存了坏值都退回默认，别让脏数据把布局顶歪 */
function restoreWidth(): number {
  const stored = Number.parseFloat(localStorage.getItem(WIDTH_STORAGE_KEY) ?? '')
  if (!Number.isFinite(stored)) return FILES_WIDTH_DEFAULT
  return clampWidth(stored)
}

export const useFilesStore = defineStore('files', () => {
  /** 展开状态：没存过时默认展开（第一次进对话就能看见工作区里有什么） */
  const visible = ref(localStorage.getItem(OPEN_STORAGE_KEY) !== '0')
  const width = ref(restoreWidth())
  const tab = ref<FilesTab>(
    'delivered' === localStorage.getItem(TAB_STORAGE_KEY) ? 'delivered' : 'workspace'
  )

  function setVisible(value: boolean): void {
    visible.value = value
    localStorage.setItem(OPEN_STORAGE_KEY, value ? '1' : '0')
  }

  function toggle(): void {
    setVisible(!visible.value)
  }

  /**
   * 记录拖拽中的宽度：splitter 的 update:size 给的是像素值，
   * 「260px」这类带单位的字符串也一并认下，统一取整后夹到上下限内。
   */
  function setWidth(size: number | string): void {
    const value = Number.parseFloat(String(size))
    if (!Number.isFinite(value)) return
    width.value = clampWidth(value)
  }

  /** 拖拽结束后落库：过程中每一帧都写一次 localStorage 没必要 */
  function saveWidth(): void {
    localStorage.setItem(WIDTH_STORAGE_KEY, String(width.value))
  }

  function setTab(value: string | number): void {
    tab.value = 'delivered' === String(value) ? 'delivered' : 'workspace'
    localStorage.setItem(TAB_STORAGE_KEY, tab.value)
  }

  return { visible, width, tab, setVisible, toggle, setWidth, saveWidth, setTab }
})
