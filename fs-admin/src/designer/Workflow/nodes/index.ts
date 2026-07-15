import Flow from '@/designer/X6/flow'
import FlowGateway from '@/designer/X6/FlowGateway'
import * as X6VueShape from '@antv/x6-vue-shape'
import * as X6 from '@antv/x6'
import BPMNNode from './BPMNNode.vue'

const { Port4TRBL } = Flow

const GatewayAttrs = {
  body: {
    stroke: 'rgb(204, 204, 204)',
    strokeWidth: '1px',
    fill: '#ffffff',
    fillOpacity: 0.3,
    refPoints: '0,10 10,0 20,10 10,20'
  },
  text: {
    fontSize: 14,
    fontWeight: 'bold'
  }
}

export function registerBPMNNodes() {
  // Vue shape nodes for events and user tasks
  X6VueShape.register({ shape: 'bpmn-start-event', component: BPMNNode, ports: Port4TRBL })
  X6VueShape.register({ shape: 'bpmn-end-event', component: BPMNNode, ports: Port4TRBL })
  X6VueShape.register({ shape: 'bpmn-user-task', component: BPMNNode, ports: Port4TRBL })

  // Gateway nodes inherit from FlowGateway with different default text markers
  // ExclusiveGateway → "X"
  X6.Graph.unregisterNode('bpmn-exclusive-gateway')
  X6.Graph.registerNode('bpmn-exclusive-gateway', {
    inherit: FlowGateway,
    ports: Port4TRBL,
    attrs: Object.assign({}, GatewayAttrs, {
      text: { ...GatewayAttrs.text, text: '×' }
    })
  })

  // ParallelGateway → "+"
  X6.Graph.unregisterNode('bpmn-parallel-gateway')
  X6.Graph.registerNode('bpmn-parallel-gateway', {
    inherit: FlowGateway,
    ports: Port4TRBL,
    attrs: Object.assign({}, GatewayAttrs, {
      text: { ...GatewayAttrs.text, text: '+' }
    })
  })

  // InclusiveGateway → "O"
  X6.Graph.unregisterNode('bpmn-inclusive-gateway')
  X6.Graph.registerNode('bpmn-inclusive-gateway', {
    inherit: FlowGateway,
    ports: Port4TRBL,
    attrs: Object.assign({}, GatewayAttrs, {
      text: { ...GatewayAttrs.text, text: '○' }
    })
  })

  // Add node shapes for drag-drop positioning
  Object.assign(Flow.NodeShapes, {
    'bpmn-start-event': { width: 60, height: 60, offsetX: 30, offsetY: 30 },
    'bpmn-end-event': { width: 60, height: 60, offsetX: 30, offsetY: 30 },
    'bpmn-user-task': { width: 150, height: 60, offsetX: 75, offsetY: 30 },
    'bpmn-exclusive-gateway': { width: 100, height: 60, offsetX: 50, offsetY: 30 },
    'bpmn-parallel-gateway': { width: 100, height: 60, offsetX: 50, offsetY: 30 },
    'bpmn-inclusive-gateway': { width: 100, height: 60, offsetX: 50, offsetY: 30 },
  })
}
