import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/analysis/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/analysis/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/analysis/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/analysis/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/analysis/save', param, tips)
  },
  results (param, tips = {}) {
    return base.post('/analysis/results', param, tips)
  },
  deleteResult (param, tips = {}) {
    return base.post('/analysis/deleteresult', param, tips)
  }
}
