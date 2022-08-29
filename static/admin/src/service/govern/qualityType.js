import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/qualityType/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/qualityType/list', param, tips)
  },
  delete (param, tips = {}) {
    return base.post('/qualityType/delete', param, tips)
  },
  config (tips = {}) {
    return base.post('/qualityType/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/qualityType/save', param, tips)
  }
}
