const config = {
  uuid () { return new Date().getTime() + ('' + Math.random()).slice(-6) },
  mergeData (obj, data) {
    data = Object.assign({}, obj.data, data)
    return Object.assign({}, obj, { data })
  },
  mergeOptions (obj, options) {
    options = Object.assign({}, obj.data.options, options)
    return this.mergeData(obj, { options })
  }
}

const DefaultOptions = () => {
  return {}
}

const CanvasOptions = () => {
  return { grid: true, concurrency: 1, concurrent: 'SkipExecution', failure: 'FinishCurrentRunning' }
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
  return { command: '' }
}

const ConditionGatewayOptions = () => {
  return { condition: '' }
}

export default Object.assign(config, {
  canvas: {
    options: CanvasOptions, property: () => import('./CanvasProperty')
  },
  edge: { options: EdgeOptions, property: () => import('./EdgeProperty') },
  widgetTransientMap: null,
  widgetByType (type) {
    if (this.widgetTransientMap === null) {
      const map = {}
      this.widgets.forEach(widget => {
        widget.children.forEach(item => {
          map[item.type] = item
        })
      })
      this.widgetTransientMap = map
    }
    return this.widgetTransientMap[type]
  },
  widgetDefaults (type) {
    return this.widgetByType(type).options()
  },
  widgets: [{
    name: '配置参数',
    children: [Object.assign({
      type: 'APIConfig', label: 'API', title: '远端接口参数', icon: 'dagConfig'
    }, {
      shape: 'flow-node', options: APIConfigOptions, property: () => import('./APIConfigProperty')
    }), Object.assign({
      type: 'DateGenerateConfig', label: 'DateGenerate', title: '生成日期参数', icon: 'dagConfig'
    }, {
      shape: 'flow-node', options: DateGenerateConfigOptions, property: () => import('./DateGenerateConfigProperty')
    })]
  }, {
    name: '基础任务',
    children: [Object.assign({
      type: 'CommandTask', label: 'Command', title: '命令行', icon: 'dagConfig'
    }, {
      shape: 'flow-node', options: CommandTaskOptions, property: () => import('./CommandTaskProperty')
    })]
  }, {
    name: '数据加工',
    children: [Object.assign({
      type: 'GroupLayout', label: 'Group', title: 'Group Layout', icon: 'workflowGroup'
    }, {
      shape: 'flow-group', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      type: 'SubprocessLayout', label: 'Subprocess', title: 'Subprocess Layout', icon: 'workflowSubprocess'
    }, {
      shape: 'flow-subprocess', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      type: 'ConditionGateway', label: 'Switch', title: '分支判断', icon: 'dagSwitch'
    }, {
      shape: 'flow-switch', options: ConditionGatewayOptions, property: () => import('./ConditionGatewayProperty')
    })]
  }],
  toolbars: [{
    type: 'hand', label: '拖动', icon: 'actionHand', callback (toolbar, flow, event) { flow.panning() }
  }, {
    type: 'lasso', label: '框选', icon: 'actionLasso', callback (toolbar, flow, event) { flow.selecting() }
  }, {
    type: 'fit', label: '适合', icon: 'actionFit', callback (toolbar, flow, event) { flow.fitting() }
  }],
  stages: [
    { label: '首次失败', value: 'FirstFailure' },
    { label: '执行结束', value: 'FlowFinished' }
  ],
  concurrents: [
    { label: '跳过执行，忽略本次调度', value: 'SkipExecution' },
    { label: '并行执行，相互独立调度', value: 'RunConcurrently' }
  ],
  failures: [
    { label: '完成当前正在运行的作业', value: 'FinishCurrentRunning' },
    { label: '立即终止正在运行的作业', value: 'CancelAll' },
    { label: '继续调度可以执行的作业', value: 'FinishAllPossible' }
  ]
})
