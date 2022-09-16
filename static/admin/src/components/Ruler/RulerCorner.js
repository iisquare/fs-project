import RulerUtil from './RulerUtil'

class RulerCorner {
  constructor (ruler, side) {
    this.ruler = ruler
    this.corner = document.createElement('div')
    const cornerStyle = 'rul_corner' + side.toUpperCase()
    this.corner.title = 'Clear Guide lines'
    RulerUtil.addClasss(this.corner, ['rul_corner', cornerStyle])
    this.corner.style.width = RulerUtil.pixelize(this.ruler.options.rulerHeight + 1)
    this.corner.style.height = RulerUtil.pixelize(this.ruler.options.rulerHeight)
    this.ruler.container.appendChild(this.corner)
    const _this = this
    this.mousedown = function (e) {
      e.stopPropagation()
      _this.ruler.clearGuides()
    }
    this.corner.addEventListener('mousedown', this.mousedown)
  }

  destroy () {
    this.corner.removeEventListener('mousedown', this.mousedown)
    this.corner.parentNode.removeChild(this.corner)
  }
}

export default RulerCorner
