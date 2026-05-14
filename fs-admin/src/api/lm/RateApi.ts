import base from './Api'

export default {
  list(param: any, tips = {}) {
    return base.post('/rate/list', param, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/rate/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/rate/config', {}, tips)
  },
  save(param: any, tips = {}) {
    return base.post('/rate/save', param, tips)
  },
}
