const DataUtil = {
    trim (str: any, charlist = ' ') {
      if (str == null) return ''
      if (Object.prototype.toString.call(str) !== '[object String]') return ''
      str = str.replace(new RegExp('^[' + charlist + ']+'), '')
      str = str.replace(new RegExp('[' + charlist + ']+$'), '')
      return str
    },
    empty (data: any) {
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
      const result: any = []
      arr.map((item: any) => {
        if (items.includes(item)) return
        result.push(item)
      })
      return result
    },
  }
  export default DataUtil
  