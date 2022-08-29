import { Graph, Shape, Addon } from '@antv/x6'
import '@antv/x6-vue-shape'
import FlowEdge from './FlowEdge'
import FlowGroup from './FlowGroup'
import FlowSubprocess from './FlowSubprocess'
import FlowSwitch from './FlowSwitch'
import EREdge from './EREdge'

const CircleAttr = {
  circle: {
    r: 4,
    magnet: true,
    stroke: '#5F95FF',
    strokeWidth: 1,
    fill: '#fff',
    style: { visibility: 'hidden' }
  }
}
const D4Port = {
  groups: {
    top: { position: 'top', attrs: CircleAttr },
    right: { position: 'right', attrs: CircleAttr },
    bottom: { position: 'bottom', attrs: CircleAttr },
    left: { position: 'left', attrs: CircleAttr }
  },
  items: [
    { id: 'top', group: 'top' },
    { id: 'right', group: 'right' },
    { id: 'bottom', group: 'bottom' },
    { id: 'left', group: 'left' }
  ]
}

Graph.registerNode('flow-node', {
  inherit: 'vue-shape',
  component: { template: `<flow-node />`, components: { FlowNode: () => import('./FlowNode') } },
  ports: D4Port
})

Graph.registerEdge('flow-edge', FlowEdge)

Graph.registerNode('flow-group', FlowGroup)

Graph.registerNode('flow-subprocess', { inherit: FlowSubprocess, ports: D4Port })

Graph.registerNode('flow-switch', { inherit: FlowSwitch, ports: D4Port })

Graph.registerEdgeTool('circle-source-arrowhead', {
  inherit: 'source-arrowhead',
  tagName: 'circle',
  attrs: {
    r: 3,
    fill: '#31d0c6',
    'fill-opacity': 0.3,
    stroke: '#fe854f',
    'stroke-width': 2,
    cursor: 'move'
  }
})

Graph.registerEdgeTool('circle-target-arrowhead', {
  inherit: 'target-arrowhead',
  tagName: 'circle',
  attrs: {
    r: 3,
    fill: '#31d0c6',
    'fill-opacity': 0.3,
    stroke: '#fe854f',
    'stroke-width': 2,
    cursor: 'move'
  }
})

const ER_LINE_HEIGHT = 24
const ER_NODE_WIDTH = 150

Graph.registerEdge('er-edge', EREdge)

Graph.registerPortLayout('er-port-position', (args) => {
  return args.map((_, index) => {
    return { position: { x: 0, y: (index + 1) * ER_LINE_HEIGHT }, angle: 0 }
  })
}, true)

Graph.registerNode('er-rect', {
  inherit: 'rect',
  markup: [
    { tagName: 'rect', selector: 'body' },
    { tagName: 'text', selector: 'label' }
  ],
  attrs: {
    rect: { strokeWidth: 1, stroke: '#5F95FF', fill: '#5F95FF' },
    label: { fontWeight: 'bold', fill: '#ffffff', fontSize: 12 }
  },
  ports: {
    groups: {
      list: {
        markup: [
          { tagName: 'rect', selector: 'portBody' },
          { tagName: 'text', selector: 'portNameLabel' },
          { tagName: 'text', selector: 'portTypeLabel' }
        ],
        attrs: {
          portBody: {
            width: ER_NODE_WIDTH,
            height: ER_LINE_HEIGHT,
            strokeWidth: 1,
            stroke: '#5F95FF',
            fill: '#EFF4FF',
            magnet: true
          },
          portNameLabel: {
            ref: 'portBody',
            refX: 6,
            refY: 6,
            fontSize: 10
          },
          portTypeLabel: {
            ref: 'portBody',
            refX: 95,
            refY: 6,
            fontSize: 10
          }
        },
        position: 'er-port-position'
      }
    }
  }
}, true)

export {
  Graph,
  Shape,
  Addon,
  FlowEdge,
  FlowGroup,
  FlowSubprocess,
  EREdge,
  ER_LINE_HEIGHT,
  ER_NODE_WIDTH
}
