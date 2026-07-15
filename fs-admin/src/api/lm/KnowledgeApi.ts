import base from './Api'

export default {
  info (id: any, tips = {}) {
    return base.get('/knowledge/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/knowledge/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/knowledge/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/knowledge/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/knowledge/save', param, tips)
  },
  embedding (param: any, tips = {}) {
    return base.post('/knowledge/embedding', param, tips)
  },
  recall (param: any, tips = {}) {
    return base.post('/knowledge/recall', param, tips)
  },
}
