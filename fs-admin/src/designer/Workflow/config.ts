const config: any = {
  uuid () { return new Date().getTime() + ('' + Math.random()).slice(-6) }
}

const EmptyOptions = () => {
  return {}
}

const activateHand = (toolbar: any, bpmn: any, event: any) => {
  bpmn.palette._handTool.activateHand(event)
}

const activateLassoSelection = (toolbar: any, bpmn: any, event: any) => {
  bpmn.palette._lassoTool.activateSelection(event)
}

const activateSpaceSelection = (toolbar: any, bpmn: any, event: any) => {
  bpmn.palette._spaceTool.activateSelection(event)
}

const activateConnect = (toolbar: any, bpmn: any, event: any) => {
  bpmn.palette._globalConnect.toggle(event)
}

const createShape = (widget: any, bpmn: any, event: any, options?: any) => {
  const shape = bpmn.palette._elementFactory.createShape(Object.assign({ type: widget.type }, options))
  if (options) {
    shape.businessObject.di.isExpanded = options.isExpanded
  }
  bpmn.palette._create.start(event, shape)
}

const createParticipant = (widget: any, bpmn: any, event: any) => {
  bpmn.palette._create.start(event, bpmn.palette._elementFactory.createParticipantShape())
}

export default Object.assign(config, {
  duration (value: any) {
    let millis = Number(value) || 0
    return [{
      value: 24 * 60 * 60 * 1000, suffix: '天'
    }, {
      value: 60 * 60 * 1000, suffix: '时'
    }, {
      value: 60 * 1000, suffix: '分'
    }, {
      value: 1000, suffix: '秒'
    }].map(item => {
      const count = Math.floor(millis / item.value)
      millis = millis % item.value
      return count + item.suffix
    }).join('')
  },
  audit (comments: any, taskId: any) {
    const items = (comments && comments[taskId]) || []
    let local = false
    const messages: string[] = []
    items.forEach((comment: any) => {
      const audit = comment.audit || {}
      local = local || !!audit.local
      if (audit.message) messages.push(audit.message)
    })
    return { audit: { local, message: messages.join(',') } }
  },
  canvas: {
    options: EmptyOptions, property: () => import('./CanvasProperty.vue')
  },
  elements: {
    'bpmn:StartEvent': { options: EmptyOptions, property: () => import('./StartEventProperty.vue') },
    'bpmn:EndEvent': { options: EmptyOptions, property: () => import('./NodeProperty.vue') },
    'bpmn:UserTask': { options: EmptyOptions, property: () => import('./UserTaskProperty.vue') },
    'bpmn:ExclusiveGateway': { options: EmptyOptions, property: () => import('./NodeProperty.vue') },
    'bpmn:ParallelGateway': { options: EmptyOptions, property: () => import('./NodeProperty.vue') },
    'bpmn:InclusiveGateway': { options: EmptyOptions, property: () => import('./NodeProperty.vue') },
    'bpmn:Participant': { options: EmptyOptions, property: () => import('./NodeProperty.vue') },
    'bpmn:Group': { options: EmptyOptions, property: () => import('./NodeProperty.vue') },
    'bpmn:SequenceFlow': { options: EmptyOptions, property: () => import('./SequenceFlowProperty.vue') }
  } as any,
  widgets: [{
    name: '事件',
    children: [{
      type: 'bpmn:StartEvent', label: '开始', title: '开始事件', icon: 'flow.startEvent', callback: createShape
    }, {
      type: 'bpmn:EndEvent', label: '结束', title: '结束事件', icon: 'flow.endEvent', callback: createShape
    }]
  }, {
    name: '任务',
    children: [{
      type: 'bpmn:UserTask', label: '用户任务', title: '用户任务节点', icon: 'flow.userTask', callback: createShape
    }]
  }, {
    name: '网关',
    children: [{ // 仅执行第一个满足条件的分支
      type: 'bpmn:ExclusiveGateway', label: '排他网关', title: '仅执行第一个满足条件的分支', icon: 'flow.exclusiveGateway', callback: createShape
    }, { // 忽略分支条件，全部执行
      type: 'bpmn:ParallelGateway', label: '并行网关', title: '忽略分支条件，全部执行', icon: 'flow.parallelGateway', callback: createShape
    }, { // 排他网关和并行网关的结合体，执行满足条件的多个分支
      type: 'bpmn:InclusiveGateway', label: '包容网关', title: '执行满足条件的多个分支', icon: 'flow.inclusiveGateway', callback: createShape
    }]
  }, {
    name: '加工',
    children: [{
      type: 'bpmn:Participant', label: '泳道', title: '池/泳道', icon: 'flow.subprocess', callback: createParticipant
    }, {
      type: 'bpmn:Group', label: '分组', title: '分组', icon: 'flow.group', callback: createShape
    }]
  }],
  toolbars: [{
    type: 'hand', label: '拖动', icon: 'action.hand', callback: activateHand
  }, {
    type: 'lasso', label: '框选', icon: 'action.lasso', callback: activateLassoSelection
  }, {
    type: 'space', label: '间隔', icon: 'action.space', callback: activateSpaceSelection
  }, {
    type: 'connection', label: '连线', icon: 'action.connection', callback: activateConnect
  }]
})
