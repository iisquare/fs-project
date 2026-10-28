import base from './Api'

export default {
  list (data: any = {}, tips: any = {}) {
    return base.post('/modelRelation/list', data, tips)
  },
  save (data: any, tips: any = {}) {
    return base.post('/modelRelation/save', data, tips)
  },
  delete (ids: any, tips: any = {}) {
    return base.post('/modelRelation/delete', { ids }, tips)
  },
  config (tips: any = {}) {
    return base.post('/modelRelation/config', {}, tips)
  },
}
