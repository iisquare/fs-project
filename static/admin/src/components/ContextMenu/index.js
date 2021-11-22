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
    handleCancel (ev) {
      if (ev && ev.target.parentElement === this.$el) {
        return true
      }
      this.handleClick({})
    },
    handleClick (item, key, keyPath) {
      this.$el.remove()
      this.$destroy()
      this.callback(item)
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
          key: item.key || this.uuid()
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
    }
  },
  mounted () {
    document.body.addEventListener('mousedown', this.handleCancel, false)
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
        top: this.event.pageY + 'px',
        left: this.event.pageX + 'px'
      },
      props: {
        mode: 'vertical'
      },
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
