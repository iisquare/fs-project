import { jsPlumb } from 'jsplumb'

class Draw {
  constructor (containment) {
    this.containment = containment
    this.counter = 0
    this.items = []
    this.ready = false
    this.endpoints = {}
    this.channels = []
    this.anchors = ['TopCenter', 'RightMiddle', 'BottomCenter', 'LeftMiddle']
    jsPlumb.ready(() => {
      this.jsPlumbInstance = jsPlumb.getInstance({
        Endpoint: ['Dot', { radius: 3 }],
        Connector: ['Bezier']
      })
      this.bindEvent()
      this.ready = true
    })
  }

  bindEvent () {
    this.jsPlumbInstance.bind('click', (connInfo, originalEvent) => {
      if (!originalEvent || !(connInfo instanceof jsPlumb.Connection)) return true
      this.jsPlumbInstance.deleteConnection(connInfo)
      return true
    })
  }

  generateItem (widget, ev) {
    const counter = ++this.counter
    const item = {
      id: `item_${counter}`,
      name: '',
      title: `${widget.label}_${counter}`,
      icon: widget.icon,
      x: ev.offsetX - 60,
      y: ev.offsetY - 18,
      index: counter,
      type: widget.type,
      description: widget.title,
      options: widget.options()
    }
    return item
  }

  addRelations (relations) {
    this.channels.push(() => {
      relations.forEach(item => {
        this.jsPlumbInstance.connect({
          source: this.endpoints[item.sourceId + '-' + item.sourceAnchor],
          target: this.endpoints[item.targetId + '-' + item.targetAnchor]
        })
      })
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
          maxConnections: -1,
          connectorOverlays: [
            ['Arrow', { width: 10, length: 10, location: 1 }],
            ['Label', { label: 'X', cssClass: 'fs-label-pointer' }]
          ]
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

  collect () {
    const data = {
      items: this.items,
      relations: []
    }
    this.jsPlumbInstance.getAllConnections().forEach(connection => {
      const item = { sourceId: connection.sourceId, targetId: connection.targetId }
      connection.endpoints.forEach(endpoint => {
        if (!item['sourceAnchor'] && connection.sourceId === endpoint.anchor.elementId) {
          item['sourceAnchor'] = endpoint.anchor.type
        } else {
          item['targetAnchor'] = endpoint.anchor.type
        }
      })
      data.relations.push(item)
    })
    return data
  }

  clear () {
    let result = true
    while (result && this.items.length > 0) {
      result = this.removeItem(this.items[0])
    }
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
