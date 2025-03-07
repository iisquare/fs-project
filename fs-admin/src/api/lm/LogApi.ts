import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/log/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/log/delete', { ids }, tips)
  },
  audit (param: any, tips = {}) {
    return base.post('/log/audit', param, tips)
  },
}
