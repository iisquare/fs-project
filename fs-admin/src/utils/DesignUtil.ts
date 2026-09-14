import DataUtil from "./DataUtil"

const DesignUtil = {
  widgets: (widgets: any) => {
    for (let i in widgets) {
      const group = widgets[i]
      group.id = group.id || `group-${i}`
      for (let j in group.children) {
        const item = group.children[j]
        item.id = item.id || `${group.id}-widget-${j}`
      }
    }
    return widgets
  },
  widgetMap: (widgets: any, key = 'id') => {
    const result: any = {}
    for (let i in widgets) {
      const group = widgets[i]
      for (let j in group.children) {
        const item = group.children[j]
        if (!item[key]) continue
        result[item[key]] = item
      }
    }
    return result
  },
  widgetByType: (type: any, config: any, widgetTypeField = 'widgetTransientTypes') => {
    if (!config[widgetTypeField]) {
      config[widgetTypeField] = DesignUtil.widgetMap(config.widgets, 'type')
    }
    return config[widgetTypeField][type]
  },
  widgetFormProperty: (activeItem: any, config: any, widgetTypeField = 'widgetTransientTypes') => {
    if (DataUtil.empty(activeItem)) {
      return config.canvas.property
    }
    return DesignUtil.widgetByType(activeItem.type, config, widgetTypeField).property
  },
  /**
   * 表单组件列表，兼容两种数据结构
   * - 后端表单定义：{ id, name, widgets: [], options: {} }
   * - 设计器数据模型：{ id, name, content: { widgets: [], ...画布配置 } }
   */
  frameWidgets: (frame: any) => {
    if (!frame) return []
    if (Array.isArray(frame.widgets)) return frame.widgets
    if (frame.content && Array.isArray(frame.content.widgets)) return frame.content.widgets
    return []
  },
  /**
   * 表单画布配置，兼容后端表单定义与设计器数据模型，参见 frameWidgets
   */
  frameOptions: (frame: any) => {
    if (!frame) return {}
    if (frame.options) return frame.options
    if (frame.content) {
      const { widgets, ...options } = frame.content
      return options
    }
    return {}
  },
  widgetFlowProperty: (activeItem: any, config: any, widgetTypeField = 'widgetTransientTypes') => {
    if (!activeItem || !activeItem.shape) {
      return config.canvas.property
    }
    if (['flow-edge', 'edge'].indexOf(activeItem.shape) !== -1) {
      return config.edge.property
    }
    // 组件类型缺失时按图形类型兜底，避免历史数据导致属性面板无法渲染
    const widget = DesignUtil.widgetByType(activeItem.data?.type, config, widgetTypeField)
      || DesignUtil.widgetByShape(activeItem.shape, config)
    return (widget || config.canvas).property
  },
  widgetByShape: (shape: any, config: any, widgetShapeField = 'widgetShapes') => {
    if (!config[widgetShapeField]) {
      config[widgetShapeField] = DesignUtil.widgetMap(config.widgets, 'shape')
    }
    return config[widgetShapeField][shape]
  },
  uuid: () => { return new Date().getTime() + ('' + Math.random()).slice(-6) },
  fixedFlowChangeData: (callback: Function) => {
    return (event: any) => { // Antv X6无法深度监听data数组，通过remove后set进行解决
      if (event.current) callback(event) // 忽略remove后data为undefined的情况
    }
  },
}

export default DesignUtil
