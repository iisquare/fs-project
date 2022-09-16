import RulerUtil from './RulerUtil'

class RulerSide {
  constructor (ruler, alignment) {
    this.ruler = ruler
    this.dimension = alignment === 'left' || alignment === 'right' ? this.ruler.VERTICAL : this.ruler.HORIZONTAL
    const rulerStyle = this.dimension === this.ruler.VERTICAL ? 'rul_ruler_this.VERTICAL' : 'rul_ruler_this.HORIZONTAL'
    this.canvas = document.createElement('canvas')
    RulerUtil.addClasss(this.canvas, ['rul_ruler', rulerStyle, 'rul_align_' + alignment])
    this.ruler.container.appendChild(this.canvas)
    this.context = this.canvas.getContext('2d')
    this.rulThickness = 0
    this.rulLength = 0
    this.rulScale = 1
    this.orgPos = 0
    this.tracker = document.createElement('div')
    const _this = this
    this.mousemove = function (e) {
      const posX = e.clientX
      const posY = e.clientY
      if (_this.dimension === 2) {
        _this.tracker.style.left = RulerUtil.pixelize(posX - Number.parseInt(_this.ruler.container.getBoundingClientRect().left))
      } else {
        _this.tracker.style.top = RulerUtil.pixelize(posY - Number.parseInt(_this.ruler.container.getBoundingClientRect().top))
      }
    }
    if (this.ruler.options.enableMouseTracking) {
      this.initTracker()
    }
  }

  getLength () {
    return this.rulLength
  }

  getThickness () {
    return this.rulThickness
  }

  getScale () {
    return this.rulScale
  }

  setScale (newScale) {
    this.rulScale = Number.parseFloat(newScale)
    this.drawPoints()
    return this.rulScale
  }

  drawRuler (rulerLength, rulerThickness, rulerScale) {
    this.rulLength = this.canvas.width = rulerLength * 4
    this.rulThickness = this.canvas.height = rulerThickness
    this.rulScale = rulerScale || this.rulScale
    this.context.strokeStyle = this.ruler.options.strokeStyle
    this.context.font = this.ruler.options.fontSize + ' ' + this.ruler.options.fontFamily
    this.context.lineWidth = this.ruler.options.lineWidth
    this.context.beginPath()
    this.drawPoints()
    this.context.stroke()
  }

  drawPoints () {
    let pointLength = 0
    let label = ''
    let delta = 0
    let draw = false
    const lineLengthMax = 0
    const lineLengthMed = this.rulThickness / 2
    const lineLengthMin = this.rulThickness / 2

    for (let pos = 0; pos <= this.rulLength; pos += 1) {
      delta = ((this.rulLength / 2) - pos)
      draw = false
      label = ''
      if (delta % 50 === 0) {
        pointLength = lineLengthMax
        label = Math.round(Math.abs(delta) * this.rulScale)
        draw = true
      } else if (delta % 25 === 0) {
        pointLength = lineLengthMed
        draw = true
      } else if (delta % 5 === 0) {
        pointLength = lineLengthMin
        draw = true
      }
      if (draw) {
        this.context.moveTo(pos + 0.5, this.rulThickness + 0.5)
        this.context.lineTo(pos + 0.5, pointLength + 0.5)
        this.context.fillText(label, pos + 1.5, (this.rulThickness / 2) + 1)
      }
    }
  }

  destroy () {
    this.ruler.container.removeEventListener('mousemove', this.mousemove)
    this.tracker.parentNode.removeChild(this.tracker)
    this.clearListeners && this.clearListeners()
  }

  initTracker () {
    this.tracker = this.ruler.container.appendChild(this.tracker)
    RulerUtil.addClasss(this.tracker, 'rul_tracker')
    const height = RulerUtil.pixelize(this.ruler.options.rulerHeight)
    if (this.dimension === 2) {
        this.tracker.style.height = height
    } else {
      this.tracker.style.width = height
    }
    this.ruler.container.addEventListener('mousemove', this.mousemove)
  }
}

export default RulerSide
