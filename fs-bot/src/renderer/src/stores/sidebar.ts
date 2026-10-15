import { ref } from 'vue'
import { defineStore } from 'pinia'

/** 侧边栏宽度：拖过一次就记住，下次打开还是这个宽度 */
const WIDTH_STORAGE_KEY = 'fs_sidebar_width'
/** 宽度上下限（px）：再窄放不下会话标题，再宽会把内容区挤没 */
export const SIDEBAR_WIDTH_MIN = 200
export const SIDEBAR_WIDTH_MAX = 480
/** 默认宽度，与「可拖拽调整」之前的固定宽度一致 */
const SIDEBAR_WIDTH_DEFAULT = 260

/** 夹到上下限内的整数值：拖拽给的是小数，存坏的数据也可能是任意数 */
function clampWidth(size: number): number {
  return Math.min(Math.max(Math.round(size), SIDEBAR_WIDTH_MIN), SIDEBAR_WIDTH_MAX)
}

/** 读取上次的宽度：没存过或存了坏值都退回默认，别让脏数据把布局顶歪 */
function restoreWidth(): number {
  const stored = Number.parseFloat(localStorage.getItem(WIDTH_STORAGE_KEY) ?? '')
  if (!Number.isFinite(stored)) return SIDEBAR_WIDTH_DEFAULT
  return clampWidth(stored)
}

export const useSidebarStore = defineStore('sidebar', () => {
  const visible = ref(true)
  const width = ref(restoreWidth())

  function setVisible(v: boolean) {
    visible.value = v
  }

  /**
   * 记录拖拽中的宽度：splitter 的 update:size 给的是像素值，
   * 「260px」这类带单位的字符串也一并认下，统一取整后夹到上下限内。
   */
  function setWidth(size: number | string) {
    const value = Number.parseFloat(String(size))
    if (!Number.isFinite(value)) return
    width.value = clampWidth(value)
  }

  /** 拖拽结束后落库：过程中每一帧都写一次 localStorage 没必要 */
  function saveWidth() {
    localStorage.setItem(WIDTH_STORAGE_KEY, String(width.value))
  }

  return { visible, width, setVisible, setWidth, saveWidth }
})
