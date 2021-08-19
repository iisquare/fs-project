import moment from 'moment'

const DateUtil = {
  format (time, fmt = 'yyyy-MM-dd HH:mm:ss') {
    if (!time) return ''
    const date = new Date(time)
    if (/(y+)/.test(fmt)) {
      fmt = fmt.replace(RegExp.$1, (date.getFullYear() + '').substr(4 - RegExp.$1.length))
    }
    const o = {
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
  dateMomentFormat () {
    return 'YYYY-MM-DD HH:mm:ss'
  },
  timeMomentFormat () {
    return 'HH:mm:ss'
  },
  timeMomentRange () {
    const format = this.timeMomentFormat()
    return [moment('00:00:00', format), moment('23:59:59', format)]
  }
}

Object.assign(DateUtil, { // 拓展UI组件，this指针可能会被修改
  dateRender (text, record, index) {
    return DateUtil.format(text)
  },
  showTime (indexRange) {
    return { format: DateUtil.timeMomentFormat(), defaultValue: DateUtil.timeMomentRange()[indexRange] }
  },
  dateFormat () {
    return DateUtil.dateMomentFormat()
  }
})

export default DateUtil
