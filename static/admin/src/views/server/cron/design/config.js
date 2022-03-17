const config = {
  uuid () { return new Date().getTime() + ('' + Math.random()).slice(-6) }
}

// const DefaultOptions = () => {
//   return {}
// }

const CanvasOptions = () => {
  return { top: 0, width: 500, height: 500 }
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

export default Object.assign(config, {
  canvas: {
    options: CanvasOptions, property: () => import('./CanvasProperty')
  },
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
    children: [{
      type: 'APIConfig', label: 'API', title: '远端接口参数', icon: 'dagConfig', options: APIConfigOptions, property: () => import('./APIConfigProperty')
    }, {
      type: 'DateGenerateConfig', label: 'DateGenerate', title: '生成日期参数', icon: 'dagConfig', options: DateGenerateConfigOptions, property: () => import('./DateGenerateConfigProperty')
    }]
  }, {
    name: '基础任务',
    children: [{
      type: 'CommandTask', label: 'Command', title: '命令行', icon: 'dagConfig', options: CommandTaskOptions, property: () => import('./CommandTaskProperty')
    }]
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
