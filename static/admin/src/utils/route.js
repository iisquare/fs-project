import DataUtil from './data'
import { Base64 } from 'js-base64'

const RouteUtil = {
  filterKey: 'filter',
  pagination (pagination, filters, search) {
    pagination.showQuickJumper = true
    pagination.showSizeChanger = true
    pagination.defaultCurrent = filters.page
    pagination.defaultPageSize = filters.pageSize
    pagination.pageSizeOptions = ['5', '10', '15', '20', '25', '30', '35', '45', '50', '60', '100']
  },
  result (result, pagination) {
    if (result.code === 0) {
      Object.assign(pagination, {
        current: result.data.page,
        pageSize: result.data.pageSize,
        total: result.data.total
      })
    }
  },
  encode (filter) {
    if (DataUtil.empty(filter)) return ''
    try {
      filter = JSON.stringify(filter)
      filter = Base64.encode(filter)
    } catch (e) {
      filter = ''
    }
    return filter
  },
  decode (filter) {
    if (DataUtil.empty(filter)) return {}
    try {
      filter = Base64.decode(filter)
      filter = JSON.parse(filter)
    } catch (e) {
      filter = {}
    }
    return filter
  },
  query2filter (obj, filter) {
    return Object.assign({}, filter, this.decode(obj.$route.query[this.filterKey]))
  },
  filter2query (obj, filter) {
    return obj.$router.push({
      path: obj.$route.fullPath,
      query: {
        [this.filterKey]: this.encode(filter)
      }
    }).catch(err => err)
  }
}

export default RouteUtil
