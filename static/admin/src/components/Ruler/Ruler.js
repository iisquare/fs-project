import RulerUtil from './RulerUtil'
import RulerSide from './RulerSide'
import RulerGuideLine from './RulerGuideLine'
import RulerCorner from './RulerCorner'

class Ruler {
  constructor (container, options = {}) {
    this.VERTICAL = 1
    this.HORIZONTAL = 2
    this.CUR_DELTA_X = 0
    this.CUR_DELTA_Y = 0
    this.CUR_SCALE = 1
    this.container = container
    this.options = RulerUtil.extend({
      rulerHeight: 15,
      fontFamily: 'arial',
      fontSize: '8px',
      strokeStyle: 'gray',
      sides: ['top', 'left'],
      cornerSides: ['TL'],
      lineWidth: 1,
      enableMouseTracking: true,
      enableToolTip: true
    }, options)
    this.rulerz = {}
    this.guides = []
    this.dom = document.createElement('div')
    this.corners = []

    RulerUtil.addClasss(this.dom, 'rul_wrapper')
    this.container.appendChild(this.dom)
    this.options.sides.forEach(side => {
      this.side(side)
    })
    this.corner(this.options.cornerSides)
    const _this = this
    this.mouseup = function (e) {
      _this.guides.forEach(guide => {
        guide.line.stopMoving()
      })
    }
    this.container.addEventListener('mouseup', this.mouseup)
  }

  side (alignment) {
    this.rulerz[alignment] = new RulerSide(this, alignment)
    this.rulerz[alignment].drawRuler(this.container.offsetWidth, this.options.rulerHeight)
    this.positionRuler(this.rulerz[alignment], alignment)
    this.attachListeners(this.rulerz[alignment])
  }

  corner (cornerSides) {
    cornerSides.forEach(side => {
      this.corners.push(new RulerCorner(this, side))
    })
  }

  guide (dimension, x, y, e, isSet) {
    this.guides.push({
      dimension: dimension,
      line: new RulerGuideLine(this, dimension, x, y, e, isSet)
    })
  }

  rotateRuler (curRuler, angle) {
    const rotation = 'rotate(' + angle + 'deg)'
    const origin = RulerUtil.pixelize(Math.abs(Number.parseInt(curRuler.canvas.style.left))) + ' 100%'
    curRuler.canvas.style.webkitTransform = rotation
    curRuler.canvas.style.MozTransform = rotation
    curRuler.canvas.style.OTransform = rotation
    curRuler.canvas.style.msTransform = rotation
    curRuler.canvas.style.transform = rotation
    curRuler.canvas.style.webkitTransformOrigin = origin
    curRuler.canvas.style.MozTransformOrigin = origin
    curRuler.canvas.style.OTransformOrigin = origin
    curRuler.canvas.style.msTransformOrigin = origin
    curRuler.canvas.style.transformOrigin = origin
  }

  positionRuler (curRuler, alignment) {
    curRuler.canvas.style.left = RulerUtil.pixelize(-((curRuler.canvas.width / 2) - curRuler.canvas.height))
    switch (alignment) {
      case 'top':
        curRuler.orgPos = Number.parseInt(curRuler.canvas.style.left)
        break
      case 'left':
        curRuler.canvas.style.top = RulerUtil.pixelize(-curRuler.canvas.height - 1)
        curRuler.orgPos = Number.parseInt(curRuler.canvas.style.top)
        this.rotateRuler(curRuler, 90)
        break
    }
  }

  attachListeners (curRul) {
    const _this = this
    const mousedown = function (e) {
      _this.guide(curRul.dimension, e.clientX, e.clientY, e)
    }
    curRul.canvas.addEventListener('mousedown', mousedown)
    curRul.clearListeners = function () {
      curRul.canvas.removeEventListener('mousedown', mousedown)
    }
  }

  forEachRuler (cb) {
    let index = 0
    for (const rul in this.rulerz) {
      if (this.rulerz.hasOwnProperty(rul)) {
        cb(this.rulerz[rul], index++)
      }
    }
  }

  setPos (values) {
    let orgX = 0
    let orgY = 0
    let deltaX = 0
    let deltaY = 0
    this.forEachRuler(curRul => {
      if (curRul.dimension === this.VERTICAL) {
        orgY = curRul.canvas.style.top
        curRul.canvas.style.top = RulerUtil.pixelize(curRul.orgPos + (Number.parseInt(values.y)))
        deltaY = Number.parseInt(orgY) - Number.parseInt(curRul.canvas.style.top)
      } else {
        orgX = curRul.canvas.style.left
        curRul.canvas.style.left = RulerUtil.pixelize(curRul.orgPos + (Number.parseInt(values.x)))
        deltaX = Number.parseInt(orgX) - Number.parseInt(curRul.canvas.style.left)
      }
    })
    this.guides.forEach(guide => {
      if (guide.dimension === this.HORIZONTAL) {
        guide.line.guideLine.style.top = RulerUtil.pixelize(Number.parseInt(guide.line.guideLine.style.top) - deltaY)
        guide.line.curPosDelta = Number.parseInt(values.y)
      } else {
        guide.line.guideLine.style.left = RulerUtil.pixelize(Number.parseInt(guide.line.guideLine.style.left) - deltaX)
        guide.line.curPosDelta = Number.parseInt(values.x)
      }
    })
    this.CUR_DELTA_X = Number.parseInt(values.x)
    this.CUR_DELTA_Y = Number.parseInt(values.y)
  }

  setScale (newScale) {
    let curPos, orgDelta, curScaleFac
    this.forEachRuler(rul => {
      rul.context.clearRect(0, 0, rul.canvas.width, rul.canvas.height)
      rul.context.beginPath()
      rul.setScale(newScale)
      rul.context.stroke()
      this.CUR_SCALE = newScale
    })
    this.guides.forEach(guide => {
      if (guide.dimension === this.HORIZONTAL) {
        curPos = Number.parseInt(guide.line.guideLine.style.top)
        orgDelta = this.options.rulerHeight + 1
        curScaleFac = (Number.parseFloat(newScale) / guide.line.curScale())
        guide.line.guideLine.style.top = RulerUtil.pixelize(((curPos - orgDelta - this.CUR_DELTA_Y) / curScaleFac) + orgDelta + this.CUR_DELTA_Y)
        guide.line.curScale(newScale)
      } else {
        curPos = Number.parseInt(guide.line.guideLine.style.left)
        orgDelta = this.options.rulerHeight + 1
        curScaleFac = (Number.parseFloat(newScale) / guide.line.curScale())
        guide.line.guideLine.style.left = RulerUtil.pixelize(((curPos - orgDelta - this.CUR_DELTA_X) / curScaleFac) + orgDelta + this.CUR_DELTA_X)
        guide.line.curScale(newScale)
      }
    })
  }

  clearGuides () {
    this.guides.forEach(guide => {
      guide.line.destroy()
    })
    this.guides = []
  }

  toggleGuideVisibility (val) {
    const func = val ? 'show' : 'hide'
    this.guides.forEach(guide => {
      guide.line[func]()
    })
  }

  toggleRulerVisibility (val) {
    const state = val ? 'block' : 'none'
    this.dom.style.display = state
    const trackers = this.container.querySelectorAll('.rul_tracker')
    if (trackers.length > 0) {
      trackers[0].style.display = state
      trackers[1].style.display = state
    }
  }

  getGuides () {
    return this.guides.map(guide => {
      return {
        posX: Math.round((Number.parseInt(guide.line.line.style.left) - this.CUR_DELTA_X - this.options.rulerHeight) * this.CUR_SCALE),
        posY: Math.round((Number.parseInt(guide.line.line.style.top) - this.CUR_DELTA_Y - this.options.rulerHeight) * this.CUR_SCALE),
        dimension: guide.dimension
      }
    })
  }

  setGuides (guides = []) {
    guides.forEach(guide => {
      this.guide(guide.dimension, guide.posX, guide.posY, null, true)
    })
  }

  destroy () {
    this.clearGuides()
    this.forEachRuler(ruler => {
      ruler.destroy()
    })
    this.corners.forEach(corner => {
      corner.destroy()
    })
    this.container.removeEventListener('mouseup', this.mouseup)
    this.dom.parentNode.removeChild(this.dom)
  }
}

export default Ruler
