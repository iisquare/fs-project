import base from './Api'

export default {
  list (data: any = {}, tips: any = {}) {
    return base.post('/qualityPlan/list', data, tips)
  },
  save (data: any, tips: any = {}) {
    return base.post('/qualityPlan/save', data, tips)
  },
  delete (ids: any, tips: any = {}) {
    return base.post('/qualityPlan/delete', { ids }, tips)
  },
  config (tips: any = {}) {
    return base.post('/qualityPlan/config', {}, tips)
  },
  check (data: any, tips: any = {}) {
    return base.get('/qualityPlan/check', data, tips)
  },
  log (data: any = {}, tips: any = {}) {
    return base.post('/qualityPlan/log', data, tips)
  },
  clear (ids: any, tips: any = {}) {
    return base.post('/qualityPlan/clear', { ids }, tips)
  },
}
