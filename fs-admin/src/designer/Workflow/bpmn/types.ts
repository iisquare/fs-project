export interface BPMNCell {
  id: string
  shape: string
  zIndex: number
  position?: { x: number; y: number }
  size?: { width: number; height: number }
  data?: Record<string, any>
  source?: { cell: string; port?: string }
  target?: { cell: string; port?: string }
  angle?: number
  parent?: string
  children?: string[]
}

export interface BPMNGraph {
  cells: BPMNCell[]
}

export interface BPMNElementData {
  id: string
  name: string
  type: string
  icon: string
  description?: string
  documentation?: string
  candidateGroups?: string
  authority?: string
  condition?: string
  loopCharacteristics?: boolean
  isSequential?: boolean
  loopCardinality?: string
  collection?: string
  elementVariable?: string
  completionCondition?: string
}

export const BPMN_TYPE_MAP: Record<string, string> = {
  'bpmn:StartEvent': 'bpmn-start-event',
  'bpmn:EndEvent': 'bpmn-end-event',
  'bpmn:UserTask': 'bpmn-user-task',
  'bpmn:ExclusiveGateway': 'bpmn-exclusive-gateway',
  'bpmn:ParallelGateway': 'bpmn-parallel-gateway',
  'bpmn:InclusiveGateway': 'bpmn-inclusive-gateway',
}

export const BPMN_SHAPE_TO_TYPE: Record<string, string> = {
  'bpmn-start-event': 'bpmn:StartEvent',
  'bpmn-end-event': 'bpmn:EndEvent',
  'bpmn-user-task': 'bpmn:UserTask',
  'bpmn-exclusive-gateway': 'bpmn:ExclusiveGateway',
  'bpmn-parallel-gateway': 'bpmn:ParallelGateway',
  'bpmn-inclusive-gateway': 'bpmn:InclusiveGateway',
}
