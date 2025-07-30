import * as X6 from '@antv/x6'
import * as X6Dnd from '@antv/x6-plugin-dnd'
import * as X6VueShape from '@antv/x6-vue-shape'
import { Transform } from '@antv/x6-plugin-transform'
import { Snapline } from '@antv/x6-plugin-snapline'
import { Clipboard } from '@antv/x6-plugin-clipboard'
import { Keyboard } from '@antv/x6-plugin-keyboard'
import { Selection } from '@antv/x6-plugin-selection'
import { History } from '@antv/x6-plugin-history'
import { Scroller } from '@antv/x6-plugin-scroller'
import { MiniMap } from '@antv/x6-plugin-minimap'
import { Export } from '@antv/x6-plugin-export'
import FlowEdge from './FlowEdge'
import FlowGroup from './FlowGroup'
import FlowSubprocess from './FlowSubprocess'
import FlowGateway from './FlowGateway'
import KGNode from './KGNode.vue'
import FlowNode from './FlowNode.vue'

class Flow {

  options: any
  graph: X6.Graph
  dnd: X6Dnd.Dnd
  counter: number = 0

  constructor (container: any, options: any) {
    this.options = Object.assign({}, defaults, options)
    const _this = this
    this.graph = new X6.Graph({
      container,
      background: {
        color: '#F2F7FA',
      },
      grid: this.options.grid,
      mousewheel: {
        enabled: this.options.mousewheel,
        zoomAtMousePosition: true,
        modifiers: 'ctrl',
        minScale: 0.5,
        maxScale: 3
      },
      connecting: {
        router: { name: 'manhattan', args: { padding: 25 } },
        connector: { name: 'rounded', args: { radius: 15 } },
        allowBlank: false,
        allowMulti: false,
        allowPort: true,
        allowEdge: false,
        allowNode: false,
        allowLoop: false,
        snap: { radius: 15 },
        createEdge () {
          return new FlowEdge()
        },
        validateConnection ({ targetMagnet, sourceCell, targetCell }) {
          if (!targetMagnet) return false
          return _this.subprocess(sourceCell) === _this.subprocess(targetCell)
        }
      },
      highlighting: {
        magnetAdsorbed: { name: 'stroke', args: { attrs: { fill: '#5F95FF', stroke: '#5F95FF' } } }
      },
      autoResize: this.options.autoResize,
      panning: this.options.panning,
      embedding: {
        enabled: true,
        findParent ({ node }) {
          const bbox = node.getBBox()
          return this.getNodes().filter((node) => {
            if (!(node instanceof FlowGroup) && !(node instanceof FlowSubprocess)) return false
            const targetBBox = node.getBBox()
            return bbox.isIntersectWithRect(targetBBox)
          })
        }
      },
    })
    this.dnd = new X6Dnd.Dnd({
      target: this.graph,
    })
    this.regist().plugin().bindEvent().panning()
  }

  remove (cell: any) {
    this.graph.removeCell(cell.id)
    this.options.onBlankClick()
  }

  panning () {
    this.options.panning && this.graph.enablePanning()
    this.graph.disableRubberband()
    return this
  }

  selecting () {
    this.graph.disablePanning()
    this.graph.enableRubberband()
    return this
  }

  fitting () {
     // 将画布中元素缩小或者放大一定级别，让画布正好容纳所有元素，可以通过 maxScale 配置最大缩放级别
    this.graph.zoomToFit({ maxScale: 1 })
    this.graph.centerContent()
    return this
  }

  /**
   * [{ id: '', x: 0, y: 0, label: '', attrs: {}, data: { description: '', ...options } }]
   */
  fromJSON (cells = [], fitting = true) {
    this.graph.fromJSON(cells)
    cells.forEach((cell: any) => {
      if (['flow-edge', 'edge'].indexOf(cell.shape) === -1) {
        if (cell.zIndex > this.counter) this.counter = cell.zIndex
      }
    })
    fitting && this.fitting()
  }

  toJSON () {
    const result = this.graph.toJSON()
    const groupIds: any = []
    result.cells = result.cells.map((cell: any) => {
      const json = this.briefJSON(cell)
      if (['flow-group', 'flow-subprocess'].indexOf(json.shape) !== -1) {
        const node: any = this.graph.getCellById(cell.id)
        Object.assign(json, node.size())
        if (json.shape === 'flow-group') groupIds.push(cell.id)
      }
      return json
    }).filter((json: any) => {
      if (['flow-edge', 'edge'].indexOf(json.shape) === -1) return true
      if (groupIds.indexOf(json.source.cell) !== -1) return false
      if (groupIds.indexOf(json.target.cell) !== -1) return false
      return true
    })
    return result
  }

  toggleGrid (visible: boolean) {
    visible ? this.graph.showGrid() : this.graph.hideGrid()
  }

  startDrag (event: any, widget: any) {
    this.counter++
    const point: any = this.graph.pageToLocal(event.pageX, event.pageY)
    const shape = NodeShapes[widget.shape]
    const metadata: X6.Node.Metadata = {
      shape: widget.shape,
      x: point.x - shape.offsetX,
      y: point.y - shape.offsetY,
      width: shape.width,
      height: shape.height,
      zIndex: this.counter,
      data: Object.assign(widget.options(), {
        name: `${widget.label}_${this.counter}`,
        icon: widget.icon,
        type: widget.type,
        description: widget.title,
      })
    }
    const node: any = this.graph.createNode(metadata)
    this.dnd.start(node, event)
    return node
  }

  updateCell (item: any) {
    if (!item || !item.shape) return false
    const cell: any = this.graph.getCellById(item.id)
    if (!cell) return false
    cell.setData(item.data)
    return true
  }

  cell2meta (cell: any) {
    const json = cell.toJSON()
    return this.briefJSON(json)
  }

  briefJSON (json: any) {
    const result: any = {
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

  subprocess (node: any): any {
    if (!node) return null
    if (node.parent instanceof FlowSubprocess) return node.parent
    return this.subprocess(node.parent)
  }

  select (cells: X6.Cell | string | (X6.Cell | string)[]): Flow {
    this.graph.resetSelection(cells)
    return this
  }

  highlight (colors: any) {
    for (const id in colors) {
      const color = colors[id]
      if (!color) continue
      const cell: any = this.graph.getCellById(id)
      if (!cell) continue
      switch (cell.shape) {
        case 'edge':
        case 'flow-edge':
          cell.attr({ line: { stroke: color } })
          break
        case 'flow-node':
          cell.attr({ foreignObject: { style: { color: color } } })
          break
        default:
          cell.attr({ text: { fill: color } })
      }
    }
  }

  regist (): Flow { // 需要在处理数据前完成图形注册
    X6.Graph.unregisterNode('flow-node')
    X6VueShape.register({ shape: 'flow-node', component: FlowNode, ports: Port2RL, })
    X6.Graph.unregisterNode('kg-node')
    X6VueShape.register({ shape: 'kg-node', component: KGNode, ports: Port4TRBL, })

    X6.Graph.unregisterNode('flow-group')
    X6.Graph.registerNode('flow-group', FlowGroup)
    X6.Graph.unregisterNode('flow-subprocess')
    X6.Graph.registerNode('flow-subprocess', { inherit: FlowSubprocess, ports: Port4TRBL, })
    X6.Graph.unregisterNode('flow-gateway')
    X6.Graph.registerNode('flow-gateway', { inherit: FlowGateway, ports: Port4TRBL, })

    X6.Graph.unregisterEdge('flow-edge')
    X6.Graph.registerEdge('flow-edge', FlowEdge)
    X6.Graph.unregisterEdgeTool('circle-source-arrowhead')
    X6.Graph.registerEdgeTool('circle-source-arrowhead', {
      inherit: 'source-arrowhead',
      tagName: 'circle',
      attrs: {
        r: 3,
        fill: '#31d0c6',
        'fill-opacity': 0.3,
        stroke: '#fe854f',
        'stroke-width': 2,
        cursor: 'move'
      }
    })
    X6.Graph.unregisterEdgeTool('circle-target-arrowhead')
    X6.Graph.registerEdgeTool('circle-target-arrowhead', {
      inherit: 'target-arrowhead',
      tagName: 'circle',
      attrs: {
        r: 3,
        fill: '#31d0c6',
        'fill-opacity': 0.3,
        stroke: '#fe854f',
        'stroke-width': 2,
        cursor: 'move'
      }
    })
    return this
  }

  plugin (): Flow {
    this.graph.use(new Export())
    this.graph.use(new Selection({
      enabled: this.options.selection,
    }))
    this.graph.use(new Keyboard({
      enabled: this.options.keyboard,
    }))
    this.options.minimap && this.graph.use(new MiniMap({
      container: this.options.minimap,
    }))
    if (this.options.readonly) return this
    this.graph.use(new Transform({
      resizing: {
        enabled: this.options.resizing,
        minWidth: 50,
        minHeight: 50,
      },
      rotating: this.options.rotating,
    }))
    this.graph.use(new Snapline({
      enabled: this.options.snapline,
      clean: false,
    }))
    this.graph.use(new Clipboard({
      enabled: this.options.clipboard,
    }))
    this.graph.use(new History({
      enabled: this.options.history,
    }))
    this.options.scroller && this.graph.use(new Scroller({
      enabled: this.options.scroller,
    }))
    return this
  }

  showPorts (ports: any, show: Boolean) {
    for (let i = 0, len = ports.length; i < len; i = i + 1) {
      ports[i].style.visibility = show ? 'visible' : 'hidden'
    }
  }

  bindEvent (): Flow {
    this.graph.bindKey(['ctrl+1', 'meta+1'], () => { // 放大
      const zoom = this.graph.zoom()
      if (zoom < 1.5) { this.graph.zoom(0.1) }
    })
    this.graph.bindKey(['ctrl+2', 'meta+2'], () => { // 缩小
      const zoom = this.graph.zoom()
      if (zoom > 0.5) { this.graph.zoom(-0.1) }
    })
    this.graph.on('node:collapse', ({ e, node } = {} as any) => {
      e.stopPropagation()
      node.toggleCollapse()
      const collapsed = node.isCollapsed()
      if (node instanceof FlowGroup) {
        collapsed ? node.addTransientEdge(this.graph) : node.removeTransientEdge(this.graph)
      }
      const collapse = (parent: any) => {
        const cells = parent.getChildren()
        if (cells) {
          cells.forEach((cell: any) => {
            collapsed ? cell.hide() : cell.show()
            if (cell instanceof FlowGroup || cell instanceof FlowSubprocess) {
              if (!cell.isCollapsed()) collapse(cell)
            }
          })
        }
      }
      collapse(node)
    })
    if (this.options.readonly) return this
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
      if (this.graph.canUndo()) {
        this.graph.undo()
      }
      return false
    })
    this.graph.bindKey(['meta+shift+z', 'ctrl+shift+z'], () => { // 重做
      if (this.graph.canRedo()) {
        this.graph.redo()
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
    this.graph.on('node:mouseenter', () => {
      const ports = this.graph.container.querySelectorAll('.x6-port-body')
      this.showPorts(ports, true)
    })
    this.graph.on('node:mouseleave', () => {
      const ports = this.graph.container.querySelectorAll('.x6-port-body')
      this.showPorts(ports, false)
    })
    this.graph.on('edge:mouseenter', ({ cell } = {} as any) => {
      cell.addTools([
        { name: 'circle-source-arrowhead' },
        { name: 'circle-target-arrowhead' }
      ])
    })
    this.graph.on('edge:mouseleave', ({ cell } = {} as any) => {
      cell.removeTools()
    })
    this.graph.on('edge:connected', (data: any) => {
      data.isNew && data.edge.setData({ name: '', description: '', options: {} })
      this.options.onEdgeConnected(data)
    })
    this.graph.on('node:added', (data: any) => {
      this.options.onNodeAdded(data)
    })
    this.graph.on('node:embedded', ({ node, currentParent } = {} as any) => {
      if (currentParent) node.setZIndex((currentParent.getZIndex() || 0) + 1)
    })
    this.graph.on('cell:click', (data: any) => {
      if (!data.cell.getData()) return true
      if (this.options.onCellClick(data)) data.e.stopPropagation()
    })
    this.graph.on('cell:dblclick', (data: any) => {
      if (this.options.onCellDoubleClick(data)) data.e.stopPropagation()
    })
    this.graph.on('cell:contextmenu', (data: any) => {
      if (this.options.onCellContextmenu(data)) data.e.stopPropagation()
    })
    this.graph.on('blank:click', (data: any) => {
      if (this.options.onBlankClick(data)) data.e.stopPropagation()
    })
    this.graph.on('blank:dblclick', (data: any) => {
      if (this.options.onBlankDoubleClick(data)) data.e.stopPropagation()
    })
    this.graph.on('blank:contextmenu', (data: any) => {
      if (this.options.onBlankContextmenu(data)) data.e.stopPropagation()
    })
    this.graph.on('edge:selected', (data: any) => {
      switch (data.cell.shape) {
        case 'flow-edge':
          data.edge.attr('line', { stroke: '#5cadf8ff' })
          break
      }
    })
    this.graph.on('edge:unselected', (data: any) => {
      switch (data.cell.shape) {
        case 'flow-edge':
          data.edge.attr('line', { stroke: '#A2B1C3' })
          break
      }
    })
    this.graph.on('node:selected', (data: any) => {
      switch (data.cell.shape) {
        case 'flow-group':
        case 'flow-subprocess':
        case 'flow-gateway':
          data.node.attr('body', { stroke: '#1890ff' })
          break
      }
    })
    this.graph.on('node:unselected', (data: any) => {
      switch (data.cell.shape) {
        case 'flow-group':
        case 'flow-subprocess':
          data.node.attr('body', { stroke: 'rgb(34, 36, 42)' })
          break
        case 'flow-gateway':
          data.node.attr('body', { stroke: 'rgb(204, 204, 204)' })
          break
      }
    })
    return this
  }

}

const NodeShapes: any = {
  'flow-node': {
    width: 60,
    height: 60,
    offsetX: 30,
    offsetY: 30
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
  },
  'flow-gateway': {
    width: 120,
    height: 80,
    offsetX: 60,
    offsetY: 40
  }
}

const CircleAttr = {
  circle: {
    r: 4,
    magnet: true,
    stroke: '#5F95FF',
    strokeWidth: 1,
    fill: '#fff',
    style: { visibility: 'hidden' }
  }
}

const Port4TRBL = {
  groups: {
    top: { position: 'top', attrs: CircleAttr },
    right: { position: 'right', attrs: CircleAttr },
    bottom: { position: 'bottom', attrs: CircleAttr },
    left: { position: 'left', attrs: CircleAttr }
  },
  items: [
    { id: 'top', group: 'top' },
    { id: 'right', group: 'right' },
    { id: 'bottom', group: 'bottom' },
    { id: 'left', group: 'left' }
  ]
}

const Port2RL = {
  groups: {
    right: { position: 'right', attrs: CircleAttr },
    left: { position: 'left', attrs: CircleAttr }
  },
  items: [
    { id: 'right', group: 'right' },
    { id: 'left', group: 'left' }
  ]
}

const HighlightColor = {
  running: '#409eff', // 正在执行
  success: '#67c23a', // 执行成功
  warning: '#e6a23c', // 跳过/警示
  error: '#f56c6c', // 执行异常
  terminated: '#909399', // 执行中止
}

const defaults = {
  grid: false,
  autoResize: true,
  panning: true,
  readonly: false,
  mousewheel: true,
  resizing: true,
  rotating: true,
  snapline: true,
  clipboard: true,
  keyboard: true,
  selection: true,
  history: true,
  scroller: false,
  onCellClick ({ e, x, y, cell, view } = {} as any) {},
  onCellDoubleClick ({ e, x, y, cell, view } = {} as any) {},
  onCellContextmenu ({ e, x, y, cell, view } = {} as any) {},
  onBlankClick ({ e, x, y } = {} as any) {},
  onBlankDoubleClick ({ e, x, y } = {} as any) {},
  onBlankContextmenu ({ e, x, y } = {} as any) {},
  onNodeAdded ({ node, index, options } = {} as any) {},
  onEdgeConnected () {},
}

export default Object.assign(Flow, {
  defaults,
  X6,
  X6VueShape,
  Port2RL,
  Port4TRBL,
  CircleAttr,
  NodeShapes,
  HighlightColor,
})
