import base from './Api'

export default {
  frame (param: any, tips = {}) {
    return base.post('/form/frame', param, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/form/list', param, tips)
  },
  delete (param: any, tips = {}) {
    return base.post('/form/delete', param, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/form/save', param, tips)
  }
}
