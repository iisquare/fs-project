import { Node } from '@antv/x6'

export default class FlowSubprocess extends Node {
  constructor (meta) {
    super(meta)
    this.meta = meta
    this.collapsed = false
    this.attr('label/text', this.meta.data.title)
  }

  postprocess () {
    this.on('change:data', ({ current }) => {
      Object.assign(this.meta, { data: current })
      this.attr('label/text', this.meta.data.title)
    })
    this.toggleCollapse(false)
  }

  isCollapsed () {
    return this.collapsed
  }

  toggleCollapse (collapsed = null) {
    const target = collapsed === null ? !this.collapsed : collapsed
    if (target) {
      this.attr('buttonSign', { d: 'M 1 5 9 5 M 5 1 5 9' })
      Object.assign(this.meta, this.getSize())
      this.resize(150, 32)
    } else {
      this.attr('buttonSign', { d: 'M 2 5 8 5' })
      if (this.meta) {
        this.resize(this.meta.width, this.meta.height)
      }
    }
    this.collapsed = target
  }
}

FlowSubprocess.config({
  markup: [
    {
      tagName: 'rect',
      selector: 'body'
    },
    {
      tagName: 'text',
      selector: 'label'
    },
    {
      tagName: 'g',
      selector: 'buttonGroup',
      children: [
        {
          tagName: 'rect',
          selector: 'button',
          attrs: {
            'pointer-events': 'visiblePainted'
          }
        },
        {
          tagName: 'path',
          selector: 'buttonSign',
          attrs: {
            fill: 'none',
            'pointer-events': 'none'
          }
        }
      ]
    }
  ],
  attrs: {
    body: {
      rx: 10,
      ry: 10,
      refWidth: '100%',
      refHeight: '100%',
      stroke: 'rgb(34, 36, 42)',
      strokeWidth: '1px',
      fill: '#ffffff',
      fillOpacity: 0.3
    },
    buttonGroup: {
      refX: 8,
      refY: 8
    },
    button: {
      height: 14,
      width: 16,
      rx: 2,
      ry: 2,
      fill: '#f5f5f5',
      stroke: '#ccc',
      cursor: 'pointer',
      event: 'node:collapse'
    },
    buttonSign: {
      refX: 3,
      refY: 2,
      stroke: '#808080'
    },
    label: {
      fontSzie: 12,
      refX: '50%',
      refY: 10,
      textAnchor: 'middle'
    }
  }
})
