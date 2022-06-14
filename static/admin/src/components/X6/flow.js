import * as X6 from './index'

class Flow {
  constructor (container, options) {
    const _this = this
    this.counter = 0
    this.container = container
    this.shapeSizes = {
      'flow-node': {
        width: 150,
        height: 40,
        offsetX: 60,
        offsetY: 18
      },
      'flow-group': {
        width: 300,
        height: 300,
        offsetX: 150,
        offsetY: 150
      },
      'flow-subprocess': {
        width: 300,
        height: 300,
        offsetX: 150,
        offsetY: 150
      }
    }
    this.options = Object.assign({
      onCellClick ({ e, x, y, cell, view }) {},
      onCellDoubleClick ({ e, x, y, cell, view }) {},
      onCellContextmenu ({ e, x, y, cell, view }) {},
      onBlankClick ({ e, x, y }) {},
      onBlankDoubleClick ({ e, x, y }) {},
      onBlankContextmenu ({ e, x, y }) {},
      onNodeAdded ({ node, index, options }) {},
      onEdgeConnected () {}
    }, options)
    this.graph = new X6.Graph({
      container: this.container,
      grid: true,
      mousewheel: {
        enabled: true,
        zoomAtMousePosition: true,
        modifiers: 'ctrl',
        minScale: 0.5,
        maxScale: 3
      },
      connecting: {
        router: { name: 'manhattan', args: { padding: 1 } },
        connector: { name: 'rounded', args: { radius: 8 } },
        allowBlank: false,
        allowMulti: false,
        allowPort: true,
        allowEdge: false,
        allowNode: false,
        allowLoop: false,
        snap: { radius: 20 },
        createEdge () {
          return new X6.FlowEdge()
        },
        validateConnection ({ targetMagnet, sourceCell, targetCell }) {
          if (!targetMagnet) return false
          return _this.subprocess(sourceCell) === _this.subprocess(targetCell)
        }
      },
      highlighting: {
        magnetAdsorbed: { name: 'stroke', args: { attrs: { fill: '#5F95FF', stroke: '#5F95FF' } } }
      },
      panning: true,
      resizing: { enabled: true, minWidth: 100, minHeight: 40 },
      rotating: true,
      selecting: { enabled: true, rubberband: false, showNodeSelectionBox: true },
      snapline: true,
      clipboard: true,
      history: {
        enabled: true
      },
      keyboard: {
        enabled: true
      },
      embedding: {
        enabled: true,
        findParent ({ node }) {
          const bbox = node.getBBox()
          return this.getNodes().filter((node) => {
            if (!(node instanceof X6.FlowGroup) && !(node instanceof X6.FlowSubprocess)) return false
            const targetBBox = node.getBBox()
            return bbox.isIntersectWithRect(targetBBox)
          })
        }
      }
    })
    this.dnd = new X6.Addon.Dnd({ target: this.graph })
    this.__bindEvent()
  }

  __bindEvent () {
    this.graph.bindKey(['meta+c', 'ctrl+c'], () => { // 复制
      const cells = this.graph.getSelectedCells()
      if (cells.length) { this.graph.copy(cells) }
      return false
    })
    this.graph.bindKey(['meta+x', 'ctrl+x'], () => { // 剪切
      const cells = this.graph.getSelectedCells()
      if (cells.length) { this.graph.cut(cells) }
      return false
    })
    this.graph.bindKey(['meta+v', 'ctrl+v'], () => { // 粘贴
      if (!this.graph.isClipboardEmpty()) {
        const cells = this.graph.paste({ offset: 32 })
        this.graph.cleanSelection()
        this.graph.select(cells)
      }
      return false
    })
    this.graph.bindKey(['meta+z', 'ctrl+z'], () => { // 撤销
      if (this.graph.history.canUndo()) {
        this.graph.history.undo()
      }
      return false
    })
    this.graph.bindKey(['meta+shift+z', 'ctrl+shift+z'], () => { // 重做
      if (this.graph.history.canRedo()) {
        this.graph.history.redo()
      }
      return false
    })
    this.graph.bindKey(['meta+a', 'ctrl+a'], () => { // 全选
      const nodes = this.graph.getNodes()
      if (nodes) { this.graph.select(nodes) }
      return false
    })
    this.graph.bindKey('delete', () => { // 删除
      const cells = this.graph.getSelectedCells()
      if (cells.length) { this.graph.removeCells(cells) }
    })
    this.graph.bindKey(['ctrl+1', 'meta+1'], () => { // 放大
      const zoom = this.graph.zoom()
      if (zoom < 1.5) { this.graph.zoom(0.1) }
    })
    this.graph.bindKey(['ctrl+2', 'meta+2'], () => { // 缩小
      const zoom = this.graph.zoom()
      if (zoom > 0.5) { this.graph.zoom(-0.1) }
    })
    this.graph.on('node:mouseenter', () => {
      const ports = this.container.querySelectorAll('.x6-port-body')
      this.showPorts(ports, true)
    })
    this.graph.on('node:mouseleave', () => {
      const ports = this.container.querySelectorAll('.x6-port-body')
      this.showPorts(ports, false)
    })
    this.graph.on('node:collapse', ({ e, node }) => {
      e.stopPropagation()
      node.toggleCollapse()
      const collapsed = node.isCollapsed()
      if (node instanceof X6.FlowGroup) {
        collapsed ? node.addTransientEdge(this.graph) : node.removeTransientEdge(this.graph)
      }
      const collapse = (parent) => {
        const cells = parent.getChildren()
        if (cells) {
          cells.forEach((cell) => {
            collapsed ? cell.hide() : cell.show()
            if (cell instanceof X6.FlowGroup || cell instanceof X6.FlowSubprocess) {
              if (!cell.isCollapsed()) collapse(cell)
            }
          })
        }
      }
      collapse(node)
    })
    this.graph.on('edge:mouseenter', ({ cell }) => {
      cell.addTools([
        { name: 'circle-source-arrowhead' },
        { name: 'circle-target-arrowhead' }
      ])
    })
    this.graph.on('edge:mouseleave', ({ cell }) => {
      cell.removeTools()
    })
    this.graph.on('edge:connected', (data) => {
      data.isNew && data.edge.setData({ title: '', description: '', options: {} })
      this.options.onEdgeConnected(data)
    })
    this.graph.on('node:added', (data) => {
      this.options.onNodeAdded(data)
    })
    this.graph.on('node:embedded', ({ node, currentParent }) => {
      if (currentParent) node.setZIndex(currentParent.getZIndex() + 1)
    })
    this.graph.on('cell:click', data => {
      if (!data.cell.getData()) return true
      if (this.options.onCellClick(data)) data.e.stopPropagation()
    })
    this.graph.on('cell:dblclick', data => {
      if (this.options.onCellDoubleClick(data)) data.e.stopPropagation()
    })
    this.graph.on('cell:contextmenu', data => {
      if (this.options.onCellContextmenu(data)) data.e.stopPropagation()
    })
    this.graph.on('blank:click', data => {
      if (this.options.onBlankClick(data)) data.e.stopPropagation()
    })
    this.graph.on('blank:dblclick', data => {
      if (this.options.onBlankDoubleClick(data)) data.e.stopPropagation()
    })
    this.graph.on('blank:contextmenu', data => {
      if (this.options.onBlankContextmenu(data)) data.e.stopPropagation()
    })
  }

  panning () {
    this.graph.enablePanning()
    this.graph.disableRubberband()
  }

  selecting () {
    this.graph.disablePanning()
    this.graph.enableRubberband()
  }

  fitting () {
    this.graph.centerContent()
  }

  showPorts (ports, show) {
    for (let i = 0, len = ports.length; i < len; i = i + 1) {
      ports[i].style.visibility = show ? 'visible' : 'hidden'
    }
  }

  reset (cells = []) {
    this.graph.freeze()
    this.graph.fromJSON(cells)
    cells.forEach(cell => {
      if (cell.shape === 'flow-edge') {
        this.graph.getCellById(cell.id).setLabels(cell.data.title)
      } else {
        if (cell.data.index > this.counter) this.counter = cell.data.index
      }
    })
    this.graph.unfreeze()
  }

  collect () {
    const result = this.graph.toJSON()
    result.cells = result.cells.map(cell => {
      return this.briefJSON(cell)
    })
    return result
  }

  toggleGrid (visible) {
    visible ? this.graph.showGrid() : this.graph.hideGrid()
  }

  generateNode (widget, ev, callback) {
    const counter = ++this.counter
    const point = this.graph.pageToLocal(ev.pageX, ev.pageY)
    const shapeSize = this.shapeSizes[widget.shape]
    const item = {
      id: `node_${counter}`,
      x: point.x - shapeSize.offsetX,
      y: point.y - shapeSize.offsetY,
      width: shapeSize.width,
      height: shapeSize.height,
      shape: widget.shape,
      data: Object.assign({
        index: counter,
        title: `${widget.label}_${counter}`,
        icon: widget.icon,
        type: widget.type,
        description: widget.title,
        options: widget.options()
      }, callback ? callback() : {})
    }
    return item
  }

  addNode (item) {
    return this.graph.addNode(item)
  }

  widgetDragStart (widget, ev, callback) {
    const item = this.generateNode(widget, ev, callback)
    const node = this.graph.createNode(item)
    this.dnd.start(node, ev)
    return item
  }

  updateCell (item) {
    if (!item) return false
    const cell = this.graph.getCellById(item.id)
    if (!cell) return false
    cell.setData(item.data)
    return true
  }

  cell2meta (cell) {
    const json = cell.toJSON()
    return this.briefJSON(json)
  }

  briefJSON (json) {
    const result = {
      id: json.id,
      shape: json.shape,
      zIndex: json.zIndex
    }
    if (json.data) result.data = json.data
    if (['flow-edge', 'edge'].indexOf(result.shape) !== -1) {
      Object.assign(result, {
        source: json.source,
        target: json.target
      })
    } else {
      Object.assign(result, {
        x: json.position.x,
        y: json.position.y,
        width: json.size.width,
        height: json.size.height
      })
      if (json.angle) result.angle = json.angle
      if (json.parent) result.parent = json.parent
      if (json.children) result.children = json.children
    }
    return result
  }

  subprocess (node) {
    if (!node) return null
    if (node.parent instanceof X6.FlowSubprocess) return node.parent
    return this.subprocess(node.parent)
  }

  highlight (cells) {
    this.graph.getCells().forEach(cell => {
      switch (cell.shape) {
        case 'flow-edge':
          cell.attr({ line: { stroke: '#A2B1C3' } })
          break
        case 'flow-group':
        case 'flow-subprocess':
          cell.attr({ text: { fill: '' } })
          break
        case 'flow-node':
          cell.attr({ foreignObject: { class: '' } })
          break
      }
    })
    cells.forEach(cell => {
      cell = this.graph.getCellById(cell.id)
      switch (cell.shape) {
        case 'flow-edge':
          cell.attr({ line: { stroke: '#409eff' } })
          break
        case 'flow-group':
        case 'flow-subprocess':
          cell.attr({ text: { fill: '#409eff' } })
          break
        case 'flow-node':
          cell.attr({ foreignObject: { class: 'fs-flow-highlight' } })
          break
      }
    })
  }
}

export default Flow
