import { Shape } from '@antv/x6'

export default class FlowSwitch extends Shape.Polygon {
  constructor (meta) {
    super(meta)
    this.meta = meta
    this.attr('label/text', this.meta.data.title)
  }

  postprocess () {
    this.on('change:data', ({ current }) => {
      Object.assign(this.meta, { data: current })
      this.attr('label/text', this.meta.data.title)
    })
  }
}

FlowSwitch.config({
  attrs: {
    body: {
      stroke: 'rgb(217, 217, 217)',
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
