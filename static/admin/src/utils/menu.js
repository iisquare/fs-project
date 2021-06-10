import Vue from 'vue'
import ContextMenu from '@/components/ContextMenu'

const ContextMenuConstructor = Vue.extend(ContextMenu)

const MenuUtil = {
  context (ev, data, callback) {
    const menu = new ContextMenuConstructor()
    menu.menus = data
    menu.event = ev
    menu.callback = callback
    menu.$mount()
    document.body.appendChild(menu.$el)
    ev.preventDefault()
    ev.stopPropagation()
    return menu
  }
}

export default MenuUtil
