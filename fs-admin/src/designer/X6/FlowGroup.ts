import DesignUtil from '@/utils/DesignUtil'
import { Node, Graph } from '@antv/x6'

export default class FlowGroup extends Node {

  meta: Node.Metadata
  collapsed: Boolean = false

  constructor (metadata?: Node.Metadata) {
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

  addTransientEdge (graph: Graph) {
    const node = this
    const incoming = graph.getConnectedEdges(node, { deep: true, incoming: true })
    const outgoing = graph.getConnectedEdges(node, { deep: true, outgoing: true })
    incoming.forEach((item: any) => {
      if (!item.source.cell || !item.source.port) return
      graph.addEdge({
        shape: 'flow-edge',
        source: { cell: item.source.cell, port: item.source.port },
        target: node,
        data: { name: '', description: '' }
      })
    })
    outgoing.forEach((item: any) => {
      if (!item.target.cell || !item.target.port) return
      graph.addEdge({
        shape: 'flow-edge',
        source: node,
        target: { cell: item.target.cell, port: item.target.port },
        data: { name: '', description: '' }
      })
    })
  }

  removeTransientEdge (graph: Graph) {
    const node = this
    const edges = graph.getConnectedEdges(node)
    edges.forEach(edge => {
      graph.removeEdge(edge)
    })
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

FlowGroup.config({
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
      fillOpacity: 0.3,
      strokeDasharray: '8, 3, 1, 3'
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
