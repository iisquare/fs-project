import type { BPMNCell, BPMNGraph } from './types'
import { BPMN_TYPE_MAP, BPMN_SHAPE_TO_TYPE } from './types'
import template from './template'

const NS = {
  bpmn: 'http://www.omg.org/spec/BPMN/20100524/MODEL',
  bpmndi: 'http://www.omg.org/spec/BPMN/20100524/DI',
  omgdc: 'http://www.omg.org/spec/DD/20100524/DC',
  omgdi: 'http://www.omg.org/spec/DD/20100524/DI',
  xsi: 'http://www.w3.org/2001/XMLSchema-instance',
  flowable: 'http://flowable.org/bpmn',
}

const ICON_MAP: Record<string, string> = {
  'bpmn:StartEvent': 'flow.startEvent',
  'bpmn:EndEvent': 'flow.endEvent',
  'bpmn:UserTask': 'flow.userTask',
  'bpmn:ExclusiveGateway': 'flow.exclusiveGateway',
  'bpmn:ParallelGateway': 'flow.parallelGateway',
  'bpmn:InclusiveGateway': 'flow.inclusiveGateway',
}

function cdata(text: string): string {
  return text ? `<![CDATA[${text}]]>` : ''
}

function parseCDATA(value: string | null): string {
  if (!value) return ''
  return value.replace(/^<!\[CDATA\[/, '').replace(/\]\]>$/, '')
}

function escapeXml(str: string): string {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;')
}

function nsResolver(prefix: string): string | null {
  const map: Record<string, string> = {
    bpmn: NS.bpmn,
    bpmndi: NS.bpmndi,
    omgdc: NS.omgdc,
    omgdi: NS.omgdi,
    xsi: NS.xsi,
    flowable: NS.flowable,
  }
  return map[prefix] || null
}

export default {
  parse(xml: string): BPMNGraph {
    const parser = new DOMParser()
    const doc = parser.parseFromString(xml, 'application/xml')
    const cells: BPMNCell[] = []
    let counter = 0

    // Parse BPMN shapes from process element
    const process = doc.querySelector('process') || doc.getElementsByTagNameNS(NS.bpmn, 'process')[0]
    if (!process) return { cells }

    const plane = doc.querySelector('BPMNPlane') || doc.getElementsByTagNameNS(NS.bpmndi, 'BPMNPlane')[0]

    // Build DI lookup: bpmnElement id → bounds/waypoints
    const diShapes: Record<string, any> = {}
    const diEdges: Record<string, any[]> = {}
    if (plane) {
      plane.querySelectorAll('BPMNShape').forEach((shape: any) => {
        const bpmnElement = shape.getAttribute('bpmnElement')
        const bounds = shape.querySelector('Bounds') || shape.getElementsByTagNameNS(NS.omgdc, 'Bounds')[0]
        if (bpmnElement && bounds) {
          diShapes[bpmnElement] = {
            x: parseFloat(bounds.getAttribute('x') || '0'),
            y: parseFloat(bounds.getAttribute('y') || '0'),
            width: parseFloat(bounds.getAttribute('width') || '100'),
            height: parseFloat(bounds.getAttribute('height') || '60'),
          }
        }
      })
      plane.querySelectorAll('BPMNEdge').forEach((edge: any) => {
        const bpmnElement = edge.getAttribute('bpmnElement')
        const waypoints: any[] = []
        edge.querySelectorAll('waypoint').forEach((wp: any) => {
          waypoints.push({ x: parseFloat(wp.getAttribute('x') || '0'), y: parseFloat(wp.getAttribute('y') || '0') })
        })
        if (bpmnElement) diEdges[bpmnElement] = waypoints
      })
    }

    // Parse child elements of process
    const children = process.children || process.childNodes
    for (let i = 0; i < children.length; i++) {
      const el: any = children[i]
      if (!el.tagName) continue
      const tag = el.tagName.includes(':') ? el.tagName.split(':')[1] : el.tagName
      const bpmnType = `bpmn:${tag}`
      const shape = BPMN_TYPE_MAP[bpmnType]

      if (shape) {
        counter++
        const id = el.getAttribute('id') || `node_${counter}`
        const name = el.getAttribute('name') || ''
        const di = diShapes[id] || { x: 100 + counter * 20, y: 100 + counter * 20, width: 100, height: 60 }

        const data: Record<string, any> = {
          type: bpmnType,
          name,
          icon: ICON_MAP[bpmnType] || '',
          description: '',
        }

        // Parse documentation
        const docEl = el.querySelector('documentation') || el.getElementsByTagNameNS(NS.bpmn, 'documentation')[0]
        if (docEl) {
          data.documentation = docEl.textContent || ''
        }

        // Parse UserTask specific attributes
        if (bpmnType === 'bpmn:UserTask') {
          data.candidateGroups = el.getAttribute('flowable:candidateGroups') || el.getAttributeNS(NS.flowable, 'candidateGroups') || ''

          // Parse extensionElements (authority)
          const extElements = el.querySelector('extensionElements') || el.getElementsByTagNameNS(NS.bpmn, 'extensionElements')[0]
          if (extElements) {
            const authorityEl = extElements.querySelector('authority') || extElements.getElementsByTagNameNS(NS.flowable, 'authority')[0]
            if (authorityEl) {
              data.authority = parseCDATA(authorityEl.textContent || '')
            }
          }

          // Parse multiInstanceLoopCharacteristics
          const multiInstance = el.querySelector('multiInstanceLoopCharacteristics') || el.getElementsByTagNameNS(NS.bpmn, 'multiInstanceLoopCharacteristics')[0]
          if (multiInstance) {
            data.loopCharacteristics = true
            data.isSequential = multiInstance.getAttribute('isSequential') === 'true'
            data.collection = multiInstance.getAttribute('flowable:collection') || multiInstance.getAttributeNS(NS.flowable, 'collection') || ''
            data.elementVariable = multiInstance.getAttribute('flowable:elementVariable') || multiInstance.getAttributeNS(NS.flowable, 'elementVariable') || ''
            const cardinality = multiInstance.querySelector('loopCardinality') || multiInstance.getElementsByTagNameNS(NS.bpmn, 'loopCardinality')[0]
            data.loopCardinality = cardinality ? (cardinality.textContent || '') : ''
            const completion = multiInstance.querySelector('completionCondition') || multiInstance.getElementsByTagNameNS(NS.bpmn, 'completionCondition')[0]
            data.completionCondition = completion ? (completion.textContent || '') : ''
          } else {
            data.loopCharacteristics = false
          }
        }

        // Parse StartEvent extensionElements (authority)
        if (bpmnType === 'bpmn:StartEvent') {
          const extElements = el.querySelector('extensionElements') || el.getElementsByTagNameNS(NS.bpmn, 'extensionElements')[0]
          if (extElements) {
            const authorityEl = extElements.querySelector('authority') || extElements.getElementsByTagNameNS(NS.flowable, 'authority')[0]
            if (authorityEl) {
              data.authority = parseCDATA(authorityEl.textContent || '')
            }
          }
        }

        // Use shape-specific dimensions
        const dims = getNodeDimensions(shape, di)

        cells.push({
          id,
          shape,
          zIndex: counter,
          position: { x: dims.x, y: dims.y },
          size: { width: dims.width, height: dims.height },
          data,
        })
      } else if (tag === 'sequenceFlow') {
        counter++
        const id = el.getAttribute('id') || `edge_${counter}`
        const sourceRef = el.getAttribute('sourceRef') || ''
        const targetRef = el.getAttribute('targetRef') || ''
        const name = el.getAttribute('name') || ''

        const data: Record<string, any> = { name, description: '', condition: '' }

        // Parse conditionExpression
        const condEl = el.querySelector('conditionExpression') || el.getElementsByTagNameNS(NS.bpmn, 'conditionExpression')[0]
        if (condEl) {
          data.condition = parseCDATA(condEl.textContent || '')
        }

        cells.push({
          id,
          shape: 'flow-edge',
          zIndex: 0,
          source: { cell: sourceRef },
          target: { cell: targetRef },
          data,
        })
      }
    }

    return { cells }
  },

  serialize(cells: BPMNCell[], processId: string, processName: string): string {
    const nodes = cells.filter(c => c.shape !== 'flow-edge')
    const edges = cells.filter(c => c.shape === 'flow-edge')

    let processElements = ''
    let diShapes = ''
    let diEdges = ''

    nodes.forEach(node => {
      const data = node.data || {}
      const bpmnType = data.type || BPMN_SHAPE_TO_TYPE[node.shape] || 'bpmn:UserTask'
      const tag = bpmnType.replace('bpmn:', '')
      const id = node.id
      const name = data.name || ''
      const x = node.position?.x ?? 0
      const y = node.position?.y ?? 0
      const w = node.size?.width ?? 100
      const h = node.size?.height ?? 60

      let attrs = `id="${escapeXml(id)}"`
      if (name) attrs += ` name="${escapeXml(name)}"`

      // UserTask specific
      if (bpmnType === 'bpmn:UserTask') {
        if (data.candidateGroups) {
          attrs += ` flowable:candidateGroups="${escapeXml(data.candidateGroups)}"`
        }
      }

      let innerXml = ''
      const docEl = data.documentation || data.description
      if (docEl) {
        innerXml += `\n      <bpmn:documentation>${escapeXml(docEl)}</bpmn:documentation>`
      }

      // Authority extension for StartEvent and UserTask
      if ((bpmnType === 'bpmn:StartEvent' || bpmnType === 'bpmn:UserTask') && data.authority) {
        innerXml += `\n      <bpmn:extensionElements>\n        <flowable:authority>${cdata(data.authority)}</flowable:authority>\n      </bpmn:extensionElements>`
      }

      // MultiInstance for UserTask
      if (bpmnType === 'bpmn:UserTask' && data.loopCharacteristics) {
        let miAttrs = ''
        if (data.isSequential) miAttrs += ` isSequential="true"`
        if (data.collection) miAttrs += ` flowable:collection="${escapeXml(data.collection)}"`
        if (data.elementVariable) miAttrs += ` flowable:elementVariable="${escapeXml(data.elementVariable)}"`
        let miInner = ''
        if (data.loopCardinality) miInner += `\n        <bpmn:loopCardinality>${escapeXml(data.loopCardinality)}</bpmn:loopCardinality>`
        if (data.completionCondition) miInner += `\n        <bpmn:completionCondition>${escapeXml(data.completionCondition)}</bpmn:completionCondition>`
        innerXml += `\n      <bpmn:multiInstanceLoopCharacteristics${miAttrs}>${miInner}\n      </bpmn:multiInstanceLoopCharacteristics>`
      }

      processElements += `\n    <bpmn:${tag} ${attrs}>${innerXml}\n    </bpmn:${tag}>`

      diShapes += `\n      <bpmndi:BPMNShape id="BPMNShape_${escapeXml(id)}" bpmnElement="${escapeXml(id)}">`
      diShapes += `\n        <omgdc:Bounds x="${x}" y="${y}" width="${w}" height="${h}" />`
      diShapes += `\n      </bpmndi:BPMNShape>`
    })

    edges.forEach(edge => {
      const data = edge.data || {}
      const id = edge.id
      const sourceRef = edge.source?.cell || ''
      const targetRef = edge.target?.cell || ''
      const name = data.name || ''

      let attrs = `id="${escapeXml(id)}" sourceRef="${escapeXml(sourceRef)}" targetRef="${escapeXml(targetRef)}"`
      if (name) attrs += ` name="${escapeXml(name)}"`

      let innerXml = ''
      if (data.condition) {
        innerXml += `\n      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${cdata(data.condition)}</bpmn:conditionExpression>`
      }

      processElements += `\n    <bpmn:sequenceFlow ${attrs}>${innerXml}\n    </bpmn:sequenceFlow>`

      // Compute waypoints from source/target node positions
      const srcNode = nodes.find(n => n.id === sourceRef)
      const tgtNode = nodes.find(n => n.id === targetRef)
      if (srcNode && tgtNode) {
        const sx = (srcNode.position?.x ?? 0) + (srcNode.size?.width ?? 100) / 2
        const sy = (srcNode.position?.y ?? 0) + (srcNode.size?.height ?? 60) / 2
        const tx = (tgtNode.position?.x ?? 0) + (tgtNode.size?.width ?? 100) / 2
        const ty = (tgtNode.position?.y ?? 0) + (tgtNode.size?.height ?? 60) / 2
        diEdges += `\n      <bpmndi:BPMNEdge id="BPMNEdge_${escapeXml(id)}" bpmnElement="${escapeXml(id)}">`
        diEdges += `\n        <omgdi:waypoint x="${sx}" y="${sy}" />`
        diEdges += `\n        <omgdi:waypoint x="${tx}" y="${ty}" />`
        diEdges += `\n      </bpmndi:BPMNEdge>`
      }
    })

    const xml = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC" xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
  <process id="${escapeXml(processId)}" name="${escapeXml(processName)}">${processElements}
  </process>
  <bpmndi:BPMNDiagram id="BpmnDiagram_1">
    <bpmndi:BPMNPlane id="BpmnPlane_1" bpmnElement="${escapeXml(processId)}">${diShapes}${diEdges}
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`

    return xml
  },

  defaultTemplate(processId: string): string {
    return template.flowable.replace('Process_iisquare', processId)
  },
}

function getNodeDimensions(shape: string, di: any): { x: number; y: number; width: number; height: number } {
  const defaults: Record<string, { width: number; height: number }> = {
    'bpmn-start-event': { width: 50, height: 50 },
    'bpmn-end-event': { width: 50, height: 50 },
    'bpmn-user-task': { width: 150, height: 60 },
    'bpmn-exclusive-gateway': { width: 100, height: 60 },
    'bpmn-parallel-gateway': { width: 100, height: 60 },
    'bpmn-inclusive-gateway': { width: 100, height: 60 },
  }
  const def = defaults[shape] || { width: 100, height: 60 }
  return {
    x: di.x,
    y: di.y,
    width: di.width || def.width,
    height: di.height || def.height,
  }
}
