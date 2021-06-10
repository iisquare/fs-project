import inherits from 'inherits'

import RuleProvider from 'diagram-js/lib/features/rules/RuleProvider'
import BpmnRules from 'bpmn-js/lib/features/rules/BpmnRules'

export default function FsRules (eventBus, bpmnRules, fsRenderer) {
  RuleProvider.call(this, eventBus)
  this.bpmnRules = bpmnRules
  this.fsRenderer = fsRenderer
}

inherits(FsRules, BpmnRules)

FsRules.$inject = [ 'eventBus', 'bpmnRules', 'fsRenderer' ]

FsRules.prototype.init = function () { // HIGH_PRIORITY
}
