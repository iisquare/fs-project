import { h, render } from 'vue'
import ContextMenu from "@/components/Menu/ContextMenu.vue"

const MenuUtil = {
  /**
   * MenuUtil.context(event, [
   *   { type: 'divider' }, // 分隔符
   *   { key: '', icon: '图标', title: '名称', disabled: false }, // 菜单项
   * ], (menu: any) => {}) // 回调函数
   */
  context (event: any, menus: any, callback: Function) {
    const container = document.createElement('div')
    const vnode = h(ContextMenu, { container, menus, event, callback })
    render(vnode, container)
    document.body.appendChild(container)
    event.preventDefault()
    event.stopPropagation()
    return vnode
  }
}

export default MenuUtil
