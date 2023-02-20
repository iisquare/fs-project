import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/qualityRule/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/qualityRule/list', param, tips)
  },
  delete (param, tips = {}) {
    return base.post('/qualityRule/delete', param, tips)
  },
  config (tips = {}) {
    return base.post('/qualityRule/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/qualityRule/save', param, tips)
  }
}
