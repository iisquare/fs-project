import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/analysisNode/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/analysisNode/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/analysisNode/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/analysisNode/save', param, tips)
  },
  jdbc (param, tips = {}) {
    return base.post('/analysisNode/jdbc', param, tips)
  },
  tree (tips = {}) {
    return base.post('/analysisNode/tree', {}, tips)
  }
}
