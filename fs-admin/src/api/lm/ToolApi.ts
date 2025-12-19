import base from './Api'

export default {
  all (param: any, tips = {}) {
    return base.post('/tool/all', param, tips)
  },
  delete (names: any, tips = {}) {
    return base.post('/tool/delete', { names }, tips)
  },
  config (tips = {}) {
    return base.post('/tool/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/tool/save', param, tips)
  },
}
