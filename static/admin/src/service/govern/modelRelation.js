import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/modelRelation/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/modelRelation/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/modelRelation/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/modelRelation/save', param, tips)
  }
}
