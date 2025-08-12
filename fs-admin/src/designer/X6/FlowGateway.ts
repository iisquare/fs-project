import DesignUtil from '@/utils/DesignUtil'
import { Shape, Node } from '@antv/x6'

export default class FlowGateway extends Shape.Polygon {

  meta: Node.Metadata

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
  }
}

FlowGateway.config({
  attrs: {
    body: {
      stroke: 'rgb(204, 204, 204)',
      strokeWidth: '1px',
      fill: '#ffffff',
      fillOpacity: 0.3,
      refPoints: '0,10 10,0 20,10 10,20'
    },
    text: {
      fontSize: 12
    }
  }
})
