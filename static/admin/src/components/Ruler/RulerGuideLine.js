import RulerUtil from './RulerUtil'

class RulerGuideLine {
  constructor (ruler, dimension, x, y, event, isSet) {
    this.ruler = ruler
    this.line = document.createElement('div')
    const guideStyle = dimension === this.ruler.VERTICAL ? 'rul_lineVer' : 'rul_lineHor'
    const curDelta = dimension === this.ruler.VERTICAL ? this.ruler.CUR_DELTA_X : this.ruler.CUR_DELTA_Y
    this.line.title = '双击删除参考线'
    RulerUtil.addClasss(this.line, ['rul_line', guideStyle])
    this.ruler.dom.appendChild(this.line)
    if (dimension === this.ruler.VERTICAL) {
      this.line.style.left = RulerUtil.pixelize(x - this.ruler.container.getBoundingClientRect().left)
      if (isSet) this.line.style.left = RulerUtil.pixelize(Math.round(x / this.ruler.CUR_SCALE) + this.ruler.options.rulerHeight)
    } else {
      this.line.style.top = RulerUtil.pixelize(y - this.ruler.container.getBoundingClientRect().top)
      if (isSet) this.line.style.top = RulerUtil.pixelize(Math.round(y / this.ruler.CUR_SCALE) + this.ruler.options.rulerHeight)
    }

    this.curScale = 1
    this.assigned = false
    this.curPosDelta = curDelta || 0
    this.dragContainer = this.ruler.container.querySelector('.rul_wrapper')
    this.dimension = dimension || 2
    const _this = this
    this.mousedown = function (e) {
      e.stopPropagation()
      _this.startMoving()
    }
    this.mouseup = function (e) {
      _this.stopMoving()
    }
    this.dblclick = function (e) {
      e.stopPropagation()
      _this.destroy()
    }
    this.line.addEventListener('mousedown', this.mousedown)
    this.line.addEventListener('mouseup', this.mouseup)
    this.line.addEventListener('dblclick', this.dblclick)
    if (event) this.startMoving(event)
  }

  moveCallback (x, y) {
    const coor = this.line.dimension === this.ruler.VERTICAL ? x : y
    if (!this.assigned) {
      if (coor > this.ruler.options.rulerHeight) {
        this.assigned = true
      }
      return
    }
    let guideIndex = 0
    if (coor < this.ruler.options.rulerHeight) {
      this.ruler.guides.some((guideLine, index) => {
        if (guideLine.line === this) {
          guideIndex = index
          return true
        }
      })
      this.destroy()
      this.ruler.guides.splice(guideIndex, 1)
    }
  }

  showToolTip (e) {
    if (!this.ruler.options.enableToolTip) {
      return
    }
    RulerUtil.addClasss(this.line, 'rul_tooltip')
  }

  updateToolTip (x, y) {
    if (y) {
      this.line.dataset.tip = 'Y: ' + Math.round((y - this.ruler.options.rulerHeight - 1 - this.curPosDelta) * this.curScale) + ' px'
    } else {
      this.line.dataset.tip = 'X: ' + Math.round((x - this.ruler.options.rulerHeight - 1 - this.curPosDelta) * this.curScale) + ' px'
    }
  }

  hideToolTip (e) {
    RulerUtil.removeClasss(this.line, 'rul_tooltip')
  }

  destroy () {
    this.stopMoving()
    this.line.removeEventListener('mousedown', this.mousedown)
    this.line.removeEventListener('mouseup', this.mouseup)
    this.line.removeEventListener('dblclick', this.dblclick)
    this.line.parentNode && this.line.parentNode.removeChild(this.line)
  }

  hide () {
    this.line.style.display = 'none'
  }

  show () {
    this.line.style.display = 'block'
  }

  move (x, y) {
    this.line.style.left = RulerUtil.pixelize(x)
    this.line.style.top = RulerUtil.pixelize(y)
    this.updateToolTip(x, y)
    this.moveCallback(x, y)
  }

  startMoving (evt) {
    RulerUtil.addClasss(this.line, ['rul_line_dragged'])
    evt = evt || window.event
    const posX = evt ? evt.clientX : 0
    const posY = evt ? evt.clientY : 0
    const divTop = Number.parseInt(this.line.style.top || 0)
    const divLeft = Number.parseInt(this.line.style.left || 0)
    const eWi = Number.parseInt(this.line.offsetWidth)
    const eHe = Number.parseInt(this.line.offsetHeight)
    const cWi = Number.parseInt(this.dragContainer.offsetWidth)
    const cHe = Number.parseInt(this.dragContainer.offsetHeight)
    const cursor = this.line.dimension === 2 ? 'ns-resize' : 'ew-resize'
    const diffX = posX - divLeft
    const diffY = posY - divTop
    this.ruler.container.style.cursor = cursor
    this.line.style.cursor = cursor

    const _this = this
    document.onmousemove = function moving (evt) {
      evt = evt || window.event
      const posX = evt.clientX
      const posY = evt.clientY
      let aX = posX - diffX
      let aY = posY - diffY

      if (aX < 0) {
        aX = 0
      }
      if (aY < 0) {
        aY = 0
      }

      if (aX + eWi > cWi) {
        aX = cWi - eWi
      }
      if (aY + eHe > cHe) {
        aY = cHe - eHe
      }
      _this.move(aX, aY)
    }
    this.showToolTip()
  }

  stopMoving () {
    this.ruler.container.style.cursor = null
    this.line.style.cursor = null
    document.onmousemove = function () {}
    this.hideToolTip()
    RulerUtil.removeClasss(this.line, ['rul_line_dragged'])
  }
}

export default RulerGuideLine
