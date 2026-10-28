import base from './Api'

export default {
  tree (data: any = {}, tips: any = {}) {
    return base.post('/qualityLogic/tree', data, tips)
  },
  list (data: any = {}, tips: any = {}) {
    return base.post('/qualityLogic/list', data, tips)
  },
  save (data: any, tips: any = {}) {
    return base.post('/qualityLogic/save', data, tips)
  },
  delete (ids: any, tips: any = {}) {
    return base.post('/qualityLogic/delete', { ids }, tips)
  },
  config (tips: any = {}) {
    return base.post('/qualityLogic/config', {}, tips)
  },
}
