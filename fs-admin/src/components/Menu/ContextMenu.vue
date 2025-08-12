<script lang="tsx">
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
