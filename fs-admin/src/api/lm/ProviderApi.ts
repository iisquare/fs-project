import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/provider/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/provider/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/provider/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/provider/save', param, tips)
  },
}
