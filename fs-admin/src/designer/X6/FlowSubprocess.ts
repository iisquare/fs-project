import DesignUtil from '@/utils/DesignUtil'
import { Node, type NodeMetadata } from '@antv/x6'

// 容器节点边框与画布内其它节点卡片保持一致（浅灰细边框 + 圆角）
export const SubprocessStroke = '#d5dae0'

export default class FlowSubprocess extends Node {

  meta: NodeMetadata
  collapsed: Boolean = false

  constructor (metadata?: NodeMetadata) {
    super(metadata)
    this.meta = metadata ?? {}
    this.attr('label/text', this.meta.data.name)
  }

  postprocess () {
    this.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
      Object.assign(this.meta, { data: current })
      this.attr('label/text', this.meta.data.name)
    }))
    this.toggleCollapse(false)
  }

  isCollapsed () {
    return this.collapsed
  }

  toggleCollapse (collapsed: any = null) {
    const target = collapsed === null ? !this.collapsed : collapsed
    if (target) {
      this.attr('buttonSign', { d: 'M 1 5 9 5 M 5 1 5 9' })
      Object.assign(this.meta, this.getSize())
      this.resize(150, 32)
    } else {
      this.attr('buttonSign', { d: 'M 2 5 8 5' })
      if (this.meta) {
        this.resize(this.meta.width || 0, this.meta.height || 0)
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
      rx: 8,
      ry: 8,
      refWidth: '100%',
      refHeight: '100%',
      stroke: SubprocessStroke,
      strokeWidth: 1,
      fill: '#ffffff',
      fillOpacity: 0.5
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
      fontSize: 12,
      refX: '50%',
      refY: 10,
      textAnchor: 'middle'
    }
  }
})
