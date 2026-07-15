import DesignUtil from '@/utils/DesignUtil'

const config = {
}

const CanvasOptions = () => {
  return {}
}

const EdgeOptions = () => {
  return {}
}

const NodeOptions = () => {
  return { name: '', description: '', documentation: '', authority: '' }
}

const UserTaskOptions = () => {
  return {
    name: '', description: '', documentation: '',
    candidateGroups: '', authority: '',
    loopCharacteristics: false, isSequential: false,
    loopCardinality: '', collection: '', elementVariable: '', completionCondition: '',
  }
}

const EdgeDefaults = () => {
  return { name: '', description: '', condition: '' }
}

export default Object.assign(config, {
  canvas: {
    options: CanvasOptions, property: () => import('./CanvasProperty.vue')
  },
  edge: { options: EdgeOptions, property: () => import('./EdgeProperty.vue') },
  widgets: DesignUtil.widgets([{
    name: '事件',
    children: [{
      type: 'bpmn:StartEvent', label: '开始', title: '开始事件',
      shape: 'bpmn-start-event', icon: 'flow.startEvent',
      options: NodeOptions, property: () => import('./NodeProperty.vue')
    }, {
      type: 'bpmn:EndEvent', label: '结束', title: '结束事件',
      shape: 'bpmn-end-event', icon: 'flow.endEvent',
      options: NodeOptions, property: () => import('./NodeProperty.vue')
    }]
  }, {
    name: '任务',
    children: [{
      type: 'bpmn:UserTask', label: '用户任务', title: '用户任务节点',
      shape: 'bpmn-user-task', icon: 'flow.userTask',
      options: UserTaskOptions, property: () => import('./UserTaskProperty.vue')
    }]
  }, {
    name: '网关',
    children: [{
      type: 'bpmn:ExclusiveGateway', label: '排他网关', title: '仅执行第一个满足条件的分支',
      shape: 'bpmn-exclusive-gateway', icon: 'flow.exclusiveGateway',
      options: NodeOptions, property: () => import('./NodeProperty.vue')
    }, {
      type: 'bpmn:ParallelGateway', label: '并行网关', title: '忽略分支条件，全部执行',
      shape: 'bpmn-parallel-gateway', icon: 'flow.parallelGateway',
      options: NodeOptions, property: () => import('./NodeProperty.vue')
    }, {
      type: 'bpmn:InclusiveGateway', label: '包容网关', title: '执行满足条件的多个分支',
      shape: 'bpmn-inclusive-gateway', icon: 'flow.inclusiveGateway',
      options: NodeOptions, property: () => import('./NodeProperty.vue')
    }]
  }]),
  toolbars: [{
    type: 'hand', label: '拖动', icon: 'action.hand', selectable: true, selected: true,
    callback (toolbar: any, instance: any) { instance.flow.panning() }
  }, {
    type: 'lasso', label: '框选', icon: 'action.lasso', selectable: true,
    callback (toolbar: any, instance: any) { instance.flow.selecting() }
  }, {
    type: 'fit', label: '适合', icon: 'action.fit',
    callback (toolbar: any, instance: any) { instance.flow.fitting() }
  }, {
    type: 'divider'
  }, {
    type: 'clean', label: '清空', icon: 'action.clean',
    callback (toolbar: any, instance: any) {
      instance.flow.fromJSON()
      instance.flow.options.onBlankClick()
    }
  }],
  status: [],
})
