import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/node/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/node/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/node/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/node/save', param, tips)
  },
  tree (param, tips = {}) {
    return base.post('/node/tree', param, tips)
  }
}
