import { jsPlumb } from 'jsplumb'

class Draw {
  constructor (containment, onClickConnection) {
    this.containment = containment
    this.onClickConnection = onClickConnection
    this.counter = 0
    this.items = []
    this.relations = {}
    this.ready = false
    this.endpoints = {}
    this.channels = []
    this.anchors = ['TopCenter', 'RightMiddle', 'BottomCenter', 'LeftMiddle']
    jsPlumb.ready(() => {
      this.jsPlumbInstance = jsPlumb.getInstance({
        Endpoint: ['Dot', { radius: 3 }],
        Connector: ['Flowchart'],
        EndpointStyle: {
          fill: '#c9c9c9'
        },
        PaintStyle: {
          strokeWidth: 3,
          stroke: '#c9c9c9'
        },
        HoverPaintStyle: {
          strokeWidth: 3,
          stroke: '#50aeff'
        }
      })
      this.bindEvent()
      this.ready = true
    })
  }

  bindEvent () {
    this.jsPlumbInstance.bind('click', (connInfo, originalEvent) => {
      if (!originalEvent || !(connInfo instanceof jsPlumb.Connection)) return true
      this.onClickConnection && this.onClickConnection(connInfo, originalEvent)
      return true
    })
    this.jsPlumbInstance.bind('connection', (connInfo, originalEvent) => {
      const item = this.generateRelation(connInfo)
      const relationIdentify = this.relationIdentify(item)
      if (this.relations[relationIdentify]) return true
      this.relations[relationIdentify] = Object.assign(item, { filter: [] })
      this.onClickConnection && this.onClickConnection(connInfo, originalEvent)
    })
  }

  deleteConnection (connInfo) {
    delete this.relations[this.relationIdentify(this.generateRelation(connInfo))]
    this.jsPlumbInstance.deleteConnection(connInfo instanceof jsPlumb.Connection ? connInfo : connInfo.connection)
  }

  generateItem (sourceId, table, ev) {
    const counter = ++this.counter
    const item = {
      id: `item_${counter}`,
      name: `${table.name}`,
      x: ev.offsetX - 60,
      y: ev.offsetY - 18,
      index: counter,
      sourceId,
      table: table.name,
      dsl: '',
      columns: table.columns
    }
    return item
  }

  fields () {
    const result = []
    this.items.forEach(item => {
      for (const index in item.columns) {
        const column = item.columns[index]
        result.push({ label: item.name + '.' + column.name, value: '`' + item.table + '`.`' + column.name + '`' })
      }
    })
    return result
  }

  addRelations (relations) {
    Object.assign(this.relations, relations)
    this.channels.push(() => {
      for (const index in relations) {
        const item = relations[index]
        this.jsPlumbInstance.connect({
          source: this.endpoints[item.sourceId + '-' + item.sourceAnchor],
          target: this.endpoints[item.targetId + '-' + item.targetAnchor]
        })
      }
    })
  }

  addItem (item) {
    if (item.index > this.counter) this.counter = item.index
    const result = this.items.push(item)
    this.channels.push(() => {
      this.jsPlumbInstance.draggable(item.id, {
        containment: this.containment,
        stop (ev) {
          [item.x, item.y] = ev.finalPos
        }
      })
      this.anchors.forEach((anchor) => {
        const el = this.jsPlumbInstance.addEndpoint(item.id, { anchor }, {
          isSource: true,
          isTarget: true,
          maxConnections: -1
        })
        this.endpoints[item.id + '-' + anchor] = el
      })
    })
    return result
  }

  updateItem (item) {
    if (!item) return false
    for (const index in this.items) {
      if (item.id === this.items[index].id) {
        this.items[index] = item
        return true
      }
    }
    return false
  }

  removeItem (item) {
    for (const index in this.items) {
      if (item.id === this.items[index].id) {
        this.jsPlumbInstance.removeAllEndpoints(item.id)
        this.items.splice(index, 1)
        return true
      }
    }
    return false
  }

  generateRelation (connection) {
    const item = {
      sourceId: connection.sourceId,
      targetId: connection.targetId
    }
    if (connection instanceof jsPlumb.Connection) {
      connection.endpoints.forEach(endpoint => {
        if (!item['sourceAnchor'] && connection.sourceId === endpoint.anchor.elementId) {
          item['sourceAnchor'] = endpoint.anchor.type
        } else {
          item['targetAnchor'] = endpoint.anchor.type
        }
      })
    } else {
      Object.assign(item, {
        sourceAnchor: connection.sourceEndpoint.anchor.type,
        targetAnchor: connection.targetEndpoint.anchor.type
      })
    }
    return item
  }

  relationIdentify (relation) {
    return [relation.sourceId, relation.sourceAnchor, relation.targetId, relation.targetAnchor].join('.')
  }

  relation (connInfo) {
    if (!connInfo) return null
    const item = this.generateRelation(connInfo)
    const relationIdentify = this.relationIdentify(item)
    return this.relations[relationIdentify]
  }

  collect () {
    const data = {
      items: this.items,
      relations: this.relations
    }
    return data
  }

  clear () {
    let result = true
    while (result && this.items.length > 0) {
      result = this.removeItem(this.items[0])
    }
    this.relations = {}
  }

  repaint () {
    if (!this.ready) return false
    while (this.channels.length > 0) {
      this.channels.shift()()
    }
    this.jsPlumbInstance.repaintEverything()
    return true
  }
}

export default Draw
