import Menu from 'ant-design-vue/es/menu'
import Icon from 'ant-design-vue/es/icon'

export default {
  name: 'SMenu',
  props: {
    menu: {
      type: Array,
      required: true
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    },
    mode: {
      type: String,
      required: false,
      default: 'inline'
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false
    }
  },
  data () {
    return {
      openKeys: [],
      selectedKeys: [],
      cachedOpenKeys: []
    }
  },
  computed: {
    rootSubmenuKeys: vm => {
      const keys = []
      vm.menu.forEach(item => keys.push(item.path))
      return keys
    }
  },
  mounted () {
    this.updateMenu()
  },
  watch: {
    collapsed (val) {
      if (val) {
        this.cachedOpenKeys = this.openKeys.concat()
        this.openKeys = []
      } else {
        this.openKeys = this.cachedOpenKeys
      }
    },
    $route: function () {
      this.updateMenu()
    }
  },
  methods: {
    // select menu item
    onOpenChange (openKeys) {
      // 在水平模式下时执行，并且不再执行后续
      if (this.mode === 'horizontal') {
        this.openKeys = openKeys
        return
      }
      // 非水平模式时
      const latestOpenKey = openKeys.find(key => !this.openKeys.includes(key))
      if (!this.rootSubmenuKeys.includes(latestOpenKey)) {
        this.openKeys = openKeys
      } else {
        this.openKeys = latestOpenKey ? [latestOpenKey] : []
      }
    },
    onSelect ({ item, key, selectedKeys }) {
      this.selectedKeys = selectedKeys
      this.$emit('select', { item, key, selectedKeys })
    },
    updateMenu () {
      const path = this.$route.path.replace(/(^[/#]*)|([/#]*$)/, '')
      const paths = path.split('/')
      paths.unshift('')
      this.selectedKeys = [paths.join('/')]
      const openKeys = []
      if (this.mode === 'inline' && paths.length > 1) {
        for (let index = 2; index <= paths.length; index++) {
          openKeys.push(paths.slice(0, index).join('/'))
        }
        const meta = this.$route.meta
        if (meta && meta.parents) {
          openKeys.push(...meta.parents)
        }
      }
      this.collapsed ? (this.cachedOpenKeys = openKeys) : (this.openKeys = openKeys)
    },

    // render
    renderItem (menu) {
      if (!menu.hidden) {
        return menu.children && menu.children.length > 0 && !menu.hideChildrenInMenu ? this.renderSubMenu(menu) : this.renderMenuItem(menu)
      }
      return null
    },
    renderMenuItem (menu) {
      const target = menu.target || null
      const CustomTag = target && 'a' || 'router-link'
      const props = { to: menu.url }
      const attrs = { href: menu.url, target: menu.target }

      if (menu.children && menu.hideChildrenInMenu) {
        // 把有子菜单的 并且 父菜单是要隐藏子菜单的
        // 都给子菜单增加一个 hidden 属性
        // 用来给刷新页面时， selectedKeys 做控制用
        menu.children.forEach(item => {
          item = Object.assign(item, { hidden: true })
        })
      }

      return (
        <Menu.Item {...{ key: menu.url }}>
          <CustomTag {...{ props, attrs }}>
            {this.renderIcon(menu.icon)}
            <span>{menu.name}</span>
          </CustomTag>
        </Menu.Item>
      )
    },
    renderSubMenu (menu) {
      const itemArr = []
      if (!menu.hideChildrenInMenu) {
        menu.children.forEach(item => itemArr.push(this.renderItem(item)))
      }
      return (
        <Menu.SubMenu {...{ key: menu.url }}>
          <span slot="title">
            {this.renderIcon(menu.icon)}
            <span>{menu.name}</span>
          </span>
          {itemArr}
        </Menu.SubMenu>
      )
    },
    renderIcon (icon) {
      if (icon === 'none' || icon === undefined || icon === '') {
        return null
      }
      const props = {}
      typeof (icon) === 'object' ? props.component = icon : props.type = icon
      return (
        <Icon {... { props } }/>
      )
    }
  },

  render () {
    const dynamicProps = {
      props: {
        mode: this.mode,
        theme: this.theme,
        openKeys: this.openKeys,
        selectedKeys: this.selectedKeys
      },
      on: {
        openChange: this.onOpenChange,
        select: this.onSelect
      }
    }

    const menuTree = this.menu.map(item => {
      if (item.hidden) {
        return null
      }
      return this.renderItem(item)
    })

    return (<Menu {...dynamicProps}>{menuTree}</Menu>)
  }
}
