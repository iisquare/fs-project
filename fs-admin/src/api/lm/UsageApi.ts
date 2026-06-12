import base from './Api'

export default {
  list(param: any, tips = {}) {
    return base.post('/usage/list', param, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/usage/delete', { ids }, tips)
  },
  audit(param: any, tips = {}) {
    return base.post('/usage/audit', param, tips)
  },
  statistic(param: any, tips = {}) {
    return base.post('/usage/statistic', param, tips)
  },
}
