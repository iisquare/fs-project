import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/ontology/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/ontology/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/ontology/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/ontology/save', param, tips)
  },
}
