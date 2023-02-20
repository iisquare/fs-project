import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/qualityPlan/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/qualityPlan/list', param, tips)
  },
  delete (param, tips = {}) {
    return base.post('/qualityPlan/delete', param, tips)
  },
  config (tips = {}) {
    return base.post('/qualityPlan/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/qualityPlan/save', param, tips)
  },
  log (param, tips = {}) {
    return base.post('/qualityPlan/log', param, tips)
  },
  clear (ids, tips = {}) {
    return base.post('/qualityPlan/clear', { ids }, tips)
  }
}
