import inherits from 'inherits'
import BaseRenderer from 'diagram-js/lib/draw/BaseRenderer'
import BpmnRenderer from 'bpmn-js/lib/draw/BpmnRenderer'

export default function FsRenderer (eventBus, bpmnRenderer) {
  BaseRenderer.call(this, eventBus, 1000)
  this.bpmnRenderer = bpmnRenderer
}

inherits(FsRenderer, BpmnRenderer)

FsRenderer.$inject = [
  'eventBus',
  'bpmnRenderer'
]
