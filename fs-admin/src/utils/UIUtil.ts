const UIUtil = {
  uuid (prefix = '') {
    return prefix + new Date().getTime() + ('' + Math.random()).slice(-6)
  },
  /**
   * 通过对象数组进行过滤，回显需自行处理
   */
  filterSuggestions (suggestions: any, query: string, valueField = 'value') {
    return query ? suggestions.filter((item: any) => {
      return item[valueField].toLowerCase().indexOf(query.toLowerCase()) !== -1
    }) : suggestions
  },
  /**
   * 通过对象数组进行过滤，回显为过滤字段
   */
  fetchSuggestions (suggestions: any, query: string, valueField = 'value') {
    suggestions = suggestions.map((item: any) => {
      return { value: item[valueField], data: item }
    })
    return query ? suggestions.filter((item: any) => {
      return item.value.toLowerCase().indexOf(query.toLowerCase()) !== -1
    }) : suggestions
  },
  /**
   * 通过字符串数组进行过滤
   */
  arraySuggestions (suggestions: any, query: string) {
    suggestions = suggestions.map((item: any) => {
      return { value: item }
    })
    return query ? suggestions.filter((item: any) => {
      return item.value.toLowerCase().indexOf(query.toLowerCase()) !== -1
    }) : suggestions
  },
  prettyFileSize (size: any) {
    if (Number.isNaN(size)) return ''
    const units = [{
      measure: 1, unit: 'B'
    }, {
      measure: 1024, unit: 'KB'
    }, {
      measure: 1024 * 1024, unit: 'MB'
    }, {
      measure: 1024 * 1024 * 1024, unit: 'GB'
    }, {
      measure: 1024 * 1024 * 1024 * 1024, unit: 'TB'
    }, {
      measure: 1024 * 1024 * 1024 * 1024 * 1024, unit: 'PB'
    }]
    for (let index = units.length - 1; index >= 0; index--) {
      const item = units[index]
      if (size >= item.measure) return Math.round(size / item.measure) + item.unit
    }
    return size + ''
  },
  highlight (highlight: any, glue = '') {
    const result: any = {}
    for (const key in highlight) {
      result[key] = highlight[key].join(glue)
    }
    return result
  }
}
export default UIUtil
