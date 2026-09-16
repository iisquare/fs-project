const DefaultOptions = () => ({})

const CoordinateOptions = () => {
  return { type: 'Line' }
}

const config: any = {
  widgetTransientMap: null,
  widgetByType (type: string) {
    if (this.widgetTransientMap === null) {
      const map: any = {}
      this.widgets.forEach((widget: any) => {
        map[widget.type] = widget
      })
      this.widgetTransientMap = map
    }
    return this.widgetTransientMap[type]
  },
  widgetDefaults (type: string) {
    const widget = this.widgetByType(type)
    return widget ? widget.options() : {}
  },
  widgets: [
    { type: 'Table', label: '表格', icon: 'Grid', options: DefaultOptions, chart: () => import('./ChartTable.vue') },
    { type: 'Coordinate', label: '坐标', icon: 'TrendCharts', options: CoordinateOptions, chart: () => import('./ChartTable.vue') },
    { type: 'Pie', label: '饼图', icon: 'PieChart', options: DefaultOptions, chart: () => import('./ChartTable.vue') },
    { type: 'Radar', label: '雷达图', icon: 'Aim', options: DefaultOptions, chart: () => import('./ChartTable.vue') },
    { type: 'Funnel', label: '漏斗图', icon: 'Filter', options: DefaultOptions, chart: () => import('./ChartTable.vue') },
    { type: 'Gauge', label: '仪表盘', icon: 'Odometer', options: DefaultOptions, chart: () => import('./ChartTable.vue') },
  ],
  coordinateTypes: [
    { value: 'Line', label: '折线图（Line）' },
    { value: 'Bar', label: '柱状图（Bar）' },
    { value: 'Area', label: '区域图（Area）' },
  ],
}

export default config
