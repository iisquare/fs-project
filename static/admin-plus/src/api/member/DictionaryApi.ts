import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/dictionary/list', param, tips)
  },
  tree (param: any, tips = {}) {
    return base.post('/dictionary/tree', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/dictionary/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/dictionary/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/dictionary/save', param, tips)
  },
  options (param: any, tips = {}) {
    return base.post('/dictionary/options', param, tips)
  }
}
