<script lang="tsx">
// 右键菜单组件 - 用 JSX 渲染的上下文菜单，在指定容器内基于鼠标事件定位显示。
// @prop {HTMLDivElement}  container - 菜单挂载容器（必填）
// @prop {MenuItem[]}      menus     - 菜单项配置数组
// @prop {MouseEvent}      event     - 触发菜单的鼠标事件（必填），用于定位
// @prop {Function}        callback  - 菜单项点击回调，参数：(menu)。menu={} 时表示取消
//
// 菜单项结构 (MenuItem):
//   { type?: 'sub-menu'|'divider', key?: string, title: string, icon?: string, disabled?: boolean, children?: MenuItem[] }
//
// 使用示例:
// const showMenu = (event: MouseEvent) => {
//   const container = document.createElement('div')
//   document.body.appendChild(container)
//   createApp(ContextMenu, { container, event, menus: [...], callback: (menu) => { ... } }).mount(container)
// }
import DesignUtil from '@/utils/DesignUtil'
import { defineComponent, render } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

const handleClick = (menu: any, props: any) => {
  render(null, props.container) // 通过置空容器内容，销毁内部组件
  props.container.remove() // 移除容器
  props.callback(menu) // 执行回调，menu={}时为取消状态
}

const renderMenuItem = (props: any, context: any, menus: any) => {
  return menus.map((item: any) => {
    const dynamicProps = {
      index: item.key || DesignUtil.uuid(),
      disabled: item.disabled,
      onClick () {
        handleClick(item, props)
      },
    }
    switch (item.type) {
      case 'sub-menu':
        return (
          <el-sub-menu {...dynamicProps}>{{
            default: () => renderMenuItem(props, context, item.children),
            title: () => [<LayoutIcon name={ item.icon } />, <span>{ item.title }</span>]
          }}</el-sub-menu>
        )
      case 'divider':
          return (
            <el-divider />
          )
      default:
        return (
          <el-menu-item {...dynamicProps}>
            <LayoutIcon name={ item.icon } />
            <span>{ item.title }</span>
          </el-menu-item>
        )
    }
  })
}

const renderMenu = (props: any, context: any) => {
  return (
    <el-menu>
      { renderMenuItem(props, context, props.menus) }
    </el-menu>
  )
}

const layoutOffset = (container: any, event: any) => {
  if (!event) return false
  if (document.body.scrollTop + event.pageY + container.offsetHeight > document.body.clientHeight) {
    container.style.top = document.body.scrollTop + document.body.clientHeight - container.offsetHeight - 12 + 'px'
  } else {
    container.style.top = event.pageY + 'px'
  }
  if (document.body.scrollLeft + event.pageX + container.offsetWidth > document.body.clientWidth) {
    container.style.left = document.body.scrollLeft + document.body.clientWidth - container.offsetWidth - 12 + 'px'
  } else {
    container.style.left = event.pageX + 'px'
  }
  container.style.visibility = 'visible'
  return true
}

export default defineComponent({
  props: {
    container: { type: HTMLDivElement, required: true },
    menus: { type: null, required: false },
    event: { type: null, required: true },
    callback: { type: Function, required: false },
  },
  setup (props, context) {
    return () => renderMenu(props, context)
  },
  methods: {
    handleMouseDown (event: any) {
      if (!this.container.contains(event.target)) {
        handleClick({}, this.$props) // 传递取消状态
      }
    }
  },
  beforeMount () {
    Object.assign(this.container.style, {
      'z-index': 999,
      'border-radius': '3px',
      border: '1px solid #ccc',
      position: 'absolute',
      visibility: 'hidden',
      top: '0px',
      left: '0px'
    })
    document.body.addEventListener('mousedown', this.handleMouseDown, false)
    window.setTimeout(() => layoutOffset(this.container, this.event), 10)
  },
  beforeUnmount () {
    document.body.removeEventListener('mousedown', this.handleMouseDown, false)
  },
})
</script>

<style lang="scss" scoped>
.el-divider {
  margin: 0 auto;
}
</style>
