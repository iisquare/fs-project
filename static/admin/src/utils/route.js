import DataUtil from './data'
import CodeUtil from './code'

const RouteUtil = {
  filterKey: 'filter',
  expandedRowKeys (tree, deep = -1, key = 'id', children = 'children') {
    const result = [];
    (function walk (data, level) {
      if (!data) return
      data.forEach(element => {
        result.push(element[key])
        if (deep !== -1 && level < deep) {
          walk(element[children], level + 1)
        }
      })
    })(tree, 1)
    return result
  },
  filterOption (input, option) {
    return (
      option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    )
  },
  selection (rowSelection = {}) {
    return Object.assign({
      selectedRowKeys: [], // 推荐仅采用selectedRowKeys进行整个交互过程的操作
      selectedRows: [], // 仅通过selectedRowKeys设置选中项，selectedRows中的内容并不会发生变化
      confirm (onOk, onCancel) {
        return {
          title: '操作提示',
          content: '确认删除所选记录吗？',
          onOk () {
            onOk && onOk()
          },
          onCancel () {
            onCancel && onCancel()
          }
        }
      },
      clear () {
        this.selectedRowKeys = []
        this.selectedRows = []
      },
      onChange (selectedRowKeys, selectedRows) {
        this.selectedRowKeys = selectedRowKeys
        this.selectedRows = selectedRows
      },
      revertSelected (rows, rowKey) {
        const result = []
        for (const index in rows) {
          const record = rows[index]
          if (this.selectedRowKeys.indexOf(record[rowKey]) === -1) {
            result.push(record[rowKey])
          }
        }
        this.selectedRowKeys = result
      }
    }, rowSelection)
  },
  paginationPageKey: 'current',
  paginationChange (oldValue, newValue) {
    if (oldValue.pageSize !== newValue.pageSize) {
      newValue[this.paginationPageKey] = 1
    }
    return newValue
  },
  paginationData (pagination, fromChange) {
    return {
      page: fromChange ? pagination[[this.paginationPageKey]] : 1,
      pageSize: pagination.pageSize
    }
  },
  pagination (filters) {
    return {
      showQuickJumper: true,
      showSizeChanger: true,
      [this.paginationPageKey]: filters.page,
      pageSize: filters.pageSize,
      total: 0,
      showTotal: (total, range) => `共 ${total} 条`,
      pageSizeOptions: ['5', '10', '15', '20', '25', '30', '35', '45', '50', '60', '100']
    }
  },
  result (result) {
    if (result.code !== 0) return {}
    return {
      [this.paginationPageKey]: result.data.page,
      pageSize: result.data.pageSize,
      total: result.data.total
    }
  },
  encode (filter) {
    if (DataUtil.empty(filter)) return ''
    try {
      filter = JSON.stringify(filter)
      filter = CodeUtil.encodeBase64(filter)
    } catch (e) {
      filter = ''
    }
    return filter
  },
  decode (filter) {
    if (DataUtil.empty(filter)) return {}
    try {
      filter = CodeUtil.decodeBase64(filter)
      filter = JSON.parse(filter)
    } catch (e) {
      filter = {}
    }
    return filter
  },
  hasFilter (obj) {
    return !DataUtil.empty(obj.$route.query[this.filterKey])
  },
  query2filter (obj, filters) {
    return Object.assign({}, filters, this.decode(obj.$route.query[this.filterKey]))
  },
  filter2query (obj, filter) {
    return obj.$router.push({
      path: obj.$route.fullPath,
      query: {
        [this.filterKey]: this.encode(filter)
      }
    }).catch(err => err)
  },
  filter (param) { // 返回编码后的请求参数
    return { [this.filterKey]: this.encode(param) }
  },
  forward (obj, event, location) {
    if (!location) { // 刷新当前页面
      obj.$router.replace({
        path: '/redirect',
        query: {
          path: obj.$route.path,
          query: this.encode(obj.$route.query)
        }
      }).catch(err => err)
    }
    if (!location.path) location.path = obj.$route.path
    if (!event || event.ctrlKey) { // 忽略事件或按住Ctr键
      const url = obj.$router.resolve(location)
      window.open(url.href) // 新页面打开
    } else { // 当前页面打开
      obj.$router.push(location).catch(err => err)
    }
  }
}

export default RouteUtil
