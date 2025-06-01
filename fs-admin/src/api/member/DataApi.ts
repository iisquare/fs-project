import base from './Api'

export default {
  info (id: any, tips = {}) {
    return base.get('/data/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/data/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/data/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/data/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/data/save', param, tips)
  }
}
