import DataUtil from './DataUtil'
import CodeUtil from './CodeUtil'

const RouteUtil = {
  filterKey: 'filter',
  expandedRowKeys (tree: any, deep = -1, key = 'id', children = 'children') {
    const result: any = [];
    (function walk (data, level) {
      if (!data) return
      data.forEach((element: any) => {
        result.push(element[key])
        if (deep !== -1 && level < deep) {
          walk(element[children], level + 1)
        }
      })
    })(tree, 1)
    return result
  },
  filterOption (input: any, option: any) {
    return (
      option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    )
  },
  selection (rowSelection = {}) {
    return Object.assign({
      selectedRowKeys: [], // 推荐仅采用selectedRowKeys进行整个交互过程的操作
      selectedRows: [], // 仅通过selectedRowKeys设置选中项，selectedRows中的内容并不会发生变化
      confirm (onOk: any, onCancel: any) {
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
      onChange (selectedRowKeys: any, selectedRows: any) {
        this.selectedRowKeys = selectedRowKeys
        this.selectedRows = selectedRows
      },
      revertSelected (rows: any, rowKey: any) {
        const result: any = []
        for (const index in rows) {
          const record: any = rows[index]
          if (this.selectedRowKeys.indexOf(record[rowKey] as never) === -1) {
            result.push(record[rowKey])
          }
        }
        this.selectedRowKeys = result
      }
    }, rowSelection)
  },
  paginationPageKey: 'current',
  paginationChange (oldValue: any, newValue: any) {
    if (oldValue.pageSize !== newValue.pageSize) {
      newValue[this.paginationPageKey] = 1
    }
    return newValue
  },
  paginationData (pagination: any, fromChange: any) {
    return {
      page: fromChange ? pagination[this.paginationPageKey] : 1,
      pageSize: pagination.pageSize
    }
  },
  pagination (filters: any) {
    return {
      showQuickJumper: true,
      showSizeChanger: true,
      [this.paginationPageKey]: filters.page,
      pageSize: filters.pageSize,
      total: 0,
      showTotal: (total: any, range: any) => `共 ${total} 条`,
      pageSizeOptions: ['5', '10', '15', '20', '25', '30', '35', '45', '50', '60', '100']
    }
  },
  result (result: any) {
    if (result.code !== 0) return {}
    return {
      [this.paginationPageKey]: result.data.page,
      pageSize: result.data.pageSize,
      total: result.data.total
    }
  },
  encode (filter: any) {
    if (DataUtil.empty(filter)) return ''
    try {
      filter = JSON.stringify(filter)
      filter = CodeUtil.encodeBase64(filter)
    } catch (e) {
      filter = ''
    }
    return filter
  },
  decode (filter: any) {
    if (DataUtil.empty(filter)) return {}
    try {
      filter = CodeUtil.decodeBase64(filter)
      filter = JSON.parse(filter)
    } catch (e) {
      filter = {}
    }
    return filter
  },
  hasFilter (obj: any) {
    return !DataUtil.empty(obj.$route.query[this.filterKey])
  },
  query2filter (obj: any, filters: any) {
    return Object.assign({}, filters, this.decode(obj.$route.query[this.filterKey]))
  },
  filter2query (obj: any, filter: any) {
    return obj.$router.push({
      path: obj.$route.fullPath,
      query: {
        [this.filterKey]: this.encode(filter)
      }
    }).catch((err: any) => err)
  },
  filter (param: any) { // 返回编码后的请求参数
    return { [this.filterKey]: this.encode(param) }
  },
  forward (obj: any, event: any, location: any) {
    if (!location) { // 刷新当前页面
      obj.$router.replace({
        path: '/redirect',
        query: {
          path: obj.$route.path,
          query: this.encode(obj.$route.query)
        }
      }).catch((err: any) => err)
    }
    if (!location.path) location.path = obj.$route.path
    if (!event || event.ctrlKey) { // 忽略事件或按住Ctr键
      const url = obj.$router.resolve(location)
      window.open(url.href) // 新页面打开
    } else { // 当前页面打开
      obj.$router.push(location).catch((err: any) => err)
    }
  }
}

export default RouteUtil
