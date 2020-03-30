const DataUtil = {
  trim (str, charlist = ' ') {
    if (str == null) return ''
    if (Object.prototype.toString.call(str) !== '[object String]') return ''
    str = str.replace(new RegExp('^[' + charlist + ']+'), '')
    str = str.replace(new RegExp('[' + charlist + ']+$'), '')
    return str
  },
  empty (data) {
    if (data == null) return true
    if (data instanceof Array && data.length < 1) return true
    if (data instanceof Object && Object.getOwnPropertyNames(data).length < 1) return true
    if (Object.prototype.toString.call(data) === '[object String]' && this.trim(data).length < 1) return true
    return false
  }
}
export default DataUtil
