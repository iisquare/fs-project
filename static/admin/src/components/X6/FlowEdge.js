import { Shape } from '@antv/x6'

export default class FlowEdge extends Shape.Edge {
}

FlowEdge.config({
  attrs: {
    line: {
      stroke: '#A2B1C3',
      strokeWidth: 2,
      targetMarker: { name: 'block', width: 12, height: 8 }
    }
  },
  zIndex: 0
})
