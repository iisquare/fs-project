import * as d3 from 'd3'

/**
 * 参考：
 * - [Mobile Patent Suits](https://observablehq.com/@d3/mobile-patent-suits)
 * - [D3 关联关系图 力学图](https://blog.csdn.net/qq_39408204/article/details/103799835)
 * - [用 D3.js 画一个手机专利关系图, 看看苹果,三星,微软间的专利纠葛](https://github.com/ssthouse/ssthouse-blog/blob/master/mobile-patent-suit/blog.md)
 */
class Relationship {
  constructor (container, options) {
    this.simulation = d3.forceSimulation()
    this.simulation.force('charge', d3.forceManyBody().strength(50))
    this.simulation.force('collide', d3.forceCollide().radius(150))
    this.container = d3.select(container)
    this.options = Object.assign({}, options)
    this.resize()
    this.simulation.on('tick', () => this.tick())
  }

  drag () {
    return d3.drag().on('start', (e, d) => {
      if (!e.active) this.simulation.alphaTarget(0.3).restart()
      d.fx = e.x
      d.fy = e.y
    }).on('drag', (e, d) => {
      d.fx = e.x
      d.fy = e.y
    }).on('end', (e, d) => {
      if (!e.active) this.simulation.alphaTarget(0)
      d.fx = null
      d.fy = null
    })
  }

  resize () {
    const container = this.container.node()
    this.simulation.force('center', d3.forceCenter().x(container.offsetWidth / 2).y(container.offsetHeight / 2))
  }

  coincide (links) { // 标记重复路径
    const result = []
    const counter = {} // min_max => count
    for (const link of links) {
      const key = Math.min(link.source, link.target) + '_' + Math.max(link.source, link.target)
      const count = counter[key] ?? 0
      result.push(Object.assign({}, link, { coincideIndex: count }))
      counter[key] = count + 1
    }
    for (const link of result) {
      const key = Math.min(link.source, link.target) + '_' + Math.max(link.source, link.target)
      link.coincideCount = counter[key]
    }
    return result
  }

  reset (nodes, links) {
    const _this = this
    links = this.coincide(links)
    this.canvas && this.canvas.remove()
    this.canvas = this.container.append('svg').attr('width', '100%').attr('height', '100%')
    this.svg = this.canvas.append('g')
    this.simulation.nodes(nodes)
    this.simulation.force('link', d3.forceLink(links).id(d => d.id))
    this.redraw()
    this.simulation.alpha(1).restart().tick()
    this.canvas.on('click', function (event) {
      _this.nodeCircle.classed('fs-node-selected', false)
      _this.edgeLine.classed('fs-edge-selected', false)
      event.stopPropagation()
      _this.options.onCanvasClick && _this.options.onCanvasClick(event)
    })
    this.canvas.call(d3.zoom().scaleExtent([1 / 2, 8]).on('zoom', event => {
      this.svg.attr('transform', event.transform)
    }))
    this.canvas.call(d3.drag().on('drag', event => {
      this.svg.attr('cx', event.x).attr('cy', event.y)
    }))
  }

  centre () {
    const nodes = this.simulation.nodes()
    const container = this.container.node()
    let x = 0
    let y = 0
    nodes.forEach(item => {
      x += item.x
      y += item.y
    })
    return { x: x / nodes.length, y: y / nodes.length, w: container.offsetWidth, h: container.offsetHeight }
  }

  tick () {
    const centre = this.centre()
    this.nodeCircle && this.nodeCircle.attr('transform', d => `translate(${d.x}, ${d.y})`)
    this.nodeText && this.nodeText.attr('transform', d => `translate(${d.x}, ${d.y})`)
    this.edgeLine && this.edgeLine.attr('d', function (d) {
      const r = 25
      const sx = d.source.x
      const sy = d.source.y
      const tx = d.target.x
      const ty = d.target.y
      if (d.source.id === d.target.id) {
        let theta = Math.atan2((sx + centre.x) / 2 - sx, sy - (sy + centre.y) / 2) * (180 / Math.PI)
        theta += Math.pow(-1, d.coincideIndex) * d.coincideIndex * 360 / d.coincideCount / Math.PI // 旋转重复路径
        d3.select(this).attr('transform', `rotate(${theta} ${sx} ${sy})`)
        return `M ${sx} ${sy} L ${sx + r} ${sy + r * 4} A ${r} ${r} 0 0 1 ${sx - r} ${sy + r * 4} L ${sx} ${sy}`
      }
      if (d.coincideCount > 1) { // 弯曲重复路径
        let offset = d.coincideIndex % 2 === 0 ? (d.coincideIndex + 1) : d.coincideIndex // 相邻奇偶位对称
        offset *= Math.pow(-1, d.coincideIndex + 1) * r * 2 // 对称偏移
        const rx = sx + (tx - sx) / 2 + offset
        const ry = sy + (ty - sy) / 2 + offset
        return `M ${sx} ${sy} Q ${rx} ${ry} ${tx} ${ty}`
      }
      return `M ${sx} ${sy} L ${tx} ${ty}`
    })
    this.edgeText && this.edgeText.attr('transform', function (d, i) {
      if (d.target.x < d.source.x) {
        const box = this.getBBox()
        return `rotate(180 ${box.x + box.width / 2} ${box.y + box.height / 2})`
      } else {
        return 'rotate(0)'
      }
    })
  }

  redraw () {
    const _this = this
    this.marker = this.svg.append('defs').selectAll('marker').data(['resolved']).enter().append('marker')
    this.marker.attr('id', d => d).attr('markerUnits', 'userSpaceOnUse')
    this.marker.attr('viewBox', '0 -5 10 10').attr('refX', 59).attr('refY', 0)
    this.marker.attr('markerWidth', 8).attr('markerHeight', 8)
    this.marker.attr('orient', 'auto').attr('stroke-width', 2)
    this.marker.append('path').attr('d', 'M0,-5L10,0L0,5').attr('fill', '#595D68')

    this.edgeLine = this.svg.append('g').attr('fill', 'none')
      .selectAll('.fs-edge-line').data(this.simulation.force('link').links()).enter().append('path')
    this.edgeLine.attr('id', (d, i) => `edgepath${i}`)
    this.edgeLine.attr('class', 'fs-edge-line')
    this.edgeLine.attr('marker-end', d => 'url(#resolved)')
    this.edgeLine.attr('d', d => `M ${d.source.x} ${d.source.y} L ${d.target.x} ${d.target.y}`)

    this.edgeText = this.svg.append('g').selectAll('.fs-edge-label').data(this.simulation.force('link').links()).enter().append('text')
    this.edgeText.attr('class', 'fs-edge-label').attr('dx', d => 120).attr('dy', -5)
    this.edgeText.append('textPath').attr('xlink:href', (d, i) => `#edgepath${i}`).text(d => d.label)
    this.edgeText.on('click', function (event, d) {
      _this.nodeCircle.classed('fs-node-selected', false)
      _this.edgeLine.classed('fs-edge-selected', false)
      d3.select(d3.select(this).selectChild('textPath').attr('xlink:href')).classed('fs-edge-selected', true)
      event.stopPropagation()
      _this.options.onLinkClick && _this.options.onLinkClick(event, d)
    })

    this.nodeCircle = this.svg.append('g').selectAll('circle').data(this.simulation.nodes()).enter().append('circle')
      .attr('class', 'fs-node-circle').attr('r', 40).call(this.drag())
    this.nodeCircle.on('click', function (event, d) {
      _this.nodeCircle.classed('fs-node-selected', false)
      _this.edgeLine.classed('fs-edge-selected', false)
      d3.select(this).classed('fs-node-selected', true)
      event.stopPropagation()
      _this.options.onNodeClick && _this.options.onNodeClick(event, d)
    })

    this.nodeText = this.svg.append('g').selectAll('text').data(this.simulation.nodes()).enter().append('text')
      .attr('dy', '.35em').attr('text-anchor', 'middle').attr('class', 'fs-node-text').text(d => d.name)
  }
}

export default Relationship
