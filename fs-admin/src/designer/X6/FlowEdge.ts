import DesignUtil from '@/utils/DesignUtil'
import { Shape, Edge } from '@antv/x6'

export default class FlowEdge extends Shape.Edge {

  meta: Edge.Metadata
  
  constructor (metadata?: Edge.Metadata) {
    super(metadata)
    this.meta = metadata ?? {}
    this.setLabels(this.meta.data?.name ?? '')
  }

  postprocess () {
    this.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
      Object.assign(this.meta, { data: current })
      this.setLabels(this.meta.data?.name ?? '')
    }))
  }

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
