const DateUtil = {
  format (time: any, fmt = 'yyyy-MM-dd HH:mm:ss') {
    if (!time) return ''
    const date = new Date(time)
    if (/(y+)/.test(fmt)) {
      fmt = fmt.replace(RegExp.$1, (date.getFullYear() + '').substr(4 - RegExp.$1.length))
    }
    const o: any = {
      'M+': date.getMonth() + 1,
      'd+': date.getDate(),
      'H+': date.getHours(),
      'm+': date.getMinutes(),
      's+': date.getSeconds()
    }
    for (const k in o) {
      if (new RegExp(`(${k})`).test(fmt)) {
        const str = o[k] + ''
        fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? str : ('00' + str).substr(str.length))
      }
    }
    return fmt
  },
  moment () {
    return 'YYYY-MM-DD HH:mm:ss'
  },
  hello () {
    const time = new Date()
    const hour = time.getHours()
    return hour < 9 ? '早上好' : hour <= 11 ? '上午好' : hour <= 13 ? '中午好' : hour < 20 ? '下午好' : '晚上好'
  },
  render (row: any, column: any, cellValue: any, index: number) {
    return DateUtil.format(cellValue)
  },
  second (row: any, column: any, cellValue: any, index: number) {
    return DateUtil.format(cellValue ? cellValue * 1000 : null)
  },
}

export default DateUtil
