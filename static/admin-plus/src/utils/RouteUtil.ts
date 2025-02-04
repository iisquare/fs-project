import DataUtil from './DataUtil'
import CodeUtil from './CodeUtil'

const RouteUtil = {
  filterKey: 'filter',
  paginationPageKey: 'currentPage',
  pagination2filter (pagination: any, keepPage: boolean) {
    return {
      page: keepPage ? pagination[this.paginationPageKey] : 1,
      pageSize: pagination.pageSize
    }
  },
  pagination (filters: any, pagination: any = {}) {
    return Object.assign({
      [this.paginationPageKey]: filters.page,
      pageSize: filters.pageSize,
      total: 0,
      background: true,
      layout: 'total, sizes, prev, pager, next, jumper',
      pageSizes: [5, 10, 15, 20, 25, 30, 35, 45, 50, 60, 100],
    }, pagination)
  },
  result2pagination (pagination: any, result: any) {
    if (result.code !== 0) return {}
    return Object.assign(pagination, {
      [this.paginationPageKey]: result.data.page,
      pageSize: result.data.pageSize,
      total: result.data.total
    })
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
  query2filter (route: any, filters: any = {}, pagination = true) {
    return Object.assign(pagination ? {
      [this.paginationPageKey]: 1,
      pageSize: 15,
    } : {}, filters, this.decode(route.query[this.filterKey]))
  },
  filter2query (route: any, router: any, filter: any) {
    return router.replace({
      path: route.fullPath,
      query: {
        [this.filterKey]: this.encode(filter)
      }
    }).catch((err: any) => err)
  },
  filter (param: any) { // 返回编码后的请求参数
    return { [this.filterKey]: this.encode(param) }
  },
  forward (route: any, router: any, event: any, location: any) {
    if (!location) { // 刷新当前页面
      return router.replace({
        path: '/redirect',
        query: {
          path: route.path,
          query: this.encode(route.query)
        }
      }).catch((err: any) => err)
    }
    if (!location.path) location.path = route.path
    if (!event || event.ctrlKey) { // 忽略事件或按住Ctr键
      const url = router.resolve(location)
      window.open(url.href) // 新页面打开
    } else { // 当前页面打开
      router.push(location).catch((err: any) => err)
    }
  }
}

export default RouteUtil
