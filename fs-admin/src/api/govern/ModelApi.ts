import base from './Api'

export default {
  info (data: any, tips: any = {}) {
    return base.post('/model/info', data, tips)
  },
  list (data: any = {}, tips: any = {}) {
    return base.post('/model/list', data, tips)
  },
  save (data: any, tips: any = {}) {
    return base.post('/model/save', data, tips)
  },
  delete (data: any, tips: any = {}) {
    return base.post('/model/delete', data, tips)
  },
  config (tips: any = {}) {
    return base.post('/model/config', {}, tips)
  },
}
