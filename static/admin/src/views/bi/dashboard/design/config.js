import UIUtil from '@/utils/ui'

const config = {
  generateItem (widget, ev) {
    const options = widget.options()
    return Object.assign(options, {
      id: UIUtil.uuid(widget.type + '-'),
      name: widget.name,
      type: widget.type,
      top: ev.offsetY - options.height / 2,
      left: ev.offsetX - options.width / 2
    })
  }
}

const ElementOptions = () => {
  return { id: '', name: '', top: 0, left: 0, width: 100, height: 100, rotate: 0, opacity: 100, locked: false, hidden: false, level: 500 }
}

const CanvasOptions = () => {
  return { top: 0, width: 800, height: 600, color: '#0e2a42', url: '' }
}

const TextOptions = () => {
  return Object.assign(ElementOptions(), {
    content: '文本内容',
    style: {},
    link: {},
    marquee: {}
  })
}

export default Object.assign(config, {
  canvas: {
    options: CanvasOptions, property: () => import('./CanvasProperty')
  },
  canvasStyle (options, zoom) {
    const style = {
      top: `${options.top}px`,
      width: `${options.width}px`,
      height: `${options.height}px`
    }
    if (zoom < 1) {
      style.transform = `scale(${window.innerWidth / options.width}, ${window.innerHeight / options.height})`
    } else {
      style.transform = `scale(${zoom / 100})`
    }
    if (options.color) {
      style['background-color'] = options.color
    }
    if (options.url) {
      style['background-repeat'] = 'no-repeat'
      style['background-position'] = 'center'
      style['background-image'] = `url(${options.url})`
    }
    return style
  },
  elementStyle (element) {
    return {
      top: `${element.top}px`,
      left: `${element.left}px`,
      width: `${element.width}px`,
      height: `${element.height}px`,
      rotate: `${element.rotate}deg`,
      opacity: element.opacity / 100,
      'z-index': element.level
    }
  },
  widgetTransientMap: null,
  widgetByType (type) {
    if (this.widgetTransientMap === null) {
      const map = {}
      this.widgets.forEach(widget => {
        widget.children.forEach(item => {
          map[item.type] = item
        })
      })
      this.widgetTransientMap = map
    }
    return this.widgetTransientMap[type]
  },
  widgetDefaults (type) {
    return this.widgetByType(type).options()
  },
  widgets: [{
    name: '图文',
    children: [
      { type: 'text', name: '文本框', image: '/bi/widgets/text.png', options: TextOptions, property: () => import('./TextProperty'), element: () => import('./TextElement') }
    ]
  }, {
    name: '图表',
    children: []
  }, {
    name: '装饰',
    children: []
  }, {
    name: '表格',
    children: []
  }]
})
