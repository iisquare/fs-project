
const config = {
}

const DefaultOptions = () => {}

const CoordinateOptions = () => {
  return { type: 'Line' }
}

export default Object.assign(config, {
  widgetTransientMap: null,
  widgetByType (type) {
    if (this.widgetTransientMap === null) {
      const map = {}
      this.widgets.forEach(widget => {
        map[widget.type] = widget
      })
      this.widgetTransientMap = map
    }
    return this.widgetTransientMap[type]
  },
  widgetDefaults (type) {
    return this.widgetByType(type).options()
  },
  widgets: [
    { type: 'Table', label: '表格', icon: 'biTable', options: DefaultOptions, chart: () => import('./ChartTable') },
    { type: 'Coordinate', label: '坐标', icon: 'biChartCoordinate', options: CoordinateOptions, chart: () => import('./ChartCoordinate') },
    { type: 'Pie', label: '饼图', icon: 'biChartPie', options: DefaultOptions, chart: () => import('./ChartPie') },
    { type: 'Radar', label: '雷达图', icon: 'biChartRadar', options: DefaultOptions, chart: () => import('./ChartRadar') },
    { type: 'Funnel', label: '漏斗图', icon: 'biChartFunnel', options: DefaultOptions, chart: () => import('./ChartFunnel') },
    { type: 'Gauge', label: '仪表盘', icon: 'biChartGauge', options: DefaultOptions, chart: () => import('./ChartGauge') }
  ],
  coordinateTypes: [
    { value: 'Line', label: '折线图（Line）' },
    { value: 'Bar', label: '柱状图（Bar）' },
    { value: 'Area', label: '区域图（Area）' }
  ]
})
