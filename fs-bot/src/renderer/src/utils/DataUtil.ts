const DataUtil = {
  isString (value: any) {
    return typeof value === 'string'
  },
  isNumber (value: any) {
    return typeof value === 'number' && !isNaN(value)
  },
  isArray (value: any) {
    return Array.isArray(value)
  },
  isObject (value: any) {
    return value !== null && typeof value === 'object' && !Array.isArray(value)
  },
  isBoolean (value: any) {
    return typeof value === 'boolean'
  },
  isFunction (value: any) {
    return typeof value === 'function'
  },
  isUndefined (value: any) {
    return typeof value === 'undefined'
  },
  isNull (value: any) {
    return value === null
  },
  isSymbol (value: any) {
    return typeof value === 'symbol'
  },
  trim (str: any, charlist = ' ') {
    if (this.isUndefined(str) || this.isNull(str)) return ''
    if (!this.isString(str)) return ''
    str = str.replace(new RegExp('^[' + charlist + ']+'), '')
    str = str.replace(new RegExp('[' + charlist + ']+$'), '')
    return str
  },
  empty (data: any) {
    if (this.isUndefined(data) || this.isNull(data)) return true
    if (this.isArray(data) && data.length < 1) return true
    if (this.isObject(data) && Object.getOwnPropertyNames(data).length < 1) return true
    if (this.isString(data) && this.trim(data).length < 1) return true
    return false
  },
  /**
   * 过滤数据字段
   * @param {Object|Array} obj 原数据
   * @param {Object} remain { '原数据字段': '目标数据字段' }
   * @param {Array} remove [ '目标数据字段' ]
   * @returns 目标数据
   */
  filtration (obj: any, remain: any, remove: any) {
    if (!obj) return obj
    const result: any = Array.isArray(obj) ? [] : {}
    for (const field in obj) {
      const item = obj[field]
      const node: any = {}
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
  values (obj: any, field: any) {
    const result: any = Array.isArray(obj) ? [] : {}
    for (const index in obj) {
      if (field instanceof String) {
        result[index] = this.value(obj[index], field as string)
      } else {
        result[index] = obj[index][field]
      }
    }
    return result
  },
  value (obj: any, field: string) {
    if (!field) return undefined
    const fields = field.split('.')
    let value = obj
    for (const key of fields) {
      value = value[key]
    }
    return value
  },
  array2map (rows: any, key = 'id') {
    const result: any = {}
    for (const index in rows) {
      const item = rows[index]
      result[item[key]] = item
    }
    return result
  },
  removeArrayItem (arr: any, items: any) {
    for (let index = arr.length - 1; index >= 0; index--) {
      if (items.includes(arr[index])) {
        arr.splice(index, 1)
      }
    }
    return arr
  },
}
export default DataUtil
