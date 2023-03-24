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
  return { id: '', name: '', top: 0, left: 0, width: 100, height: 100, rotate3d: false, rotateX: 0, rotateY: 0, rotateZ: 1, rotateAngle: 0, opacity: 100, locked: false, hidden: false, level: 500 }
}

const CanvasOptions = () => {
  return { top: 0, width: 800, height: 600, color: '#ffffff', url: '' }
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
  canvasStyle (options) {
    const style = {}
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
    const style = {
      top: `${element.top}px`,
      left: `${element.left}px`,
      width: `${element.width}px`,
      height: `${element.height}px`,
      opacity: element.opacity / 100,
      'z-index': element.level
    }
    if (element.rotate3d) {
      style.transform = `rotate3d(${element.rotateX}, ${element.rotateY}, ${element.rotateZ}, ${element.rotateAngle}deg)`
    } else {
      style.transform = `rotate(${element.rotateAngle}deg)`
    }
    if (element.hidden) {
      style.visibility = 'hidden'
    }
    return style
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
  }, {
    name: '表单',
    children: []
  }]
})
