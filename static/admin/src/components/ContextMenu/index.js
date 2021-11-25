const CLS_NAME = 'fs-context-menu'

export default {
  name: 'ContextMenu',
  props: {
    menus: {
      type: Array
    },
    event: {
      type: Object
    },
    callback: {
      type: Function
    },
    options: {
      type: Object
    }
  },
  data () {
    return {}
  },
  methods: {
    itemByPath (menus, path) {
      if (!path) return {}
      const key = path.pop()
      for (const item of menus) {
        if (item.key !== key) continue
        if (path.length === 0) return item
        return this.itemByPath(item.children, path)
      }
      return {}
    },
    handleCancel (ev) {
      if (ev && ev.target.classList.contains(CLS_NAME)) {
        return true
      }
      this.handleClick({})
    },
    handleClick (item, key, keyPath) {
      this.$el.remove()
      this.$destroy()
      this.callback(this.itemByPath(this.menus, item.keyPath), item)
    },
    renderIcon (icon) {
      if (icon === 'none' || icon === undefined || icon === '') {
        return null
      }
      const props = {}
      typeof (icon) === 'object' ? props.component = icon : props.type = icon
      return (
        <a-icon {... { props } }/>
      )
    },
    uuid () {
      return 'random-' + new Date().getTime() + ('' + Math.random()).slice(-6)
    },
    renderWalk (menus) {
      return menus.map(item => {
        const dynamicProps = {
          key: item.key || this.uuid(),
          class: CLS_NAME
        }
        switch (item.type) {
          case 'sub-menu':
            return (
              <a-sub-menu {...dynamicProps}>
                <span slot="title">
                  {this.renderIcon(item.icon)}
                  <span>{item.title}</span>
                </span>
                {this.renderWalk(item.children)}
              </a-sub-menu>
            )
          case 'divider':
              return (
                <a-menu-divider />
              )
          default:
            if (item.disabled) {
              return (
                <a-menu-item disabled {...dynamicProps}>
                  {this.renderIcon(item.icon)}
                  {item.title}
                </a-menu-item>
              )
            }
            return (
              <a-menu-item {...dynamicProps}>
                {this.renderIcon(item.icon)}
                {item.title}
              </a-menu-item>
            )
        }
      })
    },
    offset () {
      if (!this.event) return false
      if (document.body.scrollTop + this.event.pageY + this.$el.offsetHeight > document.body.clientHeight) {
        this.$el.style.top = document.body.scrollTop + document.body.clientHeight - this.$el.offsetHeight - 12 + 'px'
      } else {
        this.$el.style.top = this.event.pageY + 'px'
      }
      if (document.body.scrollLeft + this.event.pageX + this.$el.offsetWidth > document.body.clientWidth) {
        this.$el.style.left = document.body.scrollLeft + document.body.clientWidth - this.$el.offsetWidth - 12 + 'px'
      } else {
        this.$el.style.left = this.event.pageX + 'px'
      }
      this.$el.style.visibility = 'visible'
      return true
    }
  },
  mounted () {
    document.body.addEventListener('mousedown', this.handleCancel, false)
    window.setTimeout(this.offset, 10)
  },
  destroyed () {
    document.body.removeEventListener('mousedown', this.handleCancel, false)
  },
  render () {
    const dynamicProps = {
      style: {
        'z-index': 999,
        'border-radius': '3px',
        border: '1px solid #ccc',
        position: 'absolute',
        visibility: 'hidden',
        top: '0px',
        left: '0px'
      },
      props: {
        mode: 'vertical'
      },
      class: CLS_NAME,
      on: {
        click: this.handleClick
      }
    }
    if (this.options.width) dynamicProps.style.width = this.options.width + 'px'
    return (
      <a-menu {...dynamicProps}>
        {this.renderWalk(this.menus)}
      </a-menu>
    )
  }
}
