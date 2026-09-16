import { ref } from 'vue'

/**
 * 配置项展开状态 - 列表式配置（变量、条件、分支、分类）按列表下标维护收起/展开，
 * 收起后只展示标题与摘要，为属性面板节省高度、展示更多有效内容；
 * 展开态仅用于面板交互，不写入画布数据。
 *
 * @param defaultOpen 未手动操作过的项的默认展开状态，如「仅一条配置时默认展开」
 */
export const useCollapse = (defaultOpen: () => boolean = () => false) => {
  const opened = ref<Record<number, boolean>>({})
  const isOpen = (index: number) => {
    const state = opened.value[index]
    return undefined === state ? defaultOpen() : true === state
  }
  const open = (index: number) => { opened.value[index] = true }
  const toggle = (index: number) => { opened.value[index] = !isOpen(index) }
  // 删除某项后，其后各项的展开态随下标前移
  const remove = (index: number) => {
    const next: Record<number, boolean> = {}
    Object.keys(opened.value).forEach((key: string) => {
      const current = Number(key)
      if (current === index) return
      next[current > index ? current - 1 : current] = opened.value[current]
    })
    opened.value = next
  }
  return { isOpen, open, toggle, remove }
}

export default useCollapse
