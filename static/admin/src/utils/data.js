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
  },
  /**
   * 过滤数据字段
   * @param {Object|Array} obj 原数据
   * @param {Object} remain { '原数据字段': '目标数据字段' }
   * @param {Array} remove [ '目标数据字段' ]
   * @returns 目标数据
   */
  filtration (obj, remain, remove) {
    if (!obj) return obj
    const result = Array.isArray(obj) ? [] : {}
    for (const field in obj) {
      const item = obj[field]
      const node = {}
      const keys = Object.keys(item)
      if (remain) {
        for (const key in remain) {
          const value = remain[key]
          if (keys.indexOf(key) === -1) continue
          node[value] = item[key]
        }
      } else {
        Object.assign(node, item)
      }
      if (remove) {
        for (const key in remove) {
          const value = remove[key]
          if (keys.indexOf(value) === -1) continue
          delete node[value]
        }
      }
      result[field] = node
    }
    return result
  },
  values (obj, field) {
    const result = Array.isArray(obj) ? [] : {}
    for (const index in obj) {
      result[index] = obj[index][field]
    }
    return result
  }
}
export default DataUtil
