import DataUtil from './data'
import { Base64 } from 'js-base64'

const RouteUtil = {
  filterKey: 'filter',
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
