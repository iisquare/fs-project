import base from './Api'

export default {
  list (data: any = {}, tips: any = {}) {
    return base.post('/qualityRule/list', data, tips)
  },
  infos (ids: any, tips: any = {}) {
    return base.post('/qualityRule/infos', { ids }, tips)
  },
  save (data: any, tips: any = {}) {
    return base.post('/qualityRule/save', data, tips)
  },
  delete (ids: any, tips: any = {}) {
    return base.post('/qualityRule/delete', { ids }, tips)
  },
  config (tips: any = {}) {
    return base.post('/qualityRule/config', {}, tips)
  },
}
