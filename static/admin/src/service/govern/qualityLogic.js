import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/qualityLogic/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/qualityLogic/list', param, tips)
  },
  delete (param, tips = {}) {
    return base.post('/qualityLogic/delete', param, tips)
  },
  config (tips = {}) {
    return base.post('/qualityLogic/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/qualityLogic/save', param, tips)
  },
  tree (param, tips = {}) {
    return base.post('/qualityLogic/tree', param, tips)
  }
}
