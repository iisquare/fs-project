import DesignUtil from '@/utils/DesignUtil'

const config = {
}

const DefaultOptions = () => {
  return {}
}

const CanvasOptions = () => {
  return { grid: true, concurrent: 1, concurrency: 'SkipExecution', failure: 'FinishCurrentRunning' }
}

const EdgeOptions = () => {
  return {}
}

const APIConfigOptions = () => {
  return { url: '', method: 'GET', checkField: 'code', checkValue: '0', dataField: 'data' }
}

const DateGenerateConfigOptions = () => {
  return { arg: '', datetime: '', pattern: 'yyyy-MM-dd HH:mm:ss', timezone: 'GMT+8', locale: 'zh_CN' }
}

const CommandTaskOptions = () => {
  return { charset: '', command: '' }
}

const ConditionGatewayOptions = () => {
  return { condition: '' }
}

export default Object.assign(config, {
  canvas: {
    options: CanvasOptions, property: () => import('./CanvasProperty.vue')
  },
  edge: { options: EdgeOptions, property: () => import('./EdgeProperty.vue') },
  widgets: DesignUtil.widgets([{
    name: '配置参数',
    children: [Object.assign({
      type: 'APIConfig', label: 'API', title: '远端接口参数', icon: 'Link'
    }, {
      shape: 'flow-node', options: APIConfigOptions, property: () => import('./APIConfigProperty.vue')
    }), Object.assign({
      type: 'DateGenerateConfig', label: '日期', title: '生成日期参数', icon: 'Calendar'
    }, {
      shape: 'flow-node', options: DateGenerateConfigOptions, property: () => import('./DateGenerateConfigProperty.vue')
    })]
  }, {
    name: '基础任务',
    children: [Object.assign({
      type: 'CommandTask', label: '命令', title: '命令行', icon: 'flow.script'
    }, {
      shape: 'flow-node', options: CommandTaskOptions, property: () => import('./CommandTaskProperty.vue')
    })]
  }, {
    name: '数据加工',
    children: [Object.assign({
      type: 'GroupLayout', label: '分组', title: 'Group Layout', icon: 'flow.group'
    }, {
      shape: 'flow-group', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }), Object.assign({
      type: 'SubprocessLayout', label: '子流程', title: 'Subprocess Layout', icon: 'flow.subprocess'
    }, {
      shape: 'flow-subprocess', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }), Object.assign({
      type: 'ConditionGateway', label: '网关', title: '分支判断', icon: 'flow.gateway'
    }, {
      shape: 'flow-gateway', options: ConditionGatewayOptions, property: () => import('./ConditionGatewayProperty.vue')
    })]
  }]),
  toolbars: [{
    type: 'hand', label: '拖动', icon: 'action.hand', selectable: true, selected: true, callback (toolbar: any, flow: any, event: any) { flow.panning() }
  }, {
    type: 'lasso', label: '框选', icon: 'action.lasso', selectable: true, callback (toolbar: any, flow: any, event: any) { flow.selecting() }
  }, {
    type: 'fit', label: '适合', icon: 'action.fit', callback (toolbar: any, flow: any, event: any) { flow.fitting() }
  }, {
    type: 'divider'
  }, {
    type: 'clean', label: '清空', icon: 'action.clean', callback (toolbar: any, flow: any, event: any) {
      flow.fromJSON()
      flow.options.onBlankClick()
    }
  }],
  stages: [
    { label: '首次失败', value: 'FirstFailure' },
    { label: '执行结束', value: 'FlowFinished' }
  ],
  concurrences: [
    { label: '跳过执行，忽略本次调度', value: 'SkipExecution' },
    { label: '并行执行，相互独立调度', value: 'RunConcurrently' }
  ],
  failures: [
    { label: '完成当前正在运行的作业', value: 'FinishCurrentRunning' },
    { label: '立即终止正在运行的作业', value: 'CancelAll' },
    { label: '继续调度可以执行的作业', value: 'FinishAllPossible' }
  ],
  status: [], // 由后台服务补齐
})
