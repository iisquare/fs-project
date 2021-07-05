import { assign } from 'min-dash'

const config = {
  uuid () { return new Date().getTime() + ('' + Math.random()).slice(-6) }
}

const emptyOptions = () => {
  return {}
}

const activateHand = (toolbar, bpmn, event) => {
  bpmn.palette._handTool.activateHand(event)
}

const activateLassoSelection = (toolbar, bpmn, event) => {
  bpmn.palette._lassoTool.activateSelection(event)
}

const activateSpaceSelection = (toolbar, bpmn, event) => {
  bpmn.palette._spaceTool.activateSelection(event)
}

const activateConnect = (toolbar, bpmn, event) => {
  bpmn.palette._globalConnect.toggle(event)
}

const createShape = (widget, bpmn, event, options) => {
  const shape = bpmn.palette._elementFactory.createShape(assign({ type: widget.type }, options))
  if (options) {
    shape.businessObject.di.isExpanded = options.isExpanded
  }
  bpmn.palette._create.start(event, shape)
}

const createParticipant = (widget, bpmn, event, options) => {
  bpmn.palette._create.start(event, bpmn.palette._elementFactory.createParticipantShape())
}

export default Object.assign(config, {
  duration (durationInMillis) {
    const time = {
      second: 1000,
      minute: 60 * 1000,
      hour: 60 * 60 * 1000,
      day: 24 * 60 * 60 * 1000
    }
    const day = Math.floor(durationInMillis / time.day)
    const hour = Math.floor(durationInMillis % time.day / time.hour)
    const minute = Math.floor(durationInMillis % time.hour / time.minute)
    const second = Math.floor(durationInMillis % time.minute / time.second)
    return `${day}天${hour}时${minute}分${second}秒`
  },
  audit (comments, taskId) {
    comments = comments[taskId] || []
    let local = false
    const messages = []
    comments.forEach(comment => {
      local = local || comment.audit.local
      if (comment.audit.message) messages.push(comment.audit.message)
    })
    return { audit: { local, message: messages.join(',') } }
  },
  canvas: {
    options: emptyOptions, property: () => import('./CanvasProperty')
  },
  elements: {
    'bpmn:StartEvent': { options: emptyOptions, property: () => import('./NodeProperty') },
    'bpmn:EndEvent': { options: emptyOptions, property: () => import('./NodeProperty') },
    'bpmn:UserTask': { options: emptyOptions, property: () => import('./UserTaskProperty') },
    'bpmn:ExclusiveGateway': { options: emptyOptions, property: () => import('./NodeProperty') },
    'bpmn:ParallelGateway': { options: emptyOptions, property: () => import('./NodeProperty') },
    'bpmn:InclusiveGateway': { options: emptyOptions, property: () => import('./NodeProperty') },
    'bpmn:Participant': { options: emptyOptions, property: () => import('./NodeProperty') },
    'bpmn:Group': { options: emptyOptions, property: () => import('./NodeProperty') },
    'bpmn:SequenceFlow': { options: emptyOptions, property: () => import('./SequenceFlowProperty') }
  },
  widgets: [{
    name: '事件',
    children: [{
      type: 'bpmn:StartEvent', label: '开始', group: 'event', className: 'bpmn-icon-start-event-none', icon: 'workflowStartEvent', callback: createShape
    }, {
      type: 'bpmn:EndEvent', label: '结束', group: 'event', className: 'bpmn-icon-end-event-none', icon: 'workflowEndEvent', callback: createShape
    }]
  }, {
    name: '任务',
    children: [{
      type: 'bpmn:UserTask', label: '用户任务', group: 'flowable', className: 'bpmn-icon-start-event-none', icon: 'workflowUserTask', callback: createShape
    }]
  }, {
    name: '网关',
    children: [{ // 仅执行第一个满足条件的分支
      type: 'bpmn:ExclusiveGateway', label: '排他网关', group: 'gateway', className: 'bpmn-icon-gateway-none', icon: 'workflowExclusiveGateway', callback: createShape
    }, { // 忽略分支条件，全部执行
      type: 'bpmn:ParallelGateway', label: '并行网关', group: 'gateway', className: 'bpmn-icon-gateway-none', icon: 'workflowParallelGateway', callback: createShape
    }, { // 排他网关和并行网关的结合体，执行满足条件的多个分支
      type: 'bpmn:InclusiveGateway', label: '包容网关', group: 'gateway', className: 'bpmn-icon-gateway-none', icon: 'workflowInclusiveGateway', callback: createShape
    }]
  }, {
    name: '加工',
    children: [{
      type: 'fs:Pool', label: '泳道', icon: 'workflowPool', group: 'artifact', className: 'bpmn-icon-participant', callback: createParticipant
    }, {
      type: 'bpmn:Group', label: '分组', icon: 'workflowGroup', group: 'artifact', className: 'bpmn-icon-group', callback: createShape
    }]
  }],
  toolbars: [{
    type: 'hand', label: '拖动', icon: 'actionHand', callback: activateHand
  }, {
    type: 'lasso', label: '框选', icon: 'actionLasso', callback: activateLassoSelection
  }, {
    type: 'space', label: '间隔', icon: 'actionSpace', callback: activateSpaceSelection
  }, {
    type: 'connection', label: '连线', icon: 'actionConnection', callback: activateConnect
  }]
})
