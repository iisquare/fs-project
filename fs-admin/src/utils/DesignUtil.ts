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
  widgetFlowProperty: (activeItem: any, config: any, widgetTypeField = 'widgetTransientTypes') => {
    if (!activeItem || !activeItem.shape) {
      return config.canvas.property
    }
    if (['flow-edge', 'edge'].indexOf(activeItem.shape) !== -1) {
      return config.edge.property
    }
    return DesignUtil.widgetByType(activeItem.data.type, config, widgetTypeField).property
  },
  uuid: () => { return new Date().getTime() + ('' + Math.random()).slice(-6) },
  fixedFlowChangeData: (callback: Function) => {
    return (event: any) => { // Antv X6无法深度监听data数组，通过remove后set进行解决
      if (event.current) callback(event) // 忽略remove后data为undefined的情况
    }
  },
}

export default DesignUtil
