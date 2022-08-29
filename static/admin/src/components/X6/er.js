import * as X6 from './index'

class ER {
  constructor (container, options) {
    this.container = container
    this.options = Object.assign({
      readonly: true,
      onCellClick ({ e, x, y, cell, view }) {},
      onCellDoubleClick ({ e, x, y, cell, view }) {},
      onCellContextmenu ({ e, x, y, cell, view }) {},
      onBlankClick ({ e, x, y }) {},
      onBlankDoubleClick ({ e, x, y }) {},
      onBlankContextmenu ({ e, x, y }) {}
    }, options)
    this.graph = new X6.Graph({
      container: this.container,
      grid: false,
      mousewheel: {
        enabled: true,
        zoomAtMousePosition: true,
        modifiers: 'ctrl',
        minScale: 0.5,
        maxScale: 3
      },
      connecting: {
        allowBlank: false,
        allowMulti: false,
        allowPort: true,
        allowEdge: false,
        allowNode: false,
        allowLoop: true,
        router: { name: 'er', args: { offset: 25, direction: 'H' } },
        createEdge () { return new X6.EREdge() }
      },
      panning: true
    })
    this.__bindEvent()
  }

  __bindEvent () {
    this.graph.bindKey(['ctrl+1', 'meta+1'], () => { // 放大
      const zoom = this.graph.zoom()
      if (zoom < 1.5) { this.graph.zoom(0.1) }
    })
    this.graph.bindKey(['ctrl+2', 'meta+2'], () => { // 缩小
      const zoom = this.graph.zoom()
      if (zoom > 0.5) { this.graph.zoom(-0.1) }
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
    return true
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

  fromGovernMetaInfluence (data) {
    const cells = []
    let index = 0
    for (const path in data.models) {
      const model = data.models[path]
      const ports = []
      for (const column of model.columns) {
        ports.push({
          id: column.catalog + column.model + '/' + column.code,
          group: 'list',
          attrs: {
            portBody: { magnet: !this.options.readonly },
            portNameLabel: { text: column.code },
            portTypeLabel: { text: column.type }
          }
        })
      }
      cells.push({
        id: path,
        shape: 'er-rect',
        label: model.code,
        width: X6.ER_NODE_WIDTH,
        height: X6.ER_LINE_HEIGHT,
        position: { x: index++ * X6.ER_NODE_WIDTH * 2.1, y: 0 },
        ports,
        data: model
      })
    }
    for (const identity in data.relations) {
      const relation = data.relations[identity]
      const properties = relation.properties
      cells.push({
        id: identity,
        shape: 'er-edge',
        label: properties.relation,
        source: {
          cell: properties.source_catalog + properties.source_model,
          port: properties.source_catalog + properties.source_model + '/' + properties.source_column
        },
        target: {
          cell: properties.target_catalog + properties.target_model,
          port: properties.target_catalog + properties.target_model + '/' + properties.target_column
        },
        data: relation
      })
    }
    return cells
  }

  reset (cells = []) {
    this.graph.freeze()
    const result = []
    cells.forEach(cell => {
      if (cell.shape === 'er-edge') {
        result.push(this.graph.createEdge(cell))
      } else {
        result.push(this.graph.createNode(cell))
      }
    })
    this.graph.resetCells(result)
    this.graph.zoomToFit({ padding: 10, maxScale: 1 })
    this.graph.unfreeze()
  }

  toggleGrid (visible) {
    visible ? this.graph.showGrid() : this.graph.hideGrid()
  }

  highlight (cells) {
    this.graph.getCells().forEach(cell => {
      switch (cell.shape) {
        case 'er-edge':
          cell.attr({ line: { stroke: '#A2B1C3' } })
          break
        case 'er-rect':
          cell.attr({ label: { fill: '#ffffff' } })
          break
      }
    })
    cells.forEach(cell => {
      cell = this.graph.getCellById(cell.id)
      switch (cell.shape) {
        case 'er-edge':
          cell.attr({ line: { stroke: '#f16a14' } })
          break
        case 'er-rect':
          cell.attr({ label: { fill: '#f16a14' } })
          break
      }
    })
  }
}

export default ER
